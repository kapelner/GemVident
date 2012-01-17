package GemIdentView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import GemIdentImageSets.ColorVideoSet;
import GemIdentImageSets.NonGlobalImageSet;
import GemIdentOperations.Run;
import GemIdentTools.Thumbnails;


public abstract class KAbstractColorTrainPanel extends KTrainPanel {
	private static final long serialVersionUID = -8786955647180580534L;

	/** 
	 * sets up the Add / Delete color buttons and connects the training helper button.
	 * The addition and deletion of colors is dicussed in section 3.1.1 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	protected void CreateWest(){
		super.CreateWest();
		addClassInfoButton.setText("+ Color");
		deleteClassInfoButton.setText("- Color");
		viewAllTrainingPoints.setVisible(false); //one day I'll do this, not today
		TrainHelperButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (!(Run.it.imageset instanceof NonGlobalImageSet)){
						Run.it.imageset.CreateHTMLForComposite(Thumbnails.tWidth,Thumbnails.tHeight);					
					}
					else if (Run.it.imageset instanceof ColorVideoSet){
						((ColorVideoSet)Run.it.imageset).CreateFilmReelTrainer(that);
					}
				}
			}
		);
	}
	
	protected void SpawnViewAllTrainingPointsDialog(){}//one day I'll do this, not today
	
	/** spawns a dialog where user selects which image to delete, then deletes it */
	protected Bundle SpawnRemoveImageDialog() {
		final Bundle bundle=super.SpawnRemoveImageDialog();
	
		bundle.remove.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (Run.it.getUserColorsImageset().RemoveImageFromStainSet(bundle.list.getSelectedItem())){
						thumbnailPane.RemoveThumbnail(bundle.list.getSelectedItem());
						imagePanel.setDisplayImage(null);
						imagePanel.repaint();
					}					
				}
			}
		);
		return bundle;
	}

	/** adds a new color to training panel */
	protected void AddInfo(){
		AddInfo("Color"+browser.getNumInfos());
	}
	
	/** When the user beings a new project, four generic colors are added to the panel */
	public void DefaultPopulateBrowser(){
		CreateListeners();	
	}
	
	/** creates the settings box and appropriately titles the slider object */
	protected void buildSettingsBoxes(){
		super.buildSettingsBoxes("Alpha:");
	}
}
