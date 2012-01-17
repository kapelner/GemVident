
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
import GemIdentTools.Matrices.StringMatrix;

/**
 * An image set that contains images that belong to an overall global image context
 * 
 * @author Adam Kapelner
 */
public final class GlobalImageSet extends ImageSetInterfaceWithUserColors implements Serializable{
	
	private static final long serialVersionUID = 786397416431297508L;

	public GlobalImageSet(String homedir){
		super(homedir);
	}
	
	public DataImage getDataImageFromFilename(String filename) throws FileNotFoundException{
		return new RegularSubImage(filename, false);
	}

	@Override
	public void CreateHTMLForComposite(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<String> GetImages() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringMatrix GetLocalPics(String filename, Integer notused) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BufferedImage getGlobalImageSlice(int rowA, int rowB) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getFilterNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getGlobalHeight(boolean excise) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getGlobalWidth(boolean excise) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getInitializationFilename() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringMatrix getPicFilenameTable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Point getTrueLocation(String filename, Point to, boolean excise) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getXf() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getXo() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getYf() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getYo() {
		// TODO Auto-generated method stub
		return 0;
	}

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