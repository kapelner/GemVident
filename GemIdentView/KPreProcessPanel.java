
package GemIdentView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RenderedImage;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import GemIdentCentroidFinding.ViaSegmentation.ImageSegmentation;
import GemIdentImageSets.DataImage;
import GemIdentImageSets.NuanceImageListInterface;
import GemIdentImageSets.NuanceSubImage;
import GemIdentImageSets.RegularSubImage;
import GemIdentOperations.Run;
import GemIdentTools.ImageFilter;
import GemIdentTools.ImageUtils;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentTools.Matrices.ShortMatrix;


/**
 * Controls and houses the classification & post-processing panel. For discussion on
 * the execution of a classification via the classify button, see section 4.1 of the manual.
 * For discussion on setting the classification parameters, see section 4.1.1 of the manual.
 * For discussion on picking a subset of the images to classify, see section 4.1.2 of the 
 * manual. And for discussion on reclassification, see section 5.5 of the manual.
 * 
 * @author Adam Guetz
 *
 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
 */
@SuppressWarnings("serial")
public class KPreProcessPanel extends KPanel{
	
	/** the slider that controls how much neighborhood to take around a pixel  */
	protected JSlider smoothingSlider;
	/** the slider that determines the threshold between white and black */
	protected JSlider thresholdSlider;
	/** Button to make things go. **/
	protected JButton previewButton;
	/** Button to make things start over. **/
	protected JButton resetButton;
	/** Button to make things greyscale. **/
	protected JButton greyscaleButton;
	/** Button to change the waves. **/
	protected JButton waveButton;
	/** the box that holds the settings panel in the Western region */
	protected JPanel settingsPanel;
	
	protected DataImage currentImage;
	protected DataImage prevImage;

	protected boolean isGreyscale;
	protected boolean madeGreyscale;
	
	/** overview image object **/
	protected DataImage overview_image;
	protected NuanceImageListInterface imageset;

	//private static final Dimension settingsDim=new Dimension(200,500);

	protected int wave_no;
	private JButton saveFile;
	private JButton loadFile;
	private JCheckBox smoothingButton;
	private JCheckBox thresholdButton;
	private JButton applyButton;
	private JButton regionGrowingButton;
	private JButton rgMacroButton;
	
//	private RegionGrowing rg;
	private JButton regionGrowingStepButton;
	private DataImage overlayImage;
	private boolean overlay;
	private JButton overlayButton;
	private JButton regionMergingButton;
	private JButton regionMergingStepButton;
	private JSpinner seedSpinner;
	private JSpinner mergeSpinner;
	private boolean smallViewToggle;
	private JButton smallViewButton;
	private JButton spectralButton;
	private JButton evalueButton;
	//protected SpectralImagePartition sip;
	private JButton isoperimetricButton;
	private JButton invertButton;
	private JButton centroidsButton;
	private JButton addAttributeButton;
//	private JButton exportAttributeButton;
	private JButton showBoundaryButton;
	private JButton addFilterFeature;
	private JButton trainImageButton;
	private JButton overviewButton;

	
	/** initializes the image panel, sets up the option box in the west, sets listeners to the option box */
	public KPreProcessPanel(){
		super();
		imagePanel=new KImagePreProcessPanel(this);
		setImagePanel(imagePanel);
		imagePanel.setDisplayImage(null);
		buildSettingsPanel();
		SetUpListeners();
		add(settingsPanel,BorderLayout.WEST);
		
		initializeFields();
	}
	private void initializeFields(){
		wave_no = 0;
		madeGreyscale = false;
		isGreyscale = false;
		currentImage = null;
		prevImage = null;
		overlayImage = null;
		overlay = false;
		smallViewToggle = true;
//		rg = null;
	}
	
    /** initialize panel after opening **/
	public void DefaultPopulateBrowser(){
		buildImage();
	}

	/** sets an image to be displayed in the image panel (see {@link KImagePanel#setDisplayImage(DataImage) setDisplayImage}*/
	public void buildImage(){		
		imageset = null;
		if (Run.it.imageset instanceof NuanceImageListInterface)		
			imageset = (NuanceImageListInterface)Run.it.imageset;
		else return;
		overview_image = new NuanceSubImage(imageset, imageset.getWidth() * imageset.getHeight() + 1, false);
		((NuanceSubImage) overview_image).BuildDisplayAndPixelDistrs();
		currentImage = overview_image.clone();
		prevImage=currentImage.clone();
		repaintImagePanel();
	}

	/** populate the Western region after instantiating all the options components */
	protected void buildSettingsPanel(){
		settingsPanel = new JPanel();
        //settingsPanel.setPreferredSize(settingsDim);
        settingsPanel.setLayout(new BorderLayout());
		
        Box filter_box = Box.createVerticalBox();
		filter_box.add(Box.createHorizontalGlue());
		Box smooth_box = Box.createHorizontalBox();

		smoothingSlider = new JSlider(JSlider.HORIZONTAL,0,80,0);
		//smoothingSlider.setPreferredSize(new Dimension(80,this.smoothingSlider.getHeight()));
		smoothingButton = new JCheckBox();
		smooth_box.add(smoothingSlider);
		smooth_box.add(smoothingButton);
	

		Box threshold_box = Box.createHorizontalBox();
		thresholdSlider = new JSlider(JSlider.HORIZONTAL,0,80,0);
		//thresholdSlider.setPreferredSize(new Dimension(80,this.thresholdSlider.getHeight()));
		thresholdButton = new JCheckBox();
		threshold_box.add(thresholdSlider);
		threshold_box.add(thresholdButton);
		
		
		Box buttons_box = Box.createHorizontalBox();
				
		previewButton = new JButton("Preview");		
		applyButton = new JButton("Apply");
		addFilterFeature = new JButton("Add filter as feature");
		buttons_box.add(previewButton);
		buttons_box.add(applyButton);
		buttons_box.add(addFilterFeature);
		
		//filter_box.add(new JLabel("Filtration",JLabel.CENTER));
		//filter_box.add(Box.createVerticalStrut(10));
		//Box t1 = Box.createVerticalBox();
		//t1.setBorder(BorderFactory.createLineBorder(Color.black));
		//t1.add(Box.createHorizontalGlue());
		//filter_box.add(t1);
		
		filter_box.add(new JLabel("Smoothing:",JLabel.RIGHT));
		filter_box.add(smooth_box);
		filter_box.add(Box.createVerticalStrut(10));
		//filter_box.add(new JSeparator());

		filter_box.add(new JLabel("Threshold:",JLabel.RIGHT));
		filter_box.add(threshold_box);
		filter_box.add(Box.createVerticalStrut(10));
		filter_box.add(Box.createHorizontalGlue());
		//filter_box.add(new JSeparator());

		filter_box.add(buttons_box);


		
		Box regionGrowingBox = Box.createVerticalBox();
		regionGrowingBox.add(Box.createHorizontalGlue());

		Box seedSpinnerBox = Box.createHorizontalBox();
		Box mergeSpinnerBox = Box.createHorizontalBox();
		Box growButtonsBox = Box.createHorizontalBox();
		Box mergingButtonsBox = Box.createHorizontalBox();
		
		
		SpinnerModel seedSpinnerModel = new SpinnerNumberModel(10, //initial value
                2, //min
                10000, //max
                3);                //step
		
		JLabel seedSpinnerLabel = new JLabel("Number of seeds");
        seedSpinnerBox.add(seedSpinnerLabel);

        seedSpinner = new JSpinner(seedSpinnerModel);
        seedSpinnerLabel.setLabelFor(seedSpinner);
        seedSpinnerBox.add(seedSpinner);

		SpinnerModel mergeSpinnerModel = new SpinnerNumberModel(2, //initial value
                2, //min
                10000, //max
                1);                //step
		
		JLabel mergeSpinnerLabel = new JLabel("Desired # of regions");
        mergeSpinnerBox.add(mergeSpinnerLabel);

        mergeSpinner = new JSpinner(mergeSpinnerModel);
        mergeSpinnerLabel.setLabelFor(mergeSpinner);
        mergeSpinnerBox.add(mergeSpinner);

		regionGrowingButton = new JButton("Grow");
		growButtonsBox.add(regionGrowingButton);
		regionGrowingStepButton = new JButton("Step");
		growButtonsBox.add(regionGrowingStepButton);
		regionMergingButton = new JButton("Merge");
		mergingButtonsBox.add(regionMergingButton);
		regionMergingStepButton = new JButton("Step");
		mergingButtonsBox.add(regionMergingStepButton);

        Box b1Box = Box.createVerticalBox();
		b1Box.add(growButtonsBox);
		b1Box.add(mergingButtonsBox);

		rgMacroButton = new JButton("Do all");
		Box b2Box = Box.createHorizontalBox();
		b2Box.add(b1Box);
		b2Box.add(rgMacroButton);		
		
		regionGrowingBox.add(seedSpinnerBox);
		regionGrowingBox.add(mergeSpinnerBox);
		regionGrowingBox.add(b2Box);

		Box viewBox = Box.createHorizontalBox();		
		overlayButton = new JButton("Toggle Overlay");
		viewBox.add(overlayButton);
		smallViewButton = new JButton("Toggle Overview");
		viewBox.add(smallViewButton);
		showBoundaryButton = new JButton("Show Boundary");
		viewBox.add(showBoundaryButton);
		
		regionGrowingBox.add(viewBox);
		
		Box b2_box = Box.createVerticalBox();
		b2_box.add(Box.createHorizontalGlue());
		invertButton = new JButton("Invert Colors");		
		b2_box.add(invertButton);		
		waveButton = new JButton("Cycle Wavelengths");		
		b2_box.add(waveButton);		
		//b2_box.add(Box.createVerticalStrut(10));

		greyscaleButton = new JButton("Greyscale");		
		b2_box.add(greyscaleButton);		
		//b2_box.add(Box.createVerticalStrut(10));

		resetButton = new JButton("Reset Image");		
		overviewButton = new JButton("Load Overview Image");		
		trainImageButton = new JButton("Load Training Image");		
		b2_box.add(resetButton);
		b2_box.add(overviewButton);
		b2_box.add(trainImageButton);
		//b2_box.add(Box.createVerticalStrut(10));
		//b2_box.add(Box.createHorizontalGlue());

		Box file_box = Box.createHorizontalBox();
		
		saveFile = new JButton("Save");
		loadFile = new JButton("Load");
		file_box.add(saveFile);
		file_box.add(loadFile);
		
		Box master_box = Box.createVerticalBox();

		filter_box.setBorder(BorderFactory.createLineBorder(Color.black));
		//b2_box.setBorder(BorderFactory.createLineBorder(Color.black));
		//file_box.setBorder(BorderFactory.createLineBorder(Color.black));

		spectralButton = new JButton("Spectral Partition");
		isoperimetricButton = new JButton("Isoperimetric Partition");
		evalueButton = new JButton("Eigenvalue");
		centroidsButton = new JButton("Find Centroids");
		addAttributeButton = new JButton("Add Region Attribute to Classifier");
//		exportAttributeButton = new JButton("Export added attributes to File");

		Box spectralBox = Box.createVerticalBox();
		spectralBox.add(Box.createHorizontalGlue());
		spectralBox.add(spectralButton);
		spectralBox.add(isoperimetricButton);
		//spectralBox.add(evalueButton);
		spectralBox.add(centroidsButton);
		
		Box attributeBox = Box.createVerticalBox();
		attributeBox.add(addAttributeButton);
		//attributeBox.add(exportAttributeButton);
			
		GUIFrame filter_panel = new GUIFrame("Filter");
		filter_panel.add(filter_box);
		GUIFrame region_panel = new GUIFrame("Region Growing");

		region_panel.add(regionGrowingBox);
		GUIFrame spectral_panel = new GUIFrame("Spectral Partition");		
		spectral_panel.add(spectralBox);

		GUIFrame attribute_panel = new GUIFrame("export attribute");		
		attribute_panel.add(attributeBox);

		master_box.add(filter_panel);
		master_box.add(region_panel);
		master_box.add(spectral_panel);
		master_box.add(b2_box);
		master_box.add(file_box);
		master_box.add(attribute_panel);
		master_box.add(Box.createVerticalGlue());
		
		settingsPanel.add(master_box);
	}

	private void drawFilter(){
		if (currentImage == null) return;
		double smoothing = (double) smoothingSlider.getValue()/smoothingSlider.getMaximum();
		double threshold = (double) thresholdSlider.getValue()/thresholdSlider.getMaximum();
		if (prevImage != null) currentImage = prevImage.clone();

		//BufferedImage nextImage = (BufferedImage) (currentImage.clone().getAsBufferedImage());
		BufferedImage nextImage = null;
		
		if (smoothingButton.isSelected()){
//			BoxBlurFilter filt = new BoxBlurFilter();
//			filt.setRadius((int)(smoothing*20));
//			filt.setIterations(5);
//			filt.setPremultiplyAlpha(false);
			//System.err.println(filt.getRadius());
			nextImage = gaussianFilter(nextImage,smoothing);
			//nextImage = filt.filter(currentImage.clone().getAsBufferedImage(),null);
			currentImage = new RegularSubImage(nextImage);
		}

		if (thresholdButton.isSelected()){
			nextImage = thresholdFilter(currentImage.getAsBufferedImage(),threshold);
			currentImage = new RegularSubImage(nextImage);			
		}
		
		//RegularSubImage ni = newRegularSubImage(nextImage);
		
		//currentImage = ni.clone();
		repaintImagePanel();
	}
	
	/** sets up appropriate listeners for all the options and buttons in the Western region */
	private void SetUpListeners(){	
		previewButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						drawFilter();
					}
				}
			);
		
		applyButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						drawFilter();
						prevImage = currentImage.clone();
					}
				}	
			);

		thresholdSlider.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent arg0) {
						//drawFilter();						
						thresholdButton.setSelected(true);
					}
				}
				);
		smoothingSlider.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent arg0) {
						//drawFilter();						
						smoothingButton.setSelected(true);
					}
				}
				);
		rgMacroButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						regionGrowingButton.doClick();
						regionMergingButton.doClick();
					}
				});
//		regionMergingButton.addActionListener(
//				new ActionListener(){
//					public void actionPerformed(ActionEvent e){
//
//						if (currentImage == null) return;
//						if (rg == null || rg.hasBoundaryPoints()) {
//							return;
//						}
//						if (!rg.mergeInitialized)
//							rg.initMergeRegions();	
//						
//						
//						int targetNumRegions = (Integer)mergeSpinner.getValue();
//						
//						while(rg.getNumRegions() > targetNumRegions){
//							rg.merge();
//							System.out.print(".");
//						}
//						if(rg.getNumRegions() == targetNumRegions)
//							System.out.println();
//						
//						
//						overlayImage = rg.getRegionImage();
//						overlay = true;
//						
//						madeGreyscale = false;
//						repaintImagePanel();
//					}
//				}
//			);
//		regionMergingStepButton.addActionListener(
//				new ActionListener(){
//					public void actionPerformed(ActionEvent e){
//						if (currentImage == null) return;
//						if (rg == null || rg.hasBoundaryPoints()) {
//							return;
//						}
//						if (!rg.mergeInitialized)
//							rg.initMergeRegions();	
//
//						int targetNumRegions = (Integer)mergeSpinner.getValue();
//						
//						if (rg.getNumRegions() > targetNumRegions){
//							rg.merge();	
//							System.out.print(".");
//						}
//						
//						if(rg.getNumRegions() == targetNumRegions)
//							System.out.println();
//						
//						overlayImage = rg.getRegionImage();
//						overlay = true;
//						
//						madeGreyscale = false;
//						repaintImagePanel();
//					}
//				}
//			);
		spectralButton.addActionListener(
				new ActionListener(){

					public void actionPerformed(ActionEvent e){
						if (currentImage == null) return;
						
						
						
						ShortMatrix partitionMatrix = ImageSegmentation.spectralPartition(currentImage.getAsBufferedImage(),2);
						overlayImage = new RegularSubImage(ImageSegmentation.partitionToImage(partitionMatrix));
						overlay = true;
						
						madeGreyscale = false;
						repaintImagePanel();						
					}
				}
			);
		isoperimetricButton.addActionListener(
				new ActionListener(){

					public void actionPerformed(ActionEvent e){
						if (currentImage == null) return;
						
						ShortMatrix partitionMatrix = ImageSegmentation.isoperimetricPartition(currentImage.getAsBufferedImage(),2);
						overlayImage = new RegularSubImage(ImageSegmentation.partitionToImage(partitionMatrix));
						overlay = true;
						/*
						if (sip == null)
							sip = new SpectralImagePartition(new RegularSubImage(currentImage.getAsBufferedImage()));
							*/

						
						madeGreyscale = false;
						repaintImagePanel();						
					}
				}
			);
		evalueButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						if (currentImage == null) return;

						/*
						if (sip == null)
							sip = new SpectralImagePartition(new RegularSubImage(currentImage.getAsBufferedImage()));

						
						overlayImage = sip.displayEigenvectorAsRegularSubImage();
						overlay = true;
						
						madeGreyscale = false;
						*/
						repaintImagePanel();						
					}
				}
			);
		centroidsButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						if (currentImage == null) return;

						//int lowValue = 30;
						
						int cutoffTop = 115;
						int cutoffMid = 20;
						int cutoffBottom = 20;

						BoolMatrix centroids = ImageSegmentation.findBlobCentroids(currentImage.getAsBufferedImage(),cutoffTop,cutoffMid,cutoffBottom);
						
						overlayImage = new RegularSubImage(ImageSegmentation.partitionToImage(centroids));
						overlay = true;
						madeGreyscale = false;

						repaintImagePanel();						
					}
				}
			);
//		regionGrowingButton.addActionListener(
//				new ActionListener(){
//					public void actionPerformed(ActionEvent e){
//						if (currentImage == null) return;
//						if (rg == null) {
//							rg = new RegionGrowing(new RegularSubImage(currentImage.clone().getAsBufferedImage()),(Integer) seedSpinner.getValue());
//						}
//						
//						isGreyscale = false;
//						
//						rg.segment();
//
//						overlayImage = rg.getRegionImage();
//						overlay = true;
//						
//						madeGreyscale = false;
//						repaintImagePanel();						
//					}
//				}
//			);

		
//		addAttributeButton.addActionListener(
//				new ActionListener(){
//					public void actionPerformed(ActionEvent e){
//						if (Run.it.extraFeatures == null){
//							Run.it.extraFeatures = new ArrayList<Feature>();
//							Run.it.extraFeatures.add(new GlobalXPositionFeature());
//							Run.it.extraFeatures.add(new GlobalYPositionFeature());
//						}
//						if (Run.it.extraAttributes == null){
//							Run.it.extraAttributes = new ArrayList<weka.core.Attribute>();
//							Run.it.extraAttributes.add(new weka.core.Attribute("X_position"));
//							Run.it.extraAttributes.add(new weka.core.Attribute("Y_position"));
//						}
//						if (currentImage == null) return;
//						ShortMatrix attMatrix = null;
//						weka.core.FastVector attLabels = new weka.core.FastVector(); 
//						double xfactor = 0;
//						double yfactor = 0;
//						String attName = "";
//						if (false) {
//							isGreyscale = false;
//							
//							
//							System.out.println("Global width: "+Run.it.imageset.getGlobalWidth(false));
//							System.out.println("Global height: "+Run.it.imageset.getGlobalHeight(false));
//							System.out.println("Local width: "+rg.getRegionImage().getWidth());
//							System.out.println("Local height: "+rg.getRegionImage().getHeight());
//							
//							xfactor = (double)rg.getRegionImage().getWidth()/Run.it.imageset.getGlobalWidth(false);
//							yfactor = (double)rg.getRegionImage().getHeight()/Run.it.imageset.getGlobalHeight(false);
//							System.out.println("xfactor: "+xfactor);
//							System.out.println("yfactor: "+yfactor);
//							
//							
//							
//							attName = "regionInfo" + (Run.it.extraAttributes.size()+1);
//
//							attMatrix = rg.getRegionShortMatrix();
//							
//							HashSet<String> attLabelSet = new HashSet<String>();
//							for (int x = 0;x < attMatrix.getWidth();x++){
//								for (int y = 0;y < attMatrix.getHeight();y++){
//									attLabelSet.add(((Short)attMatrix.get(x,y)).toString());
//								}
//							}
//														
//							System.err.println("Number of labels: "+ attLabelSet.size());
//							for (String st:attLabelSet){
//								attLabels.addElement(st);
//							}						
//						}
//						if(true){
//							isGreyscale = false;
//				
//							DataImage thisImage;
//							if(overlayImage != null && overlay == true)
//								thisImage = overlayImage;
//							else
//								thisImage = currentImage;
//											
//							System.out.println("Global width: "+Run.it.imageset.getGlobalWidth(false));
//							System.out.println("Global height: "+Run.it.imageset.getGlobalHeight(false));
//							System.out.println("Local width: "+thisImage.getWidth());
//							System.out.println("Local height: "+thisImage.getHeight());
//							
//							xfactor = (double)thisImage.getWidth()/Run.it.imageset.getGlobalWidth(false);
//							yfactor = (double)thisImage.getHeight()/Run.it.imageset.getGlobalHeight(false);
//							System.out.println("xfactor: "+xfactor);
//							System.out.println("yfactor: "+yfactor);
//							
//							attName = "regionInfo" + (Run.it.extraAttributes.size()+1);
//
//							
//							Map<Integer, Short> attLabelMap = new TreeMap<Integer,Short>();
//							attMatrix = new ShortMatrix(thisImage.getWidth(),thisImage.getHeight());
//							short nregions = 0;
//							for (int x = 0;x < thisImage.getWidth();x++){
//								for (int y = 0;y < thisImage.getHeight();y++){
//									//String thisLabel = ((Short)attMatrix.get(x,y)).toString());
//									int thisLabel =  thisImage.getRGB(x,y);
//									Short thisRegion = attLabelMap.get(thisLabel);
//									if (thisRegion == null){
//										thisRegion = nregions;
//										attLabelMap.put(thisLabel,nregions);
//										nregions++;
//									}
//									attMatrix.set(x,y,thisRegion);
//								}
//							}
//														
//							System.err.println("Number of labels: "+ attLabelMap.size());
//							for (Integer st:attLabelMap.keySet()){
//								attLabels.addElement(attLabelMap.get(st).toString());
//							}
//						}
//						
//						weka.core.Attribute thisAtt = new weka.core.Attribute(attName);
//						System.out.println(thisAtt);
//						Run.it.extraAttributes.add(thisAtt);
//						Run.it.extraFeatures.add((Feature)new ScaledImageDiscreteFeature(attMatrix,xfactor,yfactor));
//						
//						
//						repaintImagePanel();						
//					}
//				}
//			);
//
//		
//		exportAttributeButton.addActionListener(
//				new ActionListener(){
//					public void actionPerformed(ActionEvent e){
//						if (Run.it.extraFeatures == null){
//							Run.it.extraFeatures = new ArrayList<Feature>();
//							Run.it.extraFeatures.add(new GlobalXPositionFeature());
//							Run.it.extraFeatures.add(new GlobalYPositionFeature());
//						}
//						if (Run.it.extraAttributes == null){
//							Run.it.extraAttributes = new ArrayList<weka.core.Attribute>();
//							Run.it.extraAttributes.add(new weka.core.Attribute("X_position"));
//							Run.it.extraAttributes.add(new weka.core.Attribute("Y_position"));
//						}
//						if (currentImage == null) return;
//						if (rg == null) {
//							return;
//						}
//						
//						isGreyscale = false;
//						
//						rg.segment();
//
//						
//						System.out.println("Global width: "+Run.it.imageset.getGlobalWidth(false));
//						System.out.println("Global height: "+Run.it.imageset.getGlobalHeight(false));
//						System.out.println("Local width: "+rg.getRegionImage().getWidth());
//						System.out.println("Local height: "+rg.getRegionImage().getHeight());
//						
//						double xfactor = (double)rg.getRegionImage().getWidth()/Run.it.imageset.getGlobalWidth(false);
//						double yfactor = (double)rg.getRegionImage().getHeight()/Run.it.imageset.getGlobalHeight(false);
//						System.out.println("xfactor: "+xfactor);
//						System.out.println("yfactor: "+yfactor);
//						
//						weka.core.Attribute thisAtt = new weka.core.Attribute("regionInfo" + (Run.it.extraAttributes.size()+1));
//						Run.it.extraAttributes.add(thisAtt);
//						Run.it.extraFeatures.add((Feature)new ScaledImageDiscreteFeature(rg.getRegionShortMatrix(),xfactor,yfactor));
//						
//						repaintImagePanel();						
//					}
//				}
//			);
//
//		
//		
//		regionGrowingStepButton.addActionListener(
//				new ActionListener(){
//					public void actionPerformed(ActionEvent e){
//						if (currentImage == null) return;
//						if (rg == null) {
//							rg= new RegionGrowing(new RegularSubImage(currentImage.clone().getAsBufferedImage()),(Integer)seedSpinner.getValue());
//							//System.err.println("New rg!");
//						}
//						//RegularSubImage thisImage = new RegularSubImage(currentImage.clone().getAsBufferedImage());
//						
//						isGreyscale = false;
//						
//						int count = 0;
//						int printfreq = 50000;
//						while(rg.hasBoundaryPoints() && count < printfreq){							
//							rg.grow();
//							count++;
//						}
//
//						overlayImage = rg.getRegionImage();
//						overlay = true;
//						
//						madeGreyscale = false;
//						repaintImagePanel();
//					}
//				}
//			);
		overlayButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						if (overlayImage != null)
							overlay = !overlay;	
						repaintImagePanel();
					}
				}
			);
		
		smallViewButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						smallViewToggle = !smallViewToggle;	
						repaintImagePanel();
					}
				}
			);
//		showBoundaryButton.addActionListener(
//				new ActionListener(){
//					private boolean showBoundary = true;
//
//					public void actionPerformed(ActionEvent e){
//						if (rg != null){
//							showBoundary = !showBoundary;
//							overlayImage = rg.getRegionImage(showBoundary);
//							overlay = true;
//							
//							madeGreyscale = false;
//						}
//						
//						repaintImagePanel();
//					}
//				}
//			);
		
		waveButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						if (overview_image == null) return;
						wave_no = (wave_no + 1) % ((NuanceSubImage)overview_image).getWaveImages().size();
						BufferedImage temp_wave_image = (BufferedImage) ((NuanceSubImage) overview_image).getWaveImages().toArray()[wave_no];
						currentImage = new RegularSubImage(temp_wave_image);
						madeGreyscale = false;
						repaintImagePanel();
						prevImage = currentImage.clone();						
					}
				}
			);

		invertButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						if (currentImage == null) return;
						for (int i = 0;i< currentImage.getWidth();i++){
							for (int j = 0;j< currentImage.getHeight();j++){
								Color c = new Color(currentImage.getRGB(i,j));
								currentImage.setPixel(i,j,255-c.getRed(),255-c.getBlue(),255-c.getGreen());
							}
						}
						repaintImagePanel();
						prevImage = currentImage.clone();						
					}
				}
			);
		greyscaleButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						if (currentImage == null) return;
						isGreyscale = !isGreyscale;
						madeGreyscale = false;
						smoothingSlider.setValue((int) (.8355*(double)smoothingSlider.getMaximum()));
						thresholdSlider.setValue((int) (.3*(double)thresholdSlider.getMaximum()));
						repaintImagePanel();						
						prevImage = currentImage.clone();
					}
				}
			);
		resetButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						DataImage ov = overview_image;
						initializeFields();
						if (ov != null){
							overview_image = ov;
							currentImage = overview_image.clone();
							prevImage = currentImage.clone();
						}
						repaintImagePanel();
					}
				}
			);
		overviewButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						buildImage();
						repaintImagePanel();
					}
				}
			);
		trainImageButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						overview_image = new RegularSubImage(Run.it.getGUI().getTrainingPanel().imagePanel.getActiveImage());
						resetButton.doClick();
						repaintImagePanel();
					}
				}
			);
		saveFile.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						if (currentImage == null) return;
																		
						
						JFileChooser chooser = new JFileChooser();
						
						chooser.setFileFilter(new ImageFilter());
						int returnVal = chooser.showSaveDialog(saveFile);
						if(returnVal == JFileChooser.APPROVE_OPTION) {
							try{
								String ext = ImageUtils.getExtension(chooser.getSelectedFile());
								ImageIO.write(currentImage.getAsBufferedImage(),ext,chooser.getSelectedFile());
							}
							catch (Exception e2){
								JOptionPane.showMessageDialog(chooser,
									    "Cannot save " + chooser.getSelectedFile().getName(),
									    "Image save error",
									    JOptionPane.ERROR_MESSAGE);
							}
					    }		
					}
				}
			);
		loadFile.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						JFileChooser chooser = new JFileChooser();						
						int returnVal = chooser.showOpenDialog(loadFile);
						if(returnVal == JFileChooser.APPROVE_OPTION) {
							try{
								//System.out.print(ImageIO.getWriterFormatNames());
								initializeFields();


								RenderedImage renderimage = JAI.create("fileload",chooser.getSelectedFile().toString());
						
								currentImage = new RegularSubImage(ImageUtils.convertRenderedImage(renderimage));
								prevImage = currentImage.clone();
								overview_image = currentImage.clone();
								repaintImagePanel();
							}
							catch (Exception e2){
								JOptionPane.showMessageDialog(chooser,
									    "Cannot load " + chooser.getSelectedFile().getName(),
									    "Image load error",
									    JOptionPane.ERROR_MESSAGE);
								System.err.println(e2.getMessage());
							}
					    }
					}
				}
			);
	}

	private void makeGreyscale(){
		if (madeGreyscale) return;
		BufferedImage image = imagePanel.getActiveImage();
		ColorModel cm = image.getColorModel();

		RegularSubImage nextImage = new RegularSubImage(new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_RGB));
		int totalIntensity = 0;
		for (int x = 0;x < image.getWidth();x++){
			for (int y = 0;y < image.getHeight();y++){
				int intensity = 0;
				int rgb = image.getRGB(x,y);
				intensity += cm.getRed(rgb);
				intensity += cm.getGreen(rgb);
				intensity += cm.getBlue(rgb);
				intensity = intensity / 3;
				if(!cm.isAlphaPremultiplied())
					intensity = intensity*cm.getAlpha(rgb)/255;
				totalIntensity += intensity;
				
				nextImage.setPixel(x,y,intensity,intensity,intensity);
			}
		}
		imagePanel.setDisplayImage(nextImage);
		madeGreyscale = true;
		
		System.out.println("Average intesity: " + totalIntensity/(image.getWidth()*image.getHeight()));
	}

	/**
	 * Make a Gaussian blur kernel.
     * @param radius the blur radius
     * @return the kernel
	 */
	public static Kernel makeGaussianKernel(float radius) {
		int r = (int)Math.ceil(radius);
		int rows = r*2+1;
		float[] matrix = new float[rows*rows];
		float sigma = radius/3;
		float sigma22 = 2*sigma*sigma;
		float sigmaPi2 = (float) (2*Math.PI)*sigma;
		float sqrtSigmaPi2 = (float)Math.sqrt(sigmaPi2);
		float radius2 = radius*radius;
		float total = 0;
		int index = 0;
		for (int x = -r;x <= r;x++){
			for (int y = -r; y <= r; y++) {
				float distance = x*y;
				if (distance > radius2)
					matrix[index] = 0;
				else
					matrix[index] = (float)Math.exp(-(distance)/sigma22) / sqrtSigmaPi2;
				total += matrix[index];
				index++;
			}
		}
		for (int i = 0; i < rows*rows; i++)
			matrix[i] /= total;

		return new Kernel(rows, rows, matrix);
	}
	

	private int getMaxIntensity(BufferedImage image){

		ColorModel cm = image.getColorModel();
		
		int maxIntensity = 0;
		
		//int totalIntensity = 0;
		for (int x = 0;x < image.getWidth();x++){
			for (int y = 0;y < image.getHeight();y++){
				int intensity = 0;
				int rgb = image.getRGB(x,y);
				intensity += cm.getRed(rgb);
				intensity += cm.getGreen(rgb);
				intensity += cm.getBlue(rgb);
				intensity = intensity / 3;
				if(!cm.isAlphaPremultiplied())
					intensity = intensity*cm.getAlpha(rgb)/255;
				//totalIntensity += intensity;				
				maxIntensity = Math.max(maxIntensity,intensity);
			}
		}
		return maxIntensity;
	}

	private BufferedImage thresholdFilter(BufferedImage image, double threshold){

		ColorModel cm = image.getColorModel();

		threshold = getMaxIntensity(image)*threshold;
		RegularSubImage tempImage = new RegularSubImage(image);
		
		
		//int totalIntensity = 0;
		for (int x = 0;x < image.getWidth();x++){
			for (int y = 0;y < image.getHeight();y++){
				int sumIntensity = 0;
				int rgb = image.getRGB(x,y);
				int greenIntensity = cm.getGreen(rgb);
				int redIntensity = cm.getRed(rgb);
				int blueIntensity = cm.getBlue(rgb);
				sumIntensity = (cm.getRed(rgb)+cm.getGreen(rgb)+cm.getBlue(rgb)) / 3;
				if(!cm.isAlphaPremultiplied())
					sumIntensity = sumIntensity*cm.getAlpha(rgb)/255;
				
				//totalIntensity += sumIntensity;
				
				if (sumIntensity > threshold)
					sumIntensity = 255;
				else
					sumIntensity = 0;

				if (greenIntensity > threshold)
					greenIntensity = 255;
				else
					greenIntensity = 0;

				if (blueIntensity > threshold)
					blueIntensity = 255;
				else
					blueIntensity = 0;

				if (redIntensity > threshold)
					redIntensity = 255;
				else
					redIntensity = 0;

				tempImage.setPixel(x,y,redIntensity,greenIntensity,blueIntensity);
			}
		}

		
		return tempImage.getAsBufferedImage();
	}
	
	/** Blobs the image according to smoothing and threshold parameters  **/
	private BufferedImage gaussianFilter(BufferedImage image,double smoothing){	
		BufferedImage tempImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

		double radius = 99.89*smoothing+.01;
		Kernel kernel = makeGaussianKernel((float)radius);


		
		int kernelWidth = (int)Math.ceil(radius)*2+1;
		int kernelHeight = kernelWidth;

		int xOffset = (kernelWidth - 1) / 2;
		int yOffset = (kernelHeight - 1) / 2;

		BufferedImage newSource = new BufferedImage(
		  image.getWidth() + kernelWidth - 1,
		  image.getHeight() + kernelHeight - 1,
		  BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = newSource.createGraphics();
		g2.drawImage(image, xOffset, yOffset, null);
		g2.dispose();

		ConvolveOp op = new ConvolveOp(kernel,
		    ConvolveOp.EDGE_NO_OP, null);
		tempImage = op.filter(newSource, null);		
		
		return tempImage;
	}
	
	/** during a classification and/or post-processing the user is unable to manipulate options */
	protected void DisableMostButtons(){		
	}
	/** passes the {@link KImagePanel#repaint() repaint on to the image panel} */
	public void repaintImagePanel() {
		if(isGreyscale) makeGreyscale();
		DataImage displayImage = currentImage;
		if(overlayImage != null && overlay == true){
			//displayImage = Utils.mergeImages(displayImage,overlayImage);
			//RenderedImage img = BandMergeDescriptor.create(displayImage.getAsBufferedImage(),overlayImage.getAsBufferedImage(),null).getRendering();
			//displayImage = new RegularSubImage(Utils.convertRenderedImage(img));
			displayImage = overlayImage;
		}
		if(smallViewToggle && displayImage !=null){
			BufferedImage tempImage = displayImage.getAsBufferedImage();
			int width = imagePanel.getWidth();
			int height = imagePanel.getHeight();
			
			
			BufferedImage tempDisplayImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
			Graphics g = tempDisplayImage.getGraphics();
			g.drawImage(tempImage.getScaledInstance(width,height,Image.SCALE_FAST), 0, 0, null);
			g.dispose();
			displayImage = new RegularSubImage(tempDisplayImage);
			//System.out.println("width: " + imagePanel.getScrollPane().getViewport().getWidth());
			//System.out.println("height: " + imagePanel.getScrollPane().getViewport().getHeight());
			imagePanel.getScrollPane().getHorizontalScrollBar().setValue(0);
			imagePanel.getScrollPane().getVerticalScrollBar().setValue(0);
		}
		//Point jv = imagePanel.getScrollPane().getViewport().getViewPosition();
		imagePanel.setDisplayImage(displayImage,0);
		//imagePanel.getScrollPane().getViewport().setViewPosition(jv);
		imagePanel.repaint();
	}
	public void ReenablePreProcessButton() {
	}
	/** when a project is loaded from the hard-disk, the user's preferences are populated into the Western region's option box */
	public void SetValuesToOpenProject(){
	}
}