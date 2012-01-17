
package GemIdentImageSets;

import java.io.Serializable;

/**
 * Contains information on image sets created
 * with the BLISS workstation - a product of Bacus
 * Laboratories, Inc. The class is able to support
 * the old image set.
 * 
 * Image sets are composed of: Da1.jpg, Da2.jpg, . . .,
 * Da"n".jpg (where "n" is the number of images) and an 
 * initialization file: FinalScan.ini which contains 
 * information about where images are located within 
 * the global image.
 * 
 * This class is not documented fully.
 * 
 * @author Adam Kapelner
 */
public final class BlissImageListOld extends BlissImageList implements Serializable{
	
	private static final long serialVersionUID = -515147296966562272L;
	/** Number of overlap columns on the left of old BLISS images */
	private static final int XO=3;
	/** Number of overlap columns on the right of old BLISS images */
	private static final int XF=2;
	/** Number of overlap rows on the top of old BLISS images */
	private static final int YO=2;
	/** Number of overlap rows on the bottom of old BLISS images */
	private static final int YF=3;
	
	public BlissImageListOld(String homedir){
		super(homedir);
		xo = XO;
		xf = XF;
		yo = YO;
		yf = YF;
	}
	
	protected void ProcessList(){
		for (BlissImage I:list){ //find maxs
			I.standardx=Math.abs(I.posrawx-xmax);
			I.standardy=Math.abs(I.posrawy-ymax);
			I.pixelx=(int)Math.floor(I.standardx*W/((double)NUM_GRID_PER_IMAGE_X));
			I.pixely=(int)Math.floor(I.standardy*H/((double)NUM_GRID_PER_IMAGE_Y));
			I.piclocx=(int)Math.round((double)I.pixelx/(W-XO-XF));
			I.piclocy=(int)Math.round((double)I.pixely/(H-YO-YF));
		}
	}
	public int getXo() {
		return XO;
	}

	public int getXf() {
		return XF;
	}

	public int getYo() {
		return YO;
	}

	public int getYf() {
		return YF;
	}
}
