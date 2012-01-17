package GemVidentTracking;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;



public class Particle implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<AntPath> paths;
	public ArrayList<Point> currentFalsePositives;
	double g;
	double f;
	Particle(){
		paths = new ArrayList<AntPath>();
		currentFalsePositives = new ArrayList<Point>();
		g = 0;
		f = 0;
	}
	public ArrayList<AntPath> getPaths() {
		return paths;
	}
}