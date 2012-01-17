
package GemIdentView;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentModel.Phenotype;
import GemIdentModel.TrainingImageData;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Solids;
import GemIdentTools.Matrices.ShortMatrix;

/**
 * Controls the displaying of images during {@link KPhenotypeTrainPanel phenotype training}
 * 
 * @author Adam Kapelner
 *
 */
@SuppressWarnings("serial")
public class KImagePhenotypeTrainPanel extends KImageTrainPanel {

	/** default constructor */
	public KImagePhenotypeTrainPanel(KPhenotypeTrainPanel trainPanel) {
		super(trainPanel);
	}
	/** draws the current training image with an overlay of the user's training points and the alpha masks */
	protected void ReDrawOverImageFromScratch() {
		trainPointsOverImage=new BufferedImage(displayImage.getWidth(),displayImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
		if (alphaLevelForTrainPoints > ALPHA_VISIBILITY_THRESHOLD){ //draw shadows only if it's noticeable:
			Color back=new Color(0,0,0,alphaLevelForTrainPoints);
			for (Phenotype phenotype:Run.it.getPhenotypeObjects()){
//					System.out.println("phenotype draw:"+phenotype.getName());
				TrainingImageData trainingImagedata=phenotype.getTrainingImage(displayImage.getFilename());
				if (trainingImagedata != null){ //ie it has it
					ArrayList<Point> points=trainingImagedata.getPoints();						
					for (Point to:points)
						for (Point t:Solids.GetPointsInSolidUsingCenter(phenotype.getRmax(),to))
							try {trainPointsOverImage.setRGB(t.x,t.y,back.getRGB());} catch (Exception e){}
				}
			}
		}
		for (Phenotype phenotype:Run.it.getPhenotypeObjects()){
			TrainingImageData trainingImagedata=phenotype.getTrainingImage(displayImage.getFilename());
			if (trainingImagedata != null){ //ie it has it
				ArrayList<Point> points=trainingImagedata.getPoints();
				Color display=phenotype.getDisplayColor();
				for (Point to:points)
					for (Point t:Solids.GetPointsInSolidUsingCenter(phenotype.getRmin(),to))
						try {trainPointsOverImage.setRGB(t.x,t.y,display.getRGB());} catch (Exception e){}
			}
		}

	}
	
	private ShortMatrix confusion_overlay_data_raw;
	private BufferedImage confusion_overlay_image;
	private int confusion_measure;
	private boolean displayConfusionImage;
	public void GenerateErrorOverlayImage(){
		String current_image = displayImage.getFilename();
		confusion_overlay_data_raw = ImageAndScoresBank.getOrAddConfusionImage(current_image);
		if (confusion_overlay_data_raw == null){ //ie file not found
			confusion_overlay_image = null; //this will flag it not to be painted upon a repaint
			((KPhenotypeTrainPanel)trainPanel).DisableConfusionLocator();
			//ditch because we don't need to build the image:
			return;
		}
		else {
			((KPhenotypeTrainPanel)trainPanel).ReenableConfusionLocator();
		}
		confusion_overlay_image = new BufferedImage(confusion_overlay_data_raw.getWidth(), confusion_overlay_data_raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < confusion_overlay_data_raw.getWidth(); i++){
			for (int j = 0; j < confusion_overlay_data_raw.getHeight(); j++){
				if (confusion_overlay_data_raw.get(i, j) >= confusion_measure){
					int color = 0;
					if (Math.random() >= 0.5){
						color = Color.RED.getRGB();
					}
					else {
						color = Color.BLACK.getRGB();
					}
					confusion_overlay_image.setRGB(i, j, color);
				}
			}
		}		
	}	
	public static final int MaxConfusionMeasure = 128 - 1; //-1 because it's gotta be under 50%
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		if (confusion_overlay_image != null && confusion_measure < MaxConfusionMeasure && displayConfusionImage){
			int p_width = this.getWidth();
			int p_height = this.getHeight();
			int i_width = this.displayImage.getWidth();
			int i_height = this.displayImage.getHeight();
			
			int x_0 = (p_width-i_width)/4;
			int y_0 = (p_height-i_height)/4;
			
			if ( x_0 < 0 ) x_0 = 0;
			if ( y_0 < 0 ) y_0 = 0;	
			
//			System.out.println("KImagePhenotypeTrainPanel  pw: " + p_width + " ph: " + p_height + " iw: " + i_width + " ih: " + i_height + " xo: " + x_0 + " yo: " + y_0);
			
			g.drawImage(confusion_overlay_image, x_0, y_0, i_width, i_height, null);
		}	
	}
	/**
	 * When the user selects a new training point, this function updates the
	 * image overlay mask and displays that new point. The addition of new 
	 * points to the phenotypes training set is discussed in section 3.2.4 of
	 * the manual.
	 * 
	 * @param filename		the filename of the image the user is training
	 * @param to			the new coordinate
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	public void NewPoint(String filename,Point to){
		Phenotype phenotype=(Phenotype)trainPanel.getActiveTrainer();
		abstractPaintNewPoint(filename, to, phenotype);
	}
	/**
	 * User just wanted to add a non point
	 */	
	public void NewNonPoint(String filename, Point to){
		abstractPaintNewPoint(filename, to, Run.it.getNONPhenotype());
	}	
	
	/**
	 * A general function used to paint new training points atop the points mask
	 * 
	 * @param filename		the image filename currently being painted upon
	 * @param to			the point where the user clicked
	 * @param phenotype		the phenotype the user is training
	 */	
	private void abstractPaintNewPoint(String filename, Point to, Phenotype phenotype){
		if (phenotype == null){ 
			return;
		}
		if (alphaLevelForTrainPoints > 5){ //draw shadows only if it's noticeable:
			Color back=new Color(0,0,0,alphaLevelForTrainPoints);
			for (Point t:Solids.GetPointsInSolidUsingCenter(phenotype.getRmax(),to)){
//					Color color=new Color(overImage.getRGB(t.x,t.y),true);
//					System.out.print(" color at ("+t.x+","+t.y+"): "+color.getRGB()+" ("+color.getRed()+","+color.getGreen()+","+color.getBlue()+","+color.getAlpha()+")");
				if (t.x >= 0 && t.y >= 0 && t.x < displayImage.getWidth() && t.y < displayImage.getHeight())
					if ((new Color(trainPointsOverImage.getRGB(t.x,t.y),true)).equals(new Color(0,0,0,0)))
						trainPointsOverImage.setRGB(t.x,t.y,back.getRGB());
			}
		}			
		for (Point t:Solids.GetPointsInSolidUsingCenter(phenotype.getRmin(),to))
			try {trainPointsOverImage.setRGB(t.x,t.y,phenotype.getDisplayRGB());} catch (Exception e){}
			
	}

	/** the basic setter with checks for null images, and if the image has been classified */
	protected void setDisplayImage(DataImage displayImage,boolean classified){
		super.setDisplayImage(displayImage);
		if (displayImage == null)
			((KPhenotypeTrainPanel)trainPanel).RemoveSliders();
		if (trainPanel instanceof KPhenotypeTrainPanel){
			if (classified)
				((KPhenotypeTrainPanel)trainPanel).AddOrEditSliders();
			else
				((KPhenotypeTrainPanel)trainPanel).RemoveSliders();
			//now handle confusion image:
			GenerateErrorOverlayImage();
		}
	}
	public void setConfusionMeasureAndRefresh(int flipped_confusion_measure) {
		confusion_measure = MaxConfusionMeasure - flipped_confusion_measure;
		//and we gotta make sure we refresh the image now:
		GenerateErrorOverlayImage();
	}
	public void setDisplayConfusionImage(boolean displayConfusionImage) {
		this.displayConfusionImage = displayConfusionImage;
	}
}