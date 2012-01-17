
package GemIdentView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import GemIdentCentroidFinding.PostProcess;
import GemIdentClassificationEngine.Classify;
import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.RegularSubImage;
import GemIdentModel.Phenotype;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentTools.Geometry.Solids;
import GemVidentTracking.ParticleFilterTracking;
import GemVidentTracking.PathCloud;
import GemVidentTracking.SimpleMarkovTracking;
import GemVidentTracking.ParticleFilterTracking.AntFileFilter;

/**
 * Controls and houses the classification & post-processing panel. For discussion on
 * the execution of a classification via the classify button, see section 4.1 of the manual.
 * For discussion on setting the classification parameters, see section 4.1.1 of the manual.
 * For discussion on picking a subset of the images to classify, see section 4.1.2 of the 
 * manual. And for discussion on reclassification, see section 5.5 of the manual.
 * 
 * @author Adam Kapelner
 *
 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
 */
@SuppressWarnings("serial")
public class KTrackingPanel extends KPanel{
	

	
	/** the ordered list of image files in the tracking collection */
	private ArrayList<String> trackList;

	private ParticleFilterTracking tracking;
	private GUIFrame trackPanel = null;
	private JButton forwTrackButton;
	private JButton backTrackButton;
	private JLabel trackLabelImageFile;
	private JLabel trackLabelAll;
	private String trackingPhenotype = null;
	protected int trackImageNum;
	/** whether or not to display the image on the panel */
	protected JCheckBox seeImageCheck;


	private JCheckBox seeCentroidsCheck;

	private JCheckBox seeTrackingsCheck;

	private JButton computeButton;

	private JProgressBarAndLabel trackingProgress;

	private Container progressBox;

	private Container trackPanelBox;

	private JSpinner endTimeTracking;

	private JSpinner startTimeTracking;

	private JButton stopButton;

	private JSpinner particleSpinner;

	private JCheckBox dontShowShortPathsCheck;

	private int imgHeight;

	private int imgWidth;

	private JCheckBox seeAllPathsCheck;

	private JButton loadButton;

	private JButton saveButton;

	private JSpinner pathLengthSpinner;

	private JCheckBox seeCloudCentersCheck;

	private JFrame settingsFrame=null;

	private Container settingsBox;

	private JButton settingsButton;

	private JButton selectPhenotypeButton;
	
	protected KImageTrackingPanel imagePanel;

	protected Set<PathCloud> displayClouds = null;

	private JSpinner shortPathLengthSpinner;

	private JButton clearSelectedPathCloudsButton;

	private JButton writePathCloudsButton;

	private JCheckBox autoSaveCheck;

	private JCheckBox autoWriteCSVCheck;

	private JCheckBox memReducedMode;

	private static final int maxNumParticles=1000;

	/** initializes the image panel **/
	public KTrackingPanel(){
		super();
		imagePanel = new KImageTrackingPanel(this);
		setImagePanel(imagePanel);
		tracking = new SimpleMarkovTracking();
//		tracking = new ExperimentalMarkovTracking();
//		tracking = new SimpleMarkovTracking();
	}
	public void setDisplayImage(DataImage displayImage){
		imagePanel.setDisplayImage(displayImage);
	}
	/** the label for the tracking image when tracking is not being used */
	private static final String NONE_IMAGE="<none>";
	
	private class runComputeTrackings implements Runnable{

		public void run() {
			trackingProgress.setVisible(true);
			trackingProgress.setText("computing trackings . . .");

			saveButton.setEnabled(false);
			loadButton.setEnabled(false);
			computeButton.setEnabled(false);
			stopButton.setEnabled(true);
			settingsButton.setEnabled(true);
			selectPhenotypeButton.setEnabled(false);
			clearSelectedPathCloudsButton.setEnabled(false);
			writePathCloudsButton.setEnabled(false);

			validate();
			repaint();
			long t1 = System.currentTimeMillis();
			tracking.computeTrackings(trackingProgress,(Integer)startTimeTracking.getValue(),(Integer)endTimeTracking.getValue(),(Integer)particleSpinner.getValue(),stopButton,trackingPhenotype);
			long t2 = System.currentTimeMillis();
			double ttime = ((t2-t1)/10)/100.;			
			
			saveButton.setEnabled(true);
			loadButton.setEnabled(true);
			computeButton.setEnabled(true);
			stopButton.setEnabled(false);
			writePathCloudsButton.setEnabled(true);
			trackingProgress.setVisible(false);			
			selectPhenotypeButton.setEnabled(true);
			clearSelectedPathCloudsButton.setEnabled(true);
			writePathCloudsButton.setEnabled(true);

			if (autoWriteCSVCheck.isSelected())
				writePathCloudsButton.doClick();

			
			if (autoSaveCheck.isSelected())
				saveButton.doClick();
			
			JOptionPane.showMessageDialog(Run.it.getGUI(),"Computed trackings for " + tracking.getNumImagesTracked() + " images in " + ttime + " seconds");
		}
		
	}
	private class runLoadTrackings implements Runnable{

		public void run() {
			trackingProgress.setText("loading trackings . . .");
			trackingProgress.setValue(0);
			trackingProgress.setVisible(true);
			saveButton.setEnabled(false);
			loadButton.setEnabled(false);
			computeButton.setEnabled(false);
			stopButton.setEnabled(false);
			selectPhenotypeButton.setEnabled(false);
			clearSelectedPathCloudsButton.setEnabled(false);
			writePathCloudsButton.setEnabled(false);
			validate();
			repaint();
			if(loadTracking(trackingProgress))
				JOptionPane.showMessageDialog(Run.it.getGUI(),"Loaded  trackings for " + tracking.getNumImagesTracked() + " images"); 

			makeSettingsBox();
			
			writePathCloudsButton.setEnabled(true);		
			selectPhenotypeButton.setEnabled(true);
			clearSelectedPathCloudsButton.setEnabled(true);
			saveButton.setEnabled(true);
			loadButton.setEnabled(true);
			computeButton.setEnabled(true);
			stopButton.setEnabled(false);
			trackingProgress.setVisible(false);			
		}
		
	}
	private class runSaveTrackings implements Runnable{

		public void run() {
			trackingProgress.setText("saving trackings . . .");
			trackingProgress.setValue(0);
			trackingProgress.setVisible(true);
			saveButton.setEnabled(false);
			loadButton.setEnabled(false);
			computeButton.setEnabled(false);
			stopButton.setEnabled(false);
			validate();
			repaint();
			saveTracking(trackingProgress);
			saveButton.setEnabled(true);
			loadButton.setEnabled(true);
			computeButton.setEnabled(true);
			stopButton.setEnabled(false);
			trackingProgress.setVisible(false);
			ChangeTrackImage();
		}
		
	}
	
	private void SetUpTrackingPanel(){
		trackPanel=new GUIFrame("Track on classified images");
		
		/**
		 * Set layout
		 */
		trackPanelBox = Box.createVerticalBox();
		
		/**
		 * Add panel elements
		 */
		saveButton = new JButton("Save Trackings");				
		loadButton = new JButton("Load Trackings");				
		computeButton = new JButton("Compute Trackings");				
		stopButton = new JButton("Stop");
		stopButton.setEnabled(false);
		settingsButton = new JButton("Settings");
		selectPhenotypeButton = new JButton("Select phenotype");
		clearSelectedPathCloudsButton = new JButton("Clear path selections");
		writePathCloudsButton = new JButton("Write paths as CSV");
		
		Box trackBox=Box.createHorizontalBox();		
		forwTrackButton=new JButton();
		forwTrackButton.setIcon(new ImageIcon(IOTools.OpenSystemImage("forward.png")));
		forwTrackButton.setDisabledIcon(new ImageIcon(IOTools.OpenSystemImage("forwarddis.png")));
		forwTrackButton.setSize(forwTrackButton.getIcon().getIconWidth(),forwTrackButton.getIcon().getIconHeight());
		backTrackButton=new JButton();
		backTrackButton.setIcon(new ImageIcon(IOTools.OpenSystemImage("backward.png")));
		backTrackButton.setDisabledIcon(new ImageIcon(IOTools.OpenSystemImage("backwarddis.png")));
		backTrackButton.setSize(backTrackButton.getIcon().getIconWidth(),backTrackButton.getIcon().getIconHeight());
		trackBox.add(backTrackButton);
		trackBox.add(Box.createHorizontalStrut(3));
		trackBox.add(forwTrackButton);
		trackLabelImageFile=new JLabel(NONE_IMAGE,JLabel.LEFT);
		trackLabelAll=new JLabel("Image "+trackImageNum+"/"+trackList.size(),JLabel.LEFT);
		
		Box labelBox=Box.createVerticalBox();
		labelBox.add(trackLabelImageFile);
		labelBox.add(trackLabelAll);
		trackBox.add(Box.createHorizontalStrut(5));		
		trackBox.add(labelBox);
		

		Container rangeBox = new Container();
		rangeBox.setLayout(new GridLayout(0,4,0,0));
		JLabel stt = new JLabel("start time:");
		startTimeTracking = new JSpinner(new SpinnerNumberModel(1,1,trackList.size(),1));
		JLabel ett = new JLabel("end time:");
		endTimeTracking = new JSpinner(new SpinnerNumberModel(trackList.size(),1,trackList.size(),1));
		JLabel ptt = new JLabel("num. particles:");
		particleSpinner = new JSpinner(new SpinnerNumberModel(Math.min(20,maxNumParticles),1,maxNumParticles,1));
		rangeBox.add(stt);
		rangeBox.add(startTimeTracking);
		rangeBox.add(ett);
		rangeBox.add(endTimeTracking);
		rangeBox.add(ptt);
		rangeBox.add(particleSpinner);

		makeSettingsBox();

		Container buttonBox = Box.createVerticalBox();
		Box buttonBox1 = Box.createHorizontalBox(); 
		Box buttonBox2 = Box.createHorizontalBox(); 
		Box buttonBox3 = Box.createHorizontalBox(); 
		Box buttonBox4 = Box.createHorizontalBox(); 
		buttonBox1.add(saveButton);
		buttonBox1.add(loadButton);
		buttonBox1.add(Box.createHorizontalGlue());
		buttonBox2.add(computeButton);
		buttonBox2.add(stopButton);
		buttonBox2.add(Box.createHorizontalGlue());
		buttonBox3.add(Box.createHorizontalGlue());
		buttonBox3.add(selectPhenotypeButton);
		buttonBox3.add(settingsButton);
		buttonBox3.add(new JLabel("See all paths"));
		buttonBox3.add(seeAllPathsCheck);
		buttonBox4.add(Box.createHorizontalGlue());
		buttonBox4.add(writePathCloudsButton);
		buttonBox4.add(clearSelectedPathCloudsButton);
		buttonBox.add(buttonBox1);
		buttonBox.add(buttonBox2);
		buttonBox.add(buttonBox3);
		buttonBox.add(buttonBox4);
		

		trackingProgress=new JProgressBarAndLabel(0,100,"computing trackings . . .");
		progressBox = new Container();
		progressBox.setLayout(new GridLayout(0,1,2,0));
		progressBox.add(trackingProgress.getBox());
		trackingProgress.setVisible(false);
		

		
		
			
		
		trackPanelBox.add(rangeBox);
		trackPanelBox.add(buttonBox);
		trackPanelBox.add(progressBox);
		trackPanelBox.add(trackBox);
		trackPanel.add(trackPanelBox);

		addTrackingListeners();
		createMouseListeners();
	}
	
	/** spawns the dialog to delete a color or phenotype to the {@link KClassInfoBrowser browser} */
	protected void spawnPhenotypeInfoDialog(){
		final JDialog phenotypeDialog=new JDialog();
		phenotypeDialog.setTitle("Select phenotype to track");

		Collection<Phenotype> phenotypes = Run.it.getPhenotypesSaveNON();

		final List list = new List(phenotypes.size(),false);
		
		for (Phenotype ph: phenotypes)
			list.add(ph.getName());

		final JButton select=new JButton("Select phenotype");
		select.setEnabled(false);
		
		JButton cancel=new JButton("Cancel");		
		
		list.addItemListener(
			new ItemListener(){
				public void itemStateChanged(ItemEvent e) {
					select.setEnabled(true);
				}
			}
		);
		select.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					trackingPhenotype = list.getSelectedItem();
					phenotypeDialog.dispose();	
				}
			}
		);
		cancel.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					phenotypeDialog.dispose();
				}
			}
		);
		
		phenotypeDialog.setLayout(new BorderLayout());
		phenotypeDialog.add(list,BorderLayout.NORTH);
		Box buttons=Box.createHorizontalBox();
		buttons.add(select);
		buttons.add(cancel);
		phenotypeDialog.add(buttons,BorderLayout.SOUTH);
		
		//phenotypeDialog.setLocation(null);
		phenotypeDialog.setVisible(true);
		phenotypeDialog.pack();
	}

	private void addTrackingListeners() {
		selectPhenotypeButton.addActionListener(
				new ActionListener(){

					public void actionPerformed(ActionEvent e) {
						spawnPhenotypeInfoDialog();
					}
					
				});
		clearSelectedPathCloudsButton.addActionListener(
				new ActionListener(){

					public void actionPerformed(ActionEvent e) {
						displayClouds = null;
						seeAllPathsCheck.setSelected(true);
						ChangeTrackImage();
					}
					
				});
		settingsButton.addActionListener(
				new ActionListener(){

					public void actionPerformed(ActionEvent e) {
						showSettingsFrame();
					}
					
				});

		computeButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						Run.it.FrameRepaint();
						Thread t = new Thread(new runComputeTrackings());
						t.start();
					}
				}
			);

		loadButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						Run.it.FrameRepaint();
						Thread t = new Thread(new runLoadTrackings());
						t.start();
					}
				}
			);

		saveButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						Run.it.FrameRepaint();
						Thread t = new Thread(new runSaveTrackings());
						t.start();
					}
				}
			);

		stopButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						stopButton.setEnabled(false);
					}
				}
			);
		
		forwTrackButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						if (trackImageNum < trackList.size()){
							trackImageNum++;
							ChangeTrackImage();
						}
					}
				}
			);
		backTrackButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						if (trackImageNum > 1){
							trackImageNum--;
							ChangeTrackImage();
						}					
					}				
				}
		);
		writePathCloudsButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						if (tracking != null)
							tracking.writePathCloudsToFile();
					}				
				}
		);
	}
	
	private void showSettingsFrame(){
		if (settingsFrame != null){
			settingsFrame.setVisible(false);
			settingsFrame.dispose();
			settingsFrame = null;
		}
		settingsFrame = new JFrame("Tracking settings");
		settingsFrame.add(settingsBox);
	    settingsFrame.pack();
	    settingsFrame.setVisible(true);						
	    settingsFrame.setLocationRelativeTo(null);
	}
	
	private void makeSettingsBox() {
		
		settingsBox = Box.createVerticalBox();

		seeTrackingsCheck = new JCheckBox(); 
		seeImageCheck = new JCheckBox(); 
		seeCentroidsCheck = new JCheckBox(); 
		dontShowShortPathsCheck = new JCheckBox(); 
		seeAllPathsCheck = new JCheckBox(); 
		seeCloudCentersCheck = new JCheckBox();
		autoSaveCheck = new JCheckBox();
		autoWriteCSVCheck = new JCheckBox();
		memReducedMode = new JCheckBox();
				
		pathLengthSpinner = new JSpinner(new SpinnerNumberModel(Math.min(100,trackList.size()),1,trackList.size(),1));
		shortPathLengthSpinner = new JSpinner(new SpinnerNumberModel(Math.min(20,trackList.size()),1,trackList.size(),1));
		

		setSettingsDefaultValues();
		
		seeImageCheck.addItemListener(
			new ItemListener(){
				public void itemStateChanged(ItemEvent e) {
					ChangeTrackImage();
				}
			}
		);

		seeCentroidsCheck.addItemListener(
			new ItemListener(){
				public void itemStateChanged(ItemEvent e) {
					ChangeTrackImage();
				}
			}
		);

		seeTrackingsCheck.addItemListener(
			new ItemListener(){
				public void itemStateChanged(ItemEvent e) {
					ChangeTrackImage();
				}
			}
		);
		seeCloudCentersCheck.addItemListener(
			new ItemListener(){
				public void itemStateChanged(ItemEvent e) {
					ChangeTrackImage();
				}
			}
		);

		seeAllPathsCheck.addItemListener(
			new ItemListener(){
				public void itemStateChanged(ItemEvent e) {
					ChangeTrackImage();
				}
			}
		);
		
		dontShowShortPathsCheck.addItemListener(
				new ItemListener(){
					public void itemStateChanged(ItemEvent e) {
						tracking.shortPathLengthThreshold = (Integer) shortPathLengthSpinner.getValue();
						tracking.dontShowShortPaths = dontShowShortPathsCheck.isSelected();
						ChangeTrackImage();
					}
				}
			);

		memReducedMode.addItemListener(
				new ItemListener(){
					public void itemStateChanged(ItemEvent e) {
						tracking.memReducedPathWriter = !tracking.memReducedPathWriter;
						memReducedMode.setSelected(tracking.memReducedPathWriter);
					}
				}
			);

		pathLengthSpinner.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						ChangeTrackImage();
					}	
				}
		);
		shortPathLengthSpinner.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						tracking.shortPathLengthThreshold = (Integer) shortPathLengthSpinner.getValue();
						ChangeTrackImage();
					}	
				}
		);
		

		Container checksBox = new Container();
		checksBox.setLayout(new GridLayout(0,4,0,0));
		
		checksBox.add(new JLabel("See image:",JLabel.RIGHT));
		checksBox.add(seeImageCheck);
		checksBox.add(new JLabel("See centroids:",JLabel.RIGHT));
		checksBox.add(seeCentroidsCheck);
		checksBox.add(new JLabel("See tracking:",JLabel.RIGHT));
		checksBox.add(seeTrackingsCheck);
		checksBox.add(new JLabel("See cloud centers:",JLabel.RIGHT));
		checksBox.add(seeCloudCentersCheck);
		checksBox.add(new JLabel("See all paths:",JLabel.RIGHT));
		Box pathsBox = Box.createHorizontalBox();

		pathsBox.add(seeAllPathsCheck);
		pathsBox.add(new JLabel("length:"));
		pathsBox.add(pathLengthSpinner);
		
		checksBox.add(pathsBox);
		checksBox.add(new JLabel("Don't show short paths:",JLabel.RIGHT));
		
		Box shortPathsBox = Box.createHorizontalBox();

		shortPathsBox.add(dontShowShortPathsCheck);
		shortPathsBox.add(new JLabel("cutoff:"));
		shortPathsBox.add(shortPathLengthSpinner);
		
		checksBox.add(shortPathsBox);
		checksBox.add(new JLabel("Auto save:",JLabel.RIGHT));
		checksBox.add(autoSaveCheck);
		checksBox.add(new JLabel("Auto write to CSV:",JLabel.RIGHT));
		checksBox.add(autoWriteCSVCheck);
		checksBox.add(new JLabel("Reduced memory mode (display won't work):",JLabel.RIGHT));
		checksBox.add(memReducedMode);
		settingsBox.add(checksBox);
		settingsBox.add(tracking.makeParametersBox());
		
	}
	
	
	private void setSettingsDefaultValues() {
		dontShowShortPathsCheck.setSelected(false);
		seeImageCheck.setSelected(false);
		seeCentroidsCheck.setSelected(false);
		seeTrackingsCheck.setSelected(true);
		seeCloudCentersCheck.setSelected(false);
		seeAllPathsCheck.setSelected(true);
		autoSaveCheck.setSelected(false);
		autoWriteCSVCheck.setSelected(true);
		
		dontShowShortPathsCheck.setSelected(tracking.dontShowShortPaths);
		shortPathLengthSpinner.setValue(tracking.shortPathLengthThreshold);
		memReducedMode.setSelected(tracking.memReducedPathWriter);

		
	}
	public void saveTracking(JProgressBarAndLabel saveProgress){
		if (tracking == null)
			return;
		
		String td = Run.it.imageset.getFilenameWithHomePath(ParticleFilterTracking.trackingDir);
		String [] antFiles = new File(td).list(new AntFileFilter());

		boolean haveName = false;
		String shortfname = "";
		int nameCounter = 1;
		while (!haveName){
			boolean taken = false;
			
			shortfname = String.format("tracking_%03d.data",nameCounter);
			for (String af: antFiles){
				if (af.equals(shortfname)){
					taken = true;
					break;
				}					
			}			
			if (!taken){
				haveName = true;
			}
			nameCounter++;
		}
		String filename = td + File.separatorChar + shortfname;

		/**
		 *Write tracking object to disk  
		 */
		try {
			FileOutputStream f_out = new FileOutputStream(filename);
			ObjectOutputStream obj_out = new ObjectOutputStream (f_out);
			obj_out.writeObject (tracking);
			obj_out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		saveProgress.setValue((int) (saveProgress.getMaximum()));
	}

	public boolean loadTracking(JProgressBarAndLabel saveProgress){
		JFileChooser chooser = new JFileChooser(Run.it.imageset.getFilenameWithHomePath(ParticleFilterTracking.trackingDir));
		final AntFileFilter aff = new AntFileFilter();
		chooser.setFileFilter(
				new FileFilter(){
					public boolean accept(File f){
						return aff.accept(f);
					}

					@Override
					public String getDescription() {
						// TODO Auto-generated method stub
						return "data";
					}
				}
		);		
		int result = chooser.showOpenDialog(null);
		if ( result == JFileChooser.APPROVE_OPTION ) {
			File load_file = chooser.getSelectedFile();
			try {
				FileInputStream fin = new FileInputStream(load_file.getAbsolutePath());
				// Read object using ObjectInputStream
				ObjectInputStream obj_in = 	new ObjectInputStream (fin);
				Object obj = obj_in.readObject();
				assert(obj instanceof ParticleFilterTracking);
				tracking = (ParticleFilterTracking) obj;
			} catch (Exception e){
				e.printStackTrace();
				return false;
			}
		}
		saveProgress.setValue((int) (saveProgress.getMaximum()));
		return (result== JFileChooser.APPROVE_OPTION );
	}


	public void createMouseListeners(){
		imagePanel.addMouseListener(
				new MouseListener(){				
					public void mouseClicked(MouseEvent e){
					}
					public void mouseEntered(MouseEvent e){}
					public void mouseExited(MouseEvent e){}
					public void mousePressed(MouseEvent e){
						Point local=imagePanel.adjustPoint(e.getPoint());
						switch (e.getButton()){
						case MouseEvent.BUTTON2: //ie middle click
							break;
						case MouseEvent.BUTTON3: //ie right click	
							if (tracking == null)
								return;
						    PathCloud pc = tracking.getClosestPathCloud(local,trackImageNum);
							
						    if (pc != null && displayClouds!= null){
						    	displayClouds.remove(pc);
						    }
							break;
						default: //ie left click or any other click in OS's that don't recognize the constants above
							if (tracking == null)
								return;
							PathCloud pc2 = tracking.getClosestPathCloud(local,trackImageNum);

							if (pc2 != null){
								seeAllPathsCheck.setSelected(false);
								if (displayClouds == null)
									displayClouds = new HashSet<PathCloud>();
								displayClouds.add(pc2);
							}
					     	break;
						}						
						
						ChangeTrackImage();
					}
					public void mouseReleased(final MouseEvent e){
					}
				}
		);	
	}

	
	/** 
	 * initializes the tracking image list and validates 
	 * the forward / backward buttons. For more information
	 * about tracking in <b>GemIdent</b>, please consult
	 * section 5.3 in the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	public void EnableTrackingFeature(){

		
		trackList=Classify.AllCentroids();
		System.out.println("trackList: "+ Arrays.toString(trackList.toArray()));

		if (trackList == null || trackList.size() < 1)
			return;
		
		String ttt = Run.it.imageset.GetImages().get(0);
		try {
			imgWidth = Run.it.imageset.getDataImageFromFilename(ttt).getWidth();
			imgHeight = Run.it.imageset.getDataImageFromFilename(ttt).getHeight();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		trackImageNum=0;

		boolean previouslyTracked = trackPanel != null;
		
		SetUpTrackingPanel();
		ValidateTrackPanel();
		if (!previouslyTracked)
			super.appendToEast(trackPanel);
	}
	/** ensure the tracking panel buttons are dis/enabled in a consistent way */
	private void ValidateTrackPanel(){
		if (trackList.size() > 0){
			if (trackImageNum == trackList.size()){
				forwTrackButton.setEnabled(false);
				backTrackButton.setEnabled(true);
			}
			else if (trackImageNum <= 1){
				forwTrackButton.setEnabled(true);
				backTrackButton.setEnabled(false);
			}
			else {
				forwTrackButton.setEnabled(true);
				backTrackButton.setEnabled(true);
			}				
		}
		else {
			forwTrackButton.setEnabled(false);
			backTrackButton.setEnabled(false);
		}
	}

	
	private BufferedImage ProcessRawIsCentroid(BufferedImage raw,Phenotype ph) {
		if (raw==null)
			return null;
		int rows=raw.getWidth();
		int cols=raw.getHeight();
		BufferedImage I=new BufferedImage(rows,cols,BufferedImage.TYPE_INT_ARGB);
		Color center=new Color(1,1,1,255);
		Color around=ph.getDisplayColorWithAlpha(255);
		for (int i=0;i<rows;i++)
			for (int j=0;j<cols;j++)
				if (raw.getRGB(i,j) != (Color.BLACK).getRGB()){
					for (Point t:Solids.GetPointsInSolidUsingCenter(3,new Point(i,j)))
						try {I.setRGB(t.x,t.y,center.getRGB());} catch (Exception e){}
					for (Point t:Solids.GetPointsInSolidUsingCenter(2,new Point(i,j)))
						try {I.setRGB(t.x,t.y,around.getRGB());} catch (Exception e){}
					try {I.setRGB(i,j,center.getRGB());} catch (Exception e){}
				}
		return I;
	}

	/** selects the {@link #trackImageNum track image} and displays the image to the screen, displays the filename, and validates the buttons */
	protected void ChangeTrackImage(){
		if (trackImageNum == 0 || tracking == null)
			return;
		String filename=null;
		try {
			filename=trackList.get(trackImageNum-1);
		} catch (Exception e){
			filename=NONE_IMAGE;
		}

		BufferedImage tempDisplayImage = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_INT_RGB);
		Graphics g = tempDisplayImage.getGraphics();
		if(seeImageCheck.isSelected()){
			BufferedImage tempImage=null;
			try {
				tempImage = ImageAndScoresBank.getOrAddDataImage(filename).getAsBufferedImage();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			g.drawImage(tempImage, 0, 0, null);
		}
		if(seeCentroidsCheck.isSelected()){
			Collection<Phenotype> phenotypes = Run.it.getPhenotypesSaveNON();
			Phenotype ph = phenotypes.iterator().next();
			String isCentroidName=PostProcess.GetIsCentroidName(filename,ph.getName());
			BufferedImage raw=null;
			try {
				raw = ImageAndScoresBank.getOrAddIs(isCentroidName,null);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			BufferedImage I = ProcessRawIsCentroid(raw,ph);
			g.drawImage(I,0,0,null);
		}
		if(seeTrackingsCheck.isSelected()){
			tracking.makeTrackImg(tempDisplayImage, trackImageNum);
		}
		if(displayClouds != null && displayClouds.size() >0){
			tracking.drawPaths(tempDisplayImage, trackImageNum, (Integer) pathLengthSpinner.getValue(),displayClouds);			
		}
		if(seeAllPathsCheck.isSelected()){
			tracking.drawPaths(tempDisplayImage, trackImageNum, (Integer) pathLengthSpinner.getValue());
		}
		if(seeCloudCentersCheck.isSelected()){
			tracking.drawPathCloudCenters(tempDisplayImage, trackImageNum);
		}
		g.dispose();
				
		
		imagePanel.setDisplayImage(new RegularSubImage(tempDisplayImage),0);		
		imagePanel.repaint();
		trackLabelAll.setText("Image "+trackImageNum+"/"+trackList.size());
		trackLabelImageFile.setText(filename);
		ValidateTrackPanel();
	}
}