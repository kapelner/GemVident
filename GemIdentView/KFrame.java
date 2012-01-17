
package GemIdentView;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import GemIdentAnalysisConsole.ConsoleParser;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.ImageSetInterfaceWithUserColors;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;

/**
 * Embodies the GUI for <b>GemIdent</b>.
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class KFrame extends JFrame{
	
	/** houses the familiar <b>GemIdent</b> tabs */
	private JTabbedPane tabs;
	/** the dimension of the GUI */
	public static final Dimension frameSize=new Dimension(1280,800);
	
	/** the panel that allows the user to train for the background */
	private KBackgroundColorTrainPanel backgroundColorTrainPanel;
	/** the panel that allows the user to train for colors */
	private KColorTrainPanel colorTrainPanel;
	/** the panel that allows the user to train/retrain for phenotypes */
	private KPhenotypeTrainPanel phenotypeTrainPanel;
	/** the panel that allows the user to track ants */
	private KTrackingPanel trackingPanel;
	/** the panel that allows the user to classify images */
	private KClassifyPanel classifyPanel;
	/** the panel that allows the user to analyze data */
	private KAnalysisPanel analysisPanel;
	/** the panel that allows the user to pre-process data */
//	private KPreProcessPanel preProcessPanel;
	/** the global keyboard shortcut map - from keystroke to action name */
	private InputMap input_map;
	/** the global keyboard shortcut map - from action name to action */
	private ActionMap action_map;
	/** the option in the file menu to save the current project */
	private JMenuItem file_save;
	private JFrame cacheSettingsFrame=null;
	
	/** is the user using <b>GemIdent</b> at the moment? */
	private boolean active;
	/** the string to append to the title */
	private String toappend;
	private Box cacheSettingsBox;	
	
	// the names of the tabs:
	private static final String backgroundColorTrainTabName="Background Selection";
	private static final String colorTrainTabName="Color Selection";
	private static final String phenoTrainTabName="Phenotype Training";
	private static final String trackingTabName="Tracking";
//	private static final String preProcessPanelTabName="Pre-process";
	private static final String classifyTrainTabName="Classification";
//	private static final String retrainTabName="Phenotype Retraining";
	private static final String analysisPanelTabName="Analysis";
	
	/** the filename of the manual */
	public static final String ManualFileName="GemIdentManual.pdf";
	
	/** initializes the frame, adds a listener to check if the user is using the frame */
	public KFrame(){
		super(Run.Title);
		toappend = "";
		active=true;
		this.setIconImage(IOTools.OpenSystemImage("diamondicon.png"));
		setVisible(false);
		setPreferredSize(frameSize);
		setResizable(true);
		addWindowListener(
			new WindowListener(){				
				public void windowActivated(WindowEvent e){
					active=true;
				}
				public void windowClosed(WindowEvent e){}
				public void windowClosing(WindowEvent e){
					Run.it.ExitProgram();
				}
				public void windowDeactivated(WindowEvent e){
					active=false;
				}
				public void windowDeiconified(WindowEvent e){}
				public void windowIconified(WindowEvent e){}
				public void windowOpened(WindowEvent e){}
			}
		);
	}
	/** draw the GUI */
	public void DrawAll(){
		CreateMenu();
		CreateTabbedEnvironment();
		pack();
	}
	/** Initialize the tabbed environment, add +/- keystrokes for magnifier window */
	private void CreateTabbedEnvironment(){		
		tabs=new JTabbedPane();
		addUniversalKeyStrokes();
		colorTrainPanel = new KColorTrainPanel();
		backgroundColorTrainPanel = new KBackgroundColorTrainPanel();
		phenotypeTrainPanel=new KPhenotypeTrainPanel();
		trackingPanel=new KTrackingPanel();
		classifyPanel=new KClassifyPanel();
		analysisPanel=new KAnalysisPanel();
//		preProcessPanel=new KPreProcessPanel();
		add(tabs);		
		tabs.addTab(colorTrainTabName, colorTrainPanel);
		tabs.addTab(backgroundColorTrainTabName, backgroundColorTrainPanel);
		tabs.addTab(phenoTrainTabName, phenotypeTrainPanel);
		tabs.addTab(trackingTabName, trackingPanel);
		tabs.addTab(classifyTrainTabName, classifyPanel);
		tabs.addTab(analysisPanelTabName, analysisPanel);
//		tabs.addTab(preProcessPanelTabName, preProcessPanel);
		tabs.setSelectedComponent(trackingPanel);
		tabs.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e){
				    JTabbedPane tabSource = (JTabbedPane)e.getSource();
				    String tab = tabSource.getTitleAt(tabSource.getSelectedIndex());
				    if (tab.equals(analysisPanelTabName))
				    	analysisPanel.FocusEnter();				      
				}
			}
		);		
	}
	/** 
	 * morph the phenotype training tab into a phenotype retraining tab
	 * by refreshing the {@link KThumbnailPane thumbnail pane}, adding
	 * new keystrokes for the sliders, adding boosting support along with
	 * error finding support
	 */
	public void AddOrUpdateRetrainTab(){
		phenotypeTrainPanel.AdjustThumbnailPane();
		phenotypeTrainPanel.SetUpKeyStrokesForAlphaSliders();
		phenotypeTrainPanel.SelectNON();
		phenotypeTrainPanel.EnableBoostingFeature();
		trackingPanel.EnableTrackingFeature();
		phenotypeTrainPanel.EnableErrorFinderFeature();
		phenotypeTrainPanel.SetupConfusionLocator();
		phenotypeTrainPanel.DisableConfusionLocator(); //initially it's inactive
		phenotypeTrainPanel.ClickFirstThumbnailIfExistsAndIsNecessary();
		tabs.remove(phenotypeTrainPanel);
		tabs.insertTab(phenoTrainTabName,null,phenotypeTrainPanel,null,2);	
//		tabs.setSelectedComponent(phenotypeTrainPanel);
		tabs.setSelectedComponent(trackingPanel);
		repaint();
	}
	/** during classification, remove the phenotype training panel */
	public void KillPhenotypeTab() {
		tabs.remove(phenotypeTrainPanel);
		phenotypeTrainPanel.ClearImage();	
	}
	/** create a separation in the menu */
	private class MenuSeparator extends Container {
		public MenuSeparator() {
			this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));			
			this.add(Box.createHorizontalStrut(3));
			this.add(new JSeparator(SwingConstants.HORIZONTAL));
			this.add(Box.createHorizontalStrut(3));
		}
	}
	/** create the file menu, help menu, script menu */
	private void CreateMenu() {
		
		JMenuBar menu=new JMenuBar();
		
		JMenu file_menu = new JMenu("File");
		file_menu.setMnemonic('f');

//		JMenuItem file_new = new JMenuItem("New project");
//		file_new.setMnemonic('n');
//		file_new.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,Event.CTRL_MASK));
//		file_new.addActionListener(
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
//					Run.NewProject();
//				}
//			}
//		);
		
		file_save = new JMenuItem("Save project");
		file_save.setMnemonic('s');
		file_save.setEnabled(false);
		file_save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,Event.CTRL_MASK));
		file_save.addActionListener( 
			new ActionListener(){	
				public void actionPerformed(ActionEvent e) {
					Run.SaveProject();
				}
			}
		);
		
//		JMenuItem file_save_as = new JMenuItem("Save project as");
//		file_save_as.setMnemonic('a');
//		file_save_as.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,Event.CTRL_MASK+Event.SHIFT_MASK));
//		file_save_as.addActionListener( 
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
//					Run.SaveAsProject();
//				}
//			}
//		);
		
//		JMenuItem file_open = new JMenuItem("Open project");
//		file_open.setMnemonic('o');
//		file_open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,Event.CTRL_MASK));
//		file_open.addActionListener( 
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
//					Run.OpenProject(true);				
//				}
//			}
//		);
		
		JMenuItem cacheSettings = new JMenuItem("Cache settings");
		cacheSettings.setMnemonic('c');
		cacheSettings.addActionListener( 
				new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						makeCacheSettingsBox();
						showCacheSettingsFrame();
					}
				}
			);
		
		
		JMenuItem file_quit = new JMenuItem("Quit");
		file_quit.setMnemonic('q');
		file_quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,Event.CTRL_MASK));
		file_quit.addActionListener( 
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					Run.it.ExitProgram();			
				}
			}
		);

//		file_menu.add(file_new);
//		file_menu.add(new MenuSeparator());
		file_menu.add(file_save);
		file_menu.add(new MenuSeparator());
		file_menu.add(cacheSettings);
//		file_menu.add(file_save_as);
//		file_menu.add(file_open);
		file_menu.add(new MenuSeparator());
		file_menu.add(file_quit);
		
		menu.add(file_menu);
		
		JMenu script_menu = new JMenu("Script");
		script_menu.setMnemonic('s');
		JMenuItem loadAndRun = new JMenuItem("Load and Run Analysis Script . . .");
		loadAndRun.setMnemonic('A');
		loadAndRun.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,Event.CTRL_MASK));
		loadAndRun.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					tabs.setSelectedComponent(analysisPanel);
					
					JFileChooser chooser = new JFileChooser();
					chooser.setFileFilter(
						new FileFilter(){
							public boolean accept(File f){
								if (f.isDirectory())
									return true;
								String name=f.getName();
								String[] pieces=name.split("\\.");
								if (pieces.length != 2)
									return false;
								if (pieces[1].toLowerCase().equals("txt"))
									return true;
								return false;
							}
							public String getDescription(){
								return "txt";
							}
						}
					);		
					int result = chooser.showOpenDialog(null);
					if ( result == JFileChooser.APPROVE_OPTION ){						
						final File file = chooser.getSelectedFile();
						analysisPanel.RunScript(file);																	
					}
					else
						JOptionPane.showMessageDialog(Run.it.getGUI(),"Not a valid script file");					
				}
			}
		);
		script_menu.add(loadAndRun);
		
		menu.add(script_menu);
		
		JMenu help_menu = new JMenu("Help");
		help_menu.setMnemonic('h');
		JMenuItem manual = new JMenuItem("Manual");
		manual.setMnemonic('m');
		manual.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,Event.CTRL_MASK));
		manual.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					IOTools.RunSystemProgramInOS(ManualFileName);
				}
			}
		);
		help_menu.add(manual);
		
		menu.add(help_menu);
		
		//add it finally:
		setJMenuBar(menu);
	}
	
	private void showCacheSettingsFrame(){
		if (cacheSettingsFrame != null){
			cacheSettingsFrame.setVisible(false);
			cacheSettingsFrame.dispose();
			cacheSettingsFrame = null;
		}
		cacheSettingsFrame = new JFrame("Cache settings");
		cacheSettingsFrame.add(cacheSettingsBox);
	    cacheSettingsFrame.pack();
	    cacheSettingsFrame.setVisible(true);						
	    cacheSettingsFrame.setLocationRelativeTo(null);
	}

	protected void makeCacheSettingsBox() {
		// TODO Auto-generated method stub
		cacheSettingsBox = Box.createVerticalBox();

		/** HACKS to store/retrieve cache settings. Used to be in ImageAndScoresBank.**/

		cacheSettingsBox.setBorder(new LineBorder(Color.black));

		final JSpinner SASMSSpinner = new JSpinner(new SpinnerNumberModel(Run.it.SUPER_AND_SCORES_MAX_SIZE,0,Integer.MAX_VALUE,1));
		final JSpinner DMSSpinner = new JSpinner(new SpinnerNumberModel(Run.it.DATAIMAGES_MAX_SIZE,0,Integer.MAX_VALUE,1));
		final JSpinner NRIMSSpinner = new JSpinner(new SpinnerNumberModel(Run.it.NUANCE_RAW_IMAGES_MAX_SIZE,0,Integer.MAX_VALUE,1));
		final JSpinner IIMSSpinner = new JSpinner(new SpinnerNumberModel(Run.it.IS_IMAGES_MAX_SIZE,0,Integer.MAX_VALUE,1));
		final JSpinner CIMSSpinner = new JSpinner(new SpinnerNumberModel(Run.it.CONFUSION_IMAGES_MAX_SIZE,0,Integer.MAX_VALUE,1));
		final JSpinner NFSOSpinner = new JSpinner(new SpinnerNumberModel(Run.it.NUM_FRAME_SCORE_OBJECTS,0,Integer.MAX_VALUE,1));
		
		final JButton DefaultButton = new JButton("Restore Defaults");
		
		SASMSSpinner.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						Run.it.setSASMS((Integer) SASMSSpinner.getValue());
						Run.it.GUIsetDirty(true);
					}	
				}
		);
		DMSSpinner.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						Run.it.setDMS((Integer) DMSSpinner.getValue());
						Run.it.GUIsetDirty(true);
					}	
				}
		);
		NRIMSSpinner.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						Run.it.setNRIMS((Integer) NRIMSSpinner.getValue());
						Run.it.GUIsetDirty(true);
					}	
				}
		);
		IIMSSpinner.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						Run.it.setIIMS((Integer) IIMSSpinner.getValue());
						Run.it.GUIsetDirty(true);
					}	
				}
		);
		CIMSSpinner.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						Run.it.setCIMS((Integer) CIMSSpinner.getValue());
						Run.it.GUIsetDirty(true);
					}	
				}
		);
		NFSOSpinner.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						Run.it.setNFSO((Integer) NFSOSpinner.getValue());
						Run.it.GUIsetDirty(true);
					}	
				}
		);


		DefaultButton.addActionListener(
				new ActionListener(){

					public void actionPerformed(ActionEvent e) {
						Run r = new Run();
						int val = r.getNFSO();
						NFSOSpinner.setValue(val);
						val = r.getSASMS();
						SASMSSpinner.setValue(val);
						val = r.getDMS();
						DMSSpinner.setValue(val);
						val = r.getNRIMS();
						NRIMSSpinner.setValue(val);
						val = r.getIIMS();
						IIMSSpinner.setValue(val);
						val = r.getCIMS();
						CIMSSpinner.setValue(val);
						val = r.getNFSO();
						NFSOSpinner.setValue(val);
					}
					
				}
		);
		


		Box b1 = Box.createHorizontalBox();
		b1.add(new JLabel("SUPER_AND_SCORES_MAX_SIZE:"));
		b1.add(Box.createHorizontalGlue());
		b1.add(SASMSSpinner);
		Box b2 = Box.createHorizontalBox();
		b2.add(new JLabel("DATAIMAGES_MAX_SIZE:"));
		b2.add(Box.createHorizontalGlue());
		b2.add(DMSSpinner);
		Box b3 = Box.createHorizontalBox();
		b3.add(new JLabel("NUANCE_RAW_IMAGES_MAX_SIZE:"));
		b3.add(Box.createHorizontalGlue());
		b3.add(NRIMSSpinner);
		Box b4 = Box.createHorizontalBox();
		b4.add(new JLabel("IS_IMAGES_MAX_SIZE:"));
		b4.add(Box.createHorizontalGlue());
		b4.add(IIMSSpinner);
		Box b5 = Box.createHorizontalBox();
		b5.add(new JLabel("CONFUSION_IMAGES_MAX_SIZE:"));
		b5.add(Box.createHorizontalGlue());
		b5.add(CIMSSpinner);
		Box b6 = Box.createHorizontalBox();
		b6.add(new JLabel("NUM_FRAME_SCORE_OBJECTS:"));
		b6.add(Box.createHorizontalGlue());
		b6.add(NFSOSpinner);

		Box b7 = Box.createHorizontalBox();
		b7.add(Box.createHorizontalGlue());
		b7.add(DefaultButton);

		
		cacheSettingsBox.add(b1);
		cacheSettingsBox.add(b2);
		cacheSettingsBox.add(b3);
		cacheSettingsBox.add(b4);
		cacheSettingsBox.add(b5);
		cacheSettingsBox.add(b6);
		cacheSettingsBox.add(b7);
	}
	/** gets the collection of images in the phenotype training set */
	public Collection<String> getTrainingImageSet(){
		return phenotypeTrainPanel.getTrainedImagesFilenames();
	}
	/** creates the title, and populates the training panels
	 *  
	 * @param imageset		The imageset - need to make sure it's a user color imageset
	 */
	public void DefaultPopulateWindows(ImageSetInterface imageset){
		setTitle(Run.Title+Run.it.projectName);
		if (imageset instanceof ImageSetInterfaceWithUserColors){
			colorTrainPanel.DefaultPopulateBrowser();
			backgroundColorTrainPanel.DefaultPopulateBrowser();
		}
		phenotypeTrainPanel.DefaultPopulateBrowser();
//		preProcessPanel.DefaultPopulateBrowser();
		classifyPanel.SetValuesToOpenProject();

		setVisible(true);
	}
	/** creates the title, and populates the training panels from a loaded project */
	public void OpenPopulateWindows(ImageSetInterface imageset){
		setTitle(Run.Title+Run.it.projectName);
		if (imageset instanceof ImageSetInterfaceWithUserColors){
			colorTrainPanel.PopulateFromOpen();
			backgroundColorTrainPanel.PopulateFromOpen();
		}
		phenotypeTrainPanel.PopulateFromOpen();
//		preProcessPanel.DefaultPopulateBrowser();
		classifyPanel.SetValuesToOpenProject();
		setVisible(true);
	}
	/** en/disables the add/remove image buttons after an image load */
	public void DisableButtons(){
		colorTrainPanel.RevalidateImageButtons();
		phenotypeTrainPanel.RevalidateImageButtons();
//		preProcessPanel.RevalidateImageButtons();
	}
//	/** only shows tracking **/
//	public void macro1(){
//		setTitle(Run.Title+Run.it.projectName);
//		tabs.setEnabledAt(classifyTrainTabName, false);
//		tabs.addTab(colorTrainTabName, colorTrainPanel);
//		tabs.addTab(backgroundColorTrainTabName, backgroundColorTrainPanel);
//		tabs.addTab(phenoTrainTabName, phenotypeTrainPanel);
//		tabs.addTab(trackingTabName, trackingPanel);
//		tabs.addTab(classifyTrainTabName, classifyPanel);
//		tabs.addTab(analysisPanelTabName, analysisPanel);
//
//		colorTrainPanel.RevalidateImageButtons();
//		phenotypeTrainPanel.RevalidateImageButtons();
////		preProcessPanel.RevalidateImageButtons();
//	}
	/** repaints the GUI */
	public void repaint(){
		setTitle(Run.Title+Run.it.projectName+toappend);
		super.repaint();
	}
	/** adds global keystrokes for in/decrementing zoom and toggling between mark / delete in training panels */
	private void addUniversalKeyStrokes() {
		
		input_map=tabs.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		action_map=tabs.getActionMap();
		
		String PLUS = "magnify";
		String MINUS = "demagnify";
		String SPACE = "togglemark";
		
		KeyStroke k_plus1 = KeyStroke.getKeyStroke('+');
		KeyStroke k_plus2 = KeyStroke.getKeyStroke('=');
		KeyStroke k_minus = KeyStroke.getKeyStroke('-');
		KeyStroke k_space = KeyStroke.getKeyStroke(' ');

		
		AbstractAction f_plus = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				KPanel kpanel = (KPanel)tabs.getSelectedComponent();
				kpanel.getImagePanel().adjustMagnifierIntensity(1);
			}
		};
		AbstractAction f_minus = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				KPanel kpanel = (KPanel)tabs.getSelectedComponent();
				kpanel.getImagePanel().adjustMagnifierIntensity(-1);
			}
		};
		AbstractAction f_space = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				KPanel kpanel = (KPanel) tabs.getSelectedComponent();
				if ( kpanel instanceof KTrainPanel )
					if (kpanel.imagePanel.isFocusOwner())
						((KTrainPanel)kpanel).toggleMarkOrDelete();
			}
		};
		
		input_map.put(k_plus1,PLUS);
		input_map.put(k_plus2,PLUS);
		input_map.put(k_minus,MINUS);
		input_map.put(k_space,SPACE);
		action_map.put(PLUS,f_plus);
		action_map.put(MINUS,f_minus);
		action_map.put(SPACE,f_space);
	}
	/** builds the identifier images for color train panel or phenotype train panel */
	public void buildExampleImages(boolean color) {
		phenotypeTrainPanel.buildExampleImages();
//		this.preProcessPanel.buildImage();
		if (color){
			colorTrainPanel.buildExampleImages();
			backgroundColorTrainPanel.buildExampleImages();
		}
	}
	/** sets whether the training points in the {@link KPhenotypeTrainPanel phenotype training panel} are visible */
	public void setSeeTrainingPoints(boolean see){
		if (see)
			phenotypeTrainPanel.EnableTrainPoints();		
		else
			phenotypeTrainPanel.DisableTrainPoints();
	}
	/** for some image set types, it is appropriate to not have image helpers */
	public void DisableTrainingHelpers(){
		colorTrainPanel.DisableTrainingHelper();
		phenotypeTrainPanel.DisableTrainingHelper();
	}
	public void EnableTrainingHelpers(){
		colorTrainPanel.EnableTrainingHelper();
		phenotypeTrainPanel.EnableTrainingHelper();
	}	
	public void DisableSave(){
		file_save.setEnabled(false);
	}
	public void EnableSave(){
		file_save.setEnabled(true);
	}
	public boolean isActive() {
		return active;
	}
	public ActionMap getAction_map() {
		return action_map;
	}
	public InputMap getInput_map() {
		return input_map;
	}
	public void buildGlobalsViewsInAnalysis() {
		analysisPanel.addImageIcons();		
	}
	public void KillAnalysisTab(){
		tabs.remove(analysisPanel);		
	}
	public void KillPreProcessTab(){
//		tabs.remove(preProcessPanel);		
	}
	public void SwitchToAnalysisTab(){
		tabs.setSelectedComponent(analysisPanel);
	}
	public ConsoleParser getParser(){
		return analysisPanel.getParser();
	}
	public void DeleteColorTrainTab() {
		tabs.remove(colorTrainPanel);
	}
	public KClassifyPanel getClassifyPanel() {
		return classifyPanel;
	}
	public void AddAdjustColorButton() {
		phenotypeTrainPanel.AddAdjustColorButton();		
	}
	public KTrainPanel getTrainingPanel(){
		return phenotypeTrainPanel;
	}	
	public void AppendToTitle(String toappend){
		this.toappend = toappend;
	}
	public void TurnOnSophisticatedTrainingHelper() {
		phenotypeTrainPanel.TurnOnSophisticatedTrainingHelper();		
	}
}