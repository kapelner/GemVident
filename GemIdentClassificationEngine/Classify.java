package GemIdentClassificationEngine;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import GemIdentImageSets.BackgroundHandler;
import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterfaceWithUserColors;
import GemIdentImageSets.SuperImage;
import GemIdentOperations.Run;
import GemIdentStatistics.Classifier;
import GemIdentStatistics.ClassifierThatCanHandleConfusion;
import GemIdentTools.IOTools;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentTools.Matrices.ShortMatrix;
import GemIdentView.ClassifyProgress;
import GemIdentView.KClassifyPanel;

/**
 * The class responsible for loading the image files and classifying
 * their pixels into the user-defined phenotype classes by using
 * the previously built {@link GemIdentStatistics.Classifier machine learning classifier}.
 * This class is threaded because it was slowing down swing. When classifying lots of images - over 500, there is a small memory
 * leak that compounds. This was fixed but now images won't flash during classification.
 * We hope this will fix in a later release. For discussion
 * about classification in general, consult section 4 of the manual
 * 
 * @author Adam Kapelner
 */
public class Classify {

	/** The directory of the processed images */
	public static final String processedDir="processed";
	/** the thread pool responsible for classifying the images */
	private ExecutorService classifyPool;
	/** the progress bars associated with this classification */
	private ClassifyProgress progress;
	/** the panel this classification is taking place in */
	private KClassifyPanel classifyPanel;
	/** the machine learning classifier that will classify the image pixels */
	private Classifier classifier;
	/** should the classification be stopped? */
	private boolean stop;
	/** the images to classify */
	private Collection<String> filenames;
	/** the object that contains common information for each Datum */
	private DatumSetup datumSetup;

	/**
	 * Initializes the classification of an image set.
	 * @param datumSetup 
	 * 
	 * @param filenames				the images to classify
	 * @param classifier			the machine learning classifier to use
	 * @param progress				the progress bar to use to give the user updates on progress
	 * @param classifyPanel			the panel the classification is taking place in
	 */
	public Classify(DatumSetup datumSetup, Collection<String> filenames, Classifier classifier, ClassifyProgress progress, KClassifyPanel classifyPanel){
		this.datumSetup = datumSetup;
		this.classifyPanel = classifyPanel;
		this.progress = progress;
		this.filenames = filenames;		
		this.classifier = classifier;
		stop = false;
		//go for the gold
		BeginClassification();
	}

	/** 
	 * Begins the classification. Starts the timer, initializes the thread pool, 
	 * then populates it with a worker class for each image in the set
	 */
	private void BeginClassification() {
		if (progress != null)
			progress.StartTimer();
		Run.it.imageset.LOG_AddToHistory("begun classification of " + filenames.size() + " images");
		
//		System.out.println("\n<NEW>begin classifying all images");
		classifyPool=Executors.newFixedThreadPool(Run.it.num_threads);
		for (String filename:filenames)
	    	classifyPool.execute(new SingleImageClassifier(filename));
		classifyPool.shutdown();
		try {	         
	         classifyPool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
	    if (progress != null)
	    	progress.RemoveClassifyBars();
	}
	/**
	 * First prompts the user then deletes all 
	 * processed files from a the processed directory. If killOutputDir,
	 * it does the same with the output files from the output directory
	 */
	public static void PromptToDeleteAllProjectFiles(){
		String[] allFiles;
		allFiles=(new File(Run.it.imageset.getFilenameWithHomePath(processedDir+File.separator))).list();
		if (allFiles != null){
			if (allFiles.length > 0){
				int result = JOptionPane.showConfirmDialog(null,
						"Would you like to clear all previous identification data for this project?",
						"Warning",
						JOptionPane.YES_NO_OPTION);
				if ( result == JOptionPane.YES_OPTION){
					ArrayList<String> toBeDeleted=new ArrayList<String>();
					for (String file:allFiles)
						if (file.split("-")[0].equals(Run.it.projectName))
							toBeDeleted.add(file);
					
					for (String file:toBeDeleted)
						(new File(Run.it.imageset.getFilenameWithHomePath(processedDir+File.separator+file))).delete();
				}			
			}
		}
	}
	/** Stop the current classification (called when the user presses the "stop" button */
	public void Stop(){
		if (classifyPool != null){
			stop=true;
			classifyPool.shutdownNow(); //sends interrupt call to all threads
		}
	}

	/**
	 * Class responsible for classifying one image, threaded in a thread pool
	 */
	private class SingleImageClassifier implements Runnable {
		
		/** the image being classified */
		private String filename;
		/** the beginning time of the classification */
		private long time_o;
		/** the expanded {@link GemIdentImageSets.SuperImage SuperImage} of this image */
		private SuperImage superImage;
		/** a copy of the original image */
		private DataImage actualImageInter;
		/** the background matrix - if these pixels are true, then we don't need to bother and classify them */
		private BoolMatrix master_background;		
		/** a mapping from the phenotypes to their results matrix for this image */
		private HashMap<String,BoolMatrix> is;
		/** the width of this image */
		private int width;
		/** the height of this image */
		private int height;
		/** the name of the thread this image is being classified on (for debugging purposes only) */
		private String threadName;

		
		/** initializes a new Classifier worker
		 * 
		 * @param filename		the filename of the image
		 */
		public SingleImageClassifier(String filename){
			this.filename=filename;
			is=new HashMap<String,BoolMatrix>(Run.it.numPhenotypes());
		}

		/**
		 * Starts the classification on this image. Starts the timer, adds a progress bar
		 * to the user's panel, {@link #PreProcess() preprocesses}, initializes the
		 * images inside the user's panel, {@link #ClassifyPixels() classifies all pixels},
		 * writes the results to the processed directory, and then writes debug images to the processed
		 * directory
		 */
		public void run(){
			if (filename == null){
				System.err.println("Something wrong in classify: "+filename);
				return;
			}
			if (stop)
				return;
			StartTimer();
			
			threadName=Thread.currentThread().getName();
			if (progress != null)
				progress.NewClassifyBar(threadName,filename);
			if(classifyPanel != null){
				classifyPanel.AddOrEditImageProgressBars(progress.getBox());
				classifyPanel.setDisplayImage(null);
			}
			
			PreProcess();
			if (progress != null){
				progress.preprocessed(threadName);
				progress.setTime(threadName,TimeElapsed());
				progress.setNPixels(threadName,actualImageInter.numPixels()/Run.it.pixel_skip);
			}
			
			System.out.println("begin classifying pixels in image "+filename+" on thread #"+Thread.currentThread().getId()+" . . . ");
			ShortMatrix confusion_matrix;
			try {
				confusion_matrix = ClassifyPixels();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			System.out.println("done classifying pixels in image "+filename);
			if (!stop){
				if (progress != null)
					progress.WritingImages(threadName);
				for (String phenotype:Run.it.getPhenotyeNamesSaveNONAndFindPixels())
					IOTools.WriteIsMatrix(GetIsName(filename,phenotype),is.get(phenotype));
				
//				if (!(Run.it.imageset instanceof NuanceImageListInterface)){ //too complicated to do this if its Nuance
//					MarkIntermediatePixels();
//					IOTools.WriteImage(GetIntermediateName(filename),"TIFF",actualImageInter);
//				}
				if (classifier instanceof ClassifierThatCanHandleConfusion){
					IOTools.WriteScoreImage(GetConfusionName(filename), confusion_matrix);
				}
				if (progress != null)
					progress.finished(threadName,TimeElapsed());
			}
		}
		
		/** 
		 * creates the necessary data for the classification: creates the {@link GemIdentImageSets.SuperImage SuperImage}
		 * of the image, creates a copy (for the debug image), computes the scores for
		 * the image, and then initializes the results matrix
		 */
		private void PreProcess(){
			try {
				superImage=ImageAndScoresBank.getOrAddSuperImage(filename);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			actualImageInter=superImage.getCenterImage().clone();
			width=actualImageInter.getWidth();
			height=actualImageInter.getHeight();
			for (String phenotype:Run.it.getPhenotyeNamesSaveNON())
				is.put(phenotype,new BoolMatrix(width,height));
			if (Run.it.imageset instanceof ImageSetInterfaceWithUserColors){
				//System.out.println("creating BackgroundHandler");
				master_background = new BackgroundHandler(actualImageInter).createMasterBackgroundMatrix();
				//System.out.println("done");
			}
		}

		/**
		 * Classifies the every PIXEL_SKIP pixel in the image.
		 * For each pixel, create the evaluation data for it by
		 * constructing a {@link Datum Datum}
		 * object, then evaluate that data using the machine learning
		 * classifier and record the result in the respective result matrix.
		 * If the number of pixels classified is the same as the update 
		 * "batch size" then refresh the screen for the user and show
		 * the current results. This process is described
		 * in more detail and in the context of the overall project in step 7
		 * of the Algorithms section in the IEEE paper.
		 * @throws FileNotFoundException 
		 * 
		 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
		 */
		private ShortMatrix ClassifyPixels() throws FileNotFoundException{
			ShortMatrix confusion_matrix = new ShortMatrix(width, height);
			int counter = 0;
			classify_all_pixels : {
				for (int j = 0; j < height; j++){
					for (int i = 0; i < width; i++){
						if (((i + j) % Run.it.pixel_skip) == 0){
							counter++;
							ClassifyOnePixel(i, j, confusion_matrix);
							if (counter == Run.it.num_pixels_to_batch_updates && counter != 0){
								counter=0;
								new Thread(){ //thread this off to make classification faster
									public void run(){
	//									UpdateImagePanel();
										UpdateProgressBar();									
									}
								}.start();							
							}
						}
						if (stop)
							break classify_all_pixels;						
					}
				}
			}
			return confusion_matrix;
		}
		private void ClassifyOnePixel(int i, int j, ShortMatrix confusion_matrix) throws FileNotFoundException {
			if (master_background != null && master_background.get(i, j)){
				return; //if this pixel is in the background, then we ditch
			}
			Datum d = datumSetup.generateDatum(filename, new Point(i, j));							
			int resultClass = 0;
			if (classifier instanceof ClassifierThatCanHandleConfusion){
				resultClass = ((ClassifierThatCanHandleConfusion)classifier).Evaluate(d.getRecord(), confusion_matrix, i, j);
			}
			else {
				resultClass = classifier.Evaluate(d.getRecord());
			}
//			System.out.println("evaluated: (" + i + "," + j + ") class:" + resultClass);
			if (resultClass != 0)
				(is.get(Run.classMapBck.get(resultClass))).set(i,j,true);
		}

		/** update the progress bar for the user */
		private void UpdateProgressBar(){
			if (progress == null)
				return;

			progress.update(threadName);
			progress.setTime(threadName,TimeElapsed());			
		}
//		/** show the current image being classified as well as the real time results */
//		private void UpdateImagePanel(){
//			classifyPanel.setDisplayImage(superImage.getCenterImage()); //LEAK!!!!!
//			classifyPanel.setIs(is);
////			classifyPanel.repaintImagePanel();			
//		}
		/** paint the results over a copy of the original image */
//		private void MarkIntermediatePixels(){
//			for (int i=0;i<width;i++){
//				for (int j=0;j<height;j++){
//					HashMap<String,Boolean> isij=new HashMap<String,Boolean>(Run.it.numPhenotypes());
//					for (String phenotype:Run.it.getPhenotyeNamesSaveNON())
//						isij.put(phenotype,is.get(phenotype).get(i,j));
//					int T=0;
//					for (String phenotype:Run.it.getPhenotyeNamesSaveNON()){
//						if (isij.get(phenotype)){
//							T++;
//							actualImageInter.MarkPheno(i,j,phenotype);
//						}	
//					}
//				}
//			}			
//		}
		/** start the timer for this image's classification */
		private void StartTimer() {
			time_o=System.currentTimeMillis();			
		}
		/** return the time elapsed thus far in seconds */
		private int TimeElapsed(){
			return (int)(System.currentTimeMillis()-time_o)/1000;
		}
	}

	/**
	 * Get the path to the marked image copy. The filename includes the project name
	 * (so multiple projects can be stored within one project directory without
	 * conflicts)
	 * 
	 * @param filename		the name of the original image
	 * @return				the path (relative to the project folder) of the marked image
	 */
	public static String GetIntermediateName(String filename) {
		return processedDir+File.separator+Run.it.projectName+"-i"+IOTools.GetFilenameWithoutExtension(filename)+".tif";
	}
	/**
	 * Get the path to the result matrix for a given image and a given phenotype
	 * 
	 * @param filename			the name of the image
	 * @param phenotype			the resulting matrix for this phenotype
	 * @return					the path (relative to the project folder) of the result image
	 */
	public static String GetIsName(String filename, String phenotype) {
		return processedDir+File.separator+Run.it.projectName+"-is"+phenotype+IOTools.GetFilenameWithoutExtension(filename)+".bmp";
	}

	/**
	 * Get the path to the result matrix for a given image and a given phenotype
	 * 
	 * @param filename			the name of the image
	 * @param phenotype			the resulting matrix for this phenotype
	 * @return					the path (relative to the project folder) of the result image
	 */
	public static String GetIsCName(String filename, String phenotype) {
		return processedDir+File.separator+Run.it.projectName+"-isC"+phenotype+IOTools.GetFilenameWithoutExtension(filename)+".bmp";
	}

	public static String GetConfusionName(String filename){
		return processedDir + File.separator + Run.it.projectName + "-confusion-" + filename + ".tif";
	}

	/**
	 * Gets a list of all images classified
	 * 
	 * @return		a list of those images classified previously
	 */
	public static ArrayList<String> AllClassified(){
		ArrayList<String> allFiles=Run.it.imageset.GetImages();
		ArrayList<String> set=new ArrayList<String>();
		for (String filename:allFiles)
			if (Run.it.imageset.PicWasClassified(filename))
				set.add(filename);
		return set;
	}

	
	/**
	 * Gets a list of all images with centroids
	 * 
	 * @return		a list of those images classified previously
	 */
	public static ArrayList<String> AllCentroids(){
		ArrayList<String> allFiles=Run.it.imageset.GetImages();
		ArrayList<String> set=new ArrayList<String>();
		for (String filename:allFiles)
			if (Run.it.imageset.PicHasCentroids(filename))
				set.add(filename);
		return set;
	}
	/**
	 * After classification, the result matrix is preprocessed, first to clear holes
	 * created from a greater-than-one pixel skip, then it's "closed" (eroded and dilated)
	 * to eliminate noise then "opened" (dilated and eroded) to eliminate holes
	 * 
	 * @param B			the {@link BoolMatrix BoolMatrix} result matrix to be preprocessed
	 * @return			the result matrix preprocessed
	 */
	public static synchronized BoolMatrix PreProcessIsImage(BoolMatrix B){
		for (int i=0;i<Run.it.pixel_skip-1;i++)
			B=B.Dilate();
		for (int i=0;i<Run.it.pixel_skip-1;i++)
			B=B.Erode();
		
		//close and open
		
		B=B.Erode(); //get rid of junk		
		B=B.Dilate();
		
//		B=B.Dilate(); //put close non-junk together
//		B=B.Dilate();
		B=B.Dilate(); //put close non-junk together
		B=B.Dilate();
		B=B.Erode();
		B=B.Erode();
//		B=B.Erode();
//		B=B.Erode();
		
		return B;
	}
//	public static void ChangeClassifiedNames(String newProjectName){
//		String[] dirs={processedDir,PostProcess.outputDir};
//		for (String dir:dirs){
//			String[] allFiles=(new File(Run.it.homeDir+File.separator+dir+File.separator)).list();
//
//			for (String file:allFiles){
//				String[] pieces=file.split("-");
//				if (pieces[0].equals(Run.it.projectName))
//					if(!(new File(Run.it.homeDir+File.separator+dir+File.separator+file)).renameTo(new File(Run.it.homeDir+File.separator+dir+File.separator+newProjectName+"-"+pieces[1])))
//						System.out.println("could not rename:"+file);
//			}
//		}
//	}
}