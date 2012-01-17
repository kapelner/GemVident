package GemIdentImageSets;

import java.util.HashMap;

import GemIdentModel.Stain;
import GemIdentOperations.Run;
import GemIdentTools.Matrices.BoolMatrix;

public class BackgroundHandler {
	
	/** the original image */
	private DataImage original_image;
	/** the mapping from stain name -> binary matrix that is the global background image */
	private HashMap<String, BoolMatrix> booleanBackgrounds;

	/**
	 * Initializes this background handler to handle a certain image.
	 * Creates the boolmatrix for each individual stain
	 * 
	 * @param original_image		the image to compute backgrounds for
	 */
	public BackgroundHandler(DataImage original_image) {
		this.original_image = original_image;
		if (original_image != null){
			booleanBackgrounds = new HashMap<String, BoolMatrix>();
			for (Stain stain : Run.it.getUserColorsImageset().getBackgroundColorObjects()){
				recreateBooleanBackgroundForIndividualStain(stain);
			}
		}		
	}
	
	/**
	 * Ors all the background matrices from the different stains together to form
	 * the master, official background matrix for the image
	 * 
	 * @return		the master background matrix
	 */
	public BoolMatrix createMasterBackgroundMatrix(){
		BoolMatrix master_background = new BoolMatrix(original_image.getWidth(), original_image.getHeight());
		for (BoolMatrix background : booleanBackgrounds.values()){
			for (int i = 0; i < original_image.getWidth(); i++){
				for (int j = 0; j < original_image.getHeight(); j++){
					if (background.get(i, j)){
						master_background.set(i, j, true);
					}
				}
			}
		}
		return master_background;
	}
	
	/**
	 * The user just deleted one of the stains, so remove it from 
	 * the internals in this class
	 * 
	 * @param name		the name of the stain to remove
	 */
	public void removeBackgroundStain(String name){
		booleanBackgrounds.remove(name);
	}

	/**
	 * recreate an individual background based on a stain's threshold setting. This is only to be used
	 * when the individual actually moves the slider.
	 * 
	 * @param stain		the stain in which the threshold was altered
	 */
	public void recreateBooleanBackgroundForIndividualStain(Stain stain) {
		//iterate through all the pixels and ask if it's below this maximum point
		short[][][] cube = stain.GetMahalCube();
		if (cube == null){ //don't even bother to show a dialog, let the user wait a few seconds
			try {
				stain.getStainOpener(null, 100).run();
			} catch (NullPointerException e){
				//we know this is from not being able to find a new cube, so ditch
				return;
			}
			cube = stain.GetMahalCube(); //now it's there, so get it
		}
		BoolMatrix background = getBooleanBackgroundOrCreate(stain.getName());
		for (int i = 0; i < original_image.getWidth(); i++){
			for (int j = 0; j < original_image.getHeight(); j++){
				int pixel_val = cube[original_image.getR(i, j)][original_image.getG(i, j)][original_image.getB(i, j)] + Short.MAX_VALUE;
				if (pixel_val < stain.getBackground_mahalanobis_distance()){ //mark as opaque
					background.set(i, j, true);
				}
				else {
					background.set(i, j, false);
				}
			}
		}
	}
	
	/**
	 * Creates the matrix in the hashmap if it doesn't exist, or just returns it
	 * 
	 * @param name	the name of the stain
	 * @return		the boolmatrix indicating the pixels that belong to the background
	 */
	private BoolMatrix getBooleanBackgroundOrCreate(String name) {
		BoolMatrix background = booleanBackgrounds.get(name);
		if (background == null){
			background = new BoolMatrix(original_image.getWidth(), original_image.getHeight());
			booleanBackgrounds.put(name, background);
		}
		return background;
	}	

}
