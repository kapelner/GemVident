
package GemIdentStatistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import GemIdentClassificationEngine.DatumSetup;
import GemIdentOperations.Run;
import GemIdentTools.Matrices.ShortMatrix;
import GemIdentView.JProgressBarAndLabel;

/**
 * Houses a Breiman Random Forest
 * 
 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm">Breiman's Random Forests (UC Berkeley)</a>
 * 
 * @author Adam Kapelner
 */
public class RandomForest extends ClassifierThatCanHandleConfusion implements Serializable {
	private static final long serialVersionUID = -8073610839472157131L;
	
	/** the collection of the forest's decision trees */
	private ArrayList<DTree> trees;
	/** the number of trees in this random tree */
	private int numTrees;
	/** this is an array whose indices represent the forest-wide importance for that given attribute */
	private int[] importances;
	/** the error rate of this forest */
	private double error;
	
	/** This maps from a data record to an array that records the classifications by the trees where it was a "left out" record (the indices are the class and the values are the counts) */
	private transient HashMap<int[],int[]> estimateOOB;
	/** For progress bar display for the creation of this random forest, this records the total progress */
	private transient double progress;
	/** Of the M total attributes, the random forest computation requires a subset of them
	 * to be used and picked via random selection. "Ms" is the number of attributes in this
	 * subset. The formula used to generate Ms was recommended on Breiman's website.
	 */	
	private transient int Ms;
	/** should we halt the construction of this random forest? */
	private transient boolean stop;
	
	/** Serializable is happy */
	public RandomForest(){}
	
	public RandomForest(DatumSetup datumSetup, JProgressBarAndLabel buildProgress, int numTrees){
		super(datumSetup, buildProgress);
		this.numTrees = numTrees;
	}
	/**
	 * Initializes a Breiman random forest creation. This process is described
	 * in more detail and in the context of the overall project in step 6
	 * of the Algorithms section in the IEEE paper.
	 * 
	 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
	 */
	public void Build() {
		//init some variables
		Ms = (int)Math.round(Math.log(datumSetup.NumberOfFeatures()) / Math.log(2) + 1);
		trees = new ArrayList<DTree>(numTrees);
		final double update = 100 / ((double)numTrees);
		progress = 0;
		estimateOOB = new HashMap<int[],int[]>(N);		
		ExecutorService treePool = Executors.newFixedThreadPool(1);//Run.it.NUM_THREADS);
		//go to town
		final RandomForest that = this; //gotta love java
		for (int t = 0; t < numTrees; t++){
			treePool.execute(new Runnable(){
				public void run(){
					if (!stop){
						trees.add(new DTree(datumSetup, buildProgress, raw_data, that));
						buildProgress.setValue((int)Math.round(progress += update));
					}
				}
			});
		}
		treePool.shutdown();
		try {	         
			treePool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
	    
	    CalcImportances();
	    CalcErrorRate();
	    try {
	    	new VisualizeClassifierImportances(importances, error, datumSetup).SpawnWindow();
	    } catch (Exception e){
	    	e.printStackTrace(); //don't have time to debug this now
	    }
		assert(trees.size() == numTrees);
	}
	/**
	 * This calculates the forest-wide error rate. For each "left out" 
	 * data record, if the class with the maximum count is equal to its actual
	 * class, then increment the number of correct. One minus the number correct 
	 * over the total number is the error rate.
	 */
	private void CalcErrorRate(){
		double N=0;
		int correct=0;
		for (int[] record:estimateOOB.keySet()){
			N++;
			int[] map=estimateOOB.get(record);
			int Class=FindMaxIndex(map);
			if (Class == GetKlass(record))
				correct++;
		}
		error = 1-correct/N;
	}
	/**
	 * Update the error map by recording a class prediction 
	 * for a given data record
	 * 
	 * @param record	the data record classified
	 * @param Class		the class
	 */
	public void UpdateOOBEstimate(int[] record,int Class){
		if (estimateOOB.get(record) == null){
			int[] map=new int[Run.it.numPhenotypes()];
			map[Class]++;
			estimateOOB.put(record,map);
		}
		else {
			int[] map=estimateOOB.get(record);
			map[Class]++;
		}
	}
	/**
	 * This calculates the forest-wide importance levels for all attributes.
	 *  
	 * @return 		the array of importances per each attribute
	 */
	private int[] CalcImportances() {
		importances=new int[datumSetup.NumberOfFeatures()];
		for (DTree tree:trees){
			for (int i=0;i<datumSetup.NumberOfFeatures();i++)
				importances[i]+=tree.getImportanceLevel(i);
		}
		for (int i=0;i<datumSetup.NumberOfFeatures();i++)
			importances[i]/=numTrees;
			
//		PrintImportanceLevels(importances);
		
		return importances;
	}
	
//	private void PrintImportanceLevels(int[] importances) {
//		System.out.print("importances:\n");
//		ArrayList<String> names=new ArrayList<String>(Run.it.imageset.getDatumSetup().NumberOfFeatures());
//		for (String filter:Run.it.imageset.getFilterNames())
//			for (int r=0;r<=Datum.R;r++)
//				names.add(filter + "_" + r + ",");
//		
//		for (int i=0;i<Run.it.imageset.getDatumSetup().NumberOfFeatures();i++)
//			System.out.println(names.get(i)+importances[i]);
//	}
	
	private static final int MaxConfusionValue = 255;
	/**
	 * Evaluates an incoming data record.
	 * It first allows all the decision trees to classify the record,
	 * then it returns the majority vote
	 * 
	 * @param record		the data record to be classified
	 * @param j 
	 * @param i 
	 * @param confusion_matrix 
	 */	
	public int Evaluate(int[] record, ShortMatrix confusion_matrix, Integer i, Integer j){
//		System.out.println("record length: "+record.length);
		int[] counts=new int[Run.it.numPhenotypes()];
		assert(trees.size() == numTrees);
		for (int t=0;t<numTrees;t++){
			int Class=(trees.get(t)).Evaluate(record);
			counts[Class]++;
		}
		int result = FindMaxIndex(counts);
		
		//now handle confusion in the forest's decision:
		int confusion = 0;
		for (int c = 0; c < Run.it.numPhenotypes(); c++){
			if (c != result){
				confusion += counts[c];
			}
		}
		confusion_matrix.set(i, j, (short)Math.floor(confusion / ((double) numTrees) * MaxConfusionValue));
		
		return result;
	}
	
	//I know there's some code duplication here, but cest la vie
	public int Evaluate(int[] record){
		int[] counts=new int[Run.it.numPhenotypes()];
		for (int t=0;t<numTrees;t++){
			int Class=(trees.get(t)).Evaluate(record);
			counts[Class]++;
		}
		return FindMaxIndex(counts);		
	}
	/**
	 * Given an array, return the index that houses the maximum value
	 * 
	 * @param arr	the array to be investigated
	 * @return		the index of the greatest value in the array
	 */
	public static int FindMaxIndex(int[] arr){
		int index=0;
		int max=Integer.MIN_VALUE;
		for (int i=0;i<arr.length;i++){
			if (arr[i] > max){
				max=arr[i];
				index=i;
			}				
		}
		return index;
	}

//	//ability to clone forests
//	private RandomForest(ArrayList<DTree> trees,int numTrees){
//		this.trees=trees;
//		this.numTrees=numTrees;
//	}
//	public RandomForest clone(){
//		ArrayList<DTree> copy=new ArrayList<DTree>(numTrees);
//		for (DTree tree:trees)
//			copy.add(tree.clone());
//		return new RandomForest(copy,numTrees);
//	}
	/**
	 * Attempt to abort random forest creation
	 */
	public void StopBuilding() {
		stop = true;
	}

	public ArrayList<DTree> getTrees() {
		return trees;
	}

	public void setTrees(ArrayList<DTree> trees) {
		this.trees = trees;
	}

	public int getNumTrees() {
		return numTrees;
	}

	public void setNumTrees(int numTrees) {
		this.numTrees = numTrees;
	}

	public int[] getImportances() {
		return importances;
	}

	public void setImportances(int[] importances) {
		this.importances = importances;
	}

	public double getError() {
		return error;
	}

	public void setError(double error) {
		this.error = error;
	}

	//nothing
	protected void FlushData() {}


	public int getMs() {
		return Ms;
	}
	
	public boolean supportsConfusionMatrices(){
		return true;
	}
}