
package GemIdentTools;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;

import javax.media.jai.JAI;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import no.uib.cipr.matrix.Matrices;
import GemIdentAnalysisConsole.ConsoleParser;
import GemIdentImageSets.DataImage;
import GemIdentOperations.Run;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentTools.Matrices.SimpleMatrix;
import GemIdentView.Console;
import GemIdentView.KFrame;
import GemIdentView.ScrollablePicture;

import com.sun.media.jai.codec.FileSeekableStream;


/**
 * This toolbox object is never instantiated. It
 * contains miscellaneous useful methods that are all
 * public statics.
 * <p>
 * A "project file" is a file residing in the project directory - the directory where the image
 * set is located (as well as the \<project\>.gem file
 * </p>
 * <p>
 * A "system file" is a file residing in the directory where the program itself is executed from
 * </p>
 * 
 * @author Adam Kapelner
 */
public class IOTools {	
	
	/** the maximum number of pixels allowed in a buffered image (true value is ~2.1Gpx but it's useful to limit it further to prvent overhead errors */
	public static final double MAX_NUM_PIXELS_BUFFERED_IMAGE=1.6*Math.pow(10,9); //1.6Gpx
	/** the maximum number of pixels in a displayed buffered image (this is to prevent the user from using all the RAM and creating image too large to display) */
	public static final double MAX_NUM_PIXELS_BUFFERED_IMAGE_TO_DISPLAY=100*Math.pow(10,6); //100Mpx
	
	/**
	 * Use the Java Advanced Imaging library to open a project image (see the {@link javax.media.jai.JAI#create(String, Object) JAI load function})
	 * 
	 * @param filename 		the filename (the path is the project directory) of the image of interest
	 * @return				the loaded image as a buffered image
	 */
	public static BufferedImage OpenImage(String filename) throws FileNotFoundException {
		FileSeekableStream stream = null;
		BufferedImage image = null;
    	String abs_path = Run.it.imageset.getFilenameWithHomePath(filename);
        try {
            stream = new FileSeekableStream(abs_path);
            // use the stream operator because the fileload operator does not allow to release the handle on the opened file
            image = JAI.create("stream", stream).getAsBufferedImage();
        }
        catch (FileNotFoundException e){
//        	System.err.println("could not find file: " + abs_path);
        	throw new FileNotFoundException();
        }        
        catch (IOException e){
        	e.printStackTrace();
        }
        finally {
			try {
				if (stream != null){ //ie if the file was opened at all
					stream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}            
        }
        return image;
	}
	
	/**
	 * Use the getResource to open a system image (see the {@link javax.media.jai.JAI#create(String, Object) JAI load function})
	 * 
	 * @param filename 		the filename (the path is the path of execution) of the image of interest
	 * @return				the loaded image as a buffered image
	 */
	public static Image OpenSystemImage(String filename){	
		return new ImageIcon((new Object()).getClass().getResource("/graphics/" + filename)).getImage();
	}
	/**
	 * Use the Java Advanced Imaging library to write a project image 
	 * (see the {@link javax.media.jai.JAI#create(String, java.awt.image.RenderedImage, Object, Object) JAI save function})
	 * to the directory of the current project
	 * 
	 * @param filename 		the filename not including the extension (the path is the path of the project) of the image to be saved
	 * @param format		the format desired (also the extension)
	 * @param image			the image to be saved
	 */
	public static void WriteImage(String filename,String format,BufferedImage image){
		JAI.create("filestore",image,Run.it.imageset.getFilenameWithHomePath(filename),format);
	}
	/**
	 * Use the Java Advanced Imaging library to write a project DataImage 
	 * (see the {@link javax.media.jai.JAI#create(String, java.awt.image.RenderedImage, Object, Object) JAI save function})
	 * to the desired path
	 * 
	 * @param filename 		the filename and path not including the extension of the image to be saved
	 * @param format		the format desired (also the extension)
	 * @param image			the image to be saved
	 */
	public static void WriteImage(String filename,String format,DataImage image){
		WriteImage(filename,format,image.getAsBufferedImage());		
	}
	
	/** Writes a grayscale image
	 *
	 * @param scores	The grayscale image to be written as a {@link Matrices.IntMatrix IntMatrix} object
	 * @param filename	The filename (without extension) to be written to
	*/
	public static void WriteScoreImage(String filename,SimpleMatrix scores){		
		BufferedImage image=new BufferedImage(scores.getWidth(),scores.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
		scores.FillRaster(image.getRaster());
		WriteImage(filename,"TIFF",image);		
	}
	/** Writes a binary image
	 *
	 * @param is		The binary image to be written as a {@link BoolMatrix BoolMatrix} object
	 * @param filename	The filename (without extension) to be written to
	*/
	public static void WriteIsMatrix(String filename,BoolMatrix is){
		WriteImage(filename,"BMP",is.getBinaryImage());
	}

	/** Writes an object (in our case, the training set object) to XML
	 * making use of the {@link java.io.Serializable serializable interface}.
	 *
	 * @param X			The object to be written
	 * @param filename	The filename (with extension) to be written to
	*/
	public static void saveToXML(Object X,String filename) {
		System.out.print("saving \""+filename+"\" . . . ");
		XMLEncoder xmlOut = null;
		try {
			xmlOut = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(new File(filename))));
		} catch (IOException e){e.printStackTrace();}	
		xmlOut.writeObject(X);
		xmlOut.close();
		System.out.print("done saving\n");
	}
	
	/** Opens an XML file and returns it as an object making use of the 
	 * {@link java.io.Serializable serializable interface}.
	 *
	 * @param filename	The filename (with extension) to be opened
	 * @return			The object contained within the XML code of the file
	*/
	public static Object openFromXML(String filename){
//		System.out.print("opening \""+filename+"\" . . . ");
		
		//first open file and update to latest gem version:
		UpdateFileToLatestGemVersion(filename);
		
		XMLDecoder xmlIn=null;
		try {
			xmlIn=new XMLDecoder(new BufferedInputStream(new FileInputStream(filename)));
		} catch (FileNotFoundException e) {e.printStackTrace();}
		Object O=xmlIn.readObject();
		xmlIn.close();
//		System.out.print("done\n");
		return O;		
	}
	
	private static HashMap<String, String> gem_file_format_changes_from_to;
	static {
		gem_file_format_changes_from_to = new HashMap<String, String>();
		gem_file_format_changes_from_to.put("GemIdentTools.IntMatrix", "GemIdentTools.Matrices.IntMatrix");
	}
	private static void UpdateFileToLatestGemVersion(String filename) {
		//open file
//		System.out.println("open old gem file");
		StringBuffer s = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			//step through every line and bust out when finished
			while (true){
				String line = in.readLine();				
				if (line == null){
					break;
				}
				s.append(line + "\n");
			}
			in.close();
		}
		catch (IOException e){}
		//get full gem file
		String full_gem_file = s.toString();
		String full_gem_file_updated = null;
//		System.out.println("gem file before:\n" + full_gem_file);
		//now go through each change and replace it:
		for (String from : gem_file_format_changes_from_to.keySet()){
			String to = gem_file_format_changes_from_to.get(from);
			full_gem_file_updated = full_gem_file.replaceAll(from, to);
		}
		//now write the file back to the hard drive only if they're different:
//		System.out.println("write new gem file back");
//		System.out.println("gem file after:\n" + full_gem_file_updated);
		if (!full_gem_file_updated.equals(full_gem_file)){
			System.out.println("yo");
			PrintWriter out = null;
			try {
				out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			} catch (IOException e) {}
			out.print(full_gem_file_updated);
			out.close();
		}
	}

	/**
	 * Checks the project folder for the existence of a file
	 * 
	 * @param filename	The filename (with extension) to be checked for existence
	 * @return			true if the file exists, false if not
	*/
	public static boolean FileExists(String filename) {
		return (new File(Run.it.imageset.getFilenameWithHomePath(filename))).exists();
	}
	/**
	 * Checks the system directory if a file exists
	 * 
	 * @param filename	The filename (with extension) to be checked for existence
	 * @return			true if the file exists, false if not
	*/
	public static boolean DoesSystemFileExist(String filename) {	
		return (new File(filename)).exists();
	}
	/**
	 * Checks whether or not a given directory exists within the project folder
	 * 
	 * @param dirname		the directory name
	 * @return 				true if the dir exists, false if not
	 */
	public static boolean DoesDirectoryExist(String dirname){
		return (new File(Run.it.imageset.getFilenameWithHomePath(dirname))).isDirectory();
	}
	/**
	 * Given a filename, get its name without its extension
	 * 
	 * @param filename		the filename
	 * @return				the filename without its extension
	 */
	public static String GetFilenameWithoutExtension(String filename){
		return filename.split("\\.")[0];
	}
	
	/**
	 * Moves a file to a certain directory
	 * 
	 * @param file	the file to be moved
	 * @param dir	the directory to move it to
	 */	
	public static void MoveFile(File file, File dir){
		file.renameTo(new File(dir, file.getName()));
	}
	
	/**
	 * Deletes a directory (took this from http://www.rgagnon.com/javadetails/java-0483.html)
	 */
    public static boolean deleteDirectory(File path) {
    	if( path.exists() ) {
    		File[] files = path.listFiles();
    		for(int i=0; i<files.length; i++) {
    			if (files[i].isDirectory()) {
    				deleteDirectory(files[i]);
    			}
    			else {
    				while(!files[i].delete()){
    					System.out.println("cannot delete file: " + files[i].getAbsolutePath() + " waiting one second...");
        				try {
        					Thread.sleep(1000); //wait for the images to unload
        				} catch (InterruptedException e) {}   					
    				}
    			}
    		}
    	}
    	return path.delete();
    }	

	/**
	 * Runs a project file in the native operating system
	 * 
	 * @param file		the file to be opened / executed outside of Java by the operating system
	 */
	public static void RunProgramInOS(String file){
        try {
        	String os=System.getProperty("os.name");
        	System.out.println("os:"+os);
        	if (os.split(" ")[0].equals("Windows")){
        		String home=Run.it.imageset.getHomedir().substring(0,3)+"\""+Run.it.imageset.getHomedir().substring(3,Run.it.imageset.getHomedir().length());
        		String exec="cmd /C\"start "+home+File.separator+file+"\"";
            	Runtime.getRuntime().exec(exec);
        	}
        	else
       		Runtime.getRuntime().exec(new File(Run.it.imageset.getFilenameWithHomePath(file)).getAbsolutePath());
        } catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Runs a system file in the native operating system. Total hack for Windows . . . untested
	 * on other OS's
	 * 
	 * @param file		the file to be opened / executed outside of Java by the operating system
	 */
	public static void RunSystemProgramInOS(String file){	
        try {
        	String homeDir=System.getProperty("user.dir");
        	String os=System.getProperty("os.name");
        	System.out.println("os:"+os);
        	if (os.split(" ")[0].equals("Windows")){
        		String home=homeDir.substring(0,3)+"\""+homeDir.substring(3,homeDir.length());
        		String exec="cmd /C\"start "+home+File.separator+file+"\"";
//            	System.out.println("command to windows:     "+exec);
            	Runtime.getRuntime().exec(exec);
        	}
        	else
//        	Runtime.getRuntime().exec(Subsets+NumClusters+".html",null,new File(Run.it.homeDir+File.separator));
        		Runtime.getRuntime().exec(new File(homeDir+File.separator+file).getAbsolutePath());
        } catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Joins a collection of strings into one string
	 * 
	 * @param all		the collection of substrings
	 * @param joinby	the token that joins the substrings
	 * @return			the final product: str1 + joinby + str2 + . . . + strN
	 */
	public static String StringJoin(Collection<String> all, String joinby){
		Object[] arr = all.toArray();
		String joined = "";
		for (int i = 0; i < arr.length; i++){
			joined += (String)arr[i];
			if (i < arr.length - 1)
				joined += joinby;
		}
		return joined;
	}
	
	/**
	 * Displays an image to the screen. Returns image object.
	 * 
	 * @param I		the image to display to the screen
	 */	
	public static ScrollablePicture GenerateScrollablePicElement(BufferedImage I, String title) {
		return GenerateScrollablePicElement(title, null, I, null, null, true);
	}	

	/**
	 * Displays an image to the screen. Handles the swing code
	 * 
	 * @param I		the image to display to the screen
	 */	
	public static void DisplayImageToScreen(BufferedImage I) {
		GenerateScrollablePicElement("image", null, I, null, null, true);
	}
	
	/**
	 * Generates an object that contains the image in a frame with scrolling capability
	 * 
	 * @param I		the image to display
	 * @return		the object that wraps the image and provides code for frames and scrolling
	 */
	public static ScrollablePicture GenerateScrollablePicElement(BufferedImage I) {
		return GenerateScrollablePicElement(null, null, I, null, null, true);
	}
	/**
	 * Responsible for creating a {@link GemIdentView.ScrollablePicture ScrollablePicture}
	 * object for a given result matrix. If there's not enough RAM it will display exactly how much
	 * RAM is needed for the operation.
	 * 
	 * @param name					the name of the result matrix
	 * @param time					the time when the "display" function was invoked by the user
	 * @param I						the result matrix as a buffered image		
	 * @param dir					the directory where the image is located (if saved somewhere, otherwise null)
	 * @param displayImageOnScreen  should we pop a JFrame and show it on the screen?
	 * @return						the {@link GemIdentView.ScrollablePicture ScrollablePicture} object embodying B
	 */
	public static ScrollablePicture GenerateScrollablePicElement(String name, Long time, BufferedImage I, String dir, ConsoleParser consoleParser, boolean displayImageOnScreen){
		ScrollablePicture pic=null;
		try {
			pic=new ScrollablePicture(new ImageIcon(I),10);
		} catch (Throwable t){
			long mem=(long)I.getWidth()*(long)I.getHeight();
			double gigs=mem/Math.pow(10,9);
			NumberFormat format=NumberFormat.getInstance();
			format.setMaximumFractionDigits(2);
			if (consoleParser != null){
				if (gigs > 1)
					consoleParser.WriteToScreen("out of memory - need "+format.format(gigs)+"G",null,Console.error);
				else {
					double megs=mem/Math.pow(10,6);
					consoleParser.WriteToScreen("out of memory - need "+format.format(megs)+"M",null,Console.error);
				}
			}
		}

		//display image on screen?
		if (displayImageOnScreen && name != null){
			JScrollPane view=new JScrollPane(pic);
			view.repaint();
			JFrame frame=new JFrame();
			frame.setSize(KFrame.frameSize);
			frame.add(view);
			frame.setTitle("image \""+name+"\"");
			frame.setVisible(true);
			frame.setResizable(true);
			frame.repaint();
			frame.requestFocus();
		}
		//did you supply a console parser and a timestamp to display a confirmation?
		if (consoleParser != null && time != null){
			consoleParser.WriteToScreen("Display completed in "+Run.TimeElapsed(time),null,Console.time);
		}
		//did you supply a directory where the image was saved to?
		if (dir != null && consoleParser != null)
			consoleParser.WriteToScreen("Image located at: "+dir,null,Console.neutral);
		
		return pic;
	}


	//default parameters for the initialize image method:
	private static final Color DefaultBackgroundColorOnInitializedImage = Color.WHITE;
	private static final int DefaultImageType = BufferedImage.TYPE_INT_RGB;
	
	/**
	 * Convenience method to initialize a blank image
	 * 
	 * @param w				the width of the image to be initialized
	 * @param h				the height of the image to be initialized
	 * @param type			the type of the image to be initialized
	 * @param color			the background color of the image
	 * @return				the initialized image
	 */
	public static BufferedImage InitializeImage(int w, int h, Integer type, Color color) {
		if (color == null){
			color = DefaultBackgroundColorOnInitializedImage;
		}
		if (type == null){
			type = DefaultImageType;
		}
		BufferedImage image = new BufferedImage(w, h, type);
		for (int i = 0; i < image.getWidth(); i++){
			for (int j = 0; j < image.getHeight(); j++){
				image.setRGB(i, j, color.getRGB());
			}
		}
		return image;
	}	

}