
package GemIdentClassificationEngine.FilmFrameBeforeAndAfter;

import java.awt.Color;
import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

import GemIdentClassificationEngine.Datum;
import GemIdentClassificationEngine.DatumSetupThatUsesRings;
import GemIdentClassificationEngine.StandardPlaneColor.StandardPlaneColorDatumSetup;
import GemIdentImageSets.ColorVideoSet;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.SuperImage;

public final class FilmFrameBeforeAndAfterDatumSetup extends DatumSetupThatUsesRings {

	public static final ArrayList<Integer> FrameOffsets = new ArrayList<Integer>();
	static {
		setMultipleFrames();
	}
	
	public static void setMultipleFrames(){
		FrameOffsets.clear();
		FrameOffsets.add(-2); //2 frames behind
		FrameOffsets.add(0);  //the present frame
		FrameOffsets.add(4);  //4 frames ahead		
	}

	public static void setSingleFrame(){
		FrameOffsets.clear();
		FrameOffsets.add(0);  //the present frame
	}
	
	/** the video image set */
	private ColorVideoSet videoSet;

	/**
	 * Using the maximum radius and the number of relevant colors,
	 * calculate the number of features all {@link StandardPlaneColorDatumSetup StandardPlaneColorDatum's}
	 * have
	 * 
	 * @param S		The number of relevant colors
	 * @param R		The maximum radius
	 */
	public FilmFrameBeforeAndAfterDatumSetup(ColorVideoSet videoSet, Set<String> filterNames, int R){
		super(videoSet, filterNames, R);
		this.videoSet = videoSet;
		generateFilterNamesToColors(filterNames);
		
		M = (numFilters() * (R + 1));
//		System.out.println("FilmFrameBeforeAndAfterDatumSetup created S: " + numFilters() + " R: " + R + " M: " + M);
	}
	
	/**
	 * This function needs to be overriden to artificially add more "filters" from previous and future
	 * frames and to ensure they are all endowed with proper coloring
	 */
	protected void generateFilterNamesToColors(Set<String> true_filters){
		filterNamesToColors = new LinkedHashMap<String, Color>((FrameOffsets.size() + 1) * true_filters.size());
		for (String filter : true_filters){
			for (int i : FrameOffsets){
				filterNamesToColors.put("color: " + filter + ", frame: " + i, videoSet.getWaveColor(filter));
			}
		}
	}

	public Datum generateDatum(String filename, Point t) {
//		System.out.println("FilmFrameBeforeAndAfterDatumSetup generateDatum");
		SuperImage superImage;
		try {
			superImage = ImageAndScoresBank.getOrAddSuperImage(filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return new FilmFrameBeforeAndAfterDatum(this, superImage.AdjustPointForSuper(t), filename);
	}

	public ColorVideoSet getVideoSet() {
		return videoSet;
	}

}
