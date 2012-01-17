
package GemIdentOperations;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import GemIdentCentroidFinding.PostProcess;
import GemIdentCentroidFinding.CentroidFinder.UnivariateDistribution;
import GemIdentClassificationEngine.Classify;
import GemIdentClassificationEngine.DatumSetup;
import GemIdentImageSets.BlissImageList;
import GemIdentImageSets.ColorVideoSetFromMatlab;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.ImageSetInterfaceWithUserColors;
import GemIdentImageSets.NuanceImageList;
import GemIdentImageSets.NuanceImageListInterface;
import GemIdentModel.Phenotype;
import GemIdentModel.Stain;
import GemIdentModel.TrainingImageData;
import GemIdentStatistics.Classifier;
import GemIdentStatistics.RandomForest;
import GemIdentStatistics.VisualizeClassifierImportances;
import GemIdentTools.IOTools;
import GemIdentTools.Thumbnails;
import GemIdentTools.Geometry.Rings;
import GemIdentTools.Geometry.Solids;
import GemIdentTools.Geometry.Spheres;
import GemIdentView.ClassifyProgress;
import GemIdentView.InitScreen;
import GemIdentView.JProgressBarAndLabel;
import GemIdentView.KClassifyPanel;
import GemIdentView.KFrame;

/**
 * <p>
 * This class, as a static object, begins the program, provides the
 * functionality for saving and loading, and defines program constants.
 * </p>
 * <p>
 * The one running instance, saved as the static "it," is the current
 * <b>GemIdent</b> project and is referenced all over the program. The class
 * methods provide for interaction with the model package, the other operations
 * classes, as well as the view package. This is the neural center of the
 * program - everything passes through this point in order to interact with each
 * other
 * </p>
 * 
 * @author Adam Kapelner
 */
public class Run implements Serializable {
	private static final long serialVersionUID = -772668422781511581L;

	// true for GemIdent, false for GemVident
	public static final boolean GemIdentOrGemVident = false;

	static String ourNodeName = "/GemVident/Run";
	static Preferences prefs = Preferences.userRoot().node(ourNodeName);

	/**
	 * begins the program - sets look and feel, sets this thread to high
	 * priority, and puts a {@link InitScreen new / open dialog} on the screen
	 */
	public static void main(String[] args) {

		/**
		 * Command line version has arguments, graphical version doesn't
		 */
		if (args.length > 0) {
			runCommandLine(args);
			return;
		}

		// System.setProperty("com.sun.media.jai.disableMediaLib", "true");
		// //this sometimes can be set to avoid the error
		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) {
			try {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			} catch (Exception e2) {
			}
		}

		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		new InitScreen();
	}

	/**
	 * Parses and runs command line classifier
	 * 
	 * @param args
	 */
	@SuppressWarnings("deprecation")
	private static void runCommandLine(String[] args) {
		if (args.length > 0 && args[0].compareTo("--help") == 0) {
			System.out.print("usage: gi ");
			System.out.print("classifier_filename ");
			System.out.print("gem_filename ");
			System.out.print("[percentile_range_start] ");
			System.out.print("[percentile_range_end] ");
			System.out.print("[classification_flag]\n");
			System.out.println();
			System.out.println();
			System.out
					.println("Put classification_flag to 1 to classify, 2 for centroid finding, 0 (default) for both.");
			System.out
					.println("The only required arguments are classifier_filename and gem_filename.");
			return;
		}

		File load_file = new File(args[1]);

		// String curDir = System.getProperty("user.dir");
		// System.out.println(curDir);

		RandomForest classifier = (RandomForest) IOTools.openFromXML(args[0]);
		System.out.println("loaded random forest: "
				+ args[0]
				+ " ("
				+ classifier.getNumTrees()
				+ " trees, "
				+ VisualizeClassifierImportances.ThreeDecimalDigitFormat
						.format(classifier.getError() * 100) + "% error)");

		it = (Run) IOTools.openFromXML(load_file.getAbsolutePath());

		LinkedHashMap<String, Stain> stains = ((ImageSetInterfaceWithUserColors) it.imageset)
				.getStains();

		try {
			String str = load_file.getParent();
			if (str == null)
				str = ".";
			it.imageset = new ColorVideoSetFromMatlab(str);
		} catch (Exception exc) {
			System.err
					.println("This Color Video set requires jpg files to be named '<project name>_000000n' where 'n' is the frame number. Please check your files and try again.");
			exc.printStackTrace();
			return;
		}
		if (!it.imageset.doesInitializationFileExist()) {
			System.err
					.println("This Color Video set requires jpg files to be named '<project name>_000000n' where 'n' is the frame number. Please check your files and try again.");
			return;
		}

		((ImageSetInterfaceWithUserColors) it.imageset).setStains(stains);

		// it.imageset.setHomedir(load_file.getParent());

		DatumSetup datumSetup = it.imageset.setUpDataExtractionMethod();

		JProgressBarAndLabel jpl = new JProgressBarAndLabel(0, 100, "");

		Run.InitializeClassPhenotypeMap();
		((ImageSetInterfaceWithUserColors) it.imageset)
				.OpenMahalanobisCubes(jpl);

		load_file.getParent();

		if (false)
			return;

		// System.err.println(it.imageset.getHomedir());
		ArrayList<String> files = it.imageset.GetImages();
		System.out.println("Using " + it.num_threads + " threads.");
		ArrayList<String> temp = new ArrayList<String>();

		Collections.sort(files);

		int rangeStart = 0;
		int rangeEnd = files.size();

		if (args.length >= 4) {
			rangeStart = (int) Math.ceil(Double.parseDouble(args[2])
					* files.size());
			rangeEnd = (int) Math.floor(Double.parseDouble(args[3])
					* files.size());
		}

		for (int i = 0; i < files.size(); i++) {
			if (i < rangeStart || i > rangeEnd)
				continue;
			String bb = files.get(i);
			// bb = load_file.getParent() +File.separatorChar + bb;
			System.out.println("\t" + bb);
			temp.add(bb);
		}

		System.out.println("Index range: " + rangeStart + "-" + rangeEnd + ", "
				+ temp.size() + " images");

		System.out.flush();

		ClassifyProgress progress = new ClassifyProgress(files.size());

		Timestamp t = new Timestamp(System.currentTimeMillis());
		String date = t.toString().split(" ")[0];
		PostProcess.outputDir = PostProcess.outputDirFirstName + "--" + date
				+ "-" + t.getHours() + "-" + t.getMinutes() + "-"
				+ t.getSeconds();
		if (!IOTools.DoesDirectoryExist(PostProcess.outputDir))
			(new File(Run.it.imageset
					.getFilenameWithHomePath(PostProcess.outputDir))).mkdir();

		int classifyFlag = 0;
		if (args.length >= 5) {
			classifyFlag = Integer.parseInt(args[4]);
		}

		if (classifyFlag == 0 || classifyFlag == 1)
			new Classify(datumSetup, temp, classifier, progress, null);
		if (classifyFlag == 0 || classifyFlag == 2) {
			PostProcess pp = new PostProcess(temp, progress, null, null);
			pp.FindCentroids();
		}
		return;
	}

	/** the program title */
	public static final String ProgramName = GemIdentOrGemVident ? "GemIdent"
			: "GemVident";
	/**
	 * the string to be used when building the window title (<b>GemIdent</b> is
	 * still in beta on the first release)
	 */
	private static final String VER = "1.1b";
	/** The title of the program */
	public static final String Title = ProgramName + " v" + VER + " - ";

	/** the default number of trees in the {@link RandomForest random forest} */
	transient public static final int DEFAULT_NUM_TREES = 50;
	/**
	 * the default number of threads to use during most program operations - get
	 * the number of available processors from the OS
	 */
	transient public static final int DEFAULT_NUM_THREADS = Runtime
			.getRuntime().availableProcessors();
	/**
	 * the default number of pixels to skip when {@link Classify classifying
	 * images}
	 */
	transient public static final int DEFAULT_PIXEL_SKIP = 1;
	/**
	 * the default number of pixels to classify before {@link Classify updating
	 * the screen}
	 */
	transient public static final int DEFAULT_R_BATCH_SIZE = 15000;
	/**
	 * relevant only for CRI Nuance image sets, this is the default
	 * magnification that each picture will be loaded at (see
	 * {@link Thumbnails#ScaleImage Scale Image})
	 */
	transient public static final float DEFAULT_MAGN = 1; // so nothing bad
															// happens

	transient public static final int DEFAULT_MULTIPLE_FRAMES_CHECK = 1;

	transient public static final int DEFAULT_CUTOFF_TOP = 120;
	transient public static final int DEFAULT_CUTOFF_MID = 20;
	transient public static final int DEFAULT_CUTOFF_BOTTOM = 20;

	/**
	 * this stores the current <b>GemIdent</b> project statically for convenient
	 * reference anywhere in the program (otherwise it would have to be passed
	 * to all classes)
	 */
	transient public static Run it;
	/**
	 * while the user is choosing between starting a new project or opening an
	 * old one, this stores the GUI being loaded in the background
	 */
	transient private static KFrame preloadedGUI;

	/**
	 * the minimum number of training points required for phenotypes in order to
	 * {@link Classify run a classification on this image set for this
	 * phenotype}
	 */
	private static final int MinimumPhenotypePointsNeeded = 10;

	// the project-specific data that is not saved / loaded:
	/** stores the entire {@link KFrame GUI} for the project} */
	transient private KFrame gui;
	/**
	 * the class responsible for coordinating classification, post-processing,
	 * or both
	 */
	transient private SetupClassification setupClassification;
	/**
	 * did the user make a change to the data model? If so, then the user is
	 * able to save
	 */
	transient private boolean isDirty;

	/**
	 * HUGE HACK: maps the phenotype id# to the phenotype name - better off
	 * making the whole thing a
	 * {@link GemIdentImageSets.ImageAndScoresBank.MapAndList MapAndList}
	 */
	public static HashMap<Integer, String> classMapBck;
	/**
	 * HUGE HACK: maps the phenotype name to the phenotype id# - better off
	 * making the whole thing a
	 * {@link GemIdentImageSets.ImageAndScoresBank.MapAndList MapAndList}
	 */
	public static HashMap<String, Integer> classMapFwd;

	/** HACK to map phenotype name to centroid finder parameters **/
	public HashMap<String, UnivariateDistribution> phenotypeDistMap = null;

	/**
	 * HACKS to store/retrieve cache settings. Used to be in ImageAndScoresBank.
	 **/
	public int SUPER_AND_SCORES_MAX_SIZE = 50; // large training sets?
	/** The maximum number of DataImages to be cached */
	public int DATAIMAGES_MAX_SIZE = 10; // those of you with low RAM - beware
	/** The maximum number of DataImages to be cached */
	public int NUANCE_RAW_IMAGES_MAX_SIZE = 108; // SUPER_AND_SCORES_MAX_SIZE *
													// 9, beware of low ram
	/** The maximum number of SuperImages to be cached */
	public int IS_IMAGES_MAX_SIZE = 50; // ditto
	/** The maximum number of Confusion Images to be cached */
	public int CONFUSION_IMAGES_MAX_SIZE = 10; // ditto
	/** The maximum number of frame score objects to be cached */
	public int NUM_FRAME_SCORE_OBJECTS = 1;

	/**
	 * Initializes the mappings from phenotype id# to phenotype name and vice
	 * versa
	 */
	public static void InitializeClassPhenotypeMap() {
		classMapBck = new HashMap<Integer, String>(Run.it.numPhenotypes());
		classMapBck.put(0, Phenotype.NON_NAME);
		classMapFwd = new HashMap<String, Integer>(Run.it.numPhenotypes());
		classMapFwd.put(Phenotype.NON_NAME, 0);
		int c = 0;

		Set<String> pixelFindList = Run.it
				.getPhenotyeNamesSaveNONAndFindPixels();
		for (String name : Run.it.getPhenotyeNamesSaveNON()) {
			if (pixelFindList.contains(name)) {
				c++;
				classMapBck.put(c, name);
				classMapFwd.put(name, c);
			} else
				classMapFwd.put(name, 0);
		}
	}

	// the data that is saved / loaded:

	// training data
	/**
	 * an order-preserving mapping from name of the phenotypes to the actual
	 * phenotype object
	 */
	private LinkedHashMap<String, Phenotype> phenotypes;

	// other critical variables
	/** the image set */
	public ImageSetInterface imageset;
	/**
	 * the user's project title (chosen at the {@link InitScreen New Project
	 * screen}
	 */
	public String projectName;
	/**
	 * the digital expansion factor of each of the input images (only relevant
	 * when analyzing CRI Nuance image sets)
	 */
	private float magn;

	// classify panel settings / user customizations
	/** the number of classification trees to use during classification */
	public int num_trees;
	/** the number of threads to use during classification */
	public int num_threads;
	/** the number of pixels to skip during classification */
	public int pixel_skip;
	/**
	 * the number of pixels to process before updating the screen during
	 * classification
	 */
	public int num_pixels_to_batch_updates;
	/** do multiple frames or not **/
	public int multiple_frames_check;
	/**
	 * the setting for the pics to classify (see
	 * {@link KClassifyPanel#CLASSIFY_ALL all},
	 * {@link KClassifyPanel#CLASSIFY_RANGE range},
	 * {@link KClassifyPanel#CLASSIFY_TRAINED trained},
	 * {@link KClassifyPanel#TEN_RANDOM 10 random},
	 * {@link KClassifyPanel#TWENTY_RANDOM 20 random},
	 * {@link KClassifyPanel#N_RANDOM N random}
	 */
	public int pics_to_classify;
	// /** the text the user wrote in the {@link KClassifyPanel#rangeText
	// classify specific images} field */
	// public String RANGE_TEXT;
	/**
	 * the text the user wrote in the {@link KClassifyPanel#nRandomText classify
	 * N random} field
	 */
	public Integer N_RANDOM;

	// results
	/**
	 * a mapping from the phenotype (where centroids are sought) to its total
	 * count in the last classification
	 */
	private LinkedHashMap<String, Long> totalCounts;
	/**
	 * a mapping from the phenotype (where centroids are sought) to its false
	 * negative rate (Type I error rate) in the last classification
	 */
	private LinkedHashMap<String, Double> errorRates;
	/**
	 * a mapping from image file to a set of coordinates in that image where the
	 * classification failed to find the user's training point (Type I error)
	 */
	private HashMap<String, HashSet<Point>> typeOneErrors;

	/**
	 * preloads the {@link Solids Solids reference}, the {@link Rings Rings
	 * reference} as well as the GUI while the user is selecting to
	 * {@link InitScreen Open / Create New project}
	 */
	public static void Preload() {
		// preload solids and rings vectors
		new Thread() {
			public void run() {
				setPriority(Thread.MIN_PRIORITY);
				Spheres.Build();
				Solids.Build();
				Rings.Build();
			}
		}.start();

		// preload gui components
		new Thread() {
			public void run() {
				setPriority(Thread.NORM_PRIORITY);
				preloadedGUI = new KFrame(); // create shell (just the pointer)
				preloadedGUI.DrawAll(); // put everything in gui (i need to ptr
										// to exist prior to this functiobn
										// that's why it's decomped)
			}
		}.start();
	}

	public Run() {
	} // serializable is happy

	/**
	 * Creates a new <b>GemIdent</b> project
	 * 
	 * @param projectName
	 *            the project's title
	 * @param imageset
	 *            the image set describing the analysis
	 * @param magn
	 *            the artificial magnification (only relevant in CRI Nuance
	 *            image sets)
	 */
	public Run(String projectName, ImageSetInterface imageset, float magn) {

		// System.out.println("Run() magn:"+magn);
		this.projectName = projectName;
		this.imageset = imageset;
		this.magn = magn;

		typeOneErrors = new HashMap<String, HashSet<Point>>();
	}

	/**
	 * after the project is {@link #Run initialized}, objects and parameters can
	 * be initialized and the gui can
	 * {@link KClassifyPanel #DefaultPopulateWindows() set up default views and
	 * the project title}
	 */
	private void BuildRunObjectFromDefaults() {

		// create transient info
		CreateTransientsAndThumbnails();
		// create default info
		phenotypes = new LinkedHashMap<String, Phenotype>();
		num_trees = Run.DEFAULT_NUM_TREES;
		num_threads = Run.DEFAULT_NUM_THREADS;
		pixel_skip = Run.DEFAULT_PIXEL_SKIP;
		multiple_frames_check = Run.DEFAULT_MULTIPLE_FRAMES_CHECK;
		num_pixels_to_batch_updates = Run.DEFAULT_R_BATCH_SIZE;
		pics_to_classify = KClassifyPanel.CLASSIFY_TRAINED;
		// RANGE_TEXT="";

		gui.DefaultPopulateWindows(imageset);
	}

	/**
	 * Creates the {@link Thumbnails#CreateAll thumbnails} then sets the
	 * preloaded gui to the true gui if we are loading from scratch
	 */
	private void CreateTransientsAndThumbnails() {
		gui = preloadedGUI; // preload gui
		if (!(imageset instanceof NuanceImageListInterface))
			Thumbnails.CreateAll(gui);
		GUIsetDirty(false);
	}

	// // save / save as / load / new / exit

	private static void OpenProject(File load_file) throws Exception{
		it = (Run) IOTools.openFromXML(load_file.getAbsolutePath());
		if (it.imageset instanceof ColorVideoSetFromMatlab){	
			LinkedHashMap<String, Stain> stains = ((ImageSetInterfaceWithUserColors) it.imageset)
			.getStains();
			it.imageset = new ColorVideoSetFromMatlab(load_file.getParent());
			((ImageSetInterfaceWithUserColors) it.imageset).setStains(stains);
			System.out.println("stains: "+Arrays.toString(stains.keySet().toArray()));
		}
		it.imageset.setHomedir(load_file.getParent());
		if (it.typeOneErrors == null)
			it.typeOneErrors = new HashMap<String, HashSet<Point>>();
		it.CreateTransientsAndThumbnails();
		it.gui.OpenPopulateWindows(it.imageset);
		it.GUIsetDirty(false);
		it.gui.AddOrUpdateRetrainTab(); // if we're loading chances are we don't
		// really care about the stain selection
		// as a priority (saves user a click) it
		// also sets immediately retraining
		// ability
		it.gui.setSeeTrainingPoints(true);
		InitializeBasedOnImageSetType();
		it.gui.DisableButtons();
		it.gui.repaint();
	}

	/**
	 * Opens a project from the hard disk. Prompts the user to choose the "gem"
	 * file that contains the project. "gem" files cannot be moved to other
	 * directories because they must have the image files and the project
	 * directories that <b>GemIdent</b> creates. It then creates a Run object
	 * (see {@link #it it}) by hydrating it from the
	 * {@link IOTools#openFromXML(String) opened XML file}. It then populates
	 * the GUI, creates thumbnails if necessary, and initializes, and
	 * {@link #InitializeBasedOnImageSetType() initializes the data structure
	 * specific to the image set}
	 * 
	 * @param initscreen
	 *            the initialization screen
	 */
	public static boolean OpenProject(InitScreen initscreen) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory())
					return true;
				String name = f.getName();
				String[] pieces = name.split("\\.");
				if (pieces.length != 2)
					return false;
				if (pieces[1].toLowerCase().equals("gem"))
					return true;
				return false;
			}

			public String getDescription() {
				return "gem";
			}
		});
		int result = chooser.showOpenDialog(null);
		initscreen.setVisible(false);
		if (result == JFileChooser.APPROVE_OPTION) {
			File load_file = chooser.getSelectedFile();
			Run.prefs.put("lastOpened", load_file.getAbsolutePath());
			try {
				OpenProject(load_file);
			} catch (Exception e) {
				e.printStackTrace();
				initscreen.setVisible(true);
				JOptionPane.showMessageDialog(null, "The file \""
						+ load_file.getName()
						+ "\" is not a GemIdent project or it is corrupted",
						"File Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			it.imageset.presave();
			return true;
		}
		initscreen.setVisible(true);
		return false;
	}

	/**
	 * If the user is working with a Bacus Labs set, it initializes a
	 * {@link BlissImageList BlissImageList}, if a CRI Nuance, it initializes a
	 * {@link NuanceImageList NuanceImageList}, etc
	 */
	private static void InitializeBasedOnImageSetType() {
		if (it.imageset instanceof BlissImageList) {
			it.gui.buildExampleImages(true);
			try {
				it.gui.buildGlobalsViewsInAnalysis();
			} catch (Exception e) {
			}
		} else if (it.imageset instanceof NuanceImageListInterface) {
			((NuanceImageListInterface) it.imageset).setTrainPanel(it.gui
					.getTrainingPanel());
			it.gui.DeleteColorTrainTab();
			// it.gui.DisableTrainingHelpers();
			it.gui.buildExampleImages(false);
			it.gui.AddAdjustColorButton();
			// only for old image sets:
			it.gui.TurnOnSophisticatedTrainingHelper();
		} else {
			it.gui.KillAnalysisTab();
			it.gui.KillPreProcessTab();
			it.gui.buildExampleImages(true);
			it.gui.DisableTrainingHelpers();
		}
	}

	/**
	 * Creates a new <b>GemIdent</b> project from the {@link InitScreen user's
	 * inputs}
	 * 
	 * @param projectName
	 *            the title of the project
	 * @param imageset
	 *            the image set object that describes the analysis set
	 * @param magn
	 *            the artificial magnification for CRI Nuance projects
	 */
	public static void NewProject(String projectName,
			ImageSetInterface imageset, float magn) {
		it = new Run(projectName, imageset, magn);
		it.GUIsetDirty(true);
		it.BuildRunObjectFromDefaults(); // need pointer to definitely be
											// there...
		InitializeBasedOnImageSetType();
		// this has to run in the same thread
		// System.out.println("initializing project... please wait");
		it.imageset.RunUponNewProject();
		imageset.LOG_AddToHistory("Begun Project");
	}

	/**
	 * Saves the project to the hard disk by
	 * {@link IOTools#saveToXML(Object, String) dumping to an XML file}. See
	 * section 2.2 in the manual for more information.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual<
	 *      /a>
	 */
	public static void SaveProject() {
		new Thread() { // thread off the presave . . .
			public void run() {
				it.imageset.presave();
			}
		}.start();
		try {
			IOTools.saveToXML(it, it.imageset
					.getFilenameWithHomePath(it.projectName + ".gem"));
		} catch (Exception e) {
			JOptionPane.showMessageDialog(Run.it.getGUI(), "Error saving file",
					"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		it.GUIsetDirty(false);
	}

	/** exits <b>GemIdent</b>, prompts user to save first */
	public void ExitProgram() {
		if (it.GUIisDirty()) {
			int result = JOptionPane.showConfirmDialog(null,
					"Would you like to save your changes?", "Document changed",
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION)
				SaveProject();
		}
		System.exit(0);
	}

	// ///frame stuff
	/** repaint entire GUI screen */
	public void FrameRepaint() {
		if (gui != null) // sometimes it's null
			gui.repaint();
	}

	/** get the location of the GUI on the user's desktop */
	public Point getGuiLoc() {
		if (gui == null)
			return null;
		else
			return gui.getLocation();
	}

	public KFrame getGUI() {
		return gui;
	}

	// ///all methods for phenotypes

	public int numPhenotypes() {
		return phenotypes.size();
	}

	public int numPhenotypesSaveNON() {
		return phenotypes.size() - 1;
	}

	public void AddPhenotype(Phenotype phenotype) {
		// System.out.println("adding:"+phenotype.getName());
		phenotypes.put(phenotype.getName(), phenotype);
	}

	public Phenotype getPhenotype(String name) {
		return phenotypes.get(name);
	}

	public int numPhenTrainingPoints() {
		int N = 0;
		for (Phenotype phenotype : phenotypes.values())
			N += phenotype.getTotalPoints();
		return N;
	}

	public int numPhenTrainingImages() {
		return getPhenotypeTrainingImages().size();
	}

	/** gets all filenames of images used for training phenotype examples */
	public Set<String> getPhenotypeTrainingImages() {
		HashSet<String> set = new HashSet<String>();
		for (Phenotype phenotype : phenotypes.values())
			for (TrainingImageData I : phenotype.getTrainingImages())
				set.add(I.getFilename());
		return set;
	}

	/** gets the "NON" phenotype */
	public Phenotype getNONPhenotype() {
		for (Phenotype phenotype : phenotypes.values())
			if (phenotype.isNON())
				return phenotype;
		return null;
	}

	public Collection<Phenotype> getPhenotypeObjects() {
		return phenotypes.values();
	}

	public Collection<Phenotype> getPhenotypesSaveNON() {
		HashSet<Phenotype> phenotypesSaveNON = new HashSet<Phenotype>();
		for (Phenotype phenotype : phenotypes.values())
			if (!phenotype.isNON())
				phenotypesSaveNON.add(phenotype);
		return phenotypesSaveNON;
	}

	public Set<String> getPhenotyeNames() {
		return phenotypes.keySet();
	}

	public Set<String> getPhenotyeNamesSaveNON() {
		Set<String> set = phenotypes.keySet();
		Set<String> setc = new HashSet<String>();
		for (String s : set)
			if (!Phenotype.isNONNAME(s))
				setc.add(s);
		return setc;
	}

	/** gets the phenotypes that the user wants classified */
	public Set<String> getPhenotyeNamesWithFindPixels() {
		Set<String> set = phenotypes.keySet();
		Set<String> setc = new HashSet<String>();
		for (String s : set)
			if ((phenotypes.get(s)).isFindPixels())
				setc.add(s);
		return setc;
	}

	/**
	 * gets the phenotypes that the user wants classified save the NON - the
	 * negative phenotype
	 */
	public Set<String> getPhenotyeNamesSaveNONAndFindPixels() {
		Set<String> set = getPhenotyeNamesSaveNON();
		Set<String> setc = new HashSet<String>();
		for (String s : set)
			if ((phenotypes.get(s)).isFindPixels())
				setc.add(s);
		return setc;
	}

	/**
	 * gets the phenotypes that the user wants classified and post processed for
	 * centroids (save the NON - the negative phenotype)
	 */
	public Set<String> getPhenotyeNamesSaveNONAndFindCenters() {
		Set<String> set = getPhenotyeNamesSaveNON();
		Set<String> setc = new HashSet<String>();
		for (String s : set)
			if ((phenotypes.get(s)).isFindCentroids())
				setc.add(s);
		return setc;
	}

	public Color getPhenotypeDisplayColor(String name) {
		return phenotypes.get(name).getDisplayColor();
	}

	/**
	 * the default radius multiple that <b>GemIdent</b> checks when identifying
	 * pixels
	 */
	// private static final double InfluenceRadius=1.5;
	private static final double InfluenceRadius = 1;

	/**
	 * the radius (in pixels) that <b>GemIdent</b> checks around when
	 * identifying pixels
	 */
	public int getMaxPhenotypeRadiusPlusMore(Double influence_radius) {
		int M = 0;
		for (Phenotype phenotype : phenotypes.values()) {
			int Mo = phenotype.getRmax();
			// System.out.println(phenotype.getName() + " radius: " + Mo);
			if (Mo > M)
				M = Mo;
		}
		M = (int) Math.round((influence_radius == null ? InfluenceRadius
				: influence_radius)
				* M);
		// System.out.println("Maximum radius: "+M);
		return M;
	}

	public void DeletePhenotype(String key) {
		phenotypes.remove(key);
	}

	/**
	 * Checks if the user supplied <b>GemIdent</b> with sufficient number of
	 * training points for all phenotypes to go ahead with classification.
	 */
	public HashSet<String> EnoughPhenotypeTrainPoints() {
		HashSet<String> deliquents = new HashSet<String>();
		for (Phenotype phenotype : phenotypes.values())
			if (phenotype.getTotalPoints() < MinimumPhenotypePointsNeeded)
				deliquents.add(phenotype.getName());
		return deliquents;
	}

	/**
	 * removes a training image used for phenotype training (if there is data in
	 * it, confirms with the user)
	 */
	public boolean RemoveImageFromPhenotypeSet(String filename) {
		// System.out.println("Run.RemoveImageFromPhenotypeSet()");
		boolean dialog = false;
		for (Phenotype phenotype : phenotypes.values()) {
			if (phenotype.hasImage(filename)) {
				TrainingImageData image = phenotype.getTrainingImage(filename);
				if (image.getNumPoints() > 0)
					dialog = true;
			}
		}
		int result = JOptionPane.YES_OPTION;
		if (dialog)
			result = JOptionPane
					.showConfirmDialog(
							it.getGUI(),
							"This image contains training data. Are you sure you want to remove it?",
							"Points deletion", JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION) {
			for (Phenotype phenotype : phenotypes.values())
				if (phenotype.hasImage(filename))
					phenotype.RemoveTrainingImage(filename);
			GUIsetDirty(true);
			return true;
		}
		return false;
	}

	// ////actual functional methods

	public LinkedHashMap<String, Long> getTotalCounts() {
		return totalCounts;
	}

	public LinkedHashMap<String, Double> getErrorRates() {
		return errorRates;
	}

	@SuppressWarnings("unchecked")
	public Collection<String> GetAllClassifiedImages() {
		Collection<String> files = (Collection<String>) imageset.GetImages()
				.clone();
		for (String phenotype : Run.it.getPhenotyeNamesSaveNONAndFindPixels())
			for (String file : imageset.GetImages())
				if (!IOTools.FileExists(Classify.GetIsName(file, phenotype)))
					files.remove(file);
		return files;
	}

	/** gets the collection of files to classify based on the user's choice */
	public Collection<String> GetImageListToClassify() {
		Collection<String> files = null;
		if (pics_to_classify == KClassifyPanel.CLASSIFY_ALL)
			files = imageset.GetImages();
		// System.out.println("CLASSIFY_ALL");
		// else if (PICS_TO_CLASSIFY == KClassifyPanel.CLASSIFY_RANGE){
		// files=gui.getClassifyPanel().RangeToFilenames();
		// files.addAll(gui.getTrainingImageSet());
		// // System.out.println("CLASSIFY_RANGE");
		// }
		else if (pics_to_classify == KClassifyPanel.CLASSIFY_TRAINED)
			files = gui.getTrainingImageSet();
		else if (pics_to_classify == KClassifyPanel.CLICKED_ON)
			files = imageset.getClickedonimages();
		else if (pics_to_classify == KClassifyPanel.CLASSIFY_REMAINING)
			files = imageset.getImagesNotClassifiedYet();
		// System.out.println("CLASSIFY_TRAINED");
		else {
			ArrayList<String> all = imageset.GetImages();
			Collection<String> trained = gui.getTrainingImageSet();
			all.removeAll(trained);
			files = new HashSet<String>();
			if (pics_to_classify == KClassifyPanel.TEN_RANDOM) {
				if (all.size() < 10)
					files.addAll(all);
				else {
					for (int i = 0; i < 10; i++) {
						int k = (int) Math.floor(Math.random() * all.size());
						files.add(all.remove(k));
					}
				}
			} else if (pics_to_classify == KClassifyPanel.TWENTY_RANDOM) {
				if (all.size() < 20)
					files.addAll(all);
				else {
					for (int i = 0; i < 20; i++) {
						int k = (int) Math.floor(Math.random() * all.size());
						files.add(all.remove(k));
					}
				}
			} else if (pics_to_classify == KClassifyPanel.N_RANDOM) {
				if (N_RANDOM != null) {
					if (all.size() < N_RANDOM)
						files.addAll(all);
					else {
						for (int i = 0; i < N_RANDOM; i++) {
							int k = (int) Math
									.floor(Math.random() * all.size());
							files.add(all.remove(k));
						}
					}
				}
			}
			// add those trained back in only if it's not a "clicked-on set"
			files.addAll(trained);
		}
		// for (String file : files){
		// System.out.println("file to be classified: " + file);
		// }
		return files;
	}

	/**
	 * Run a classification and a post-processing
	 * 
	 * @param openProgress
	 *            the open color cubes progress bar
	 * @param trainingProgress
	 *            the create training data progress bar
	 * @param buildProgress
	 *            the construct machine learning classifiers progress bar
	 * @param classifier
	 *            did the user load a classifier from the HD?
	 */
	public void DoBothOnSepThread(JProgressBarAndLabel openProgress,
			JProgressBarAndLabel trainingProgress,
			JProgressBarAndLabel buildProgress, Classifier classifier) {
		setupClassification = new SetupClassification(this, openProgress,
				trainingProgress, buildProgress, true, true, classifier);
	}

	/**
	 * Run just a classification
	 * 
	 * @param openProgress
	 *            the open color cubes progress bar
	 * @param trainingProgress
	 *            the create training data progress bar
	 * @param buildProgress
	 *            the construct machine learning classifiers progress bar
	 * @param classifier
	 *            did the user load a classifier from the HD?
	 */
	public void DoClassificationOnSepThread(JProgressBarAndLabel openProgress,
			JProgressBarAndLabel trainingProgress,
			JProgressBarAndLabel buildProgress, Classifier classifier) {
		setupClassification = new SetupClassification(this, openProgress,
				trainingProgress, buildProgress, true, false, classifier);
	}

	/**
	 * Run just a post-processing
	 * 
	 * @param openProgress
	 *            the open color cubes progress bar
	 * @param trainingProgress
	 *            the create training data progress bar
	 * @param buildProgress
	 *            the construct machine learning classifiers progress bar
	 * @param classifier
	 *            did the user load a classifier from the HD?
	 */
	public void DoPostProcessOnSepThread(JProgressBarAndLabel openProgress,
			JProgressBarAndLabel trainingProgress,
			JProgressBarAndLabel buildProgress, Classifier classifier) {
		setupClassification = new SetupClassification(this, openProgress,
				trainingProgress, buildProgress, false, true, classifier);
	}

	/** stop the classification or post-processing at whatever stage it's at */
	public void StopClassifying() {
		setupClassification.Stop();
	}

	/** is the project dirty? If so, it can be saved */
	public boolean GUIisDirty() {
		return isDirty;
	}

	/** set the project to dirty */
	public void GUIsetDirty(boolean isDirty) {
		this.isDirty = isDirty;
		if (gui != null) {
			if (!isDirty)
				gui.DisableSave();
			else
				gui.EnableSave();
		}
	}

	/**
	 * given a previous time in ms, returns the time since elapsed in hr/min/s
	 * format as a string
	 */
	public static String TimeElapsed(long time) {
		return FormatSeconds((int) (System.currentTimeMillis() - time) / 1000);
	}

	/** given a time in ms, return the time in hr/min/s format as a string */
	public static String FormatSeconds(int s) {
		int h = (int) Math.floor(s / ((double) 3600));
		s -= (h * 3600);
		int m = (int) Math.floor(s / ((double) 60));
		s -= (m * 60);
		return "" + h + "hr " + m + "m " + s + "s";
	}

	// getters and setters for serializable to work:

	/**
	 * Now obsolete.
	 */
	public void setCUTOFF_BOTTOM(int value) {
	}

	/**
	 * Now obsolete.
	 */
	public void setCUTOFF_MID(int value) {
	}

	/**
	 * Now obsolete.
	 */
	public void setCUTOFF_TOP(int value) {
	}

	public int getNFSO() {
		return this.NUM_FRAME_SCORE_OBJECTS;
	}

	public void setNFSO(int value) {
		this.NUM_FRAME_SCORE_OBJECTS = value;
	}

	public int getCIMS() {
		return this.CONFUSION_IMAGES_MAX_SIZE;
	}

	public void setCIMS(int value) {
		this.CONFUSION_IMAGES_MAX_SIZE = value;
	}

	public int getIIMS() {
		return this.IS_IMAGES_MAX_SIZE;
	}

	public void setIIMS(int value) {
		this.IS_IMAGES_MAX_SIZE = value;
	}

	public int getNRIMS() {
		return this.NUANCE_RAW_IMAGES_MAX_SIZE;
	}

	public void setNRIMS(int value) {
		this.NUANCE_RAW_IMAGES_MAX_SIZE = value;
	}

	public int getDMS() {
		return this.DATAIMAGES_MAX_SIZE;
	}

	public void setDMS(int value) {
		this.DATAIMAGES_MAX_SIZE = value;
	}

	public int getSASMS() {
		return this.SUPER_AND_SCORES_MAX_SIZE;
	}

	public void setSASMS(int value) {
		this.SUPER_AND_SCORES_MAX_SIZE = value;
	}

	public HashMap<String, UnivariateDistribution> getPhenotypeDistMap() {
		return phenotypeDistMap;
	}

	public void setPhenotypeDistMap(
			HashMap<String, UnivariateDistribution> phenotypeDistMap) {
		this.phenotypeDistMap = phenotypeDistMap;
	}

	public int getMULTIPLE_FRAMES_CHECK() {
		return multiple_frames_check;
	}

	public void setMULTIPLE_FRAMES_CHECK(int multiple_frames_check) {
		this.multiple_frames_check = multiple_frames_check;
	}

	public int getNUM_THREADS() {
		return num_threads;
	}

	public void setNUM_THREADS(int num_threads) {
		this.num_threads = num_threads;
	}

	public int getNUM_TREES() {
		return num_trees;
	}

	public void setNUM_TREES(int num_trees) {
		this.num_trees = num_trees;
	}

	public int getPICS_TO_CLASSIFY() {
		return pics_to_classify;
	}

	public void setPICS_TO_CLASSIFY(int pics_to_classify) {
		this.pics_to_classify = pics_to_classify;
	}

	public int getPIXEL_SKIP() {
		return pixel_skip;
	}

	public void setPIXEL_SKIP(int pixel_skip) {
		this.pixel_skip = pixel_skip;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public int getR_BATCH_SIZE() {
		return num_pixels_to_batch_updates;
	}

	public void setR_BATCH_SIZE(int r_batch_size) {
		this.num_pixels_to_batch_updates = r_batch_size;
	}

	// public String getRANGE_TEXT() {
	// return RANGE_TEXT;
	// }
	// public void setRANGE_TEXT(String range_text) {
	// RANGE_TEXT = range_text;
	// }

	public void setPhenotypes(LinkedHashMap<String, Phenotype> phenotypes) {
		this.phenotypes = phenotypes;
	}

	public LinkedHashMap<String, Phenotype> getPhenotypes() {
		return phenotypes;
	}

	public Integer getN_RANDOM() {
		return N_RANDOM;
	}

	public void setN_RANDOM(Integer n_random) {
		N_RANDOM = n_random;
	}

	public void setErrorRates(LinkedHashMap<String, Double> errorRates) {
		this.errorRates = errorRates;
	}

	public void resetErrorRates() {
		errorRates = null;
	}

	public void setTotalCounts(LinkedHashMap<String, Long> totalCounts) {
		this.totalCounts = totalCounts;
	}

	public void resetTotalCounts() {
		totalCounts = null;
	}

	public float getMagn() {
		return magn;
	}

	public void setMagn(float magn) {
		this.magn = magn;
	}

	public HashMap<String, HashSet<Point>> getTypeOneErrors() {
		return typeOneErrors;
	}

	public void setTypeOneErrors(HashMap<String, HashSet<Point>> typeOneErrors) {
		this.typeOneErrors = typeOneErrors;
	}

	public void resetTypeOneErrors() {
		typeOneErrors = new HashMap<String, HashSet<Point>>();
	}

	public ImageSetInterface getImageset() {
		return imageset;
	}

	public void setImageset(ImageSetInterface imageset) {
		this.imageset = imageset;
	}

	// some convenience methods of image set types
	public ImageSetInterfaceWithUserColors getUserColorsImageset() {
		if (imageset instanceof ImageSetInterfaceWithUserColors)
			return (ImageSetInterfaceWithUserColors) imageset;
		return null;
	}

	public static boolean macro1(InitScreen initscreen) {
		String lastFile = Run.prefs.get("lastOpened", "null");
		if (lastFile == "null") {
			return false;
		}
		File load_file = new File(lastFile);
		try {
			OpenProject(load_file);
		} catch (Exception e) {
			e.printStackTrace();
			initscreen.setVisible(true);
			JOptionPane.showMessageDialog(null, "The file \""
					+ load_file.getName()
					+ "\" is not a GemIdent project or it is corrupted",
					"File Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		it.imageset.presave();
		return true;
	}
}