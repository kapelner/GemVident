package GemIdentClassificationEngine.StandardPlaneColor;

import java.awt.Point;
import java.util.HashMap;

import GemIdentClassificationEngine.DatumThatUsesRingScores;
import GemIdentTools.Matrices.ShortMatrix;

/**
 * This class constructs a record from images by pulling out "ring scores"
 * for each "color" at the given pixel for many different radii
 * 
 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
 * @author Adam Kapelner
 */
public final class StandardPlaneColorDatum extends DatumThatUsesRingScores {

	/** A map from color name --> intensity values for each pixel, for this image */
	private HashMap<String, ShortMatrix> scores;
	/** the object that contains common information for each Datum */
	private StandardPlaneColorDatumSetup datumSetup;

	/** constructs a Datum from pixel location. See {@link #BuildRecord() BuildRecord()} 
	 * for more information
	 * 
	 * @param to			The location of the pixel in the image
	 * @param scores		The score matrices
	 * 
	 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
	 */		
	public StandardPlaneColorDatum(StandardPlaneColorDatumSetup datumSetup, Point to, HashMap<String, ShortMatrix> scores) {
		super(datumSetup, to);
		this.datumSetup = datumSetup;
		this.scores = scores;
		BuildRecord();
	}

	/**
	 * Using the image, this method builds the attributes in the record by cycling through the colors
	 * and generating all rings 0, 1, . . ., R for each. See step 5b in the 
	 * Algorithm section of the IEEE paper for a formal mathematical description.
	 */	
	protected void BuildRecord() {
		BuildManyRingsFromManyColors(datumSetup.getMaximumRadius(), scores);
	}

}
