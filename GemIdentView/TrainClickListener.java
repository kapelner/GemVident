
package GemIdentView;

import java.awt.Point;

/**
 * An interface that simplifies what happens when the 
 * user clicks on a point in a {@link KTrainPanel training panel}
 * 
 * @author Adam Kapelner
 *
 */
public interface TrainClickListener {
	/**
	 * The user has just clicked on a point in an image
	 * 
	 * @param filename		the image filename
	 * @param t				the coordinate where the user clicked
	 */
	public void NewPoint(String filename,Point t);
	
	/**
	 * The user just deleted a point
	 * 
	 * @param filename		the image filename
	 * @param t				the coordinate where the user clicked
	 */
	public void DeletePoint(String filename, Point t);

	/**
	 * The user just added a point to the set of NON's
	 * 
	 * @param filename		the image filename
	 * @param t				the coordinate where the user clicked
	 */
	public void NewNonPoint(String filename, Point t);
}