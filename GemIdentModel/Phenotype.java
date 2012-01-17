package GemIdentModel;

import java.awt.Color;
import java.io.Serializable;

/**
 * Houses the specific data necessary to build a phenotype (minimal). Built on top of
 * {@link TrainSuperclass TrainSuperclass}. It implements {@link java.io.Serializable Serializable} 
 * to easily dump its data to XML format when saving a <b>GemIdent</b> project
 * 
 * @author Adam Kapelner
 */
public class Phenotype extends TrainSuperclass implements Serializable{

	private static final long serialVersionUID = 4391353231882685179L;

	/** the name of the NON phenotype */
	transient public static final String NON_NAME="NON";
	
	/** should this phenotype's centroids be found during classification? */
	private boolean findCentroids;
	/** should this phenotype be looked for at all during classification? */
	private boolean findPixels;
	
	/** basically the default constructor */
	public Phenotype(){
		super();
		dirty=false;
		findCentroids=true;
		findPixels=true;
	}
	/**
	 * Gets the display color with a specified opacity
	 * 
	 * @param alpha		the opacity
	 * @return			the display color with the specified opacity
	 */
	public Color getDisplayColorWithAlpha(int alpha) {
		return new Color(
			displayColor.getRed(),
			displayColor.getGreen(),
			displayColor.getBlue(),
			alpha
		);
	}
	/** is this the NON phenotype? */
	public boolean isNON(){
		return name.equals(NON_NAME);
	}
	/** is this name the NON name? */
	public static boolean isNONNAME(String name){
		return name.equals(NON_NAME);
	}
	public boolean isFindCentroids() {
		return findCentroids;
	}
	public void setFindCentroids(boolean findCentroids) {
		this.findCentroids = findCentroids;
	}
	public boolean isFindPixels() {
		return findPixels;
	}
	public void setFindPixels(boolean findPixels) {
		this.findPixels = findPixels;
	}
}