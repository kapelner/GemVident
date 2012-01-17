package GemIdentStatistics;

import GemIdentClassificationEngine.DatumSetup;
import GemIdentTools.Matrices.ShortMatrix;
import GemIdentView.JProgressBarAndLabel;

public abstract class ClassifierThatCanHandleConfusion extends Classifier {
	private static final long serialVersionUID = -5732887780746750740L;

	public ClassifierThatCanHandleConfusion(){}
	
	public ClassifierThatCanHandleConfusion(DatumSetup datumSetup, JProgressBarAndLabel buildProgress){
		super(datumSetup, buildProgress);		
	}
	
	public abstract int Evaluate(int[] record, ShortMatrix confusion_matrix, Integer i, Integer j);

}
