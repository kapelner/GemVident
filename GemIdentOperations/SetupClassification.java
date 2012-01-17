package GemIdentOperations;

import java.io.File;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;

import GemIdentCentroidFinding.PostProcess;
import GemIdentClassificationEngine.Classify;
import GemIdentClassificationEngine.Datum;
import GemIdentClassificationEngine.DatumSetup;
import GemIdentClassificationEngine.TrainingData;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.ImageSetInterfaceWithUserColors;
import GemIdentStatistics.Classifier;
import GemIdentStatistics.DTree;
import GemIdentStatistics.RandomForest;
import GemIdentTools.IOTools;
import GemIdentView.ClassifyProgress;
import GemIdentView.JProgressBarAndLabel;
import GemIdentView.KClassifyPanel;
import GemIdentView.KFrame;

/**
 * Class responsible for setting up classifications, centroid-findings, or both. 
 * It is threaded as to not hog swing.
 * 
 * @author Adam Kapelner
 */
public class SetupClassification extends Thread {		
	
	/** Pointer to the project data */
	private Run it;
	/** the progress bar that informs the user of the opening of the color cubes from the hard disk */
	private JProgressBarAndLabel openProgress;
	/** the progress bar that informs the user of the creation of the training data */
	private JProgressBarAndLabel trainingProgress;
	/** the progress bar that informs the user of the building of the machine learning classifiers */
	private JProgressBarAndLabel buildProgress;
	/** should <b>GemIdent</b> do a classification on the specified images? */
	private boolean toClassify;
	/** should <b>GemIdent</b> do a post-process to find centroids on the specified images? */
	private boolean toPostProcess;
	/** generates the user's phenotype training data in order to be used during creation of the machine learning classifiers */
	private TrainingData trainingData;
	/** generates the machine learning classifier, then uses it to evaluate the images */
	private Classifier classifier;
	/** controls the classification */
	private Classify classify;
	/** controls the post-processing to find centroids */
	private PostProcess postProcess;
	/** did the user push the stop button? */
	private boolean stopped;
	/** Pointer to the visual frame */
	private KFrame gui;
	/** pointer to the object that holds the image set data */
	private ImageSetInterface imageset;

	
	/** default constructor - also begins the thread */
	public SetupClassification(Run it, JProgressBarAndLabel openProgress,JProgressBarAndLabel trainingProgress,JProgressBarAndLabel buildProgress,boolean toClassify,boolean toPostProcess, Classifier classifier){
		this.it = it;
		gui = it.getGUI();
		imageset = it.imageset;
		this.toClassify = toClassify;
		this.toPostProcess = toPostProcess;
		this.openProgress = openProgress;
		this.trainingProgress = trainingProgress;
		this.buildProgress = buildProgress;
		this.classifier = classifier;
		stopped = false;
		Run.InitializeClassPhenotypeMap();
		start();
	}
	/** gets the list of images the user specified to classify,
	 * initializes the progress bars for to update the user on
	 * the progress of the classification, then if {@link #toClassify
	 * classify}, it classifies, and/or if {@link #toPostProcess postProcess},
	 * it will post process to find centroids
	 */
	public void run(){
		Run.SaveProject();
		Collection<String> files = it.GetImageListToClassify();
		ClassifyProgress progress = new ClassifyProgress(files.size());
		gui.KillPhenotypeTab();
		it.resetTypeOneErrors();
		if (toClassify)
			DoTheClassification(files,progress);
		if (toPostProcess)
			PostProcess(it.GetAllClassifiedImages(), progress);
	}
	/** runs a classification. First creates the processed directory (if
	 * needed), then prompts the user if he wants to flush the previous
	 * classification's files, flushes previously classified masks,
	 * as well as the error mapping, then attempts to open the color
	 * cubes from the hard disk. Then it populates the global {@link Datum 
	 * Datum} parameters, creates the {@link TrainingData#TrainingData
	 * training data} from the user's phenotype training points, creates
	 * a classification object, and attempts to run the classification 
	 * 
	 * @param files			the images to classify
	 * @param progress		the progress bars
	 */
	private void DoTheClassification(Collection<String> files, ClassifyProgress progress) {	
		CreateDirectoriesAndFlushRAM();
		
		//create the method to which data is built from the image set
		DatumSetup datumSetup = imageset.setUpDataExtractionMethod();
		
		//if the user didn't supply a classifier, we're going to have to build one:
		if (classifier == null){
			if (imageset instanceof ImageSetInterfaceWithUserColors){ //this is ugly but conceptually it's the only way to go I believe
				if (!((ImageSetInterfaceWithUserColors)imageset).OpenMahalanobisCubes(openProgress)){ //there was a problem with openCubes....
					gui.getClassifyPanel().ReenableClassifyButton();
					gui.getClassifyPanel().RemoveAllBars();
					return;
				}
			}
			//now we're good to go:
			trainingData = new TrainingData(datumSetup, it.num_threads, trainingProgress);

			if (!stopped){
				CreateClassifier(datumSetup);
			}
			trainingData = null; //we no longer need this, let's conserve RAM		
		}
		
		if (!stopped){
			classify = new Classify(datumSetup, files, classifier, progress, gui.getClassifyPanel());
		}
		gui.getClassifyPanel().ClassificationDone();
	}
	
	/** The name of the "Random Forest" machine learning classifier */
	public static final String RandomForestSymbol = "Random Forest";
	/** The name of the "Decision Tree" machine learning classifier */
	public static final String DTreeSymbol = "Decision Tree";
	/** The current classifier the user is using */
	public static String classifierType = RandomForestSymbol; //default is random forests for now
	
	/**
	 * This method creates the machine learning classifier for this analysis
	 * 
	 * @param datumSetup	the data object that holds information about how the Datums are constructed	
	 */	
	private void CreateClassifier(DatumSetup datumSetup) {
		//this is where you can switch the machine learning engine if desired:
		classifier = null;
		if (classifierType.equals(RandomForestSymbol)){
			classifier = new RandomForest(datumSetup, buildProgress, it.num_trees);
		}
		else if (classifierType.equals(DTreeSymbol)){
			classifier = new DTree(datumSetup, buildProgress);
		}
		System.out.println("training data: "+Arrays.toString(trainingData.getData().toArray()));
		classifier.setData(trainingData.getData());
		classifier.Build();					
		//now save the forest to the hd (on another thread to not slow us down):
		SaveClassifierToHardDrive();
	}
	
	/** Creates necessary directories for classification and centroid-finding, and flushes the caches to release memory */
	private void CreateDirectoriesAndFlushRAM() {
		//create directories, delete old files and initialize some vars
		if (!IOTools.DoesDirectoryExist(Classify.processedDir))
			(new File(Run.it.imageset.getFilenameWithHomePath(Classify.processedDir))).mkdir();
		if (!IOTools.DoesDirectoryExist(PostProcess.analysisDir))
			(new File(Run.it.imageset.getFilenameWithHomePath(PostProcess.analysisDir))).mkdir();
		if (!IOTools.DoesDirectoryExist(ImageSetInterface.checkDir))
			(new File(Run.it.imageset.getFilenameWithHomePath(ImageSetInterface.checkDir))).mkdir();
		
		if (it.pics_to_classify != KClassifyPanel.CLASSIFY_REMAINING)
			Classify.PromptToDeleteAllProjectFiles();
		
		//we don't need these in memory any longer
		ImageAndScoresBank.FlushAllIsImages();
		ImageAndScoresBank.FlushAllConfusionImages();			
	}
	
	/** Upon construction, dumps the classifier to disk in XML. Threaded as to not hog swing */
	private void SaveClassifierToHardDrive(){
		final Classifier final_classifier = classifier;
		new Thread(){
			public void run(){
				setPriority(Thread.MIN_PRIORITY);
				System.out.println("saving classifier to HD...");
				IOTools.saveToXML(final_classifier, imageset.getFilenameWithHomePath(it.projectName+".classifier"));
				System.out.println("done saving classifier");							
			}
		}.start();
	}
		
	/** runs a post-processing to find centroids. First creates the output 
	 * directory (if needed), flushes the error mapping, creates
	 * a post processing object, and attempts to run a post-processing,
	 * then saves the counts of the phenotypes and their false negative rates 
	 * 
	 * @param files			the images to post-process
	 * @param progress		the progress bars
	 */
	@SuppressWarnings("deprecation")
	private void PostProcess(Collection<String> files,ClassifyProgress progress) {
		Timestamp t = new Timestamp(System.currentTimeMillis());
		String date = t.toString().split(" ")[0];
		PostProcess.outputDir = PostProcess.outputDirFirstName + "--"+date+"-"+t.getHours()+"-"+t.getMinutes()+"-"+t.getSeconds();
		if (!IOTools.DoesDirectoryExist(PostProcess.outputDir))
			(new File(Run.it.imageset.getFilenameWithHomePath(PostProcess.outputDir))).mkdir();

		if (!stopped){
			it.resetTotalCounts();
			it.resetErrorRates();
			postProcess=new PostProcess(files, progress, gui.getClassifyPanel(), it.getTypeOneErrors());	
			postProcess.FindCentroids();
		}
		if (!stopped){
			it.setTotalCounts(postProcess.getTotalCounts());
			it.setErrorRates(postProcess.getErrorRates());
			postProcess=null; //we have to keep this guy around
			if (imageset instanceof ImageSetInterfaceWithUserColors){ //this is ugly but conceptually it's the only way to go I believe
				((ImageSetInterfaceWithUserColors)imageset).FlushCubes();
			}
			ImageAndScoresBank.FlushAllCaches();
			Run.SaveProject();
		}
	}
	/** Stops the current executing process */
	public void Stop(){
		stopped=true;
//			System.out.println("try to stop classifying");
		try {
			if (trainingData != null)
				trainingData.Stop();
			if (classifier != null)
				classifier.StopBuilding();
			if (classify != null)
				classify.Stop();
			if (postProcess != null)
				postProcess.Stop();
		} catch (NullPointerException e){} //things aren't initialized yet
	}
}
