package GemIdentTools.Matrices;

import java.awt.Dimension;
import java.awt.image.WritableRaster;
import java.io.Serializable;

/**
 * A base class for matrices in various data formats
 * 
 * @author Adam Kapelner
 */
public abstract class SimpleMatrix implements Serializable {
	private static final long serialVersionUID = -2693043114346006482L;
	
	/** the width of the matrix */
	protected int width;
	/** the height of the matrix */
	protected int height;
	
	/** need Serializable to be happy */
	public SimpleMatrix(){}

	/** construct a matrix from dimensions */
	public SimpleMatrix(int width, int height){
		this.width = width;
		this.height = height;
	}
	
	/** construct a matrix from dimensions in a Dimension object */
	public SimpleMatrix(Dimension d){
		this(d.width, d.height);
	}
	
	/**
	 * Find the total number of pixels in the matrix
	 * 
	 * @return		the area
	 */
	public long Area() {
		return (long)width * (long)height;
	}	
	
	/**
	 * Fill a raster (an image's internals) with data.
	 * If the value is zero, set it to black; one, set it to white
	 * 
	 * @param raster		the raster to fill
	 */	
	public void FillRaster(WritableRaster raster){
		for (int j=0;j<height;j++)
			for (int i=0;i<width;i++)
				setPixelInRaster(i, j, raster);		
	}
	
	/**
	 * Fill the raster one pixel at a time
	 * @param i				the i coordinate in the raster
	 * @param j				the j coordinate in the raster
	 * @param raster		the raster itself
	 */
	protected abstract void setPixelInRaster(int i, int j, WritableRaster raster);
	
	public int getHeight(){
		return height;
	}
	
	public int getWidth(){
		return width;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
}