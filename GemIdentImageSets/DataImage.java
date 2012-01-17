
package GemIdentImageSets;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;

import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentTools.Thumbnails;
import GemIdentTools.Geometry.Solids;
import GemIdentView.KImageTrainPanel;


/**
 * For most imagesets this object is a convenience image class which
 * gives easy access to the pixel RGB values of an image. 
 * It is a "hasA" of a BufferedImage class
 * 
 * For Nuance image sets there's a whole infrastructure here
 * 
 * @author Adam Kapelner
 */
public abstract class DataImage implements Cloneable{

	/** the BufferedImage behind the scenes */
	protected BufferedImage displayimage;
	/** the filename of the image */
	protected String filename;
	/** the {@link KImageTrainPanel imagePanel} this image is being displayed on */ 
	protected KImageTrainPanel imagePanel;

	/** Clones the DataImage by creating a new BufferedImage and copying the pixel values */
	public abstract DataImage clone();
	
	/**
	 * Constructs a DataImage from a BufferedImage. If crop,
	 * then it will carve off the image's sized based on the
	 * type of image set
	 *  
	 * @param filename		the filename of the image
	 * @param image			the BufferedImage
	 * @param crop			whether or not to crop the sides
	 */
	public DataImage(String filename,BufferedImage image,boolean crop){
		this.displayimage=image;
		this.filename=filename;
		
		if (crop) 
			CropImage();
	}
	/** another default constructor */
	public DataImage(BufferedImage image){
		this.displayimage=image;
	}
	
	/** default constructor - needed by daughter class */
	protected DataImage(){}
	
	/** constructs a DataImage from a filename
	 * 
	 * @param filename	the filename
	 * @param crop		whether or not to crop the sides
	 */
	public DataImage(String filename,boolean crop) throws FileNotFoundException{
		this.filename=filename;
//		System.out.println("public DataImage() "+filename+" "+crop);
		try {
			displayimage = IOTools.OpenImage(filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			System.err.println("can't open "+filename);
			throw new FileNotFoundException();
		}

		if (crop) 
			CropImage();
	}
	
	/** Crop image based on the imageset trim values */
	protected void CropImage(){
		displayimage=Crop(displayimage,
				Run.it.imageset.getXo(),
				Run.it.imageset.getXf(),
				Run.it.imageset.getYo(),
				Run.it.imageset.getYf()
			);		
	}
	/**
	 * constructs a DataImage from a filename and artificially magnifies it
	 * 
	 * @param filename		the filename
	 * @param crop			whether or not to crop the image
	 * @param magn			the magnification level
	 * @throws FileNotFoundException 
	 */
	public DataImage(String filename,boolean crop,float magn) throws FileNotFoundException {
		this(filename,crop);
//		System.out.println("before: w:"+image.getWidth()+" h:"+image.getHeight());
		displayimage=Thumbnails.ScaleImage(displayimage, magn, magn);
//		System.out.println("after: w:"+image.getWidth()+" h:"+image.getHeight());
	}
	/**
	 * Cut off the sides of the DataImage
	 * 
	 * @param Xo		cut off Xo pixels from the left
	 * @param Xf		cut off Xf pixels from the right
	 * @param Yo		cut off Yo pixels from the top
	 * @param Yf		cut off Yf pixels from the bottom
	 * @return			the cropped image
	 */
	public static BufferedImage Crop(BufferedImage I, int Xo, int Xf, int Yo, int Yf){
		if (Xo == 0 && Xf == 0 && Yo == 0 && Yf == 0) 
			return I;
		
		BufferedImage temp = new BufferedImage((Xf - Xo + 1), (Yf - Yo + 1), BufferedImage.TYPE_INT_RGB);		
		
		for (int i=Xo;i<Xf;i++)
			for (int j=Yo;j<Yf;j++)
				temp.setRGB(i-Xo,j-Yo,I.getRGB(i,j));
		
		return temp;
	}
	

	public static BufferedImage Crop(BufferedImage I, int[] crop) {
		return Crop(I, crop[0], crop[1], crop[2], crop[3]);
	}
	
	/** sets a specific pixel to a specific color
	 * 
	 * @param i		The x coordinate of desired pixel to be set
	 * @param j		The y coordinate of desired pixel to be set
	 * @param r		The red intensity value of set pixel
	 * @param g		The green intensity value of set pixel
	 * @param b		The blue intensity value of set pixel
	 */
	public void setPixel(int i,int j, int r,int g,int b){
		Color color=new Color(r,g,b);
		try {displayimage.setRGB(i,j,color.getRGB());} catch (Exception e){}	
	}
	/** gets the 24bit RGB integer representation of a pixel at a specified location
	 * 
	 * @param i		The x coordinate of desired pixel
	 * @param j		The y coordinate of desired pixel
	 * @return		the integer pixel at that location
	 */
	public int getRGB(int i,int j){
		return displayimage.getRGB(i,j);
	}
	/** gets the red component of a pixel at a specified location
	 * 
	 * @param i		The x coordinate of desired pixel
	 * @param j		The y coordinate of desired pixel
	 * @return		the 8bit Red intensity value
	 */
	public int getR(int i,int j){
//		return (new Color(image.getRGB(i,j))).getRed();
		return (displayimage.getRGB(i,j) >> 16) & 0xff;
	}
	/** gets the green component of a pixel at a specified location
	 * 
	 * @param i		The x coordinate of desired pixel
	 * @param j		The y coordinate of desired pixel
	 * @return		the 8bit Green intensity value
	 */
	public int getG(int i,int j){
		return (displayimage.getRGB(i,j) >>  8) & 0xff;
	}
	/** gets the blue component of a pixel at a specified location
	 * 
	 * @param i		The x coordinate of desired pixel
	 * @param j		The y coordinate of desired pixel
	 * @return		the 8bit Blue intensity value
	 */
	public int getB(int i,int j){
		return (displayimage.getRGB(i,j)      ) & 0xff;
	}
	
	/**
	 * Get the "intensity" - the average of the red, green, blue intensities
	 * 
	 * @param i		the i pixel coordinate
	 * @param j		the j pixel coordinate
	 * @return		the intensity as a double because it's an average
	 */	
	public double getIntensity(int i, int j) {
		return ((double)getR(i,j) + (double)getG(i,j) + (double)getB(i,j))/3;
	}	
	
	/** gets the height dimension of the image
	 * 
	 * @return		the height
	 */
	public abstract int getHeight();
	/** gets the width dimension of the image
	 * 
	 * @return		the width
	 */
	public abstract int getWidth();

	/** marks the point as a positively classified phenotype (makes a colored dot)
	 * 
	 * @param i			The x coordinate to mark in this image
	 * @param j			The y coordinate to mark in this image
	 */
	public void MarkPheno(int i,int j,String phenotype){
		setPixel(i,j,Run.it.getPhenotypeDisplayColor(phenotype));
	}
	/** sets a specific pixel to a specific color
	 * 
	 * @param i			The x coordinate of desired pixel to be set
	 * @param j			The y coordinate of desired pixel to be set
	 * @param color		the color to set the pixel to
	 */
	public void setPixel(int i, int j, Color color) {
		displayimage.setRGB(i,j,color.getRGB());	
	}

	/**
	 * Marks a centroid on this image
	 * 
	 * @param i					the ith pixel coordinate
	 * @param j					the jth pixel coordinate
	 * @param displayColor		the display color of the centroid
	 */
	public void MarkPhenoCentroid(int i,int j,Color displayColor){
		for (Point t:Solids.GetPointsInSolidUsingCenter(3,new Point(i,j)))
			try {setPixel(t.x,t.y,Color.BLACK);} catch (Exception e){}
		for (Point t:Solids.GetPointsInSolidUsingCenter(2,new Point(i,j)))
			try {setPixel(t.x,t.y,displayColor);} catch (Exception e){}
		try {setPixel(i,j,Color.BLACK);} catch (Exception e){}
	}

	/**
	 * Get color at a specific pixel
	 * 
	 * @param t		the location of the pixel
	 * @return		the color at that pixel
	 */
	public abstract Color getColorAt(Point t);
	
	/** get the internal BufferedImage */
	public BufferedImage getAsBufferedImage() {
		return displayimage;
	}
	public String getFilename() {
		return filename;
	}
	/** get total number of pixels (width*height) */
	public int numPixels() {
		return getWidth()*getHeight();
	}
	public void setPanel(KImageTrainPanel imagePanel) {
		this.imagePanel = imagePanel;		
	}

	/**
	 * Get a small snippet from the overall image
	 * 
	 * @param p						the center of the snippet image in the coordinates of the overall image
	 * @param half_width			the width of the snippet image divided by 2
	 * @param half_height			the height of the snippet image divided by 2
	 * @param phenotype_color		the color of the phenotype under consideration (set as null to ignore) 
	 * @return						a small snippet of the overall image 
	 */	
	public BufferedImage getSnippet(Point p, int half_width, int half_height, Color phenotype_color){
		BufferedImage image = IOTools.InitializeImage(half_width * 2 + 1, half_height * 2 + 1, null, null);
		for (int i = p.x - half_width; i <= p.x + half_width; i++){
			for (int j = p.y - half_height; j <= p.y + half_height; j++){
				if (i == p.x && j == p.y && phenotype_color != null){
					image.setRGB(i - (p.x - half_width), j - (p.y - half_height), phenotype_color.getRGB());					
				}
				else {
					try {
						image.setRGB(i - (p.x - half_width), j - (p.y - half_height), this.getAsBufferedImage().getRGB(i, j));
					} catch (ArrayIndexOutOfBoundsException e){} //yeah some will be over the edge, who cares
				}
			}
		}
		return image;
	}

}