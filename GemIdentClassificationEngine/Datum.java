package GemIdentClassificationEngine;

import java.awt.Point;
import java.util.Set;

/**
 * This class is the abstract class responsible for pulling data from images
 * and constructing an array of integers (a "record") that can be processed
 * by a machine learning classifier. The {@link #datumSetup datum setup} object
 * holds common information about each Datum. There are many ways to pull data
 * from images and construct features. Extend this class to write your own such method.
 * 
 * @author Adam Kapelner
 */
public abstract class Datum implements Cloneable{


	/** the coordinate in the that a Datum is being generated for in the {@link GemIdentImageSets.SuperImage SuperImage} of interest */
	protected Point to;
	/** the data record as an integer array (holds the attribute values as entries, the last entry is the class - the response value) */ 
	protected int[] record;
	/** the object that contains common information for each Datum */	
	private DatumSetup datumSetup;
	
	/**
	 * constructs a Datum from pixel location. See the superclass's constructor
	 * for information
	 * 
	 * @param datumSetup	the object that contains common information for each Datum 	
	 * @param to			the pixel location in the image this Datum is constructed for
	 */
	public Datum(DatumSetup datumSetup, Point to){
		this.datumSetup = datumSetup;
		this.to=to;
		
		record = new int[datumSetup.NumberOfFeatures() + 1]; //plus 1 is for the class y: X_i = [x1, x2, ..., xM, y_i]
	}

	/**
	 * From the image data, builds the {@link #record record} consisting of ints for
	 * later classification
	 */
	protected abstract void BuildRecord();

	/**
	 * Sets the response variable, the phenotype class
	 * 
	 * @param classNum		the class to set
	 */
	public void setClass(int classNum){
		record[datumSetup.NumberOfFeatures()] = classNum; //the last value is the y_i
	}
	
	public int[] getRecord(){
		return record;
	}
	
	public Set<String> filterNames(){
		return datumSetup.getFilterNamesToColors().keySet();
	}
	
	/** debug purposes only */
	public void Print(){
		for (int i = 0; i <= datumSetup.NumberOfFeatures(); i++){
			System.out.print(record[i] + ":");
		}
		System.out.print("\n");
	}
}