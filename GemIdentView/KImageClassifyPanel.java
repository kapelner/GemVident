package GemIdentView;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import GemIdentOperations.Run;
import GemIdentTools.Matrices.BoolMatrix;

/**
 * Controls the displaying of result images during {@link 
 * GemIdentClassificationEngine.Classify classification} and
 * {@link GemIdentCentroidFinding.PostProcess post-processing}.
 * For discussion on viewing the classification via watching the
 * image panel, see section 4.1.3 of the manual.
 * 
 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class KImageClassifyPanel extends KImagePanel{
	
	/** the alpha level of the displayed result masks */
	private static final int overAlpha=127;
	/** the master panel incorporating this image panel */
	private KClassifyPanel classifyPanel;

	/** default constructor */
	public KImageClassifyPanel(KClassifyPanel classifyPanel){
		super();
		this.classifyPanel=classifyPanel;		
	}
	/**
	 * Paints the image panel. First {@link KImagePanel#paintComponent(Graphics)
	 * draw the actual image}. If there is no underlying image, display
	 * nothing and return. Otherwise, for each of the phenotypes being classified,
	 * {@link GemIdentTools.Matrices.BoolMatrix#IllustrateAsMask(BufferedImage image,Color color)
	 * create an overlay mask} and draw it on top of the underlying image.
	 */
	public void paintComponent(Graphics g){
//		System.out.println("KICP.repaint");
		super.paintComponent(g); //first draw the actual image
		// quit if there's no image to load...
		if ( super.displayImage == null ){ 
			g.drawImage(null,0,0,getWidth(),getHeight(),null);
			return;		
		}
		
		int p_width = getWidth();
		int p_height = getHeight();
		int i_width = displayImage.getWidth();
		int i_height = displayImage.getHeight();
		
		int x_0 = (p_width-i_width)/2;
		int y_0 = (p_height-i_height)/2;
		
		if ( x_0 < 0 ) x_0 = 0;
		if ( y_0 < 0 ) y_0 = 0;
		
		try {
	//		System.out.println("inside KImageClassifyPanel repaint");	
			if (classifyPanel.is != null){
				for (String phenotype:classifyPanel.is.keySet()){
					BufferedImage overDisplay=new BufferedImage(displayImage.getWidth(),displayImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
					Color color=Run.it.getPhenotype(phenotype).getDisplayColorWithAlpha(overAlpha);
					
					BoolMatrix B=classifyPanel.is.get(phenotype);
					if (B != null){
						B.IllustrateAsMask(overDisplay,color);			
		//				g.drawImage(overDisplay, 0, 0, null);
						g.drawImage(overDisplay,x_0,y_0,i_width,i_height,null);//draw on top of original image
					}
				}
			}
		} catch (Exception e){}
		UpdateMagnifier(getMousePosition());
	}
}