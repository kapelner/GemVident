package GemIdentImageSets;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;

/**
 * Basically a wrapper around the super class, adding no new functionality of its
 * own. This class is intended to wrap images that don't have any strings attached.
 * 
 * @author Adam Kapelner
 */
public class RegularSubImage extends DataImage {
	
	public RegularSubImage(){} //makes daughter class happy

	public RegularSubImage(String filename) throws FileNotFoundException{
		super(filename, false);
	}
	
	public RegularSubImage(String filename, boolean crop) throws FileNotFoundException{
		super(filename, crop);
	}
	
	public RegularSubImage(String filename, BufferedImage clone, boolean crop) {
		super(filename, clone, crop);
	}
	
	public RegularSubImage(BufferedImage image) {
		super(image);
	}

	public Color getColorAt(Point t) {
		return new Color(displayimage.getRGB(t.x,t.y));
	}

	public DataImage clone(){
		BufferedImage clone=new BufferedImage(
				getWidth(),
				getHeight(),
				BufferedImage.TYPE_INT_RGB
		);
		for (int i=0;i<getWidth();i++)
			for (int j=0;j<getHeight();j++)
				clone.setRGB(i,j,displayimage.getRGB(i,j));
		return new RegularSubImage(filename,clone,false);
	}
	
	public int getHeight() {
		return displayimage.getHeight();
	}

	public int getWidth() {
		return displayimage.getWidth();
	}
}