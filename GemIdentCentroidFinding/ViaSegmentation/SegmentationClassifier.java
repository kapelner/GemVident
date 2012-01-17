package GemIdentCentroidFinding.ViaSegmentation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import GemIdentCentroidFinding.CentroidFinder;
import GemIdentCentroidFinding.PostProcess.BooleanFlag;
import GemIdentOperations.Run;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentView.KClassifyPanel;

public class SegmentationClassifier extends CentroidFinder {
	
	public SegmentationClassifier(HashMap<String,HashMap<String,BoolMatrix>> allTrainingIs,Set<String> postProcessSet,KClassifyPanel classifyPanel, BooleanFlag stop){
		super(allTrainingIs, postProcessSet, classifyPanel, stop);
	}
	
	protected void GetEstimatedCentersFromSplitBlob(UnivariateDistribution uniDist,BoolMatrix centroids,ArrayList<Point> blob,MiniDatum d,String phenotype, String filename){
		for (Point c : GetEstimatedCenters(uniDist,blob, 0, phenotype, filename))
			centroids.set(c,true);		
	}
	
	protected ArrayList<Point> GetEstimatedCenters(UnivariateDistribution uniDist, ArrayList<Point> blob,int numCenters,String phenotype, String filename){
		
		LinkedList<ArrayList<Point>> ll = new LinkedList<ArrayList<Point>>();
		ll.add(blob);
		
		UnivariateDistribution thisDist = null;

		if (Run.it.getPhenotypeDistMap() != null && Run.it.getPhenotypeDistMap().get(phenotype)!=null)
			thisDist = Run.it.getPhenotypeDistMap().get(phenotype);
		else
			thisDist = uniDistributions.get(phenotype).get(0);
		
		
//		System.out.println("CutoffTop: " + thisDist.getCutoffTop());
//		System.out.println("CutoffMid: " + thisDist.getCutoffMid());
//		System.out.println("CutoffBottom: " + thisDist.getCutoffBottom());
		
		return ImageSegmentation.findBlobCentroids(ll,thisDist.getCutoffTop(),thisDist.getCutoffMid(),thisDist.getCutoffBottom());
	}	

}
