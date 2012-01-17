
package GemIdentModel;

import java.awt.Color;
import java.io.File;
import java.io.Serializable;

import javax.swing.JProgressBar;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageSetInterfaceWithUserColors;
import GemIdentOperations.StainMaker;
import GemIdentOperations.StainOpener;
import GemIdentTools.Matrices.DoubleMatrix;
import GemIdentTools.Matrices.ShortMatrix;
import GemIdentView.JProgressBarAndLabel;

/**
 * Houses the specific data and functions necessary to build a stain. Built on top of
 * {@link TrainSuperclass TrainSuperclass}. It implements {@link java.io.Serializable Serializable} 
 * to easily dump its data to XML format when saving a <b>GemIdent</b> project
 * 
 * @author Adam Kapelner
 */
public class Stain extends TrainSuperclass implements Serializable {
	
	private static final long serialVersionUID = -4605439129835115290L;
	
	/** the cube that stores the Mahalanobis score for all 16.8M RGB colors (the "Mahalanobis score cube") */
	transient private short[][][] cube;
	
	public Stain(){
		super();		
	}

	/**
	 * Gets a StainOpener object that will load the Mahalanobis score cube from the hard disk file
	 * @param openProgress		the progress bar for the opening of the stain
	 * @param increment 		the progress bar increases this amount for each stain successfully opened
	 * @return					the StainOpener object
	 */
	public StainOpener getStainOpener(JProgressBarAndLabel openProgress,int increment){
		return new StainOpener(this,openProgress,increment);
	}
	/**
	 * Gets a StainMaker object that will build the Mahalanobis score cube from the training points' colors
	 * @param openProgress		the progress bar for the opening of the stain
	 * @return					the StainOpener object
	 */
	public StainMaker getStainMaker(JProgressBar progress, JProgressBarAndLabel openProgress){
		if (getTotalPoints() >= ImageSetInterfaceWithUserColors.MinimumColorPointsNeeded)
			return new StainMaker(this,progress,openProgress);
		else
			return null;
	}
	public short[][][] GetMahalCube(){
		return cube;
	}
	public void SetMahalCube(short[][][] cube){
		this.cube=cube;
	}
	/**
	 * Takes in a {@link GemIdentImageSets.DataImage DataImage} and returns a 
	 * {@link GemIdentTools.Matrices.ShortMatrix ShortMatrix} of the same dimension where
	 * the entries are the Mahalanobis scores of the image's pixels. See step 4a in the 
	 * Algorithm section of the IEEE paper for a formal mathematical description.
	 * 
	 * @param image		the image to be scored
	 * @return			the image's scores
	 * 
	 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
	 */
	public ShortMatrix MahalanobisColorScoring(DataImage image) {
		int rows=image.getWidth();
		int cols=image.getHeight();
		ShortMatrix scores=new ShortMatrix(rows,cols);

		for (int i=0;i<rows;i++)
			for (int j=0;j<cols;j++)
				scores.set(i,j,(short)(cube[image.getR(i,j)][image.getG(i,j)][image.getB(i,j)]+Short.MAX_VALUE));
		return scores;
	}
	/** delete the file that stores the Mahalanobis score cube */
	public void DeleteFile(){
		(new File(name)).delete();
	}
	/** free the memory that previously housed the Mahalanobis score cube */
	public void FlushCube(){
		cube=null;		
	}
	
	public Color getAverageColor(){
		DoubleMatrix mu = StainMaker.CalculateAverageColor(getColors());
		return new Color((int)Math.floor(mu.get(0, 0)), (int)Math.floor(mu.get(1, 0)), (int)Math.floor(mu.get(2, 0)));
	}
}