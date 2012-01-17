
package GemIdentImageSets;

/**
 * A convenience interface to express data about each image
 * in the context of an overall global image set
 *  
 * @author Adam Kapelner
 */
public interface MiniImageInImageSet {
	
	/** the data for this image as a string (for debugging purposes) */
	public String toString();
	
	/** the x-location of this image in the context of a global image set */
	public int xLoc();
	
	/** the y-location of this image in the context of a global image set */
	public int yLoc();
}