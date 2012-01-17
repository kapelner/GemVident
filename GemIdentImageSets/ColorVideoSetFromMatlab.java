package GemIdentImageSets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class ColorVideoSetFromMatlab extends ColorVideoSet implements Serializable{
	private static final long serialVersionUID = -5092687908170965078L;
	private String[] frame_images;
	
	public ColorVideoSetFromMatlab(){}
	
	public ColorVideoSetFromMatlab(String homeDir) throws FileNotFoundException{
		super(homeDir);
		frame_images = (new File(homedir + File.separator)).list(new MatlabFileFilter());
		System.out.println("frame length: "+frame_images.length);
		if (frame_images.length == 0)
			throw new FileNotFoundException();
		Arrays.sort(frame_images);
//		System.out.println(Arrays.toString(frame_images));
		createFrameNumbersToFilenames();
//		System.out.println(Arrays.toString(frame_number_to_filename.toArray()));
	}
	
	public int filenameToFrameNum(String filename){
		String[] splits = filename.replace(".jpg", "").split("_");
		return Integer.parseInt(splits[splits.length - 1])-first_true_frame;		
	}
	private void createFrameNumbersToFilenames() {
		frame_number_to_filename = new ArrayList<String>(frame_images.length);
		int max = Integer.MIN_VALUE;
		int min = Integer.MAX_VALUE;
		for (String filename : frame_images){
			int n = parseFrameNumFromFilename(filename);
			if (n > max){
				max = n;
			}
			if (n < min)
				min = n;
		}
		first_true_frame = min;
		for (int i = 0; i <= max-min; i++){ //fill him up inclusive
			frame_number_to_filename.add(null);
		}
		for (String filename : frame_images){
			frame_number_to_filename.set(parseFrameNumFromFilename(filename)-min, filename);
//			System.out.println(parseFrameNumFromFilename(filename)+"\t"+ filename);
		}
	}

	/**
	 * This {@link java.io.FilenameFilter file filter} returns only jpg files
	 * 
	 */
	private class MatlabFileFilter implements FilenameFilter {
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
			if (fileparts.length >= 2 && fileparts[0].split("_").length == 2) {
				String ext = fileparts[fileparts.length - 1].toLowerCase();
				// System.out.println("ext:"+ext);
				if (ext.equals("jpg"))
					return true;
				else
					return false;
			} else
				return false;
		}
	}
	
	public boolean doesInitializationFileExist(){
		if (frame_images.length > 0){
			return true;
		}
		return false;		
	}

}
