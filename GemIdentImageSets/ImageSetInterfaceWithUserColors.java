
package GemIdentImageSets;

import java.awt.Color;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import GemIdentModel.Stain;
import GemIdentModel.TrainingImageData;
import GemIdentOperations.Run;
import GemIdentOperations.StainMaker;
import GemIdentOperations.StainOpener;
import GemIdentTools.IOTools;
import GemIdentTools.Matrices.ShortMatrix;
import GemIdentView.JProgressBarAndLabel;

/**
 * A base class for imagesets that use user-specified colors. The user must
 * train for the colors of interest in a separate "color training" tab.
 * 
 * @author Adam Kapelner
 */
public abstract class ImageSetInterfaceWithUserColors extends ImageSetInterface {
	private static final long serialVersionUID = 1L;
	
	/** the minimum number of training points required for colors in order to {@link StainMaker compute the Mahalanobis cube} */
	transient public static final int MinimumColorPointsNeeded = 5;
	/** when {@link StainOpener opening} the Mahalanobis cubes, this is the number of concurrent threads to use - basically infinity in order to smoothen the opening */
	transient public static final int N_THREADS_OPEN_OR_CREATE_CUBES=Integer.MAX_VALUE;
	
	/** the thread pool that controls the opening of the color cubes from the hard drive (see {@link #N_THREADS_OPEN_OR_CREATE_CUBES}) */
	transient private ExecutorService openColorCubePool;
	/** the thread pool that controls the construction of the color cubes (see {@link #N_THREADS_OPEN_OR_CREATE_CUBES}) */
	transient private ExecutorService createColorCubePool;

	/** an order-preserving mapping from name of the stains to the actual stain object */
	protected LinkedHashMap<String,Stain> stains;
	/** an order-preserving mapping from name of the background color to the actual stain object */
	protected LinkedHashMap<String, Stain> background_colors;
	
	public ImageSetInterfaceWithUserColors(){}
	
	public ImageSetInterfaceWithUserColors(String homedir){
		super(homedir);
		stains = new LinkedHashMap<String, Stain>();
		background_colors = new LinkedHashMap<String, Stain>();
	}
	
	public Collection<Stain> getStainObjects() {
		return stains.values();
	}

	public Collection<Stain> getBackgroundColorObjects() {
		return getBackground_colors().values();
	}	

	public void AddStain(Stain stain){
		stains.put(stain.getName(), stain);		
	}	

	public void AddBackgroundColor(Stain stain) {
		getBackground_colors().put(stain.getName(), stain);
	}
	
	// these are for Serializable
	public void setStains(LinkedHashMap<String,Stain> stains){
		this.stains = stains;
	}
	public LinkedHashMap<String,Stain> getStains() {
		return stains;
	}
	
	public Stain getStain(String stainname){
		return stains.get(stainname);
	}
	
	public Color getWaveColor(String stainname){
		return stains.get(stainname).getAverageColor();
	}	
	
	/**
	 * Removes a stain from the {@link #stains master mapping}
	 * Also deletes its color cube file from the hard disk
	 * @param name
	 */
	public void DeleteStain(String name) {
		Stain stain=stains.get(name);
		if (stain == null)
			return;
		else 
			stain.DeleteFile();			
		stains.remove(name);
	}
	

	public void DeleteBackgroundColor(String name) {
		Stain background_color = getBackground_colors().get(name);
		if (background_color == null)
			return;
		else 
			background_color.DeleteFile();			
		getBackground_colors().remove(name);		
	}	
	/** removes the color cubes for each stain from memory */
	public void FlushCubes() {
		for (Stain stain:stains.values())
			stain.FlushCube();		
	}
	/** gets the image filenames that the user used to train for stains */
	public Collection<String> getStainTrainingImages(){
		HashSet<String> set=new HashSet<String>();
		for (Stain stain:stains.values())
			for (TrainingImageData I:stain.getTrainingImages())
				set.add(I.getFilename());
		return set;
	}
	
	/** gets the image filenames that the user used to train for background colors */
	public Collection<String> getBackgroundColorTrainingImages() {
		HashSet<String> set = new HashSet<String>();
		for (Stain stain : getBackground_colors().values())
			for (TrainingImageData I : stain.getTrainingImages())
				set.add(I.getFilename());
		return set;
	}
	
	/** Checks if the user supplied <b>GemIdent</b> with sufficient number of
	 * training points to go ahead with classification. If a NuanceSet, there
	 * is no color training, so the function checks out automatically
	 */
	public boolean EnoughStainTrainPoints() {
		for (Stain stain:stains.values()){
//					System.out.println("stain:"+stain.getName());
			if (stain.getTotalPoints() < MinimumColorPointsNeeded)
				return false;
		}
		return true;	
	}
	/** checks if all the color cubes were computed by checking if the file exists in the "colors" directory */ 
	public boolean StainCubesComputed() {
		for (Stain stain:stains.values())
			if (!IOTools.FileExists(StainMaker.colorSubDir+File.separator+stain.getName()))
				return false;
		return true;
	}

	/** removes a training image used for color training (if there is data in it, confirms with the user) */
	public boolean RemoveImageFromStainSet(String filename) {
		boolean dialog=false;
//		System.out.println("Run.RemoveImageFromStainSet()");
		for (Stain stain:stains.values()){
			if (stain.hasImage(filename)){
				TrainingImageData image=stain.getTrainingImage(filename);
				if (image.getNumPoints() > 0)
					dialog=true;
			}
		}
		int result=JOptionPane.YES_OPTION;
		if (dialog)
			result = JOptionPane.showConfirmDialog(Run.it.getGUI(),
					"This image contains training data. Are you sure you want to remove it?",
					"Points deletion",
					JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION){
			for (Stain stain:stains.values())
				if (stain.hasImage(filename))
					stain.RemoveTrainingImage(filename);
			Run.it.GUIsetDirty(true);
			return true;
		}
		return false;
	}
	
	/**
	 * Attempts to open the color cubes from the hard drive using
	 * a separate thread for each file. If the file has not been
	 * created, it attempts to {@link StainMaker build} it from the 
	 * user's color training data
	 * 
	 * @param openProgress		the progress bar
	 * @return					whether or not the opening was successful
	 */
	public boolean OpenMahalanobisCubes(JProgressBarAndLabel openProgress){
		int increment=(int)Math.round(100/((double)stains.size()));
		openColorCubePool=Executors.newFixedThreadPool(N_THREADS_OPEN_OR_CREATE_CUBES);
		for (Stain stain:getStainObjects()){
			if (stain.GetMahalCube() == null){
//				System.out.println("stain:"+stain.getName()+" null");
				if (IOTools.FileExists(StainMaker.colorSubDir+File.separator+stain.getName()))
					openColorCubePool.execute(stain.getStainOpener(openProgress,increment));
				else {
//							System.out.println("cannot find file:"+stain.getName());
					StainMaker maker=stain.getStainMaker(null,openProgress);
					if (maker == null){
						JOptionPane.showMessageDialog(Run.it.getGUI(),"Not enough points in stain:"+stain.getName());
						if (openProgress != null)
							openProgress.setValue(0); //reset!
						openColorCubePool.shutdownNow();						
						return false;
					}
					else
						addToCubeComputePool(maker);
				}	
			}
			else {
				if (openProgress != null)
					openProgress.setValue(openProgress.getValue()+(int)Math.round(100/((double)stains.size())));
			}
		}
		openColorCubePool.shutdown();
		try {	         
	         openColorCubePool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
	    if (openProgress != null)
	    	openProgress.setValue(100); //hack but who cares
	    openColorCubePool = null;
	    return true;
	}
	/** executes a color cube construction */
	public void addToCubeComputePool(StainMaker stainMaker) {
		if (createColorCubePool == null)
			createColorCubePool=Executors.newFixedThreadPool(N_THREADS_OPEN_OR_CREATE_CUBES);
		else if (createColorCubePool.isShutdown())
			createColorCubePool=Executors.newFixedThreadPool(N_THREADS_OPEN_OR_CREATE_CUBES);
		createColorCubePool.execute(stainMaker);
		ImageAndScoresBank.FlushAllScores();
	}
	
	public int NumFilters(){
		return stains.size();
	}

	public HashMap<String, ShortMatrix> GenerateStainScores(SuperImage superImage){
		HashMap<String, ShortMatrix> stainScores = new HashMap<String, ShortMatrix>(Run.it.imageset.NumFilters());
		for (String filter:Run.it.imageset.getFilterNames()){
			stainScores.put(filter, stains.get(filter).MahalanobisColorScoring((DataImage)superImage));
//			System.out.println(filter+":"+stains.get(filter).MahalanobisColorScoring((DataImage)superImage));
		}
		
		
		return stainScores;
	}

	public LinkedHashMap<String, Stain> getBackground_colors() {
		if (background_colors == null){ //backwards compatibility, eventually, delete this and replace "getBackground_colors" with the ivar
			background_colors = new LinkedHashMap<String, Stain>();
		}
		return background_colors;
	}

	public void setBackground_colors(LinkedHashMap<String, Stain> background_colors) {
		this.background_colors = background_colors;
	}

}