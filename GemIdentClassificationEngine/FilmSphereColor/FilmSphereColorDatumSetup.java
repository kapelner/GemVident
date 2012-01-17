package GemIdentClassificationEngine.FilmSphereColor;

import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.Set;

import GemIdentClassificationEngine.Datum;
import GemIdentClassificationEngine.DatumSetupThatUsesRings;
import GemIdentImageSets.ColorVideoSet;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.SuperImage;

public class FilmSphereColorDatumSetup extends DatumSetupThatUsesRings {

	/** the video image set */
	private ColorVideoSet videoSet;

	public FilmSphereColorDatumSetup(ColorVideoSet videoSet, Set<String> filterNames, int R){
		super(videoSet, filterNames, R);
		this.videoSet = videoSet;
		generateFilterNamesToColors(filterNames);		
		M = numFilters() * (R + 1);
//		System.out.println("FilmSphereColorDatumSetup created S: " + numFilters() + " R: " + R + " M: " + M);
	}
	
	public ColorVideoSet getVideoSet() {
		return videoSet;
	}	

	public Datum generateDatum(String filename, Point t) throws FileNotFoundException {
//		System.out.println("FilmFrameBeforeAndAfterDatumSetup generateDatum");
		SuperImage superImage = ImageAndScoresBank.getOrAddSuperImage(filename);		
		return new FilmSphereColorDatum(this, superImage.AdjustPointForSuper(t), filename);
	}
}
