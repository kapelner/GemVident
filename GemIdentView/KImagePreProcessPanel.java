
package GemIdentView;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import GemIdentImageSets.NuanceSubImage;

/**
 * Controls the displaying of result images during {@link 
 * GemIdentClassificationEngine.Classify classification} and
 * {@link GemIdentCentroidFinding.PostProcess post-processing}.
 * For discussion on viewing the classification via watching the
 * image panel, see section 4.1.3 of the manual.
 * 
 * see the Nuance scanner webpage (http://www.cri-inc.com/products/nuance.asp)
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class KImagePreProcessPanel extends KImagePanel{
	
	/** the alpha level of the displayed result masks */
	//private static final int overAlpha=127;

	
	/** the master panel incorporating this image panel */
	//private KPreProcessPanel preProcessPanel;

	/** default constructor */
	
	public KImagePreProcessPanel(KPreProcessPanel preProcessPanel){
		super();
		//this.preProcessPanel=preProcessPanel;		
	}
	
	/**
	 * Paints the image panel. First {@link KImagePanel#paintComponent(Graphics)
	 * draw the actual image}. If there is no underlying image, display
	 * nothing and return. Otherwise, for each of the phenotypes being classified,
	 * {@link GemIdentTools.Matrices.BoolMatrix#IllustrateAsMask(BufferedImage image,Color color)
	 * create an overlay mask} and draw it on top of the underlying image.
	 */

	public void paintComponent( Graphics g ) {
		super.paintComponent(g);

		if ( g == null ) {
			System.out.println("Graphics NULL");
			return;
		}

		int p_width = that.getWidth();
		int p_height = that.getHeight();
		g.setColor(Color.BLACK);
		g.fillRect(0,0,p_width,p_height);

		if ( displayImage != null ) {
			int i_width = displayImage.getWidth();
			int i_height = displayImage.getHeight();
			
			int x_0 = (p_width-i_width)/2;
			int y_0 = (p_height-i_height)/2;
			
			i_width=p_width;
			i_height=p_height;
			
			//double scale_rat = java.lang.Math.max((double)i_width/p_width,(double)i_height/p_height);
			//i_width = (int) ((double)i_width/scale_rat);
			//i_height = (int) ((double)i_height/scale_rat);
			
				
			if ( x_0 < 0 ) x_0 = 0;
			if ( y_0 < 0 ) y_0 = 0;
			
			
			
			g.drawImage(displayImage.getAsBufferedImage(),x_0,y_0,i_width,i_height,null);
			
			if (displayImage instanceof NuanceSubImage) //draw the Nuance filters atop the regular image
				for (BufferedImage I : ((NuanceSubImage)displayImage).getWaveImages())
					g.drawImage(I,x_0,y_0,i_width,i_height,null);			
		} else {
			g.setColor(Color.WHITE);
			g.drawString("[no image selected]",p_width/2-50,p_height/2-5);
		}
		//scrollPane.revalidate();
					
	}

}
	