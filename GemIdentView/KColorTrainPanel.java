
package GemIdentView;

import GemIdentModel.Stain;
import GemIdentModel.TrainSuperclass;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;

/**
 * An extension of {@link KTrainPanel the generic training panel}
 * with specific functionality for color training. See step 1 in the 
 * Algorithm section of the IEEE paper for this
 * panel's purpose and see fig. 3 in the for an example
 * of the user training. Also, for a more detailed discussion on the
 * purpose of color training, see section 3.1 in the manual.
 * 
 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class KColorTrainPanel extends KAbstractColorTrainPanel {

	/** Deletes a training color */
	protected void DeleteInfo(String name){
		Run.it.getUserColorsImageset().DeleteStain(name);
		super.DeleteInfo(name);
	}
	/** resets the panel */
	protected void ResetImagePanelAndAddListeners() {
		imagePanel=new KImageColorTrainPanel(this);
		super.ResetImagePanelAndAddListeners();
	}

	/** adds a color with a specific name */
	protected void AddInfo(String text){
		super.AddInfo(text);
		KStainInfo info = new KStainInfo(((KImageTrainPanel)this.imagePanel),text,this);
		Run.it.getUserColorsImageset().AddStain(info.getStain());
		this.browser.addInfoClass(info);
	}	
	/** adds a color based the {@link GemIdentModel.TrainSuperclass underlying model} of that color */
	protected void AddInfo(TrainSuperclass trainer){
		KStainInfo info=new KStainInfo(((KImageTrainPanel)imagePanel),(Stain)trainer,this);
		browser.addInfoClass(info);
	}
	/** Creates a delete info dialog with the appropriate title for deleting a color */
	protected void SpawnDeleteInfoDialog(){
		super.SpawnDeleteInfoDialog("Color", browser.getClassInfoNames());
	}

	/** When the user opens a saved project, the panel is populated from the stain objects from the gem file */
	public void PopulateFromOpen(){
		for (Stain stain : Run.it.getUserColorsImageset().getStainObjects())
			AddInfo(stain);
		CreateListeners();
		for (String filename : Run.it.getUserColorsImageset().getStainTrainingImages()){
			if(!IOTools.FileExists(filename)){
				System.out.println("Can't find training image " + filename +", skipping...");
				continue;
			}
			thumbnailPane.addThumbnail(new KThumbnail(this,filename),false);
		}
		super.PopulateFromOpen();
	}
	
	/** create an add color dialog with the appropriate title for adding colors */
	protected void SpawnAddInfoDialog(){
		super.SpawnAddInfoDialog("color");
	}
	
	/** the number of infos to create on a new project */
	private static final int NumberOfInitialColors = 2;
	
	public void DefaultPopulateBrowser(){
		super.DefaultPopulateBrowser();
		for (int i = 0; i < NumberOfInitialColors; i++){
			AddInfo();
		}	
	}

}