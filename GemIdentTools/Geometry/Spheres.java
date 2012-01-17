package GemIdentTools.Geometry;

import java.util.ArrayList;
import java.util.HashMap;

import GemIdentTools.Matrices.BoolMatrix;

/**
 * Provides masks of sphere-shells of varius radii 
 * similiar to the {@link Rings Rings} class
 * which provides masks of unfilled circles
 * 
 * @author Adam Kapelner
 */
public class Spheres {
	
	/** the maxium sphere-shell's radius for the initial build */
	public static final int INIT_MAX_RADIUS = 30;
	/** the mapping from radius to the sphere-shell list stored as a list of {@link java.awt.Point Point} objects */
	private static HashMap<Integer, ArrayList<Point3d>> sphereLists;
	
	/** To construct the static object, call Build */
	static {
		Build();
	}
	
	/** Build the initial sphere-shells for radius = {0, 1, . . ., INIT_MAX_RADIUS} */
	public static void Build(){
		//init the mapping
		sphereLists = new HashMap<Integer,ArrayList<Point3d>>(INIT_MAX_RADIUS);
		//init the first sphere-shell
		ArrayList<Point3d> zeroList = new ArrayList<Point3d>();
		zeroList.add(new Point3d(0, 0, 0));
		sphereLists.put(0, zeroList);
		//init all other sphere-shells
		for (int r = 1; r <= INIT_MAX_RADIUS; r++)
			GenerateSphereShell(r);
	}
	
	/**
	 * Generate a sphere-shell of radius r using an inefficient algorithm
	 * 
	 * @param r			the radius of the sphere-shell to be generated
	 * @return			the sphere-shell as a list of 3-d coordinates
	 */
	private static ArrayList<Point3d> GenerateSphereShell(int r){
		int r_sqd=r*r;
		int r_minus_1_sqd = (r - 1) * (r - 1);
		ArrayList<Point3d> sphereShellist = new ArrayList<Point3d>();
		for (int x = -r; x <= r; x++){
			for (int y =- r; y <= r; y++){
				for (int z = -r; z <= r; z++){
					int sum_of_squared_dims = x * x + y * y + z * z;
					if (sum_of_squared_dims <= r_sqd && sum_of_squared_dims > r_minus_1_sqd){
						sphereShellist.add(new Point3d(x, y, z));
					}
				}
			}
		}
		sphereLists.put(r,sphereShellist);
		return sphereShellist;
	}
	
	/**
	 * Return a sphere-shell of radius r. If the sphere-shell is not yet generated, 
	 * it will autogenerate, cache it, and return it
	 * 
	 * @param r		radius of the sphere-shell desired
	 * @return		the sphere-shell
	 */
	public static ArrayList<Point3d> getSphereShell(int r){
		ArrayList<Point3d> sphereShell = sphereLists.get(r);
		if (sphereShell == null)
			return GenerateSphereShell(r);
		else
			return sphereShell;
	}
	
	/**
	 * Return a sphere-shell of radius r centered at to (not at the origin). If the sphere-shell 
	 * is not yet generated, it will autogenerate, cache it, and return it.
	 * 
	 * @param r			the radius of the sphere-shell desired
	 * @param to		the center of the sphere-shell
	 * @return			the sphere-shell desired
	 */
	public static ArrayList<Point3d> GetPointsInSphereShellUsingCenter(int r, Point3d to){
		ArrayList<Point3d> shell = getSphereShell(r);
		ArrayList<Point3d> points = new ArrayList<Point3d>(shell.size());
		for (Point3d t : shell)
			points.add(new Point3d(to.x + t.x, to.y + t.y, to.z + t.z));
		return points;
	}
	
	public static void Print(int r){		
		System.out.println("level planes for r = " + r + "\n\n");
		HashMap<Integer, BoolMatrix> planes = new HashMap<Integer, BoolMatrix>(r * 2 + 1);
		for (int z = -r; z <= r; z++){
			planes.put(z, new BoolMatrix(r * 2 + 1, r * 2 + 1));
		}
		for (Point3d t : getSphereShell(r)){
			planes.get(t.z).set(t.x + r, t.y + r, true);
		}
		for (int z = -r; z <= r; z++){
			System.out.println("z = " + z + "\n" + planes.get(z).toString() + "\n\n");			
		}
	}
}
