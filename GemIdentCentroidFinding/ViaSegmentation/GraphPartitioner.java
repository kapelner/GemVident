package GemIdentCentroidFinding.ViaSegmentation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.sparse.CG;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.IterativeSolverNotConvergedException;

import org.jgrapht.graph.SimpleGraph;


public abstract class GraphPartitioner{
	protected static int displayLevel = 0;
	private static final double QUALITY_THRESHOLD = .06;
	protected static final int PART_MAX_ITER = 10000;
//	private static final int QUALITY_TYPE = 1;
	private static final int QUALITY_TYPE = 2;
	/**
	 * quality can be measured as edge expansion (QUALITY_TYPE=1),
	 * 	   $min_{1\leq |S| \leq n/2} c(S, \bar S)/|S|$,
	 * or as conductance (QUALITY_TYPE=2),
	 * 	   $min_{1\leq |S| \leq n/2} c(S, \bar S)/Vol(S),$ where
	 * $Vol(S)$ is the number of edges in the subgraph induced by $S$.
	 */
	public double quality = Double.MAX_VALUE;
	public DenseVector bestPar = null;	
	
	public abstract <V,E> Set<V> partition(SimpleGraph<V, E> G);
	public abstract <V,E> Set<V> partition(GraphUtils.AdjacencyMatrix<V> L);
	/**
	 * Finds the quality of a cut with respect to QUALITY_TYPE. 
	 *
	 * QUALITY_TYPE = 1 corresponds to edge expansion, which is defined
	 * as $\frac{c(S,\bar S)}{\min(|S|,|\bar S|)}$.
	 * 
	 * QUALITY_TYPE = 2 corresponds to conductance, which is defined
	 * as $\frac{c(S,\bar S)}{Vol(S),Vol(\bar S))}$, where $Vol(S)$ is the
	 * number of edges between vertices of $S$.
	 *  
	 * @param partition		the cut -- for each element 1 if present 0 otherwise
	 * @param L				a graph Laplacian matrix
	 * @return
	 */
	protected double cutQuality(DenseVector partition, CompRowMatrix L){
		double partitionVolume = 0;
		DenseVector y = new DenseVector(partition.size());
		double thisCut = L.mult(partition,y).dot(partition);

		double thisVolume = 0;
		for (int i = 0;i< partition.size();i++)
			partitionVolume = partitionVolume + partition.getData()[i];	
		
		if (QUALITY_TYPE == 1){
			thisVolume = Math.min(partitionVolume, partition.size()-partitionVolume);
		}
		else if (QUALITY_TYPE == 2){
			double degSum = 0;
			if (2*partitionVolume > partition.size()){
				for (int i = 0;i< partition.size();i++){
					if (partition.getData()[i]==0)
						degSum= degSum + L.get(i,i);
				}	
			}
			else{
				for (int i = 0;i< partition.size();i++){
					if (partition.getData()[i]>0)
						degSum= degSum + L.get(i,i);
				}					
			}
			thisVolume = degSum-thisCut;
		}
		thisVolume = thisVolume * Math.pow(Math.log(partition.size()),1/3);
		if (thisVolume == 0)
			return partition.size()*partition.size();
		else
			return thisCut/thisVolume;
	}

/**
 * Find best threshold value through exhaustive search.	
 */
	protected double getBestThreshold(double [] x, CompRowMatrix L){

		double threshold = 0;
		int nnodes = x.length;
		SortedMap<Double,Integer> vv = new TreeMap<Double,Integer>();
		for (int i = 0; i < nnodes;i++){
			vv.put(x[i],i);
		}
						
		DenseVector p = new DenseVector(nnodes);
		Arrays.fill(p.getData(),0);
		double bestVal = nnodes*nnodes;
		Iterator<Entry<Double, Integer>> it = vv.entrySet().iterator();
		while (it.hasNext()){
			Entry<Double, Integer> entry = it.next();
			if (!it.hasNext()) break;
			p.set(entry.getValue(),1);
			double thisVal = cutQuality(p,L);
			if(thisVal <= bestVal){
				bestVal = thisVal;
				bestPar = p.copy();
				threshold = entry.getKey();
			}
		}
		
		quality = bestVal;
		
		
		return threshold;
	}
	
	public boolean passesQualityThreshold(){
		int nnodes = bestPar.size();
		/*
		 * worstAsymptoticQuality is for 2-cube with nnodes nodes.
		 * cut is $2\sqrt(N)/\pi)$
		 * volume is $N/2$  
		 */
		double worstAsymptoticQuality = 1e5;
		if (QUALITY_TYPE == 1){
			worstAsymptoticQuality = 6/Math.sqrt(nnodes);
		}
		else{
			worstAsymptoticQuality = 6/Math.sqrt(nnodes);		
		}
		
//		boolean pqt =(quality < worstAsymptoticQuality*QUALITY_THRESHOLD);
		boolean pqt =(quality < QUALITY_THRESHOLD);
		if(displayLevel > 1){
			System.out.println("Worst asymptotic quality: "+worstAsymptoticQuality);

			System.out.println("This quality: "+ quality);
			System.out.println("Ratio: " + quality/worstAsymptoticQuality);
			System.out.println("Passes quality threshold: " + pqt);
		}

		return pqt;
	}
	
}


class IsoperimetricPartition extends GraphPartitioner{
	public <V,E> Set<V> partition(SimpleGraph<V, E> G){
		return partition(GraphUtils.graphLaplacianMatrix(G));
	}
	public <V,E> Set<V> partition(GraphUtils.AdjacencyMatrix<V> L){
		CompRowMatrix A = L.A.copy();
		
        int nnodes = L.vertices.size();
        //Choose row to zero at random
        int row = (int) (nnodes*Math.random());

        //Set this row/column to zero
        //int [] thisRow = Arrays.copyOfRange(A.getColumnIndices(), A.getRowPointers()[row], A.getRowPointers()[row + 1]);
        for(int i = A.getRowPointers()[row];i < A.getRowPointers()[row+1]; i++){
			A.set(A.getColumnIndices()[i],row,0);
			A.set(row,A.getColumnIndices()[i],0);
		}
		A.set(row,row,1);

		
		//Make sure A has no zero eigenvalues
		for (int i = 0;i<nnodes;i++){
			A.set(i,i,A.get(i,i)+.0001);
		}
			
		double[] tempvec = new double[nnodes];
		for (int i = 0;i < nnodes;i++)
			tempvec[i] = 1;
		DenseVector rhs = new DenseVector(tempvec);
		CG cg = new CG(rhs);
		DenseVector x = rhs.copy();
		try {
			x = (DenseVector) cg.solve(A, x, rhs);
		} catch (IterativeSolverNotConvergedException e) {
			e.printStackTrace();
		}

		if (displayLevel > 4)
			System.out.println("CG residual: " + cg.getIterationMonitor().residual());


		getBestThreshold(x.getData(),L.A);

		
		bestPar.set(row,0);
		double v1 = cutQuality(bestPar,L.A);
		bestPar.set(row,1);
		double v2 = cutQuality(bestPar,L.A);
		
		if (v1 < v2) bestPar.set(row,0);
		
		Set<V> partition = new HashSet<V>();
		for (int i=0;i<nnodes;i++){
			if (bestPar.get(i) >  0)
				partition.add(L.vertices.get(i));
		}		
		
		return partition;
	}

}


class SpectralPartition extends GraphPartitioner{
	public <V,E> Set<V> partition(SimpleGraph<V, E> G){
		//GraphUtils.AdjacencyMatrix<V> L = GraphUtils.graphNormalizedLaplacianMatrix(G);
		GraphUtils.AdjacencyMatrix<V> L = GraphUtils.graphLaplacianMatrix(G);
		return partition(L);
	}	

	public <V,E> Set<V> partition(GraphUtils.AdjacencyMatrix<V> L){	
		int nnodes = L.vertices.size();

		
		//get second eigenvalue and eigenvector
		double[] tempvec = new double[nnodes];
		for (int i = 0;i < nnodes;i++)
			tempvec[i] = Math.random();
		DenseVector x = new DenseVector(tempvec);

		for (int i = 0;i < nnodes;i++)
			tempvec[i] = 1;

		DenseVector e = new DenseVector(tempvec);
		double tol = 1e-10;
		double err = 1.;
		int numiter = 0;
		while (err > tol  && numiter < PART_MAX_ITER){
			numiter = numiter + 1;
			DenseVector t = new DenseVector(e);
			//use power method to find second eigenvector
			double sumx = 0;
			for (int i = 0; i < x.size();i++)
				sumx = sumx+x.get(i);
			t = t.scale(sumx);
			t=(DenseVector) t.add(-nnodes,x);
			t = (DenseVector) L.A.multAdd(1,x,t);

			t = t.scale(-1/Math.sqrt(t.dot(t)));
			//System.out.println("t1: " +t.dot(t)+" ");
			DenseVector resid = new DenseVector(t);
			resid = (DenseVector) resid.add(-1,x);
			err = Math.sqrt(resid.dot(resid));
			x = new DenseVector(t);
			//System.out.println("err: " + err + " " + x.dot(x));
		}
		System.out.println("err: " + err);
		//System.out.println();
		
		//get best threshold value
		for (int i = 0;i<nnodes;i++){
			tempvec[i] = x.get(i);
		}
		//System.out.println(Arrays.toString(tempvec));


		double threshold = getBestThreshold(tempvec, L.A);
		//double threshold = 0;

		Set<V> partition = new HashSet<V>();
		for (int i=0;i<nnodes;i++){
			if (tempvec[i] <= threshold)
				partition.add(L.vertices.get(i));
		}

		
		return partition;
		
	}

}
