package GemIdentView;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Collection;

import GemIdentImageSets.BackgroundHandler;
import GemIdentImageSets.DataImage;
import GemIdentModel.Stain;
import GemIdentOperations.Run;
import GemIdentTools.Matrices.BoolMatrix;

@SuppressWarnings("serial")
public class KImageBackgroundColorTrainPanel extends KImageColorTrainPanel {
	
	/** the image that holds the true background (basically just a clone of the original) */
	protected BufferedImage trueBackground;
	/** the image that contains the background pixels */
	protected BufferedImage backgroundImage;
	/** the object that handles the computation for the background matrix */
	private BackgroundHandler backgroundHandler;
	
	public KImageBackgroundColorTrainPanel(KBackgroundColorTrainPanel backgroundColorTrainPanel) {
		super(backgroundColorTrainPanel);		
	}
	
	/** sets a new training image. During retraining, creates the {@link #errorOverlay mask} that will display type I errors */
	protected void setDisplayImage(DataImage displayImage) {
		super.setDisplayImage(displayImage);
		if (displayImage != null){
			backgroundHandler = new BackgroundHandler(displayImage);
			redrawBackgroundImage();
			//since a new image is selected, we must change the true background image as well:
			setAlphaOfTrueBackgroundAndRepaint(((KBackgroundColorTrainPanel)trainPanel).getTrue_background_alpha());
			repaint();
		}
	}
	
	public void DeleteBackgroundColorAndRepaint(String name){
		backgroundHandler.removeBackgroundStain(name);
		redrawBackgroundImage();
		repaint();		
	}
	
	public void recreateBooleanBackgroundForIndividualStainRedrawAndRepaint(Stain stain){
		backgroundHandler.recreateBooleanBackgroundForIndividualStain(stain);
		redrawBackgroundImage();
		repaint();
	}	
	
	private static final int CheckerSize = 3;
	private void redrawBackgroundImage() {
		BoolMatrix master_background = backgroundHandler.createMasterBackgroundMatrix();
		backgroundImage = new BufferedImage(displayImage.getWidth(), displayImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		int transparent_black = new Color(0, 0, 0, 0).getRGB();
		int transparent_white = new Color(255, 255, 255, 0).getRGB();
		int opaque_black = new Color(0, 0, 0, 255).getRGB();
		int opaque_white = new Color(255, 255, 255, 255).getRGB();
		for (int i = 0; i < displayImage.getWidth(); i++){
			for (int j = 0; j < displayImage.getHeight(); j++){
				if ((i / CheckerSize + j / CheckerSize) % 2 == 0){					
					if (master_background.get(i, j)){
						backgroundImage.setRGB(i, j, opaque_black);
					}
					else {
						backgroundImage.setRGB(i, j, transparent_black);
					}				
				}
				else {
					if (master_background.get(i, j)){
						backgroundImage.setRGB(i, j, opaque_white);
					}
					else {
						backgroundImage.setRGB(i, j, transparent_white);
					}
				}
			}
		}		
	}

	public void paintComponent(Graphics g){
		super.paintComponent(g);
		if (displayImage != null){
			
			int p_width = this.getWidth();
			int p_height = this.getHeight();
			int i_width = this.displayImage.getWidth();
			int i_height = this.displayImage.getHeight();
			
			int x_0 = (p_width-i_width)/4;
			int y_0 = (p_height-i_height)/4;
			
			if ( x_0 < 0 ) x_0 = 0;
			if ( y_0 < 0 ) y_0 = 0;
	
	        g.drawImage(backgroundImage, x_0, y_0, i_width, i_height, null);
	        if (trueBackground != null){
	        	g.drawImage(trueBackground, x_0, y_0, i_width, i_height, null);
	        }
		}
	}

	protected Collection<Stain> getStainObjects(){
		return Run.it.getUserColorsImageset().getBackgroundColorObjects();
	}

	public void setAlphaOfTrueBackgroundAndRepaint(int opacity_of_true_background) {
		trueBackground = new BufferedImage(displayImage.getWidth(), displayImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < displayImage.getWidth(); i++){
			for (int j = 0; j < displayImage.getHeight(); j++){
				trueBackground.setRGB(i, j, new Color(displayImage.getR(i, j), displayImage.getG(i, j), displayImage.getB(i, j), opacity_of_true_background).getRGB());
			}
		}
		repaint();
	}
}
