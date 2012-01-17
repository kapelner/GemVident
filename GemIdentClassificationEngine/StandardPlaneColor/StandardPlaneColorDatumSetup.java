
package GemIdentClassificationEngine.StandardPlaneColor;

import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Set;

import GemIdentClassificationEngine.Datum;
import GemIdentClassificationEngine.DatumSetupThatUsesRings;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.SuperImage;
import GemIdentTools.Matrices.ShortMatrix;

/**
 * This class contains common information for all {@link StandardPlaneColorDatumSetup StandardPlaneColorDatum's}
 * 
 * @author Adam Kapelner
 * 
 */
public class StandardPlaneColorDatumSetup extends DatumSetupThatUsesRings {

	/**
	 * Using the maximum radius and the number of relevant colors,
	 * calculate the number of features all {@link StandardPlaneColorDatumSetup StandardPlaneColorDatum's}
	 * have
	 * 
	 * @param filter_names		The names of relevant colors
	 * @param R					The maximum radius
	 */
	public StandardPlaneColorDatumSetup(ImageSetInterface imageset, Set<String> filterNames, int R){
		super(imageset, filterNames, R);
		System.out.println("here2");
		generateFilterNamesToColors(filterNames);
		M = numFilters() * (R + 1);
	}

	public Datum generateDatum(String filename, Point t) throws FileNotFoundException {
		System.out.println("here1");
		SuperImage superImage = ImageAndScoresBank.getOrAddSuperImage(filename);
		HashMap<String, ShortMatrix> scores = ImageAndScoresBank.getOrAddScores(filename);
		return new StandardPlaneColorDatum(this, superImage.AdjustPointForSuper(t), scores);
	}

}
