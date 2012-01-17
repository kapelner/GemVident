package GemIdentTools;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Hashtable;

import javax.swing.ImageIcon;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.RegularSubImage;


/* Utils.java is used by FileChooserDemo2.java. */
public class ImageUtils {
    public final static String jpeg = "jpeg";
    public final static String jpg = "jpg";
    public final static String gif = "gif";
    public final static String tiff = "tiff";
    public final static String tif = "tif";
    public final static String png = "png";

    /*
     * Get the extension of a file.
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = ImageUtils.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
    
	public static BufferedImage convertRenderedImage(RenderedImage img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage)img;	
		}	
		ColorModel cm = img.getColorModel();
		int width = img.getWidth();
		int height = img.getHeight();
		WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		Hashtable<String, Object> properties = new Hashtable<String,Object>();
		String[] keys = img.getPropertyNames();
		if (keys!=null) {
			for (int i = 0; i < keys.length; i++) {
				properties.put(keys[i], img.getProperty(keys[i]));
			}
		}
		BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
		img.copyData(raster);
		return result;
	}

    
    public static RegularSubImage mergeImages(DataImage img1, DataImage img2){
    	RegularSubImage fimg = new RegularSubImage(new BufferedImage(img1.getWidth(),img1.getHeight(),BufferedImage.TYPE_INT_RGB));
    	for (int i=0;i<img1.getWidth();i++)
    		for (int j=0;j<img1.getHeight();j++){
    			if (img1.getRGB(i,j) == Color.BLACK.getRGB()){
    				fimg.setPixel(i,j,new Color(img2.getRGB(i,j)));
    			}
    			else if (img2.getRGB(i,j) == Color.BLACK.getRGB()){
    				fimg.setPixel(i,j,new Color(img1.getRGB(i,j)));	
    			}
    			else{
    				int r = (img1.getR(i,j) + img2.getR(i,j))/2;
    				int g = (img1.getG(i,j) + img2.getG(i,j))/2;
    				int b = (img1.getB(i,j) + img2.getB(i,j))/2;
    				fimg.setPixel(i,j,r,g,b);
    			}
    		}    		      	
    	return fimg;
    }
    
	public static Color randomColor(){		
		double theta = Math.PI / 2 * Math.random();
		double phi = Math.PI / 2 * Math.random();

		double r = Math.sin(theta) * Math.cos(phi);
		double g = Math.sin(theta) * Math.sin(phi);
		double b = Math.cos(phi);

		double mult = 255 / Math.max(r, Math.max(g, b));
		int red = (int) (r * mult);
		int green = (int) (g * mult);
		int blue = (int) (b * mult);
		return new Color(red,green,blue);
	}

    /*
	public static BufferedImage convertRenderedImage(RenderedImage img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage)img;	
		}	
		ColorModel cm = B//img.getColorModel();
		int width = img.getWidth();
		int height = img.getHeight();
		WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		java.util.Hashtable<String, Object> properties = new java.util.Hashtable<String,Object>();
		String[] keys = img.getPropertyNames();
		if (keys!=null) {
			for (int i = 0; i < keys.length; i++) {
				properties.put(keys[i], img.getProperty(keys[i]));
			}
		}
		BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
		img.copyData(raster);
		return result;
	}
	*/

}