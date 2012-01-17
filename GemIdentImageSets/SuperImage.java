
package GemIdentImageSets;

import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * A "SuperImage" is only relevant when each subimage has a global context.
 * It is a spruced up DataImage, including the original data image 
 * in the center with a number of pixels ("c") from the surrounding subimages.
 * 
 * c is determined by 150% of the maximum radius of all phenotypes of interest.
 * 
 * If an image does not exist in a certain direction from this subimage, the center 
 * image is mirror-reflected (either through the line for N,S,E,W or through
 * the corner for NW,NE,SW,SE) to provide an estimate as to what it may look like.
 * 
 * If only Java had multiple-inheritance, there would not have to be any code duplication
 * 
 * @author Adam Kapelner
 */
public interface SuperImage {
	
	/** Gets the true seed (center) image
	 *
	 * @return			The center image
	*/
	public DataImage getCenterImage();
	
	/** It is necessary to translate a coordinate in the
	 * image of interest to the correct coorindate in
	 * the superimage. Basically just the point is translated up
	 * and to the right by c.
	 *
	 * @param t			The point to be translated
	 * @return			The translated point
	*/
	public Point AdjustPointForSuper(Point t);
	
	/** Java is dump due to lack of multiple inheritance - this is a method 
	 * in DataImage as well
	 */
	public BufferedImage getAsBufferedImage();
	
//	/**
//	 * ###### This method cannot be abstract so look in daughter classes #####
//	 * 
//	 * Initializes a SuperImage by loading the file, figuring
//	 * out the number of pixels, c, to surround the image by, 
//	 * looking up its surrounding images, and copying the pixels
//	 * into the N,S,E,W,NW,NE,SW,SE locations (reflecting the
//	 * original if non-existent).
//	 * 
//	 * @param filename 		the image to be loaded
//	 */
//	public SuperImage(String filename){
//		super();
//		//load file
//		C=ImageAndScoresBank.getOrAddImage(filename);
//		this.filename=filename;
//		w=C.getWidth();
//		h=C.getHeight();
//		
//		// get global image context
//		localPics=Run.it.imageset.GetLocalPics(filename);
//
//		//get number of pixels to surround by
//		c=Run.it.getMaxPhenotypeRadiusPlusMore()+2;
//		
//		//initialize the SuperImage
//		displayimage=new BufferedImage(w+2*c,h+2*c,BufferedImage.TYPE_INT_RGB);
		
		//let the daughter classes take over from here

//		IOTools.WriteImage("O___"+filename,"JPEG",C);
//		IOTools.WriteImage("superO___"+filename,"JPEG",image);
		

		
//		System.out.println("C.W:"+C.getWidth()+" C.H:"+C.getHeight()+" cut:"+cut+" xcut:"+xcut+" ycut:"+ycut+" totrows:"+totrows+" totcols:"+totcols);
		
//		this.image=new BufferedImage(C.getWidth()+2*cut,C.getHeight()+2*cut,BufferedImage.TYPE_INT_RGB);
//		for (int i=0;i<C.getWidth()+2*cut;i++)
//			for (int j=0;j<C.getHeight()+2*cut;j++)
//				this.image.setRGB(i,j,image.getRGB(xcut+i,ycut+j));
//		image=image.getSubimage(xcut,ycut,C.getWidth()+2*cut,C.getHeight()+2*cut);
		
//		IOTools.WriteImage("super___"+filename,"JPEG",this);
		
//		int time=(int)(System.currentTimeMillis()-time_o)/1000;
//		System.out.println("finish creating superImage for picture #"+n+" in "+time+"sec");
//	}

//	private void SetPieceOfImage(int xo,int xf,int yo,int yf,DataImage I,BufferedImage image){
////		System.out.println("image rows:"+image.getWidth()+" cols:"+image.getHeight());
////		System.out.println("I rows:"+I.rows()+" cols:"+I.cols());
////		System.out.println("xo:"+xo+" xf:"+xf+" yo:"+yo+" yf:"+yf);
//		for (int i=xo;i<xf;i++)
//			for (int j=yo;j<yf;j++){
////				System.out.println("get i-xo:"+(i-xo)+" j-yo:"+(j-yo));
//				int rgb=I.getRGB(i-xo,j-yo);
////				System.out.println("set i:"+i+" j:"+j);
//				image.setRGB(i,j,rgb);	
//			}
//	}
}


//private static DataImage CreateSE(DataImage C){
//	int W=C.getWidth();
//	int H=C.getHeight();
//	BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
//	for (int i=0;i<W;i++)
//		for (int j=0;j<H;j++)
//			image.setRGB(i,-(j-H+1),C.getRGB(-(i-W+1),j));
//	return new DataImage(image);
//}
//private static DataImage CreateS(DataImage C){
//	int W=C.getWidth();
//	int H=C.getHeight();
//	BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
//	for (int i=0;i<W;i++)
//		for (int j=0;j<H;j++)
//			image.setRGB(i,-(j-H+1),C.getRGB(i,j));
//	return new DataImage(image);
//}
//private static DataImage CreateSW(DataImage C){
//	int W=C.getWidth();
//	int H=C.getHeight();
//	BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
//	for (int i=0;i<W;i++)
//		for (int j=0;j<H;j++)
//			image.setRGB(-(i-W+1),-(j-H+1),C.getRGB(i,j));
//	return new DataImage(image);
//}
//private static DataImage CreateE(DataImage C){
//	int W=C.getWidth();
//	int H=C.getHeight();
//	BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
//	for (int i=0;i<W;i++)
//		for (int j=0;j<H;j++)
//			image.setRGB(i,j,C.getRGB(-(i-W+1),j));
//	return new DataImage(image);
//}
//private static DataImage CreateW(DataImage C){
//	int W=C.getWidth();
//	int H=C.getHeight();
//	BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
//	for (int i=0;i<W;i++)
//		for (int j=0;j<H;j++)
//			image.setRGB(-(i-W+1),j,C.getRGB(i,j));
//	return new DataImage(image);
//}
//private static DataImage CreateNE(DataImage C){
//	int W=C.getWidth();
//	int H=C.getHeight();
//	BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
//	for (int i=0;i<W;i++)
//		for (int j=0;j<H;j++)
//			image.setRGB(i,j,C.getRGB(-(i-W+1),-(j-H+1)));
//	return new DataImage(image);
//}
//private static DataImage CreateN(DataImage C){
//	int W=C.getWidth();
//	int H=C.getHeight();
//	BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
//	for (int i=0;i<W;i++)
//		for (int j=0;j<H;j++)
//			image.setRGB(i,j,C.getRGB(i,-(j-H+1)));
//	return new DataImage(image);
//}
//private static DataImage CreateNW(DataImage C){
//	int W=C.getWidth();
//	int H=C.getHeight();
//	BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
//	for (int i=0;i<W;i++)
//		for (int j=0;j<H;j++)
//			image.setRGB(-(i-W+1),j,C.getRGB(i,-(j-H+1)));
//	return new DataImage(image);
//}