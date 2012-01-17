package GemIdentView;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import GemIdentImageSets.ColorVideoSet;
import GemIdentImageSets.DataImage;
import GemIdentModel.Phenotype;
import GemIdentModel.TrainingImageData;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentTools.Thumbnails;

@SuppressWarnings("serial")
public class ViewTrainingPoints extends JFrame {
	
	/** the title of the window */
	private static final String Title = "View all training points";
	/** the dimension of the window */
	private static final Dimension frameSize = new Dimension(500, 400);
	
	private LinkedHashMap<String, Phenotype> phenotypes_map;

	
	public ViewTrainingPoints(LinkedHashMap<String, Phenotype> phenotypes_map){
		super(Title);
		this.phenotypes_map = phenotypes_map;
		
		if (doesNotHavePointsAtAll()){
			return; //ditch if no training points exist
		}

		DisplayFrame(SetUpPhenotypeTabs());

	}

	private boolean doesNotHavePointsAtAll() {
		for (Phenotype phenotype : phenotypes_map.values()){
			if (phenotype.getTotalPoints() > 0){
				return false;
			}
		}
		return true;
	}

	private JTabbedPane SetUpPhenotypeTabs() {
		JTabbedPane phenotype_tabbed_pane = new JTabbedPane();
		//iterate over phenotypes
		for (String phenotype_name : phenotypes_map.keySet()){
			Phenotype phenotype = phenotypes_map.get(phenotype_name);
			//only add the tab if training points actually exist
			if (phenotype.getTotalPoints() > 0){
				JPanel phenotye_panel = new JPanel();
				phenotype_tabbed_pane.addTab(phenotype_name, phenotye_panel);
				SetUpPhenotypeTab(phenotye_panel, phenotype);
			}
		}
		//add it to the frame
		return phenotype_tabbed_pane;
	}
	/** the dimension of the window */
	private static final Dimension innerTabsSize = new Dimension(470, 330);
	private void SetUpPhenotypeTab(JPanel phenotye_panel, Phenotype phenotype) {
		JTabbedPane image_tabbed_pane = new JTabbedPane();
		image_tabbed_pane.setPreferredSize(innerTabsSize);
		HashMap<String, TrainingImageData> training_map = phenotype.getTrainingImagesMap();
		for (String filename : training_map.keySet()){
			TrainingImageData data = training_map.get(filename);
			//only add the tab if training points exist
			if (data.getNumPoints() > 0){
				JPanel image_panel = new JPanel();
				image_panel.add(new JLabel("click on a point to delete it"));
				//make sure we don't need to change the dispaly of the filename
				
				String display_filename = Run.it.imageset instanceof ColorVideoSet ? ((ColorVideoSet)Run.it.imageset).filenameToFrameNum(filename) + "" : filename;
				image_tabbed_pane.addTab(display_filename, image_panel);
				try {
					SetUpImageTab(image_panel, data, phenotype, filename);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}				
			}
		}
		phenotye_panel.add(image_tabbed_pane);
	}
	
	private static final Dimension scrollpaneSize = new Dimension(450, 280);
	private void SetUpImageTab(JPanel image_panel, TrainingImageData data, Phenotype phenotype, String filename) throws FileNotFoundException {
		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(scrollpaneSize);
		JScrollBar vbar = scrollPane.getVerticalScrollBar(); //set up vbar
		vbar.setUnitIncrement(75);
		vbar.setPreferredSize(new Dimension(7, vbar.getHeight()));
		JPanel pane = new JPanel();
		pane.setLayout(new WrapLayout(WrapLayout.LEFT));
		PopulatePaneWithSnippetButtons(pane, data, phenotype, filename);
		scrollPane.setViewportView(pane);
		image_panel.add(scrollPane);
	}

	private void PopulatePaneWithSnippetButtons(JPanel pane, TrainingImageData data, Phenotype phenotype, String filename) throws FileNotFoundException {
		DataImage image = data.getImage();
		//iterate over the points, generate a snippet for each one, and add a little image button
		for (Point p : data.getPoints()){
			pane.add(GenerateSnippetButton(image.getSnippet(p, phenotype.getRmax(), phenotype.getRmax(), phenotype.getDisplayColor()), p, phenotype, filename));
		}
	}
	
	private static final float SnippetImageBlowUpScale = 3;
	private JButton GenerateSnippetButton(BufferedImage snippet, final Point p, final Phenotype phenotype, final String filename) {
		final JButton button = new JButton();
		button.setPreferredSize(new Dimension((int)Math.round(snippet.getWidth() * SnippetImageBlowUpScale), (int)Math.round(snippet.getHeight() * SnippetImageBlowUpScale)));
		button.setIcon(new ImageIcon(Thumbnails.ScaleImage(snippet, SnippetImageBlowUpScale, SnippetImageBlowUpScale))); //scale it too
		//now add a listener that will respond when user clicks:
		button.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					phenotype.deletePointFromTrainingSet(filename, p);
					Run.it.getGUI().repaint();
					Run.it.GUIsetDirty(true); //now you can save
					button.setIcon(new ImageIcon(IOTools.OpenSystemImage("ex.gif")));
				}
			}
		);	
		return button;
	}

	private void DisplayFrame(JTabbedPane tabbedPane) {
		setPreferredSize(frameSize);
		setResizable(true);
		add(tabbedPane);
		pack();
		setVisible(true);
	}
}
