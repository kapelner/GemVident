package GemIdentStatistics;

import java.io.Serializable;
import java.util.ArrayList;

import GemIdentClassificationEngine.DatumSetup;
import GemIdentView.JProgressBarAndLabel;

/**
 * The base class for all machine learning / statistical-learning
 * algorithms. Extend this class to add your own implementation
 * 
 * @author Adam Kapelner
 */
public abstract class Classifier implements Serializable {
	private static final long serialVersionUID = -2857913059676679308L;
	
	/** an object that holds information for all Datums */
	protected transient DatumSetup datumSetup;
	/** the raw training data consisting of xi = [xi1,...,xiM, yi] that will be used to construct the classifier */
	protected transient ArrayList<int[]> raw_data;
	/** the number of records in the training set */
	protected transient int N;
	/** the progress bar that gets updated as the classifier is being built */
	protected transient JProgressBarAndLabel buildProgress;
	
	/** Serializable happy */
	public Classifier(){}

	public Classifier(DatumSetup datumSetup, JProgressBarAndLabel buildProgress){
		this.datumSetup = datumSetup;
		this.buildProgress = buildProgress;
	}
	
	/** adds the data to the classifier - data is always a list of int[]'s - call this before calling {@link #Build() Build()} */
	public void setData(ArrayList<int[]> raw_data){
		this.raw_data = raw_data;
		N = raw_data.size();
	}
	
	/** build the machine learning classifier, you must {@link #setData(ArrayList) set the data} first */
	public abstract void Build();
	
	/** deletes all data that's unneeded to save memory */
	protected abstract void FlushData();
	
	/** After the classifier has been built, new records can be evaluated */
	public abstract int Evaluate(int[] record);
	
	/**
	 * Given a data record, return the Y value - take the last index
	 * 
	 * @param record		the data record
	 * @return				its y value (class)
	 */
	public int GetKlass(int[] record){
		return record[datumSetup.NumberOfFeatures()];
	}

	/** Stop the classifier in its building phase */
	public abstract void StopBuilding();
}
