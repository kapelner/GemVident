package GemIdentClassificationEngine;

import java.util.Set;

import GemIdentImageSets.ImageSetInterface;

public abstract class DatumSetupThatUsesRings extends DatumSetup {

	/** maximum radius */
	protected int R;

	public DatumSetupThatUsesRings(ImageSetInterface imageset, Set<String> filterNames, int R){
		super(imageset, filterNames);
		this.R = R;
	}
	
	public int getMaximumRadius(){
		return R;
	}

}
