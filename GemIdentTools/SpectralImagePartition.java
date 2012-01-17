
package GemIdentTools;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.EVD;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.NotConvergedException;
import no.uib.cipr.matrix.sparse.CompColMatrix;
import GemIdentImageSets.RegularSubImage;
import GemIdentTools.Matrices.ShortMatrix;

/**
 * Given a nxm image matrix, this class first generates a sparse 
 * distance matrix between pixels, then performs.
 * 
 * @author Adam Guetz
 *
 */
public class SpectralImagePartition {
	ShortMatrix image;
	private HashMap<Integer,Integer> nzMap;
	private int nnodes;
	private int width;
	private int height;
	private CompColMatrix laplacianMatrix;
	private double quality;
	private double[] tempVec;
	private final double threshold = 10;
	private double [] evalues;
	private DenseMatrix evectors;
	private int evcount;
	
	public static class SparseVector{
		public SortedSet<Integer> nonzeros;
		public SparseVector(){
			nonzeros = new TreeSet<Integer>();
		}
	};
	
	/**
	 * Create a sparse double matrix in MTJ's CCM format.
	 * @param adjacencyList 	Adjacency list in HashMap form.
	 * @return					the matrix
	 */
	public static Matrix createSparseMatrix(HashMap<Point,Double> adjacencyList){

		HashMap<Integer, SparseVector> nzEntries = new HashMap<Integer, SparseVector>();

		Iterator<Map.Entry<Point, Double>> it = adjacencyList.entrySet().iterator();
		
		while (it.hasNext()){
			Map.Entry<Point,Double> entry = it.next();
			Point pt = entry.getKey();
			if (!nzEntries.containsKey(pt.x)){
				nzEntries.put(pt.x, new SparseVector());
			}
			nzEntries.get(pt.x).nonzeros.add(pt.y);
		}
		
		int nnodes = nzEntries.size();
		
		int [][] nz = new int[nnodes][];
		for(int i = 0;i<nnodes;i++){			
			SparseVector thisCol = nzEntries.get(i);
			if (thisCol != null){
				nz[i] = new int[thisCol.nonzeros.size()];
				Iterator <Integer> it2 = thisCol.nonzeros.iterator();
				int j = 0;
				while (it2.hasNext()){
					nz[i][j] = it2.next();
					j++;
				}
			}
		}
		
		CompColMatrix spmat = new CompColMatrix(nnodes,nnodes, nz);
		
		return spmat;
	}
	
	public SpectralImagePartition(RegularSubImage _image){
		width = _image.getWidth();
		height = _image.getHeight();
		ShortMatrix img = new ShortMatrix(width,height,(short)0);
		for (int i=0;i<width;i++){
			for (int j=0;j<height;j++){
				int r = _image.getR(i, j);
				int g = _image.getG(i, j);
				int b = _image.getB(i, j);
				double intensity = (r + g + b)/3;
				if (intensity < threshold)
					img.set(i,j,1);
			}
		}
		initialize(img);
	}
	public SpectralImagePartition(ShortMatrix _image){
		initialize(_image);
	}
	
	private int getIndex(int x, int y){
		return x+y*width;
	}

	@SuppressWarnings("unused")
	private int getIndex(Point pt){
		return getIndex(pt.x,pt.y);
	}
	
	private void initialize(ShortMatrix _image){
		image = _image;
		
		evcount = -1;
		width = image.getWidth();
		height = image.getHeight();
		
		nzMap = new HashMap<Integer,Integer>();
		
		/* Build adjacency matrix */
		
			/* First construct nonzero column structure */
		Vector<Vector<Integer>> nzArray = new Vector<Vector<Integer>>();
		Vector<Double> diagVector = new Vector<Double>(nnodes);
		for(int i = 0;i<width;i++){
			for (int j = 0;j<height;j++){
				int thisValue = image.get(i,j);
				if (thisValue != 0){
					SortedSet<Integer> entries = new TreeSet<Integer>();
					if (j > 0 && image.get(i,j-1)!=0) entries.add(getIndex(i,j-1));
					if (i > 0 && image.get(i-1,j)!=0) entries.add(getIndex(i-1,j));
					//entries.add(getIndex(i,j));
					if (i < width-1 && image.get(i+1,j)!=0) entries.add(getIndex(i+1,j));
					if (j < height-1 && image.get(i,j+1)!=0) entries.add(getIndex(i,j+1));
					nzMap.put(getIndex(i,j), nzArray.size());
					diagVector.add(1/Math.sqrt(entries.size()-1));
					nzArray.add(new Vector<Integer>(entries));
				}
			}
		}

		nnodes = nzArray.size();
		
		int [][] nz = new int[nnodes][];
		for(int i = 0;i<nnodes;i++){
			Vector<Integer> thisCol = nzArray.get(i);
			nz[i] = new int[thisCol.size()];
			for (int j = 0;j<thisCol.size();j++){
				nz[i][j] = nzMap.get(thisCol.get(j));
			}
		}
		
		laplacianMatrix = new CompColMatrix(nnodes,nnodes, nz);
		
			/* Now fill in adjacency data */

		Iterator<MatrixEntry> it = laplacianMatrix.iterator();
		while(it.hasNext()){
			MatrixEntry thisEntry = it.next();
			if (thisEntry.row() == thisEntry.column())
				thisEntry.set(1);
			else
				//thisEntry.set(-diagVector.get(thisEntry.row())*diagVector.get(thisEntry.column()));
				thisEntry.set(1);
		}
		
		System.out.println(laplacianMatrix.toString());
//		Iterator<Map.Entry<Integer,Integer>> it2 = nzMap.entrySet().iterator();
//		while(it2.hasNext()){
//			Map.Entry<Integer,Integer> entry = it2.next();
//			System.out.println(entry.getKey() + "\t" + entry.getValue());
//		}
		System.out.println("width: " + width);
		System.out.println("height: " + height);
		//System.out.println(nzMap);
		EVD evd = new EVD(nnodes);
		try {
			evd = EVD.factorize((Matrix)laplacianMatrix);
		} catch (NotConvergedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		evalues = evd.getRealEigenvalues();
		//Arrays.sort(evalues);
		quality = evalues[1];
		System.out.println("Partition quality: " + quality);
		
		evectors = evd.getLeftEigenvectors();
		
		
		FileWriter of;
		try {
			of = new FileWriter(new File("eMat.ascii"));
			of.write(evectors.toString());
			of.close();
			File f = new File("eMat.ascii");
			System.out.println(f.getAbsolutePath());
			
			of = new FileWriter(new File("adjMat.ascii"));
			of.write(Arrays.toString(evalues));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		tempVec = new double[nnodes];
		for(int i = 0;i<nnodes;i++)
			tempVec[i] = evectors.get(i,1);

		
		//System.out.println(Arrays.toString(tempVec));
		//MatrixVectorWriter mvw;
	}
	public double getQuality(){
		return quality;
	}
	
	public ShortMatrix getPartitionAsShortMatrix(){
		ShortMatrix newimg = new ShortMatrix(width,height,(short) 0);
		for(int i = 0;i<width;i++){
			for(int j = 0;j<height;j++){
				if (nzMap.containsKey(getIndex(i,j))){
					double pval = tempVec[nzMap.get(getIndex(i,j))];
					if (pval < 0)
						newimg.set(i,j,1);
					else
						newimg.set(i,j,2);
				}
			}
		}
		return newimg;
	}
	
	public RegularSubImage getPartitionAsRegularSubImage(){

		RegularSubImage newimg = new RegularSubImage(IOTools.InitializeImage(image.getWidth(), image.getHeight(), null, null));
		for(int i = 0;i<width;i++){
			for(int j = 0;j<height;j++){
				if (nzMap.containsKey(getIndex(i,j))){
					double pval = tempVec[nzMap.get(getIndex(i,j))];
					if (pval < 0)
						newimg.setPixel(i,j,Color.BLUE);
					else
						newimg.setPixel(i,j,Color.RED);
				}
			}
		}
		
		return newimg;
	}

	public RegularSubImage displayEigenvectorAsRegularSubImage(){
		evcount = (evcount + 1) % 5;
		System.out.println("EVector #"+ evcount + ":" + evalues[evcount]);
		return displayEigenvectorAsRegularSubImage(evcount);
	}
	public RegularSubImage displayEigenvectorAsRegularSubImage(int evindex){

		Vector<Double> tv = new Vector<Double>(nnodes);
	
		for (int i = 0;i<nnodes;i++){
			//tv.add(evectors.get(evindex,i));
			tv.add(evectors.get(i,evindex));
		}
		
		System.out.println(Arrays.toString(tv.toArray()));
		
		
		SortedSet<Double> temp = new TreeSet<Double>(tv);
		double maxIntensity =  Math.max(Math.abs(temp.last()),Math.abs(temp.first()));
		
		//double mult =2*Math.PI/maxIntensity;
		double mult = 1/maxIntensity;
		
		RegularSubImage newimg = new RegularSubImage(IOTools.InitializeImage(image.getWidth(), image.getHeight(), null, null));
		for(int i = 0;i<width;i++){
			for(int j = 0;j<height;j++){
				if (nzMap.containsKey(i+j*height)){
					int index = nzMap.get(i+j*height);

					double cval = tv.get(index) * mult;
					
					float r = 0;
					float g = 0;
					float b = 0;
					
					if(cval < 0)
						b = 1;
					else
						r = 1;

					r = r* (float)Math.abs(cval);
					g = g* (float)Math.abs(cval);
					b = b* (float)Math.abs(cval);
					
					newimg.setPixel(i,j,new Color(r,g,b));
				}
			}
		}
		
		return newimg;
	}

/*

	private static CompColMatrix makeDiagCompColMatrix(Vector<Double> diagVector) {
		int n = diagVector.size();
		int [][] nz = new int[n][];
		for(int i=0;i<n;i++){
			nz[i] = new int[1];
			nz[i][0] = i;
		}
		CompColMatrix A = new CompColMatrix(n,n,nz);
		
		for(int i=0;i<n;i++){
			A.set(i,i,diagVector.get(i));
		}
		
		return A;
	}


	private static CompColMatrix makeEyeCompColMatrix(int n) {
		Vector<Double> diagVector = new Vector<Double>(n);
		for (int i = 0;i<n;i++)
			diagVector.set(i,1.);
		return makeDiagCompColMatrix(diagVector);
	}
	*/
	
}