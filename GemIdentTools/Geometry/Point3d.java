package GemIdentTools.Geometry;

/**
 * Dumb struct to hold 3-d point information
 * 
 * @author Adam Kapelner
 */
public class Point3d {

	public int x;
	public int y;
	public int z;
	
	public Point3d(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/** for debugging purposes exclusively */
	public String toString(){
		return "x: " + x + " y: " + y + " z: " + z;
	}
	
}
