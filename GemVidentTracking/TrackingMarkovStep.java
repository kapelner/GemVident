package GemVidentTracking;

import java.awt.Point;
import java.util.ArrayList;


public interface TrackingMarkovStep{
	public void simulateStep(ArrayList<Point> nextPts,ArrayList<AntPath> thisParticle);

}