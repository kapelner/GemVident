
package GemIdentOperations;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JProgressBar;

import GemIdentModel.Stain;
import GemIdentTools.IOTools;
import GemIdentTools.Matrices.DoubleMatrix;
import GemIdentView.JProgressBarAndLabel;

/**
 * The class responsible for creating a "Mahalanobis Cube" for one of the
 * relevant colors. A "Mahalanobis Cube" is a 3d array of short values where
 * each is a Mahalanobis score. Each coordinate in the cube in the specific
 * score from that coordinate to the mean of the distribution of colors given by
 * the user. StainMaker can also be threaded in a thread pool. See step 2 in the
 * Algorithm section of the IEEE paper for a formal mathematical description.
 * 
 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
 * @see <a href="http://en.wikipedia.org/wiki/Mahalanobis_distance">Mahalanobis
 *      Distance (wikipedia)</a>
 * 
 * @author Adam Kapelner
 */
public class StainMaker implements Runnable {

	/** the directory where the color cubes are stored */
	public static final String colorSubDir = "colors";
	/**
	 * every score is multiplied by this scaling factor in order to keep higher
	 * resolution at low score values
	 */
	private static final int MAHAL_MULT = 10;

	/**
	 * the {@link GemIdentModel.Stain Stain} object that the cube will be
	 * generated from
	 */
	private Stain stain;
	/** the progress bar that updates the user on the construction of the cube */
	private JProgressBar progress;
	/**
	 * in the unlikely event that the cube must be constructed during
	 * classification, this progress bar is used
	 */
	private JProgressBarAndLabel openProgress;

	/**
	 * Default constructor - also creates the
	 * {@link #colorSubDir color directory} (if necessary)
	 */
	public StainMaker(Stain stain, JProgressBar progress,
			JProgressBarAndLabel openProgress) {
		this.stain = stain;
		this.progress = progress;
		this.openProgress = openProgress;

		if (!IOTools.DoesDirectoryExist(colorSubDir))
			(new File(Run.it.imageset.getFilenameWithHomePath(colorSubDir)))
					.mkdir();

	}

	/**
	 * This {@link #CreateMahalonobisCube(ArrayList, String) creates the cube}
	 * and stores it within the {@link GemIdentModel.Stain Stain} object, and
	 * then {@link #saveCube(short[][][], String) saves} it to the hard disk
	 */
	public void run() {
		// System.out.println("inside stain maker:"+stain.getName());
		stain.SetMahalCube(CreateMahalonobisCube(stain.getColors(), stain
				.getName()));
		saveCube(stain.GetMahalCube(), stain.getName());
		stain.setDirty(false); // no longer dirty!!!
	}

	/**
	 * Saves the cube to the hard disk in the
	 * {@link #colorSubDir colors directory}. The short values are just dumped
	 * straight to a file in order i-j-k
	 * 
	 * @param cube
	 *            the Mahalanobis cube to be saved
	 * @param filename
	 *            the name of the file to write to
	 */
	private void saveCube(short[][][] cube, String filename) {
		if (cube == null)
			return;
		if (progress != null)
			progress.setValue(90);
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(Run.it.imageset
							.getFilenameWithHomePath(colorSubDir
									+ File.separator + filename))));
		} catch (IOException e) {
			System.out.println("file not found: " + filename);
		}

		// System.out.println("begin saving "+filename);
		for (int i = 0; i < 256; i++) {
			for (int j = 0; j < 256; j++) {
				for (int k = 0; k < 256; k++) {
					if (i == 128 && j == 128 && k == 128)
						if (progress != null)
							progress.setValue(95);
					try {
						out.writeShort(cube[i][j][k]);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// System.out.println("done saving "+filename);
		if (progress != null)
			progress.setValue(100);
		if (openProgress != null)
			openProgress.setValue(openProgress.getValue()
					+ (int) Math.round(100 / ((double) Run.it
							.getUserColorsImageset().NumFilters())));
	}

	/**
	 * Given a list of colors, compute the average color by averaging all reds, blues, and greens
	 * 
	 * @param data	the list of colors
	 * @return		the average color as a 3x1 matrix of doubles
	 */	
	public static DoubleMatrix CalculateAverageColor(ArrayList<Color> data) {
		double uR = 0;
		double uG = 0;
		double uB = 0;

		for (Color i : data) {
			uR += i.getRed();
			uG += i.getGreen();
			uB += i.getBlue();
		}

		uR /= data.size() - 1;
		uG /= data.size() - 1;
		uB /= data.size() - 1;

		DoubleMatrix u = new DoubleMatrix(3, 1);
		u.set(0, 0, uR);
		u.set(1, 0, uG);
		u.set(2, 0, uB);

		return u;
	}

	/**
	 * Creates the "Mahalanobis Cube" from the sample data:
	 * <p>
	 * a) find the sample mean (u) and
	 * {@link #FindCovarianceMatrix(ArrayList, double, double, double) calculate}
	 * the sample variance-covariance matrix (S) of the data set
	 * </p>
	 * <p>
	 * b) For each coordinate x=(i,j,k) \in {0,...,255}^3 calculate the
	 * Mahalanobis distance:
	 * <ul>
	 * <li>MahalDistance(x) = Transpose(x - u) * Inverse(S) * (x - u)</li>
	 * </ul>
	 * The distances are artificially scaled by a {@link #MAHAL_MULT multiple}
	 * for greater resolution at low values, and artificially truncated at the
	 * maximum value of a short, 65536
	 * </p>
	 * 
	 * @param data
	 *            the sample data to generate the cube from
	 * @param cubeName
	 *            the name of the cube (for debugging purposes only)
	 * @return the cube: 256x256x256 (16.8 million) short values that is the
	 *         scores for every 24bit RGB color
	 */
	private short[][][] CreateMahalonobisCube(ArrayList<Color> data,
			String cubeName) {

		if (data.size() == 0)
			return null; // initial condition

		// System.out.println("creating color-cube:"+cubeName);

		DoubleMatrix mu = CalculateAverageColor(data);

		DoubleMatrix covinv = FindCovarianceMatrix(data, mu.get(0, 0),
				mu.get(1, 0), mu.get(2, 0)).inverse();
		if (progress != null)
			progress.setValue(2);
		short[][][] cube = new short[256][256][256];
		// System.out.println("got cov matrix");
		for (int i = 0; i < 256; i++) {
			for (int j = 0; j < 256; j++) {
				for (int k = 0; k < 256; k++) {
					if (i == 64 && j == 64 && k == 64) {
						// System.out.println(cubeName+" 25% complete");
						if (progress != null)
							progress.setValue(23);
					}
					if (i == 128 && j == 128 && k == 128) {
						// System.out.println(cubeName+" 50% complete");
						if (progress != null)
							progress.setValue(45);
					}
					if (i == 192 && j == 192 && k == 192) {
						// System.out.println(cubeName+" 75% complete");
						if (progress != null)
							progress.setValue(78);
					}
					DoubleMatrix tempPixel = new DoubleMatrix(3, 1);
					tempPixel.set(0, 0, i);
					tempPixel.set(1, 0, j);
					tempPixel.set(2, 0, k);
					DoubleMatrix scalar = ((tempPixel.transpose()).minus(mu
							.transpose())).times(covinv).times(
							tempPixel.minus(mu));
					short dist = (short) (Math.round(scalar.get(0, 0)
							* MAHAL_MULT) - Short.MAX_VALUE); // amplify it by
																// 100 to get
																// more accuracy
					if (dist < Short.MAX_VALUE)
						cube[i][j][k] = dist;
					else
						cube[i][j][k] = Short.MAX_VALUE;
				}
			}
			// System.out.println("distance:"+cube[i][100][100]+" percent
			// completed:"+(double)i/256*100);
		}
		// System.out.println(cubeName+" 100% complete");
		return (cube);
	}

	/**
	 * Given a dataset, data, calculates the sample var-cov matrix
	 * 
	 * @param data
	 *            the training data of RGB color values
	 * @param uR
	 *            the mean of the red values
	 * @param uG
	 *            the mean of the green values
	 * @param uB
	 *            the mean of the blue values
	 * @return the sample var-cov matrix
	 */
	private DoubleMatrix FindCovarianceMatrix(ArrayList<Color> data, double uR,
			double uG, double uB) {

		DoubleMatrix cov = new DoubleMatrix(3, 3);

		double n = data.size();

		double sRR = 0;
		double sGG = 0;
		double sBB = 0;
		double sRG = 0;
		double sRB = 0;
		double sGB = 0;

		for (Color i : data) {
			double devR = i.getRed() - uR;
			double devG = i.getGreen() - uG;
			double devB = i.getBlue() - uB;
			sRR += devR * devR;
			sGG += devG * devG;
			sBB += devB * devB;
			sRG += devR * devG;
			sRB += devR * devB;
			sGB += devG * devB;
		}

		sRR /= (n - 1);
		sGG /= (n - 1);
		sBB /= (n - 1);
		sRG /= (n - 1);
		sRB /= (n - 1);
		sGB /= (n - 1);

		cov.set(0, 0, sRR);
		cov.set(0, 1, sRG);
		cov.set(0, 2, sRB);
		cov.set(1, 0, sRG);
		cov.set(1, 1, sGG);
		cov.set(1, 2, sGB);
		cov.set(2, 0, sRB);
		cov.set(2, 1, sGB);
		cov.set(2, 2, sBB);

		return (cov);
	}
}