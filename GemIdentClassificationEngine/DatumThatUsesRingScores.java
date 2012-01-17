package GemIdentClassificationEngine;

import java.awt.Point;
import java.util.HashMap;

import GemIdentTools.Geometry.Rings;
import GemIdentTools.Matrices.ShortMatrix;

/**
 * An intermediate base class that houses common methods among datums that
 * construct their data using "ring scores"
 * 
 * @author Adam Kapelner
 */
public abstract class DatumThatUsesRingScores extends Datum {
	
	public DatumThatUsesRingScores(DatumSetup datumSetup, Point to){
		super(datumSetup, to);
	}	

	/**
	 * Builds the {@link #record record} from all the rings from 0, ..., max radius
	 * for all the included colors 
	 * 
	 * @param R			the maximum radius
	 * @param scores	the mapping from color name --> score matrix
	 */
	protected void BuildManyRingsFromManyColors(int R, HashMap<String, ShortMatrix> scores) {
		int Lo=0;
		for (String color : scores.keySet()){
			for (int r = 0; r <= R; r++){
				record[Lo] = GetScore(scores.get(color), r);
				Lo++;
			}
		}
	}

	/**
	 * Generates a "ring score" -  a scalar score for 
	 * a given radius and a given score matrix
	 * by adding up the scores in the score matrix at each coordinate in
	 * the discretized ring of the given radius. See step 5a in the 
	 * Algorithm section of the IEEE paper for a formal mathematical description.
	 *  
	 * @param scoreMatrix		the matrix of scores for a given color
	 * @param r					the radius of the ring
	 * @return					the summed up total score
	 * 
	 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
	 */
	protected int GetScore(ShortMatrix scoreMatrix, int r){
		int score = 0;
		int width = scoreMatrix.getWidth();
		int height = scoreMatrix.getHeight();
		for (Point t : Rings.getRing(r)){
//			try {
				int ptx = t.x+to.x;
				int pty = t.y+to.y;
				if (ptx >=0 && ptx < width && pty >= 0 &&pty < height)
					score += scoreMatrix.get(t.x + to.x, t.y + to.y);
//			} catch (ArrayIndexOutOfBoundsException e){
//				System.out.println("scoreMatrix: " + scoreMatrix.getWidth() + "x" + scoreMatrix.getHeight() + " point: (" + (t.x + to.x) + "," + (t.y + to.y) + ") r: " + r);
//			}
		}
		return score;
	}
}
