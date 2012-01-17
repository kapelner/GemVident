package GemVidentTracking;

import java.awt.Point;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map.Entry;



public class PathCloud implements Serializable{
	/**
	 * A PathCloud keeps track of a set of particles birthed at the same 
	 * moment.  Ideally they should more or less have the same set of 
	 * observations that they are attached to.
	 */
	private static final long serialVersionUID = 1L;
	private static final double strongPref = 3;
	private static final double tentativeThreshold = .8;
	private Vector<ArrayList<AntPath> > cloudPaths; 
	private ArrayList<Particle> particles; 
	private Point startingPoint;
	private Vector<Integer> times;
	PathCloud(Point startingPoint, int startingTime,ArrayList<Particle> particles){
		this.startingPoint = startingPoint;
		this.particles = particles;
		times = new Vector<Integer>();
		times.add(startingTime);
		cloudPaths = new Vector<ArrayList<AntPath>>();
		cloudPaths.add(new ArrayList<AntPath>(particles.size()));
		for (int i = 0;i< particles.size();i++){
			cloudPaths.lastElement().add(null);
		}
	}
	
	
	public void addPath(AntPath path, Particle particle){
		int idx = particles.indexOf(particle);
		assert(idx != -1);
		cloudPaths.lastElement().set(idx,path);
	}
		
	
	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException{
		startingPoint = (Point) stream.readObject();
		times = (Vector<Integer>) stream.readObject();
		cloudPaths = (Vector<ArrayList<AntPath>>) stream.readObject();
		particles = null;
	} 
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException{
		stream.writeObject(startingPoint);
		stream.writeObject(times);
		stream.writeObject(cloudPaths);
	}

	
	private Integer getIndex(int t){
		
		if (t < times.get(0))
			return null;
		
		int idx = times.size()-1;
		while(times.get(idx) > t){
			idx = idx-1;
		}
		return idx;
	}
	
	public Double getMeanLogProb(int t){
		Integer idx = getIndex(t);
		if (idx == null){
			return null;
		}
		ArrayList<AntPath> cp = cloudPaths.get(idx);
		double sum = 0;
		int numAlive = getNumAlive(t);
		for (AntPath p: cp){
			if(!p.isAlive(t))
				continue;
			sum = sum + p.getLogProb(t);
		}
		
		return sum/(double)numAlive;
	}
	
	public Point getCenter(int t){
		Integer idx = getIndex(t);
		if (idx == null){
			return null;
		}
		ArrayList<AntPath> cp = cloudPaths.get(idx);
		Point.Double temppt = new Point.Double(0,0);
		int numAlive = 0;
		for (AntPath p: cp){
			if(!p.isAlive(t))
				continue;
			numAlive += 1;
			Point thispt = p.getPosition(t);
			temppt.x = temppt.x + thispt.x;
			temppt.y = temppt.y + thispt.y;
		}
		if (numAlive == 0)
			return null;
		
		
		temppt.x = (temppt.x/(double)numAlive);
		temppt.y = (temppt.y/(double)numAlive);
		
		Point center = new Point((int)Math.round(temppt.x),(int)Math.round(temppt.y)); 
		
		return center;
	} 	
	public int getStartingTime(){
		return times.get(0);
	}
	
	public Vector<Point> getPositions(int t){
		Integer idx = getIndex(t);
		if (idx == null){
			return null;
		}
		ArrayList<AntPath> cp = cloudPaths.get(idx);
		Vector<Point> pts = new Vector<Point>();
		int numAlive = 0;
		for (AntPath p: cp){
			if(!p.isAlive(t))
				continue;
			numAlive += 1;
			pts.add(p.getPosition(t));
		}
		
		return pts;		
	}

	public Vector<Point> getObservations(int t){
		Integer idx = getIndex(t);
		if (idx == null){
			return null;
		}
		ArrayList<AntPath> cp = cloudPaths.get(idx);
		Vector<Point> pts = new Vector<Point>();
		for (AntPath p: cp){
			if(!p.isAlive(t))
				continue;
			pts.add(p.getObservation(t));
		}
		
		return pts;		
	}
	
	public ArrayList<AntPath> getPaths(int t){
		Integer idx = getIndex(t);
		if (idx == null){
			return null;
		}
		return cloudPaths.get(idx);
	}
	
		
	public Point getStartingPoint(){
		return this.startingPoint;
	}
	public Integer getNumAlive(int t) {
		Integer idx = getIndex(t);
		if (idx == null){
			return 0;
		}
		ArrayList<AntPath> cp = cloudPaths.get(idx);
		int numAlive = 0;
		for (AntPath p: cp){
			if(p.isAlive(t))
				numAlive += 1;
		}
		return numAlive;
	}
	public Integer getNumTentative(int t) {
		Integer idx = getIndex(t);
		if (idx == null){
			return null;
		}
		ArrayList<AntPath> cp = cloudPaths.get(idx);
		int numTentative = 0;
		for (AntPath p: cp){
			if(!p.isAlive(t))
				continue;
			if(p.isTentative(t))
				numTentative += 1;
		}
		return numTentative;
	}
	public Integer getLength(int t) {
		return Math.max(t-getStartingTime()+1,0);
	}
	public Integer getLength() {
		Integer idx = times.size()-1;
		if (idx <0){
			return 0;
		}
		int startTime = getStartingTime();
		ArrayList<AntPath> cp = cloudPaths.get(idx);
		int endingTime = startTime;
		for (AntPath p: cp){
			endingTime = Math.max(endingTime, p.getEndTime());				
		}
		
		return endingTime-startTime;
	}
	
	public static void printHeader(PrintWriter out){
		
		out.print("time"+",");
		out.print("position x"+",");
		out.print("position y"+",");
		out.print("observation x"+",");
		out.print("observation y"+",");
		out.print("fraction tentative"+",");
		out.print("logprob"+"\n");
	}
	
	public void print(PrintWriter out,HashMap<Integer, Integer> frameNumbers) {
		
		PathCloud.printHeader(out);
		int ml = getLength();
		for (int t = getStartingTime();t < getStartingTime()+ml;t++){
			out.print(t + ",");
			Point pt = this.getCenter(t);
			out.print(pt.x + ",");
			out.print(pt.y + ",");
			Point obs = this.getConsensusObservation(t);
			if (obs == null){
				obs = new Point();
				obs.x = -1;
				obs.y = -1;
			}
			out.print(obs.x + ",");
			out.print(obs.y + ",");
				
			out.print(this.getNumTentative(t)/(double)this.getNumPaths() + ",");
			out.print(this.getMeanLogProb(t) + ",");
			out.print(frameNumbers.get(t) + "\n");
		}
	}

	
	public Point getConsensusObservation(int t){
		HashMap<Point, Double> obsMap = new HashMap<Point,Double>();
		for (AntPath cp: getPaths(t)){
			Point obs = cp.getObservation(t);
			if (cp.isDead(t))
				continue;
			Double zz = obsMap.get(obs);
			if (zz == null)
				zz = 0.;
			if (cp.isTentative())
				obsMap.put(obs, zz+1);
			else
				obsMap.put(obs, zz+strongPref);				
		}
		
		double maxNum = 0;
		Point maxObs = null;
		for (Entry<Point, Double> entry : obsMap.entrySet()){
			if(entry.getValue() > maxNum){
				maxNum = entry.getValue();
				maxObs = entry.getKey();
			}
		}
		
		return maxObs;
	}
	
	public boolean resamplePaths(int t, ParticleFilterTracking tracking){
		
		ArrayList<AntPath> cp = cloudPaths.lastElement();
		
		Vector<Integer> deadIdx = new Vector<Integer>();
		Vector<Integer> tentIdx = new Vector<Integer>();
		Vector<Integer> strongIdx = new Vector<Integer>();
		
		Point consensusObs = getConsensusObservation(t); 
		
		int numPaths = cp.size();
		for (int i = 0; i < numPaths;i++){
			AntPath ap = cp.get(i);
			if (ap == null)
				deadIdx.add(i);
			else if (!ap.isAlive(t))
				deadIdx.add(i);
			else if (!(ap.getObservation(t) == null) && !ap.getObservation(t).equals(consensusObs) )
				deadIdx.add(i);
			else if (cp.get(i).isTentative(t))
				tentIdx.add(i);
			else
				strongIdx.add(i);
		}
		
		Vector<Integer> aliveIdx = new Vector<Integer>();
		aliveIdx.addAll(tentIdx);
		aliveIdx.addAll(strongIdx);

		int numTentative = tentIdx.size();
		int numStrong = strongIdx.size();
		int numAlive = aliveIdx.size();
		int numDead = cp.size()-numAlive;
//		double fracAlive = numAlive/(double)(cp.size());

		if (numDead == 0 || numAlive == 0)
			return false;

		
		ArrayList<AntPath> newPathList = new ArrayList<AntPath>(cp.size()); 
		for (int i = 0;i< cp.size();i++){
			newPathList.add(null);
		}
		
		for(Integer idx : aliveIdx){
			newPathList.set(idx, cp.get(idx));
		}
		
		double weightsum = numTentative + numStrong*strongPref;
		/**
		 * Replace dead particles by sampling with replacement from live particles.
		 */
		for (Integer idx : deadIdx){
			//Prefer strong paths by a factor strongPref

			Integer sampleIdx = null;
			if (Utils.rand.nextDouble() < numTentative/weightsum)
				sampleIdx = tentIdx.get(Utils.rand.nextInt(numTentative));
			else
				sampleIdx = strongIdx.get(Utils.rand.nextInt(numStrong));
				
//			AntPath thisPath = new AntPath(cp.get(sampleIdx));
			AntPath thisPath = new AntPath(t,cp.get(sampleIdx).getPosition(t),cp.get(sampleIdx).getLogProb(t),tracking);
			ArrayList<AntPath> thesePaths = particles.get(idx).getPaths();
			thesePaths.remove(cp.get(idx));
			thesePaths.add(thisPath);
			newPathList.set(idx,thisPath);
		}

		assert(getIndex(t) == times.size()-1);
		times.add(t);
		cloudPaths.add(newPathList);

		return true;
	}
	public int getNumPaths(){
		if (cloudPaths.size() == 0)
			return 0;
		return cloudPaths.lastElement().size();
	}
	public int getNumParticles(){
		if (cloudPaths == null)
			return 0;
		return cloudPaths.size();
	}
	
	public boolean isAlive(int t) {
		return getNumAlive(t) > 0;
	}
	public boolean isTentative(int t) {
		return (getNumTentative(t)/(double)getNumPaths()) > tentativeThreshold;
	}
//	public static PathCloud combine(PathCloud pc1, PathCloud pc2,ParticleFilterTracking tracking){
//
//		int start1 = pc1.getStartingTime();
//		int start2 = pc2.getStartingTime();
//		int end1 = pc1.getLength() - start1;
//		int end2 = pc2.getLength() - start2;
//
//		int start = Math.min(start1,start2);
//		int end = Math.max(end1,end2);
//		
//		
//		Point initialObservation = new Point(-1,-1);
//		if (start1 < start2){
//			initialObservation = pc1.getStartingPoint();
//		}
//		else
//			initialObservation = pc2.getStartingPoint();
//		
//		assert(pc1.getNumParticles() == pc2.getNumParticles());
//		int numParticles = pc1.getNumParticles();
//		
//		ArrayList<Particle> particles = pc1.particles;
//		if (particles == null){
//			particles = new ArrayList<Particle>();
//			for (int i = 0;i<numParticles;i++){
//				particles.add(new Particle());
//			}
//		}
//		
//		
//		PathCloud newCloud = new PathCloud(initialObservation,start,particles);
//		ArrayList<AntPath> paths = new ArrayList<AntPath>();
//		for (int i = 0;i< numParticles;i++){
//			paths.add(new AntPath(start,initialObservation,tracking.initialLogProb,tracking));
//		}
//		for (int t = start;t <= end;t++){
//			ArrayList<AntPath> p1 = pc1.getPaths(t);
//			ArrayList<AntPath> p2 = pc2.getPaths(t);
//			for (int i = 0;i<numParticles;i++){
//				AntPath ap1 = null;
//				AntPath ap2 = null;
//
//				Point obs1 = null;
//				Point obs2 = null;
//
//				Point pos1 = null;
//				Point pos2 = null;
//				
//				Point cobs = null;
//				Point cpos = new Point(-1,-1);
//				
//				if (p1 != null){
//					ap1 = p1.get(i);
//					obs1 = ap1.getObservation(t);
//					pos1 = ap1.getPosition(t);
//				}
//				
//				if (p2 != null){
//					ap2 = p2.get(i);
//					obs2 = ap2.getObservation(t);
//					pos2 = ap2.getPosition(t);
//				}
//				
//				if (obs1 != null && obs2 == null)
//					cobs = obs1;
//				if (obs1 == null && obs2 != null)
//					cobs = obs2;
//				
//				if (obs1 != null && obs2 != null && obs1.equals(obs2))
//					cobs = obs1;
//				
////				paths.get(i).up;
//			}
//
//		}
//			
//		return newCloud;
//	}
}