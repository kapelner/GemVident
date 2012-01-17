package GemVidentTracking;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

import org.jgrapht.graph.SimpleWeightedGraph;

import GemIdentTools.ValueSortedMap;


public class Utils {
	public static Random rand = new Random();

	public static double computeSum(Collection<Double> values) {
		double sum = 0;
		for(Double v : values){
			sum += v;
		}
		return sum;
	}
	/**
	 * Implements Knuth shuffle on an ArrayList
	 * @param <V>
	 * @param deck
	 */
	public static <V> void shuffle(ArrayList<V> deck){
		int len = deck.size();
		for (int i = 0; i < len-1;i++){
			int j = rand.nextInt(len-i);
			V temp = deck.get(j);
			deck.set(j, deck.get(i));
			deck.set(i,temp);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Comparable> T sampleMap(ValueSortedMap<T,Double> edgeMap, double weightSum) {
		double cumsum = 0;

		T sample = null;
		
		Iterator<T> it = edgeMap.keyIterator();
		assert(it.hasNext());
		double U = weightSum*rand.nextDouble();
		while(cumsum < U && it.hasNext()){
			sample = it.next();
			cumsum += Math.exp(-edgeMap.get(sample));
		}
		return sample;
	}
	@SuppressWarnings("unchecked")
	public static <T extends Comparable> T sampleMap(ValueSortedMap<T,Double> edgeMap) {
		return sampleMap(edgeMap,Math.exp(maxstar(edgeMap)));
	}

	public static double sampleGaussian(Point avgPos, double sigma2, Point newPos) {
		newPos.x = (int) (avgPos.x + rand.nextGaussian()*Math.sqrt(sigma2));
		newPos.y = (int) (avgPos.y + rand.nextGaussian()*Math.sqrt(sigma2));
		
		double normConst = Math.log(1/Math.sqrt(2*Math.PI*sigma2));
		
		double dist = computeDistance(avgPos,newPos);
		double logprob =  normConst - Math.pow(dist,2)/(2*sigma2);
		return logprob;
	}

	public static double computeDistance(Point pt1, Point pt2) {
		double distance = 0;
		distance += Math.pow(pt1.x-pt2.x,2);
		distance += Math.pow(pt1.y-pt2.y,2);
		distance = Math.sqrt(distance);
		return distance;
	}

	public static <V,E>  void computeVertexSums(
			SimpleWeightedGraph<V, E> g,
			HashMap<V, Double> vertexSums) {
		vertexSums.clear();
		/**
		 * Compute vertex weight sums.  Since we are storing the log-probability of each event,
		 * we need to take the exponent first.
		 */
		for (V v : g.vertexSet()){
			double thisSum = 0; 
			Set<E> edges = g.edgesOf(v);
			for(E edge: edges){
				thisSum += Math.exp(g.getEdgeWeight(edge));
			}
			vertexSums.put(v, thisSum);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <V extends Comparable> double maxstar(ValueSortedMap<V, Double> edgeMap) {
		SortedSet<ValueSortedMap<V,Double>.KV> ks = edgeMap.keySet();
		double M = ks.first().value;
		double expsum = 0;
		for (ValueSortedMap<V,Double>.KV key:ks){
			expsum += Math.exp(key.value-M);
		}
		
		return M+Math.log(expsum);
	}
	public static double maxstar(Collection<Double> col) {
		
		double M = Double.MIN_VALUE;
		double expsum = 0;
		for (Double val:col){
			if (val > M)
				M = val;
		}
		for (Double val:col){
			expsum += Math.exp(val-M);
		}
		
		return M+Math.log(expsum);
	}
}
