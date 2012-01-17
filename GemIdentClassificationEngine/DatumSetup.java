package GemIdentClassificationEngine;

import java.awt.Color;
import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Set;

import GemIdentImageSets.ImageSetInterface;

/**
 * 
 * The interface that all types of datum setup must implement
 * 
 * @author Adam Kapelner
 */
public abstract class DatumSetup {
	
	/** the set of colors / filters */
	protected LinkedHashMap<String, Color> filterNamesToColors;
	/** the image set */
	private ImageSetInterface imageset;
	/** the number of features */
	protected int M;

	public DatumSetup(ImageSetInterface imageset, Set<String> filterNames){
		this.imageset = imageset;
	}
	
	/**
	 * What are the names of the colors / filters
	 * @return	a list of the filters
	 */	
	public LinkedHashMap<String, Color> getFilterNamesToColors(){
		return filterNamesToColors;
	}
	
	/**
	 * How many filters / colors are there?
	 * @return	the number of filters / colors
	 */
	public int numFilters(){
		return filterNamesToColors.size();
	}
	
	/**
	 * How many features does this datum have? 
	 * What is M? xi. = [xi1, xi2, ..., xim]
	 * 
	 * @return	The size of the features vector
	 */	
	public int NumberOfFeatures(){
		return M;
	}
	
	/**
	 * Generate a datum of the correct type
	 * 
	 * @param filename		the filename of the image the datum is being generated for
	 * @param to			the coordinate of the pixel the datum is being generated for
	 * @return				the Datum especially tailored
	 * @throws FileNotFoundException 
	 */
	public abstract Datum generateDatum(String filename, Point to) throws FileNotFoundException;
	
	/** 
	 * ensure each filter name gets a representative color 
	 * and that this data structure is built immediately and cached.
	 * 
	 * The default is to just call the getWaveColor for each. Override
	 * this function to do your own custom stuff
	 *  
	 * @param filterNames		the names of the colors / filters
	 */
	protected void generateFilterNamesToColors(Set<String> filterNames){
		filterNamesToColors = new LinkedHashMap<String, Color>(filterNames.size());
		for (String filter : filterNames){
			filterNamesToColors.put(filter, imageset.getWaveColor(filter));
		}
	}
}
