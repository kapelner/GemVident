
package GemIdentImageSets;

import java.awt.Point;
import java.io.FileNotFoundException;

import GemIdentOperations.Run;


/**
 * An implementation of a {@link NuanceSubImage NuanceSubImage} that contains information
 * on its edges of the surrounding images
 * 
 * @author Adam Kapelner
 */
public final class SuperNuanceImage extends NuanceSubImage implements SuperImage {

	/** the center image */
	private NuanceSubImage C;
	/** the number of surrounding pixels */
	private int c;
	
	public SuperNuanceImage(NuanceImageListInterface imagelist, Object num_or_filename) throws FileNotFoundException{
		this.imagelist = imagelist;
		
		if (num_or_filename instanceof String){
			filename=(String)num_or_filename;
			num = NuanceImageListInterface.ConvertFilenameToNum(filename);
		}
		else if (num_or_filename instanceof Integer){
			num = (Integer)num_or_filename;
			filename = NuanceImageListInterface.ConvertNumToFilename(num);
		}
		
		C = (NuanceSubImage)ImageAndScoresBank.getOrAddDataImage(filename);

		//get number of pixels to surround by
		c = Run.it.getMaxPhenotypeRadiusPlusMore(null) + 2; //add 2 just in case!
		//debug:
//		c = 600; //delete this!!!!!!
		
		//now we don't worry about the displayimage at all, we need to get
		//the data for all nuance images surrounding this one
		nuanceimages = imagelist.getNuanceImagePixelDataForSuper(num, c, C);
		cropped = false;
		setupDisplayImage();
		//debug:
//		IOTools.WriteImage("super_" + filename + ".tif", "TIFF", getAsBufferedImage());
	}
	
	public DataImage getCenterImage(){
		return C;
	}
	
	public Point AdjustPointForSuper(Point t){
		return new Point(t.x+c,t.y+c);
	}
}