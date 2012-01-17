package GemVidentTracking;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import org.jgrapht.graph.SimpleWeightedGraph;

import GemIdentCentroidFinding.ViaSegmentation.DefaultWeightedComparableEdge;
import GemIdentCentroidFinding.ViaSegmentation.GraphUtils.Vertex;
import GemIdentTools.ValueSortedMap;

public class SimpleMarkovTracking extends ParticleFilterTracking{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	protected void simulateForwardStep(ArrayList<Point> nextPts,
			Particle thisParticle) {

		HashMap<Vertex,Point> thisObservedPosMap = new HashMap<Vertex,Point>();
		HashMap<Vertex,AntPath> vertexPathMap = new HashMap<Vertex,AntPath>();
		HashMap<AntPath,Vertex> pathVertexMap = new HashMap<AntPath,Vertex>();

		thisParticle.currentFalsePositives = new ArrayList<Point>();
		for (Point pt : nextPts){
			Vertex vx = new ObsVertex();
			thisObservedPosMap.put(vx, pt);
		}

		for (AntPath ap : thisParticle.getPaths()){
			if (ap.isDead())
				continue;
			Vertex vx = new PathVertex();
			vertexPathMap.put(vx, ap);
			pathVertexMap.put(ap,vx);
		}
		
		/*
		 * Compute probability graph for this particle.
		 */
		SimpleWeightedGraph<Vertex, DefaultWeightedComparableEdge> probabilityGraph = new SimpleWeightedGraph<Vertex, DefaultWeightedComparableEdge>(DefaultWeightedComparableEdge.class);
		computeLogProbabilityGraph(probabilityGraph,thisObservedPosMap,vertexPathMap);

		
		/*
		 * Generate new ant locations based on probability graph.  We do this by cycling through each path in a random order,
		 * then cycling through tentative paths in a random order.
		 * 
		 *
		 * We're also keeping track of the posterior log-probability of the simulated step.
		 */
	
		ArrayList<AntPath> strongPaths = new ArrayList<AntPath>();
		ArrayList<AntPath> tentativePaths = new ArrayList<AntPath>();
		
		
		for (AntPath ap: thisParticle.getPaths()){
			if(ap.isDead())
				continue;
			if (ap.isTentative())
				tentativePaths.add(ap);
			else
				strongPaths.add(ap);
		}
		
		Utils.shuffle(strongPaths);
		Utils.shuffle(tentativePaths);
		for (AntPath ap: strongPaths){
			forwardPath(ap,probabilityGraph,pathVertexMap,thisObservedPosMap);
		}			
		for (AntPath ap: tentativePaths){
			forwardPath(ap,probabilityGraph,pathVertexMap,thisObservedPosMap);
		}
		
		probabilityGraph.removeVertex(falsePositive);
		probabilityGraph.removeVertex(falseNegative);
		
		thisParticle.currentFalsePositives = new ArrayList<Point>();
		Set<Vertex> vs = probabilityGraph.vertexSet();
		for (Vertex v: vs){
			Point obs = thisObservedPosMap.get(v);
			assert(obs != null);
			thisParticle.currentFalsePositives.add(obs);
		}
	}

	public double forwardPath(AntPath ap, SimpleWeightedGraph<Vertex,DefaultWeightedComparableEdge> probabilityGraph, HashMap<AntPath,Vertex> pathVertexMap, HashMap<Vertex,Point> thisObservedPosMap){

		/*
		 * Sample an event from the probability graph.
		 */
		Vertex v = pathVertexMap.get(ap);
		
		assert(probabilityGraph.edgesOf(v).size() >0);
		
		DefaultWeightedComparableEdge event = sampleVertexEvent(probabilityGraph,v);
							
		double edgeWeight = probabilityGraph.getEdgeWeight(event);
		double thisLogProb = edgeWeight;

		
		
		Vertex v1 = probabilityGraph.getEdgeSource(event);
		Vertex v2 = probabilityGraph.getEdgeTarget(event);
		
		if (v1.getClass().equals(ObsVertex.class)){
			assert(v2.getClass().equals(PathVertex.class));
			Vertex tv = v1;
			v1=v2;
			v2=tv;
		}
		assert(v1 == v);

		/*
		 * Update the probability graph.
		 */
		
		probabilityGraph.removeVertex(v1);
		
		if (!v2.equals(falseNegative)){
			boolean tt = probabilityGraph.removeVertex(v2);
			assert(tt);
		}

		/*
		 * Update AntPath trajectory.
		 */
		
		Point obs = thisObservedPosMap.get(v2); 

		Point newPos = new Point();
		double thislp = sampleConditionalPos(ap,obs,newPos) + thisLogProb;
		ap.updatePosition(newPos,obs,thislp);
					
		/*
		 * Compute likelihoods. 
		 */

		return thislp;
	}

	private DefaultWeightedComparableEdge sampleVertexEvent(
			SimpleWeightedGraph<Vertex, DefaultWeightedComparableEdge> probabilityGraph,
			Vertex v1) {

		ValueSortedMap<DefaultWeightedComparableEdge, Double> vsm = new ValueSortedMap<DefaultWeightedComparableEdge,Double>(true);
		
		Set<DefaultWeightedComparableEdge> es = probabilityGraph.edgesOf(v1);
		for(DefaultWeightedComparableEdge thisEdge: es){
			vsm.put(thisEdge, probabilityGraph.getEdgeWeight(thisEdge));
		}
		return Utils.sampleMap(vsm);
	}

	/**
	 * 	Compute the log-probability that each observation arises for each ant.  This is represented by a simple weighted graph,
	 *  where the absence of an edge represents zero probability that the given observation was caused by the given ant. 
	 *  Currently the log-prob is set to be a truncated iid Gaussian around an ant location.     
	 * @param probabilityGraph		The probability graph being constructed.
	 * @param edgeMap				A sorted map of the edges in probabilityGraph.
	 * @param thisObservedPosMap    Observed points along with index. 
	 * @param thisParticlePosMap  	Current particle locations.
	 * @param obsSums 
	 * @param antSums 
	 * @return						Sum of edge weights
	 */
	private void computeLogProbabilityGraph(SimpleWeightedGraph<Vertex,DefaultWeightedComparableEdge> probabilityGraph,
			HashMap<Vertex, Point> thisObservedPosMap,
			HashMap<Vertex, AntPath> thisParticlePathMap) {
		/**
		 * Initialize vertices.
		 */

		probabilityGraph.addVertex(falseNegative);
		probabilityGraph.addVertex(falsePositive);

		for (Vertex v : thisObservedPosMap.keySet()){
			probabilityGraph.addVertex(v);
			DefaultWeightedComparableEdge edge = probabilityGraph.addEdge(v,falsePositive);
			probabilityGraph.setEdgeWeight(edge,falsePositiveLogProb);
		}
		for (Vertex v : thisParticlePathMap.keySet()){
			probabilityGraph.addVertex(v);
			DefaultWeightedComparableEdge edge = probabilityGraph.addEdge(v,falseNegative);
			probabilityGraph.setEdgeWeight(edge,falseNegativeLogProb);
		}

		/**
		 * Compute probability of each observation given each ant path 
		 */
		for (Entry<Vertex, Point> obsEntry : thisObservedPosMap.entrySet()){
			for (Entry<Vertex, AntPath> parEntry : thisParticlePathMap.entrySet()){
				double logprob = observationLogProbGivenAntPath(obsEntry.getValue(),parEntry.getValue());
				if (logprob > logProbThreshold){
					DefaultWeightedComparableEdge edge = probabilityGraph.addEdge(obsEntry.getKey(), parEntry.getKey());
					probabilityGraph.setEdgeWeight(edge, logprob);
				}
			}
		}


//		/**
//		 * Adjust probabilities according to log-probability of an AntPath existing.
//		 */
//		for (Entry<Vertex, AntPath> antEntry: thisParticlePathMap.entrySet()){
//			Set<DefaultWeightedComparableEdge> edge = probabilityGraph.edgesOf(antEntry.getKey());
//			double antProb = antEntry.getValue().getCurrentLogProb();
//			for (DefaultWeightedComparableEdge e: edge){
//				double ew = probabilityGraph.getEdgeWeight(e);
//				probabilityGraph.setEdgeWeight(e,ew+antProb);
//			}
//		}


		/**
		 * ANG -- to do
		 * Interaction effects:
		 *  -- 	probability of false negative higher when multiple ants in same area.
		 *      however, there is a high probability that there will be SOME observation in 
		 *      the area.       
		 *  --  probability of a false positive higher when there is one ant in a region. 
		 *  	this is because sometimes one ant is split into two.
		 */

		/**
		 * compute vertex sums
		 */
		//Utils.computeVertexSums(probabilityGraph);
	}



	/**
	 * The log-probability of an observation 
	 * 
	 * @param obsPt
	 * @param thisAntPath
	 * @return
	 */
	private double observationLogProbGivenAntPath(Point obsPt, AntPath thisAntPath) {
		final double normConst = Math.log(1/Math.sqrt(2*Math.PI*sigma2obs));

		double dist = Utils.computeDistance(obsPt,thisAntPath.getCurrentProjectedPosition());
		double logprob =  normConst - Math.pow(dist,2)/(2*sigma2obs);
		return logprob;
	}
	
	
	private double sampleConditionalPos(AntPath ap, Point obs, Point newPos) {
		Point projPos = ap.getCurrentProjectedPosition();
		if (obs == null){
			return Utils.sampleGaussian(projPos,sigma2ant,newPos);
		}
		else{
			final double combinedSigma2 = (sigma2ant*sigma2obs)/(double)(sigma2ant+sigma2obs);
			Point combinedPos = new Point();
			combinedPos.x = (int) Math.round((projPos.x*sigma2obs+obs.x*sigma2ant)/(double)(sigma2ant+sigma2obs));
			combinedPos.y = (int) Math.round((projPos.y*sigma2obs+obs.y*sigma2ant)/(double)(sigma2ant+sigma2obs));
			return Utils.sampleGaussian(combinedPos,combinedSigma2,newPos);
		}
	}
}