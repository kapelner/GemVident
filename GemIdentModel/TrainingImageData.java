
package GemIdentModel;

import java.awt.Point;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;

/**
 * Provides the infrastructure to house training points in one image.
 * It implements {@link java.io.Serializable Serializable} to easily dump its data to XML format 
 * when saving a <b>GemIdent</b> project
 * 
 * @author Adam Kapelner
 */
public class TrainingImageData implements Serializable{
	
	private static final long serialVersionUID = 1205339007491217013L;
	
	/** **Deprecated do not use** */
	private int blissNum;
	/** the filename of this image */
	private String filename;
	/** the list of the training point locations */
	private ArrayList<Point> points;

	/** keeps serializable happy */
	public TrainingImageData(){}
	/** basically a defauly constructor */
	public TrainingImageData(String filename){
		this.filename=filename;
		points=new ArrayList<Point>();
	}
	/**
	 * Adds a training point to this training image
	 * @param t		the point's location
	 */
	public void addPoint(Point t){
		points.add(t);
	}
	/**
	 * Deletes a training point to this training image
	 * @param t		the point's location
	 */
	public void deletePoint(Point t){
		points.remove(t);
	}
	/**
	 * The number of training points in this image
	 * @return		the number of points
	 */
	public int getNumPoints(){
		return points.size();
	}
	/** Deprecated do not use */
	public int getBlissNum() {
//		System.out.println("TrainingImageData.getBlissNum()");
		return blissNum;
	}
	public String getFilename() {
//		System.out.println("TrainingImageData.getFilename()");
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	/** Deprecated do not use */
	public void setBlissNum(int num) {
		this.blissNum = num;
	}
	public ArrayList<Point> getPoints() {
//		System.out.println("TrainingImageData.getPoints()");
		return points;
	}
	public void setPoints(ArrayList<Point> points) {
		this.points = points;
	}
	/**
	 * Actually returns the image itself as a {@link GemIdentImageSets.DataImage DataImage}
	 * @return		the actual image
	 * @throws FileNotFoundException 
	 */
	public DataImage getImage() throws FileNotFoundException{
		return ImageAndScoresBank.getOrAddDataImage(filename);
	}
}