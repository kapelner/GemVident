
package GemIdentImageSets;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import GemIdentClassificationEngine.DatumSetup;
import GemIdentClassificationEngine.StandardPlaneColor.StandardPlaneColorDatumSetup;
import GemIdentOperations.Run;
import GemIdentTools.Thumbnails;
import GemIdentTools.Matrices.StringMatrix;

/**
 * An image set that contains images that do not belong to an overall global image context.
 * Since most of the abstract functions in the super classes deal with global image context
 * functionality, there's not much to implement in this class
 * 
 * @author Adam Kapelner
 */
public class NonGlobalImageSet extends ImageSetInterfaceWithUserColors implements Serializable {

	private static final long serialVersionUID = 7928568299846106407L;

	public NonGlobalImageSet(){}
	
	public NonGlobalImageSet(String homedir) throws FileNotFoundException {
		super(homedir);
	}
	
	public DataImage getDataImageFromFilename(String filename) throws FileNotFoundException{
		return new RegularSubImage(filename, false);
	}

	@Override
	public void CreateHTMLForComposite(int width, int height) {}

	@Override
	public ArrayList<String> GetImages() {
		return Thumbnails.GetImageListAsCollection(this.getHomedir());
	}
	
	@Override //return null to force SuperImage to make reflections
	public StringMatrix GetLocalPics(String filename, Integer notused) {return null;}

	@Override
	public BufferedImage getGlobalImageSlice(int rowA, int rowB) {return null;}

	@Override
	public Set<String> getFilterNames() {
		return stains.keySet();
	}

	@Override
	public int getGlobalHeight(boolean excise) {return 0;}

	@Override
	public int getGlobalWidth(boolean excise) {return 0;}

	@Override
	public String getInitializationFilename() {return null;}

	@Override
	public StringMatrix getPicFilenameTable() {return null;}

	@Override
	public Point getTrueLocation(String filename, Point to, boolean excise) {
		return to;
	}

	@Override
	public int getXf() {return 0;}

	@Override
	public int getXo() {return 0;}

	@Override
	public int getYf() {return 0;}

	@Override
	public int getYo() {return 0;}

	@Override
	public BufferedImage getGlobalThumbnailImage(Float approxGlobalScaledWidth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashSet<String> getClickedonimages() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void presave() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void spawnSophisticatedHelper() {}

	@Override
	public void RunUponNewProject() {
		// TODO Auto-generated method stub
		
	}
	
	public void ThumbnailsCompleted(){}
	
	public DatumSetup setUpDataExtractionMethod(){
		return new StandardPlaneColorDatumSetup(this, getFilterNames(), Run.it.getMaxPhenotypeRadiusPlusMore(null));
	}	
}