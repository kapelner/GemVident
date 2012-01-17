
package GemIdentImageSets;

import java.io.Serializable;

/**
 * Contains information on image sets created
 * with the BLISS workstation - a product of Bacus
 * Laboratories, Inc. The class is able to support
 * both the new BLISS workstation and the NanoZoomer
 * 
 * This class is not documented fully.
 * 
 * @author Adam Kapelner
 */
public final class BlissImageListNanoZoomer extends BlissImageList implements Serializable{
	
	private static final long serialVersionUID = -1246964666479869275L;
	/** The offset on the grid metric for the NanoZoomer */
	private static final int NanoZoomerGridOffset = -20;
	/** Number of overlap columns on the left of new BLISS images */
	private static final int XO=0;
	/** Number of overlap columns on the right of new BLISS images */
	private static final int XF=0;
	/** Number of overlap rows on the top of new BLISS images */
	private static final int YO=0;
	/** Number of overlap rows on the bottom of new BLISS images */
	private static final int YF=0;
	
	public BlissImageListNanoZoomer(String homedir){
		super(homedir);
		xo = XO;
		xf = XF;
		yo = YO;
		yf = YF;		
		NUM_GRID_PER_IMAGE_X += NanoZoomerGridOffset;
		NUM_GRID_PER_IMAGE_Y += NanoZoomerGridOffset;
	}
	
	protected void ProcessList(){	
		for (BlissImage I:list){ //find maxs
			I.standardx=Math.abs(I.posrawx-xmax);
			I.standardy=Math.abs(I.posrawy); //the one difference!!!!!!
			I.pixelx=(int)Math.floor(I.standardx*W/((double)NUM_GRID_PER_IMAGE_X));
			I.pixely=(int)Math.floor(I.standardy*H/((double)NUM_GRID_PER_IMAGE_Y));
			I.piclocx=(int)Math.round((double)I.pixelx/(W-XO-XF));
			I.piclocy=(int)Math.round((double)I.pixely/(H-YO-YF));
		}		
	}
}