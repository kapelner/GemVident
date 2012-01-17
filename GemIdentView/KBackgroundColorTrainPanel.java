package GemIdentView;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JSlider;

import GemIdentModel.Stain;
import GemIdentModel.TrainSuperclass;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;

@SuppressWarnings("serial")
public class KBackgroundColorTrainPanel extends KAbstractColorTrainPanel {
	
	private JSlider true_background_slider;
	private int true_background_alpha;

	/** Deletes a background color */
	protected void DeleteInfo(String name){
		Run.it.getUserColorsImageset().DeleteBackgroundColor(name);
		super.DeleteInfo(name);
		((KImageBackgroundColorTrainPanel)imagePanel).DeleteBackgroundColorAndRepaint(name);		
	}
	
	/** resets the panel */
	protected void ResetImagePanelAndAddListeners() {
		imagePanel = new KImageBackgroundColorTrainPanel(this);
		super.ResetImagePanelAndAddListeners();
	}	
	
	/** adds a background color with a specific name */
	protected void AddInfo(String text){
		super.AddInfo(text);
		KBackgroundStainInfo info = new KBackgroundStainInfo(((KImageBackgroundColorTrainPanel)this.imagePanel), text, this);
		Run.it.getUserColorsImageset().AddBackgroundColor(info.getStain());
		this.browser.addInfoClass(info);
	}
	
	/** Creates a delete info dialog with the appropriate title for deleting a color */
	protected void SpawnDeleteInfoDialog(){
		super.SpawnDeleteInfoDialog("Background Color", browser.getClassInfoNames());
	}
	

	/** When the user opens a saved project, the panel is populated from the stain objects from the gem file */
	public void PopulateFromOpen(){
		for (Stain stain : Run.it.getUserColorsImageset().getBackgroundColorObjects())
			AddInfo(stain);
		CreateListeners();
		for (String filename : Run.it.getUserColorsImageset().getBackgroundColorTrainingImages()){
			if(!IOTools.FileExists(filename)){
				System.out.println("Can't find training image " + filename +", skipping...");
				continue;
			}
			thumbnailPane.addThumbnail(new KThumbnail(this,filename),false);
		}
		super.PopulateFromOpen();
	}	
	
	/** adds a color based the {@link GemIdentModel.TrainSuperclass underlying model} of that color */
	protected void AddInfo(TrainSuperclass trainer){
		KBackgroundStainInfo info = new KBackgroundStainInfo(((KImageBackgroundColorTrainPanel)imagePanel), (Stain)trainer, this);
		browser.addInfoClass(info);
	}	
	
	/** create an add color dialog with the appropriate title for adding colors */
	protected void SpawnAddInfoDialog(){
		super.SpawnAddInfoDialog("background color");
	}
	
	/** adds the mark / delete button box and the location sensor */
	protected void AppendEast() {
		super.AppendEast();
		
		GUIFrame true_background_panel = new GUIFrame("True Background");		
		true_background_slider = new JSlider(JSlider.HORIZONTAL, 0, 255, 0);
		true_background_slider.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent arg0) {}
			public void mouseEntered(MouseEvent arg0) {}
			public void mouseExited(MouseEvent arg0) {}
			public void mousePressed(MouseEvent arg0) {}
			public void mouseReleased(MouseEvent arg0) {
				true_background_alpha = true_background_slider.getValue();
				((KImageBackgroundColorTrainPanel)imagePanel).setAlphaOfTrueBackgroundAndRepaint(true_background_alpha);
			}
			
		});
		true_background_panel.add(true_background_slider);	
		
		super.appendToEast(true_background_panel);
	}

	public int getTrue_background_alpha() {
		return true_background_alpha;
	}	
}
