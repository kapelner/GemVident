/*
 * @(#)FrameAccess.java	1.5 01/03/13
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 * 
 * Modified by Adam Kapelner, 2 / 2009
 */
package GemVident;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.UnsupportedPlugInException;
import javax.media.control.TrackControl;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.jai.JAI;
import javax.media.util.BufferToImage;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import GemIdentImageSets.ColorVideoSet;
import GemIdentImageSets.DataImage;


/**
 * Sample program to access individual video frames by using a 
 * "pass-thru" codec.  The codec is inserted into the data flow
 * path.  As data pass through this codec, a callback is invoked
 * for each frame of video data.
 */
public class FrameAccess implements ControllerListener {
	private static final int EveryTFrames = 1;

	private Processor p;
	private Object waitSync = new Object();
    boolean stateTransitionOK = true;
    private ColorVideoSet imageset;
	private MediaLocator ml;
	private String frame_text;
	private boolean finished = false;
	private double update;
	private JProgressBar progress;
	private JFrame dialog;

	private int total_num_frames;

	private ExecutorService image_builders;

	private boolean first_frame_approached;
	private int[] crop_coordinates;
	
	
    private static final int num_image_builder_threads = 5;
    public FrameAccess(JFrame frame_to_center_about, ColorVideoSet imageset, String movie, String frame_text, final Runnable on_finish){
//    	System.out.println("project name: " + frame_text);
    	ml = new MediaLocator("file:" + imageset.getHomedir() + File.separator + movie);
    	this.imageset = imageset;
    	this.frame_text = frame_text;    	
    	spawnProgressBarAndDependents(frame_to_center_about);
    	//create thread pool
    	image_builders = Executors.newFixedThreadPool(num_image_builder_threads);
    	processFrames();

		//wait for it to finish
    	new Thread(){
    		public void run(){
    			while (!finished) {
    				try {
    					waitSync.wait();
    				} catch (Exception e) {}
    			}    			
    			on_finish.run();
    		}
    	}.start();
		
    }

    private static final String DialogTitle = "Converting the Movie's Frames into Images (please wait)";
	private static final int dialog_width = 500;
	private static final int dialog_height = 50;    
    private void spawnProgressBarAndDependents(JFrame frame) {
		dialog = new JFrame();
		
		//generate all the stuff for the progress bar frame
		update = 0; //reset the progress bar value
		progress = new JProgressBar();
		progress.setStringPainted(true);
		progress.setSize(new Dimension(dialog_width, dialog_height));
		Point origin = frame.getLocation();
		origin.translate(frame.getSize().width / 2 - dialog_width / 2, frame.getSize().height / 2 - dialog_height / 2);
		dialog.setLocation(origin);
		dialog.setLayout(new BorderLayout());
		dialog.setTitle(DialogTitle);		
		dialog.add(progress, BorderLayout.CENTER);
		dialog.pack();
		dialog.setResizable(false);
		dialog.setVisible(true);
		dialog.setSize(new Dimension(dialog_width, dialog_height));
		dialog.repaint();
	}
    
	private void UpdateProgressBar() {
		progress.setValue((int)Math.round(update));		
	}    


	/**
     * Given a media locator, create a processor and use that processor
     * as a player to playback the media.
     *
     * During the processor's Configured state, two "pass-thru" codecs,
     * PreAccessCodec and PostAccessCodec, are set on the video track.  
     * These codecs are used to get access to individual video frames 
     * of the media.
     *
     * Much of the code is just standard code to present media in JMF.
     */
    public void processFrames() {

		try {
		    p = Manager.createProcessor(ml);
		} catch (Exception e) {
		    System.err.println("Failed to create a processor from the given url: " + e);
		    return;
		}
		try {
			System.out.println("opened: " + ml.getURL());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
	
		p.addControllerListener(this);
	
		// Put the Processor into configured state.
		p.configure();
		if (!waitForState(Processor.Configured)) {
		    System.err.println("Failed to configure the processor.");
		    return;
		}
	
		// So I can use it as a player.
		p.setContentDescriptor(null);
	
		// Obtain the track controls.
		TrackControl tc[] = p.getTrackControls();
		for (TrackControl t : tc) {
		    t.setEnabled(t.getFormat() instanceof VideoFormat);
		}	
	
		if (tc == null) {
		    System.err.println("Failed to obtain track controls from the processor.");
		    return;
		}
	
		// Search for the track control for the video track.
		TrackControl videoTrack = null;
	
		for (int i = 0; i < tc.length; i++) {
		    if (tc[i].getFormat() instanceof VideoFormat) {
			videoTrack = tc[i];
			break;
		    }
		}
	
		if (videoTrack == null) {
		    System.err.println("The input media does not contain a video track.");
		    return;
		}
	
		total_num_frames = (int)Math.ceil(GetTotalNumFrames(videoTrack.getFormat(), p));
		System.out.println("Video format: " + videoTrack.getFormat() + " frames=" + total_num_frames);
		
		ArrayList<String> frame_number_to_filename = new ArrayList<String>(total_num_frames);
		for (int i = 0; i < total_num_frames; i++){
			frame_number_to_filename.add(null);
		}
		System.out.println("fnf size: "+frame_number_to_filename.size());
		imageset.setFrame_number_to_filename(frame_number_to_filename);
	
		// Instantiate and set the frame access codec to the data flow path.
		try {
		    Codec codec[] = {new PostAccessCodec()};
		    videoTrack.setCodecChain(codec);
		} catch (UnsupportedPlugInException e) {
		    System.err.println("The process does not support effects.");
		}
	
		// Realize the processor.
		p.prefetch();
		if (!waitForState(Processor.Prefetched)) {
		    System.err.println("Failed to realize the processor.");
		    return;
		}
	
		// Start the processor.
		p.start();
    }

    private double GetTotalNumFrames(Format format, Processor p) {
    	imageset.setFrame_rate(29.97); //use regex when I can
    	imageset.setDuration(p.getDuration().getSeconds());
    	return imageset.getFrame_rate() * imageset.getDuration();
	}
	/**
     * Block until the processor has transitioned to the given state.
     * Return false if the transition failed.
     */
    boolean waitForState(int state) {
	synchronized (waitSync) {
	    try {
		while (p.getState() != state && stateTransitionOK)
		    waitSync.wait();
	    } catch (Exception e) {}
	}
	return stateTransitionOK;
    }


    /**
     * Controller Listener.
     */
    public void controllerUpdate(ControllerEvent evt) {

	if (evt instanceof ConfigureCompleteEvent ||
	    evt instanceof RealizeCompleteEvent ||
	    evt instanceof PrefetchCompleteEvent) {
	    synchronized (waitSync) {
		stateTransitionOK = true;
		waitSync.notifyAll();
	    }
	} else if (evt instanceof ResourceUnavailableEvent) {
	    synchronized (waitSync) {
		stateTransitionOK = false;
		waitSync.notifyAll();
	    }
	} else if (evt instanceof EndOfMediaEvent) {		
	    p.close();
	    p.deallocate();
	    //close widow
	    dialog.dispose();
	    image_builders.shutdown();
		try {	         
			image_builders.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
	    //now return back to GemIdent...
	    finished = true;
	}
    }

    /*********************************************************
     * Inner class.
     *
     * A pass-through codec to access to individual frames.
     *********************************************************/

    public class PreAccessCodec implements Codec {

	/**
         * Callback to access individual video frames.
         */
	void accessFrame(final Buffer frame) {
	}	


	/**
 	 * The code for a pass through codec.
	 */

	// We'll advertize as supporting all video formats.
	protected Format supportedIns[] = new Format [] {
	    new VideoFormat(null)
	};

	// We'll advertize as supporting all video formats.
	protected Format supportedOuts[] = new Format [] {
	    new VideoFormat(null)
	};

	Format input = null, output = null;

	public String getName() {
	    return "Pre-Access Codec";
	}

	// No op.
    public void open() {
	}

	// No op.
	public void close() {
	}

	// No op.
	public void reset() {
	}

	public Format [] getSupportedInputFormats() {
	    return supportedIns;
	}

	public Format [] getSupportedOutputFormats(Format in) {
	    if (in == null)
		return supportedOuts;
	    else {
		// If an input format is given, we use that input format
		// as the output since we are not modifying the bit stream
		// at all.
		Format outs[] = new Format[1];
		outs[0] = in;
		return outs;
	    }
	}

	public Format setInputFormat(Format format) {
	    input = format;
	    return input;
	}

	public Format setOutputFormat(Format format) {
	    output = format;
	    return output;
	}

	public int process(Buffer in, Buffer out) {

	    // This is the "Callback" to access individual frames.
	    accessFrame(in);

	    // Swap the data between the input & output.
	    Object data = in.getData();
	    in.setData(out.getData());
	    out.setData(data);

	    // Copy the input attributes to the output
	    out.setFormat(in.getFormat());
	    out.setLength(in.getLength());
	    out.setOffset(in.getOffset());

	    return BUFFER_PROCESSED_OK;
	}

	public Object[] getControls() {
	    return new Object[0];
	}

	public Object getControl(String type) {
	    return null;
	}
    }
	public static final int FirstFrameToCapture = 6;
	public static final int LastFrameFromEndToCapture = 8;
    public class PostAccessCodec extends PreAccessCodec {
    	// We'll advertise as supporting all video formats.
    	public PostAccessCodec() {
    	    supportedIns = new Format [] {
    		new RGBFormat()
    	    };
    	}
    	
    	void accessFrame(final Buffer frame) {
    		
    		final long n = frame.getSequenceNumber() + 1;
    		
    		if (n % EveryTFrames != 0){
    			return;
    		}
    		//first few frames blank, and last few frames blank
    		if (n > total_num_frames - LastFrameFromEndToCapture || n < FirstFrameToCapture){
    			return;
    		}

    	    long t = (long)(frame.getTimeStamp()/10000000f);

    	    System.out.println("processing frame #" + (frame.getSequenceNumber() + 1) + ", time: " + ((float)t)/100f);
    	    
    		
    		//now ask if they want to crop on the first frame
    		if (!first_frame_approached){
    			first_frame_approached = true;
				int result = JOptionPane.showConfirmDialog(null,
						"Would you like to crop the movie's frames?",
						"Warning",
						JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION){
					crop_coordinates = new CropMovieFrames(convertFrameToImage(frame)).getCoordinates();
				}
    		}
    		
    	    image_builders.execute(new Runnable(){
    	    	public void run(){    	    	    
    	    	    update += 1 / (double)total_num_frames * 100;
    	    	    UpdateProgressBar();
    	    	    saveFrameImage(convertFrameToImage(frame), n);
    	    	}
    	    });
    	}
    	
    	private BufferedImage convertFrameToImage(Buffer frame){
    	    /* Determine the format of the frame */
    	    VideoFormat vf = (VideoFormat) frame.getFormat();
    	 
    	    /* Initialize our BufferToImage */
    	    BufferToImage bufferToImage = new BufferToImage(vf);
    	 
    	    /* Create the image */
    	    int w = vf.getSize().width;
    	    int h = vf.getSize().height;
    		BufferedImage I = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
    	
    		// convert the image to a BufferedImage
    		Graphics g = I.getGraphics();
    		g.drawImage(bufferToImage.createImage(frame), 0, 0, w, h, null);
    		g.dispose();

    		return I;
    	}
    	
    	private void saveFrameImage(BufferedImage I, long n){    		
    		String image_filename = frame_text + "_" + frameNumberDisplay(n) + ".jpg";
    		imageset.getFrame_number_to_filename().set((int)n, image_filename);    		
    		//handle cropping if need be
    		if (crop_coordinates != null){
    			I = DataImage.Crop(I, crop_coordinates);
    		}
    		JAI.create("filestore", I, imageset.getHomedir() + File.separator + image_filename, "JPEG");    		
    	}

		private String frameNumberDisplay(long f){
    		if (f < 10){
    			return "0000000" + f;
    		}
    		else if (f < 100){
    			return "000000" + f;
    		}
    		else if (f < 1000){
    			return "00000" + f;
    		}	
    		else if (f < 10000){
    			return "0000" + f;
    		}	
    		else if (f < 100000){
    			return "000" + f;
    		}
    		else if (f < 1000000){
    			return "00" + f;
    		}
    		else if (f < 10000000){
    			return "0" + f;
    		}
    		else {
    			return "" + f;
    		}
    	}

    	public String getName() {
    	    return "Post-Access Codec";
    	}
    }
}
