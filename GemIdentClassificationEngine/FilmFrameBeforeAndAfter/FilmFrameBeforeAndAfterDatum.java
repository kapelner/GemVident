package GemIdentClassificationEngine.FilmFrameBeforeAndAfter;

import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import GemIdentClassificationEngine.DatumThatUsesRingScores;
import GemIdentImageSets.ColorVideoSet;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentOperations.Run;
import GemIdentTools.Matrices.ShortMatrix;

public final class FilmFrameBeforeAndAfterDatum extends DatumThatUsesRingScores {

	private FilmFrameBeforeAndAfterDatumSetup datumSetup;
	private int f;

	public FilmFrameBeforeAndAfterDatum(FilmFrameBeforeAndAfterDatumSetup datumSetup, Point to, String filename) {
		super(datumSetup, to);
		this.datumSetup = datumSetup;		
		f = ((ColorVideoSet)Run.it.imageset).filenameToFrameNum(filename);
//		System.out.println("Record for frame: " + f + "  pt:" + to.x + "," + to.y);
		BuildRecord();
	}

	protected void BuildRecord() {
		HashMap<String, ShortMatrix> master_scores = new HashMap<String, ShortMatrix>((FilmFrameBeforeAndAfterDatumSetup.FrameOffsets.size() + 1) * datumSetup.numFilters());
		LinkedHashMap<Integer, String> hash = datumSetup.getVideoSet().getFramesInRange(f, FilmFrameBeforeAndAfterDatumSetup.FrameOffsets);
		for (int frame_offset : hash.keySet()){
			String filename = hash.get(frame_offset);
			HashMap<String, ShortMatrix> scores;
			try {
				scores = ImageAndScoresBank.getOrAddScores(filename);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				System.err.println("frame_offset: " + frame_offset);
				e.printStackTrace();
				continue;
			}
			for (String color : scores.keySet()){
				master_scores.put(color + "_frame: " + frame_offset, scores.get(color));
			}
		}
		BuildManyRingsFromManyColors(datumSetup.getMaximumRadius(), master_scores);
	}

}
