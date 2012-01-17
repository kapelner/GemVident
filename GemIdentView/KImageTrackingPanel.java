
package GemIdentView;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;

import javax.swing.ScrollPaneConstants;

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
public class KImageTrackingPanel extends KImagePanel{
	
	/** the alpha level of the displayed result masks */
	//private static final int overAlpha=127;

	
	/** the master panel incorporating this image panel */
	//private KPreProcessPanel preProcessPanel;

	/** default constructor */
	
	public KImageTrackingPanel(KTrackingPanel trackingPanel){
		super(251,251);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//this.preProcessPanel=preProcessPanel;		
	}

	
	public Point adjustPoint(Point point) {
		if (displayImage == null)
			return point;
		
		//rescale point to location in displayImage
		int p_width = scrollPane.getWidth();
		int p_height = scrollPane.getHeight();
		
		int d_width = displayImage.getWidth();
		int d_height = displayImage.getHeight();
		
		Point pt = new Point();
		pt.x = (int) (point.x*(double) d_width/ (double) p_width); 
		pt.y = (int) (point.y*(double) d_height/ (double) p_height); 
		
		return pt;
	}


	/**
	 * Paints the image panel. First {@link KImagePanel#paintComponent(Graphics)
	 * draw the actual image}. If there is no underlying image, display
	 * nothing and return. Otherwise, for each of the phenotypes being classified,
	 * {@link GemIdentTools.Matrices.BoolMatrix#IllustrateAsMask(BufferedImage image,Color color)
	 * create an overlay mask} and draw it on top of the underlying image.
	 */

	public void paintComponent( Graphics g ) {
//		super.paintComponent(g);

//		scrollPane.
		
		if ( g == null ) {
			System.out.println("Graphics NULL");
			return;
		}

		int p_width = scrollPane.getWidth();
		int p_height = scrollPane.getHeight();

		if ( displayImage != null ) {
			g.drawImage(displayImage.getAsBufferedImage().getScaledInstance(p_width,p_height,Image.SCALE_FAST),0,0,null);
		} else {
			g.create(0, 0, p_width, p_height);
			g.setColor(Color.BLACK);
			g.fillRect(0,0,p_width,p_height);
			g.setColor(Color.WHITE);
			g.drawString("[no image selected]",p_width/2-50,p_height/2-5);
		}
	}

}
	