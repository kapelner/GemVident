package GemIdentClassificationEngine.FilmSphereColor;

import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import GemIdentClassificationEngine.Datum;
import GemIdentImageSets.ColorVideoSet;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Point3d;
import GemIdentTools.Geometry.Spheres;
import GemIdentTools.Matrices.ShortMatrix;

public final class FilmSphereColorDatum extends Datum {

	private FilmSphereColorDatumSetup datumSetup;
	private int f;
	
	public FilmSphereColorDatum(FilmSphereColorDatumSetup datumSetup, Point to, String filename) {
		super(datumSetup, to);
		this.datumSetup = datumSetup;		
		f = ((ColorVideoSet)Run.it.imageset).filenameToFrameNum(filename);
		BuildRecord();		
	}

	protected void BuildRecord() {
		BuildManySphereShellsFromManyColors(datumSetup.getMaximumRadius(), ImageAndScoresBank.getOrAddFrameScoreObject(this, f));		
	}
	
	public LinkedHashMap<Integer, HashMap<String, ShortMatrix>> generateFrameScoreObject(){
		LinkedHashMap<Integer, HashMap<String, ShortMatrix>> frame_scores = new LinkedHashMap<Integer, HashMap<String, ShortMatrix>>(datumSetup.getMaximumRadius() * 2 + 1);
		//get all score for all frames +- R (ie 2 * R + 1 toal)
		LinkedHashMap<Integer, String> hash = datumSetup.getVideoSet().getFramesInRange(f, datumSetup.getMaximumRadius());
		for (int frame_offset : hash.keySet()){
			String filename = hash.get(frame_offset);
			HashMap<String, ShortMatrix> scores;
			try {
				scores = ImageAndScoresBank.getOrAddScores(filename);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
			frame_scores.put(frame_offset, scores);
		}
		return frame_scores;
	}
	
	/**
	 * Builds the {@link #record record} from all the sphere-shells from 0, ..., max radius
	 * for all the included colors 
	 * 
	 * @param R					the maximum radius
	 * @param frame_scores		the mapping from frame offset --> the mapping from color name --> score matrix
	 */
	protected void BuildManySphereShellsFromManyColors(int R, LinkedHashMap<Integer, HashMap<String, ShortMatrix>> frame_scores) {
		int Lo=0;
		for (String color : filterNames()){
			for (int r = 0; r <= R; r++){
				for (Point3d t : Spheres.GetPointsInSphereShellUsingCenter(r, new Point3d(to.x, to.y, 0))){
					ShortMatrix scoreMatrix = null;
					try {
						scoreMatrix = frame_scores.get(t.z).get(color);
						record[Lo] += scoreMatrix.get(t.x, t.y);
					}
					catch (ArrayIndexOutOfBoundsException e){
						System.out.println("ArrayIndexOutOfBoundsException z: " + t.z + "\t\tscoreMatrix: " + scoreMatrix.getWidth() + "x" + scoreMatrix.getHeight() + " point: (" + (t.x + to.x) + "," + (t.y + to.y) + ") r: " + r);
					}
				}
				Lo++;
			}
		}
//		Print();
	}	
}
