
package GemIdentImageSets;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JScrollPane;

import GemIdentTools.IOTools;

/**
 * A convenience wrapper for {@link JScrollPane JScrollPane}
 * that includes the capability to take a "screenshot" of whatever is currently visible
 * in the panel, and save it to BufferedImage which can then be saved to the hard disk
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class JScrollPaneWithScreenshot extends JScrollPane {
	
	/**
	 * Save the image currently visible to the hard disk as a JPEG
	 * 
	 * @param filename	the filename to save the image to
	 */	
	public void saveScreenshot(String filename){
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		paint(g);
//		System.out.println("filename: " + filename);
		IOTools.WriteImage(filename + ".jpg", "JPEG", image);
	}
}