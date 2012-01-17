package GemVidentTracking;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.jgrapht.graph.SimpleWeightedGraph;

import GemIdentCentroidFinding.ViaSegmentation.DefaultWeightedComparableEdge;
import GemIdentCentroidFinding.ViaSegmentation.GraphUtils.Vertex;
import GemIdentTools.ValueSortedMap;


public class ExperimentalMarkovTracking extends ParticleFilterTracking{//implements TrackingMarkovStep{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private DefaultWeightedComparableEdge sampleEdge(
			SimpleWeightedGraph<Vertex, DefaultWeightedComparableEdge> probabilityGraph,
			ValueSortedMap<DefaultWeightedComparableEdge,Double> edgeMap) {
		double logsum = Utils.maxstar(edgeMap);
		DefaultWeightedComparableEdge ee = Utils.sampleMap(edgeMap, logsum);

		return ee;
	}

	/**
	 * Simulate next step for the given particle.
	 *
	 * @param nextPts
	 * @param particleNum
	 */
	public void simulateForwardStep(ArrayList<Point> nextPts,Particle thisParticle){
		HashMap<Vertex,Point> thisObservedPosMap = new HashMap<Vertex,Point>();
		HashMap<Vertex,AntPath> thisAntPathMap = new HashMap<Vertex,AntPath>();

		thisParticle.currentFalsePositives = new ArrayList<Point>();
		for (Point pt : nextPts){
			Vertex vx = new ObsVertex();
			thisObservedPosMap.put(vx, pt);
		}

		for (AntPath ap : thisParticle.getPaths()){
			Vertex vx = new PathVertex();
			thisAntPathMap.put(vx, ap);
		}

		
		/*
		 * Compute probability graph for this particle.
		 */
		SimpleWeightedGraph<Vertex, DefaultWeightedComparableEdge> probabilityGraph = new SimpleWeightedGraph<Vertex, DefaultWeightedComparableEdge>(DefaultWeightedComparableEdge.class);
		ValueSortedMap<DefaultWeightedComparableEdge, Double> edgeMap = new ValueSortedMap<DefaultWeightedComparableEdge, Double>(true);
		computeLogProbabilityGraph(probabilityGraph,edgeMap,thisObservedPosMap,thisAntPathMap);

//		displayLogProbabilityGraph(probabilityGraph,thisObservedPosMap,thisAntPathMap,vertexSums);
		
		/*
		 * Generate new ant locations based on probability graph.  We do this by sampling the most likely event,
		 * removing this event from the probability graph, updating the prob. graph, then repeating until all 
		 * observations/causes are accounted for. 
		 * 
		 * To do this (relatively) efficiently, we maintain a sorted hash-map of possible observation/cause pairs. 
		 * To avoid having to rescale the entire hash-map every step, we maintain the current sum of weights.
		 *
		 * We're also keeping track of the posterior log-probability of the simulated step.
		 */
		
		double logprob = 0;
		int nfp = 0;
		int nfn = 0;
		
		
		int doOutput = 0;
		while(edgeMap.size() > 0){

			/*
			 * Sample an event from the probability graph.
			 */
			
			DefaultWeightedComparableEdge event = sampleEdge(probabilityGraph,edgeMap);
								
				
			edgeMap.remove(event);
			double edgeWeight = probabilityGraph.getEdgeWeight(event);
			double thisLogProb = edgeWeight;
			logprob = logprob + thisLogProb;

			Vertex v1 = probabilityGraph.getEdgeSource(event);
			Vertex v2 = probabilityGraph.getEdgeTarget(event);
		
			if (doOutput > 0){
				System.err.println("edgeWeight: "+edgeWeight);
				doOutput = outputInterestingStuff(v1,v2,probabilityGraph);
			}
			
			
			if (v1.getClass().equals(ObsVertex.class)){
				assert(v2.getClass().equals(PathVertex.class));
				Vertex tv = v1;
				v1=v2;
				v2=tv;
			}

			/*
			 * Update the probability graph.
			 */
			if (!v1.equals(falsePositive)){
				Set<DefaultWeightedComparableEdge> es = probabilityGraph.edgesOf(v1);
				for(DefaultWeightedComparableEdge ed : es)
					edgeMap.remove(ed);
				boolean tt = probabilityGraph.removeVertex(v1);
				assert(tt);
			}
			else{
				nfp += 1;
				thisParticle.currentFalsePositives.add(thisObservedPosMap.get(v2));
			}
			
			if (!v2.equals(falseNegative)){
				Set<DefaultWeightedComparableEdge> es = probabilityGraph.edgesOf(v2);
				for(DefaultWeightedComparableEdge ed : es)
					edgeMap.remove(ed);
				boolean tt = probabilityGraph.removeVertex(v2);
				assert(tt);
			}
			else
				nfn += 1;

			//Utils.computeVertexSums(probabilityGraph,vertexSums);
			
			/*
			 * Update AntPath trajectory.
			 */
			
			if (!v1.equals(falsePositive)){
				AntPath ap = thisAntPathMap.get(v1);
				assert(ap != null);
				Point obs = thisObservedPosMap.get(v2); 

				Point newPos = new Point();
				double thislp = sampleConditionalPos(ap,obs,newPos) + thisLogProb;
				ap.updatePosition(newPos,obs,thislp);
			}
						
			/*
			 * Compute likelihoods. 
			 */
		}			
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
		private double computeLogProbabilityGraph(SimpleWeightedGraph<Vertex,DefaultWeightedComparableEdge> probabilityGraph,
				ValueSortedMap<DefaultWeightedComparableEdge, Double> edgeMap,
				HashMap<Vertex, Point> thisObservedPosMap,
				HashMap<Vertex, AntPath> thisParticlePathMap) {
			/**
			 * Initialize vertices.
			 */
			edgeMap.clear();
			
			
			probabilityGraph.addVertex(falseNegative);
			probabilityGraph.addVertex(falsePositive);
			
			for (Vertex v : thisObservedPosMap.keySet()){
				probabilityGraph.addVertex(v);
				DefaultWeightedComparableEdge edge = probabilityGraph.addEdge(v,falsePositive);
				probabilityGraph.setEdgeWeight(edge,falsePositiveLogProb);
				edgeMap.put(edge, falsePositiveLogProb);
			}
			for (Vertex v : thisParticlePathMap.keySet()){
				probabilityGraph.addVertex(v);
				DefaultWeightedComparableEdge edge = probabilityGraph.addEdge(v,falseNegative);
				probabilityGraph.setEdgeWeight(edge,falseNegativeLogProb);
				edgeMap.put(edge, falseNegativeLogProb);
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
						edgeMap.put(edge,logprob);
					}
				}
			}
			

			/**
			 * Adjust probabilities according to log-probability of an AntPath existing.
			 */
			for (Entry<Vertex, AntPath> antEntry: thisParticlePathMap.entrySet()){
				Set<DefaultWeightedComparableEdge> edge = probabilityGraph.edgesOf(antEntry.getKey());
				double antProb = antEntry.getValue().getCurrentLogProb();
				for (DefaultWeightedComparableEdge e: edge){
				 double ew = probabilityGraph.getEdgeWeight(e);
				 probabilityGraph.setEdgeWeight(e,ew+antProb);
				//edgeMap.remove(edge);
				 edgeMap.put(e, ew+antProb);	
				}
			}

			
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
			double totalLogProb = Utils.maxstar(edgeMap);

			return totalLogProb;
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

		private int outputInterestingStuff(
				Vertex v1,
				Vertex v2,
				SimpleWeightedGraph<Vertex, DefaultWeightedComparableEdge> probabilityGraph) {

			for (DefaultWeightedComparableEdge e: probabilityGraph.edgesOf(v1)){
				System.err.print(probabilityGraph.getEdgeWeight(e)+"\t");
			}

			System.err.println();
			for (DefaultWeightedComparableEdge e: probabilityGraph.edgesOf(v2)){
				System.err.print(probabilityGraph.getEdgeWeight(e)+"\t");
			}
			System.err.println();

			JFrame frame = new JFrame("Continue?");
			frame.pack();
			frame.setVisible(false);
			return JOptionPane.showConfirmDialog(frame,"hey","there",2);	
		}

		private double sampleConditionalPos(AntPath ap, Point obs, Point newPos) {
			Point projPos = ap.getCurrentProjectedPosition();
			if (obs == null){
				return Utils.sampleGaussian(projPos,sigma2ant,newPos);
			}
			else{
				return Utils.sampleGaussian(obs,sigma2ant,newPos);
			}
		}

//		private void displayLogProbabilityGraph(
//				SimpleWeightedGraph<Vertex, DefaultWeightedComparableEdge> probabilityGraph,
//				HashMap<Vertex, Point> thisObservedPosMap,
//				HashMap<Vertex, AntPath> thisAntPathMap,
//				HashMap<Vertex, Double> vertexSums) {
//
//
//		}

}