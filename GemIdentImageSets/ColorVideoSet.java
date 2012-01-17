
package GemIdentImageSets;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import GemIdentClassificationEngine.DatumSetup;
import GemIdentClassificationEngine.FilmFrameBeforeAndAfter.FilmFrameBeforeAndAfterDatumSetup;
import GemIdentOperations.Run;
import GemIdentView.KThumbnail;
import GemIdentView.KTrainPanel;
import GemIdentView.WrapLayout;
import GemVident.FrameAccess;

public class ColorVideoSet extends NonGlobalImageSet implements Serializable {
	private static final long serialVersionUID = 4044824398383860977L;
	
	private transient String[] video_list;
	protected ArrayList<String> frame_number_to_filename;
	protected Integer first_true_frame = null;
	private double frame_rate;
	private double duration;
	
	
	public ColorVideoSet(){}
	
	public ColorVideoSet(String homeDir) throws FileNotFoundException{
		super(homeDir);
		video_list = GetVideoList(homeDir);
	}
	
	/**
	 * This {@link java.io.FilenameFilter file filter} returns only movie files
	 * of type "mov"
	 * 
	 */
	private class VideoFileFilter implements FilenameFilter {
		/**
		 * Given a file, returns true if it is an image
		 * 
		 * @param dir
		 *            the directory the file is located in
		 * @param name
		 *            the file itself
		 * @return whether or not the file is an image
		 */
		public boolean accept(File dir, String name) {
			// System.out.println("name:"+name);
			String[] fileparts = name.split("\\.");
			if (fileparts.length >= 2) {
				String ext = fileparts[fileparts.length - 1].toLowerCase();
				// System.out.println("ext:"+ext);
				if (ext.equals("mov"))
					return true;
				else
					return false;
			} else
				return false;
		}
	}

	private String[] GetVideoList(String dir) {
		return (new File(dir + File.separator)).list(new VideoFileFilter());
	}	
	
	public boolean doesInitializationFileExist(){ //ie does the video exist?
		if (video_list.length == 1){
			return true;
		}
		return false;
	}

	public void convertVideoToImages(String projectName, ColorVideoSet imageset, JFrame frame_to_center_about, Runnable on_finish) {
		new FrameAccess(frame_to_center_about, imageset, video_list[0], projectName, on_finish);
	}

	public void ThumbnailsCompleted(){
//		System.out.println("inside ThumbnailsCompleted");
		for (int i = 0; i < 100; i++){
			Run.it.getGUI().EnableTrainingHelpers();
		}
	}

	private static final Dimension film_reel_size = new Dimension(720, 540);
	private static final Dimension film_reel_pane_size = new Dimension(700, 430);
	private static final String film_reel_title = "Choose which movie frames you would like to add to the training set";
	private static final int max_num_images_to_pick_for_training = 10;
	private static final int num_frames_in_training_reel = 400;
	public void CreateFilmReelTrainer(final KTrainPanel trainPanel) {
		final JFrame film_reel_frame = new JFrame();
		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(film_reel_pane_size);
		JScrollBar vbar = scrollPane.getVerticalScrollBar(); //set up vbar
		vbar.setUnitIncrement(75);
		vbar.setPreferredSize(new Dimension(7, vbar.getHeight()));	
		JPanel film_reel_pane = new JPanel();
		film_reel_pane.setLayout(new WrapLayout(WrapLayout.LEFT));
		scrollPane.setViewportView(film_reel_pane);
		CreateFilmReelButtons(film_reel_pane, trainPanel);	
		film_reel_frame.setPreferredSize(film_reel_size);
		film_reel_frame.setTitle(film_reel_title);
		film_reel_frame.setResizable(false);
		film_reel_frame.setLayout(new WrapLayout(WrapLayout.CENTER));
		JLabel title = new JLabel("                           Frame selector" + (frame_number_to_filename.size() > num_frames_in_training_reel ? " (" + num_frames_in_training_reel + " frames are shown)" : "") + "                     ");
		title.setFont(new Font("SansSerif", Font.BOLD, 20));
		title.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		film_reel_frame.add(title);
		film_reel_frame.add(new JLabel("automatically choose n spaced out frames for the training set: "));
		final JSpinner pick_n = new JSpinner(new SpinnerNumberModel(5, 1, max_num_images_to_pick_for_training, 1));
		film_reel_frame.add(pick_n);
		JButton pick = new JButton("choose");
		pick.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					pickNEvenlySpacedOutFrames((Integer)pick_n.getValue(), trainPanel);
					film_reel_frame.dispose();
					CreateFilmReelTrainer(trainPanel);
				}
			}
		);
		film_reel_frame.add(pick);
		JLabel disclaimer = new JLabel("(those autochosen may not come from below)");
		disclaimer.setFont(new Font("Serif", Font.PLAIN, 12));
		film_reel_frame.add(disclaimer);
		film_reel_frame.add(scrollPane);
		film_reel_frame.add(new JLabel("(click on a frame to add it to the training set)"));
		film_reel_frame.pack();
		film_reel_frame.setVisible(true);		
	}
	
	private void CreateFilmReelButtons(JPanel film_reel_pane, KTrainPanel trainPanel) {
		if (frame_number_to_filename.size() > num_frames_in_training_reel){
//			System.out.println("frame_number_to_filename.size() > num_frames_in_training_reel");
			double interval = frame_number_to_filename.size() / ((double)num_frames_in_training_reel);
			for (double f = 0; f < frame_number_to_filename.size(); f += interval){
				int frame_no = (int)Math.floor(f);
//				System.out.println("f: " + f + " frame_no: " + frame_no);
				String filename = frame_number_to_filename.get(frame_no);
				if (filename != null){
					GenerateFilmReelButtonWithFrameNo(film_reel_pane, filename, frame_no, trainPanel);
				}
			}			
		}
		else {
//			System.out.println("frame_number_to_filename.size() < num_frames_in_training_reel");
			for (int f = 0; f < frame_number_to_filename.size(); f++){
				String filename = frame_number_to_filename.get(f);
				if (filename != null){
					GenerateFilmReelButtonWithFrameNo(film_reel_pane, filename, f, trainPanel);
				}
			}			
		}
	}

	private void pickNEvenlySpacedOutFrames(int n, KTrainPanel trainPanel){
		int interval = (frame_number_to_filename.size() - FrameAccess.FirstFrameToCapture - FrameAccess.LastFrameFromEndToCapture) / (n + 1);
		for (int f = FrameAccess.FirstFrameToCapture + interval; f < frame_number_to_filename.size() - FrameAccess.FirstFrameToCapture - FrameAccess.LastFrameFromEndToCapture; f += interval){
			trainPanel.AddNewThumbnail(frame_number_to_filename.get(f));
		}
	}

	private static final Dimension thumbnail_dimension = new Dimension(KThumbnail.THUMB_WIDTH, KThumbnail.THUMB_HEIGHT);
	private void GenerateFilmReelButtonWithFrameNo(JPanel film_reel_pane, final String filename, final int f, final KTrainPanel trainPanel) {
		final JButton button = new JButton();
		button.setPreferredSize(thumbnail_dimension);
		BufferedImage icon = ImageAndScoresBank.getOrAddThumbnail(filename);
		final Graphics g = icon.getGraphics();
		g.setColor(NuanceImageListInterface.InTrainingSetBackground);
		g.setFont(new Font("SansSerif", Font.BOLD, 20));
		String to_draw = "" + f;
		if (trainPanel.isInTrainingSet(filename)) {
			to_draw += "**";
		}		
		
		g.drawString(to_draw, thumbnail_dimension.width + 10, thumbnail_dimension.height);
		button.setIcon(new ImageIcon(icon));
		//now add a listener that will respond when user clicks:
		button.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					trainPanel.AddNewThumbnail(filename);
					g.drawString("" + f + "**", thumbnail_dimension.width + 10, thumbnail_dimension.height);
				}
			}
		);
		film_reel_pane.add(button);
	}

	public ArrayList<String> getFrame_number_to_filename() {
		return frame_number_to_filename;
	}

	public void setFrame_number_to_filename(ArrayList<String> frame_number_to_filename) {
		this.frame_number_to_filename = frame_number_to_filename;
	}
	
	public String filenameToFrameString(String filename){
		return "Frame " + filenameToFrameNum(filename);
	}
	public int filenameToFrameNum(String filename){
		String[] splits = filename.replace(".jpg", "").split("_");
		return Integer.parseInt(splits[splits.length - 1]);		
	}

	public double getFrame_rate() {
		return frame_rate;
	}

	public void setFrame_rate(double frame_rate) {
		this.frame_rate = frame_rate;
	}

	public double getDuration() {
		return duration;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}
	
	public double convertFrameNumToSeconds(int frame){
		this.frame_rate = 29.97; //delete this eventually
		return frame / frame_rate;
	}
	
	public String getFrameNum(int n){
		if (0 <= n && n< frame_number_to_filename.size())
			return frame_number_to_filename.get(n);
		else 
			return null;
	}
	
	protected int parseFrameNumFromFilename(String filename) {
		return Integer.parseInt(filename.split("\\.")[0].split("_")[1]);
	}

	/**
	 * If this frame does not exist, we'll reflect into the beginning or the end of the movie.
	 * 
	 * For example:
	 * 
	 * frame_1: null
	 * frame_2: null
	 * frame_3: null
	 * frame_4: first true frame
	 * frame_5: second true frame
	 * frame_6: third true frame
	 * frame_7: fourth true frame
	 * frame_8: null
	 * frame_9: null
	 * frame_10: null
	 * 
	 * Then I ask for frame_3, I should get frame_5. If I ask for frame_1, I should get frame_7.
	 * If I ask for frame_8, I should get frame_6. If I ask for frame_10, I should get frame_4
	 * 
	 * @param n		which frame do I want?
	 * @return		the frame itself, if it doesn't exist, then the reflected frame
	 */
	private String reflectFrameNumIfNull(int n){
		String filename = getFrameNum(n);
		if (filename == null){
			//is it close to zero or close to the end
//			System.out.println(frame_number_to_filename.size()+"\t"+n);
//			System.out.println(Arrays.toString(frame_number_to_filename.toArray()));
			if (first_true_frame == null){
				int i = 0;
				while (frame_number_to_filename.get(i) == null)
					i++;
				first_true_frame = parseFrameNumFromFilename(frame_number_to_filename.get(i));
			}
//			System.out.println("first true frame: " + first_true_frame);
			if (n < frame_number_to_filename.size() - n || n < first_true_frame){ //close to the beginning
				int t = 0;
				while (true){
					t++;
					if (getFrameNum(n + t) != null){
						break;
					}
				}
				//we need t frames forward from the true beginning of the movie, so we need
				//to be n + 2 * t frames into the movie
				return getFrameNum(n + 2 * t);
			}
			else { //close to the end
				int t = 0;
				while (true){
					t++;
					if (getFrameNum(n - t) != null){
						break;
					}
				}
				//we need t frames back from the true end of the movie, so we need
				//to be n + 2 * t frames into the movie
				return getFrameNum(n - 2 * t);				
			}
		}
		//if it was a regular frame, just return it
		return filename;
	}
	
//	public ArrayList<String> getFrameFilenames(int start, int end){
//		ArrayList<String> frame_filenames = new ArrayList<String>(end - start + 1);
//		for (int i = start; i <= end; i++){
//			frame_filenames.add(reflectFrameNumIfNull(i));
//		}
//		return frame_filenames;
//	}
//	
//	public ArrayList<RegularSubImage> getFrames(int start, int end){
//		ArrayList<RegularSubImage> images = new ArrayList<RegularSubImage>(end - start + 1);
//		for (int i = start; i <= end; i++){
//			images.add(new RegularSubImage(reflectFrameNumIfNull(i)));
//		}
//		return images;		
//	}
	
	public LinkedHashMap<Integer, String> getFramesInRange(int f, int plus_minus){
		LinkedHashMap<Integer, String> images = new LinkedHashMap<Integer, String>(plus_minus * 2 + 1);
		for (int i = -plus_minus; i <= plus_minus; i++){
			images.put(i, reflectFrameNumIfNull(f + i));
//			System.out.println("i: " + i + " frames in ranges: " + images.get(i));
		}
		return images;		
	}
	
	public LinkedHashMap<Integer, String> getFramesInRange(int f, ArrayList<Integer> frames){
		LinkedHashMap<Integer, String> images = new LinkedHashMap<Integer, String>(frames.size() + 1);
		for (int i : frames){
//			System.out.print("i: " + i);
			images.put(i, reflectFrameNumIfNull(f + i));
//			System.out.println(" reflectFrameNumIfNull: " + images.get(i));
		}
		return images;		
	}	
	
	public DatumSetup setUpDataExtractionMethod(){
		int M = Run.it.getMaxPhenotypeRadiusPlusMore(null);
		System.out.println("Maximum radius: "+M);
		return new FilmFrameBeforeAndAfterDatumSetup(this, getFilterNames(), M);
	}	
}
