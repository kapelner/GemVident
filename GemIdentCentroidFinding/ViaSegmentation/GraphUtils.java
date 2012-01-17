package GemIdentCentroidFinding.ViaSegmentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.CompRowMatrix;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

public class GraphUtils{
	@SuppressWarnings("unchecked")
	public static class Vertex extends Object implements Comparable{

		public int compareTo(Object o) {
			// TODO Auto-generated method stub
			int val = ((Integer) hashCode()).compareTo((Integer) o.hashCode());
			assert(!(hashCode() == o.hashCode() && this != o));

			return val;
		}};

	public static class AdjacencyMatrix<V>{
		public CompRowMatrix A;
		public Vector<V> vertices;
	}
	
	public static <V, E> Set<V> neighborsOf(Graph<V,E> G, V v){
		Set<V> neighbors = new HashSet<V>();
		Set<E> es = G.edgesOf(v);
		Iterator<E> it = es.iterator();
		while (it.hasNext()){
			E edge = it.next();
			V v1 = G.getEdgeSource(edge);
			V v2 = G.getEdgeTarget(edge);
			if (v1 != v) neighbors.add(v1);
			if (v2 != v) neighbors.add(v2);
		}
		return neighbors;
	}
	
	@SuppressWarnings("unchecked")
	public static <V, E> ArrayList<Set<V>> components(Graph<V,E> G){
		HashSet<V> vertices = new HashSet<V>(G.vertexSet());

		//ArrayList<Subgraph<V,E, SimpleGraph<V,E>>> componentGraphs = new ArrayList<Subgraph<V,E, SimpleGraph<V,E>>>();  
		ArrayList<Set<V>> componentVertices = new ArrayList<Set<V>>();  
		
		while (vertices.size() != 0) {
			
			Set<V> thisCompVertices = new HashSet<V>();
			V v = vertices.iterator().next();
			
			thisCompVertices.add(v);

			LinkedList<V> nextVertices = new LinkedList<V>();
			nextVertices.addAll(neighborsOf(G, v));
			while (nextVertices.size() != 0) {
				v = nextVertices.poll();
				if (!thisCompVertices.contains(v)) {
					thisCompVertices.add(v);
					nextVertices.addAll(neighborsOf(G, v));
				}
			}

			componentVertices.add(thisCompVertices);
			vertices.removeAll(thisCompVertices);
			HashSet<V> v2 = (HashSet<V>) vertices.clone();
			vertices = v2;
		}
		
		return componentVertices;
	}

	public static <V, E> AdjacencyMatrix<V> graphAdjacencyMatrix(SimpleGraph<V,E> G){
		Vector<V> vertices = new Vector<V>(G.vertexSet());
		int nnodes = vertices.size();
		HashMap<V, Integer> indexMap = new HashMap<V,Integer>();
		Vector<SortedSet<Integer>> rows = new Vector<SortedSet<Integer>>(nnodes);

		for (int i =0;i < nnodes;i++){
			indexMap.put(vertices.get(i),i);
			rows.add(new TreeSet<Integer>());
		}
		
		
		//construct non-zero structure
		Iterator<E> edgeIt = G.edgeSet().iterator();
		while (edgeIt.hasNext()){
			E thisEdge = edgeIt.next();
			Integer v1Ind = indexMap.get(G.getEdgeSource(thisEdge));
			Integer v2Ind = indexMap.get(G.getEdgeTarget(thisEdge));
			rows.get(v1Ind).add(v2Ind);
			rows.get(v2Ind).add(v1Ind);
		}
		
		int [][] nz = new int[nnodes][];
		for(int i = 0;i<nnodes;i++){
			nz[i] = new int[rows.get(i).size()];
			Iterator<Integer> it = rows.get(i).iterator();
			int j = 0;
			while (it.hasNext()){
				nz[i][j] = (int)it.next();
				j++;
			}
		}
		
		//initialize sparse matrix
		CompRowMatrix A = new CompRowMatrix(nnodes,nnodes,nz);
		
		//set edge weights
		edgeIt = G.edgeSet().iterator();
		while (edgeIt.hasNext()){
			E thisEdge = edgeIt.next();
			double wt = G.getEdgeWeight(thisEdge);
			Integer v1Ind = indexMap.get(G.getEdgeSource(thisEdge));
			Integer v2Ind = indexMap.get(G.getEdgeTarget(thisEdge));
			A.set(v1Ind,v2Ind,wt);
			A.set(v2Ind,v1Ind,wt);			
		}

		AdjacencyMatrix<V> adj = new AdjacencyMatrix<V>();
		adj.A = A;
		adj.vertices = vertices;
		
		return adj;		
	}
	
	
	public static <V, E> AdjacencyMatrix<V> graphLaplacianMatrix(SimpleGraph<V,E> G){
		Vector<V> vertices = new Vector<V>(G.vertexSet());
		int nnodes = vertices.size();
		HashMap<V, Integer> indexMap = new HashMap<V,Integer>();
		Vector<SortedSet<Integer>> rows = new Vector<SortedSet<Integer>>(nnodes);
		double [] rowSums = new double[nnodes];
		
		for (int i =0;i < nnodes;i++){
			indexMap.put(vertices.get(i),i);
			SortedSet<Integer> thisRow = new TreeSet<Integer>();
			thisRow.add(i);
			rows.add(thisRow);
			rowSums[i] = 0;
		}
		
		
		//construct non-zero structure
		Iterator<E> edgeIt = G.edgeSet().iterator();
		while (edgeIt.hasNext()){
			E thisEdge = edgeIt.next();
			Integer v1Ind = indexMap.get(G.getEdgeSource(thisEdge));
			Integer v2Ind = indexMap.get(G.getEdgeTarget(thisEdge));
			rows.get(v1Ind).add(v2Ind);
			rows.get(v2Ind).add(v1Ind);
		}
		
		int [][] nz = new int[nnodes][];
		for(int i = 0;i<nnodes;i++){
			nz[i] = new int[rows.get(i).size()];
			Iterator<Integer> it = rows.get(i).iterator();
			int j = 0;
			while (it.hasNext()){
				nz[i][j] = (int)it.next();
				j++;
				//System.out.print(".");
			}
			//System.out.println(Arrays.toString(nz[i]));
		}
		
		
		
		//initialize sparse matrix
		CompRowMatrix A = new CompRowMatrix(nnodes,nnodes,nz);
		
		//set edge weights
		edgeIt = G.edgeSet().iterator();
		while (edgeIt.hasNext()){
			E thisEdge = edgeIt.next();
			double wt = G.getEdgeWeight(thisEdge);
			Integer v1Ind = indexMap.get(G.getEdgeSource(thisEdge));
			Integer v2Ind = indexMap.get(G.getEdgeTarget(thisEdge));
			A.set(v1Ind,v2Ind,-wt);
			A.set(v2Ind,v1Ind,-wt);
			rowSums[v1Ind] += wt;
			rowSums[v2Ind] += wt;
		}
		
		for (int i = 0;i < nnodes;i++){
			A.set(i,i,rowSums[i]);
		}
				
		
		AdjacencyMatrix<V> adj = new AdjacencyMatrix<V>();
		adj.A = A;
		adj.vertices = vertices;

		//System.out.println(Arrays.toString(A.getData()));
		
		return adj;		
	}
	public static <V, E> AdjacencyMatrix<V> graphNormalizedLaplacianMatrix(SimpleGraph<V,E> G){
		Vector<V> vertices = new Vector<V>(G.vertexSet());
		int nnodes = vertices.size();
		HashMap<V, Integer> indexMap = new HashMap<V,Integer>();
		Vector<SortedSet<Integer>> rows = new Vector<SortedSet<Integer>>(nnodes);
		double [] rowSums = new double[nnodes];
		
		for (int i =0;i < nnodes;i++){
			indexMap.put(vertices.get(i),i);
			SortedSet<Integer> thisRow = new TreeSet<Integer>();
			thisRow.add(i);
			rows.add(thisRow);
			rowSums[i] = 0;
		}
		
		//construct non-zero structure
		Iterator<E> edgeIt = G.edgeSet().iterator();
		while (edgeIt.hasNext()){
			E thisEdge = edgeIt.next();
			Integer v1Ind = indexMap.get(G.getEdgeSource(thisEdge));
			Integer v2Ind = indexMap.get(G.getEdgeTarget(thisEdge));
			rows.get(v1Ind).add(v2Ind);
			rows.get(v2Ind).add(v1Ind);
		}
		
		int [][] nz = new int[nnodes][];
		for(int i = 0;i<nnodes;i++){
			nz[i] = new int[rows.get(i).size()];
			Iterator<Integer> it = rows.get(i).iterator();
			int j = 0;
			while (it.hasNext()){
				nz[i][j] = (int)it.next();
				j++;
				//System.out.print(".");
			}
			//System.out.println(Arrays.toString(nz[i]));
		}		
		
		//initialize sparse matrix
		CompRowMatrix A = new CompRowMatrix(nnodes,nnodes,nz);
		
		//set edge weights
		edgeIt = G.edgeSet().iterator();
		while (edgeIt.hasNext()){
			E thisEdge = edgeIt.next();
			double wt = G.getEdgeWeight(thisEdge);
			Integer v1Ind = indexMap.get(G.getEdgeSource(thisEdge));
			Integer v2Ind = indexMap.get(G.getEdgeTarget(thisEdge));
			A.set(v1Ind,v2Ind,-1);
			A.set(v2Ind,v1Ind,-1);
			rowSums[v1Ind] += wt;
			rowSums[v2Ind] += wt;
		}
		
		Iterator<MatrixEntry> it = A.iterator();
		while (it.hasNext()){
			MatrixEntry thisME = it.next();
			thisME.set(-1/Math.sqrt(rowSums[thisME.row()]*rowSums[thisME.column()]));
		}
		
		for (int i = 0;i < nnodes;i++){
			A.set(i,i,1);
		}
				
		
		AdjacencyMatrix<V> adj = new AdjacencyMatrix<V>();
		adj.A = A;
		adj.vertices = vertices;

		//System.out.println(Arrays.toString(A.getData()));
		
		return adj;		
	}
}