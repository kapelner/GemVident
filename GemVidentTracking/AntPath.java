package GemVidentTracking;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;

public class AntPath implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int velocityStep = 5;
	private ArrayList<Point> trajectory;
	private Integer startTime;
	private Point2D.Double velocity;
	private ArrayList<Double> logprob;
	private ArrayList<Point> observations;
	private double alpha=.8;
	
	private ParticleFilterTracking tracking = null;
	
	
	/**
	 * 
	 * @param _startTime
	 * @param pos
	 * @param thislp
	 */
	public AntPath(int _startTime,Point pos, double thislp,ParticleFilterTracking pft){
		this();
		tracking = pft;
		
		startTime = _startTime;
		observations.add(pos);
		
		Point newPos = new Point();
		Utils.sampleGaussian(pos,tracking.sigma2obs,newPos);
		trajectory.add(newPos);
		logprob.add(thislp);
	}
	private AntPath(){
		trajectory = new ArrayList<Point>();
		observations = new ArrayList<Point>();
		velocity = new Point2D.Double(0,0);
		logprob = new ArrayList<Double>();
		startTime = null;
		tracking = null;
	}
	
	public AntPath(AntPath antPath) {
		this();
		trajectory.addAll(antPath.trajectory); 
		observations.addAll(antPath.observations);
		logprob.addAll(antPath.logprob);
		startTime = antPath.startTime;
		tracking = antPath.tracking;
		computeVelocity();
	}
	public void updatePosition(Point pt){
		updatePosition(pt,null,0);
	}
	public void updatePosition(Point pt,Point obs,double thislogprob){
		trajectory.add(pt);		
		observations.add(obs);
		computeLogProb(thislogprob);
		computeVelocity(); 
	}
	
	private void computeLogProb(double lp) {
		logprob.add(alpha*getCurrentLogProb()+(1-alpha)*lp);
	}
	
	private void computeVelocity() {
		
		int endTime = getEndTime();
		int step = Math.min(endTime-startTime,velocityStep);
		int stepPos = endTime-step;
		if (step > 0){
			velocity.x = (getCurrentPosition().x-getPosition(stepPos).x)/(double)stepPos;
			velocity.y = (getCurrentPosition().y-getPosition(stepPos).y)/(double)stepPos;
		}
		else {
			velocity.x = 0;
			velocity.y = 0;
		}
	}

	public Point getCurrentProjectedPosition(){
		Point ret = new Point();
		ret.x = (int) (getCurrentPosition().x - getCurrentVelocity().x);
		ret.y = (int) (getCurrentPosition().y - getCurrentVelocity().y);
		return ret;
	}
	public Point getCurrentPosition(){
		return trajectory.get(trajectory.size()-1);
	}
	public Point2D.Double getCurrentVelocity(){
		return velocity;
	}

	public Point getPosition(int t){
		return trajectory.get(t-startTime);
	}

	public int getStartTime(){
		return startTime;
	}
	public int getEndTime(){
		return startTime + trajectory.size()-1;
	}

	public Point getLastObs() {
		return observations.get(observations.size()-1);
	}

	public int length(){
		return trajectory.size();
	}
	
	public Point getObservation(int t){
		if (isAlive(t))
			return observations.get(t-startTime);
		else 
			return null;
	}

	public double getCurrentLogProb() {
		return logprob.get(logprob.size()-1);
	}

	public double getLogProb(int t) {
		return logprob.get(t-startTime);
	}
	
	public void print(PrintWriter out) {
		for (int i = 0;i < trajectory.size();i++){
			out.print(startTime+i + "\t");
			out.print(trajectory.get(i).x + "\t");
			out.print(trajectory.get(i).y + "\t");
			if (observations.get(i) != null){
				out.print(observations.get(i).x + "\t");
				out.print(observations.get(i).y + "\t");
			}
			else
			{
				out.print("-1\t-1\t");
			}
			
			out.print(logprob.get(i) + "\n");
		}		
	}
	public static AntPath load(BufferedReader in){
		AntPath ap = null;
		try {
			ap = new AntPath();
			String str;
			while((str = in.readLine()) != null){
				String[] pieces = str.split("[\t]");
				assert(pieces.length == 6);
				if(ap.startTime == null)
					ap.startTime = Integer.parseInt(pieces[0]);
				Point pt = new Point();
				pt.x = Integer.parseInt(pieces[1]);
				pt.y = Integer.parseInt(pieces[2]);
				ap.trajectory.add(pt);
				pt = new Point();
				pt.x = Integer.parseInt(pieces[3]);
				pt.y = Integer.parseInt(pieces[4]);
				if (pt.x > -1)
					ap.observations.add(pt);
				else
					ap.observations.add(null);
				ap.logprob.add(Double.parseDouble(pieces[5]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ap;		
	}
	public boolean isAlive(int t) {
		return (this.getStartTime() <= t) && (this.getEndTime() >= t);
	}
	public boolean isTentative(int t) {
		if (isAlive(t) && getLogProb(t) < tracking.tentativeThreshold)
			return true;
		else
			return false;
	}
	public boolean isTentative() {
		if (this.getCurrentLogProb() < tracking.tentativeThreshold)
			return true;
		else
			return false;
	}
	public boolean isDead(int t) {
		if (isAlive(t) && getLogProb(t) < tracking.deadThreshold)
			return true;
		else
			return false;
	}
	public boolean isDead() {
		if (getCurrentLogProb() < tracking.deadThreshold)
			return true;
		else
			return false;
	}
	public int getLength() {
		return trajectory.size();
	}
}
