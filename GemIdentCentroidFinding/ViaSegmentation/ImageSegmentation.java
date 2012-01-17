package GemIdentCentroidFinding.ViaSegmentation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import no.uib.cipr.matrix.DenseVector;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.Subgraph;

import GemIdentCentroidFinding.ViaErosions.LabelViaSmartErosions;
import GemIdentTools.ImageGraphUtils;
import GemIdentTools.ImageUtils;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentTools.Matrices.ShortMatrix;



public class ImageSegmentation{
	
	private static int displayLevel = 0;

	private static int numTries = 5;
//	private static final double LOW_THRESHOLD_PCT = .02;
//	private static final double MAX_THRESHOLD_PCT = .95;

	public static ArrayList<ArrayList<Point>> getBlobsFromImage(BufferedImage blobImage){
		ArrayList<ArrayList<Point>> bloblist = null;
		try {
			bloblist = LabelViaSmartErosions.FloodfillLabelPoints(new BoolMatrix(blobImage), " ", " ",null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return bloblist;
	}	

	public static LinkedList<ArrayList<Point>> breakBlob(ArrayList<Point> blob,boolean checkQuality){
		SimpleGraph<Point,DefaultEdge> G = ImageGraphUtils.imageDistanceGraph(blob);
				
		assert(GraphUtils.components(G).size() == 1);
		
		GraphPartitioner part = null;
		GraphUtils.AdjacencyMatrix<Point> L = GraphUtils.graphLaplacianMatrix(G);

		int numComponents = 0;
		ArrayList<Set<Point>> compList = null;
		LinkedList<ArrayList<Point>> blobLinkedList = null;
		
		int maxiter = 10;
		int numiter = 0;

		while(numComponents != 2 && numiter < maxiter){
			numiter++;
			Set<Point> partition = null;
			for (int i = 0;i<numTries;i++){
				GraphPartitioner p2 = new IsoperimetricPartition();
				Set<Point> tp = p2.partition(L);
				if (part == null || p2.quality < part.quality){
					part = p2;
					partition = tp;
				}
			}
			blobLinkedList = new LinkedList<ArrayList<Point>>();
			Set<Point> p2 = new HashSet<Point>();
			Set<Point> p3 = new HashSet<Point>();
			Iterator<Point> it3 = blob.iterator();
			while (it3.hasNext()) {
				Point tmp = it3.next();
				if (partition != null && partition.contains(tmp))
					p2.add(tmp);
				else
					p3.add(tmp);
			}

			// count number of connected components.  if more than two, partition again.
			Subgraph<Point, DefaultEdge, SimpleGraph<Point, DefaultEdge>> G2 = new Subgraph<Point, DefaultEdge, SimpleGraph<Point, DefaultEdge>>(
					G, p2);
			assert (G2.vertexSet().size() == p2.size());
			compList = GraphUtils.components(G2);
			Subgraph<Point, DefaultEdge, SimpleGraph<Point, DefaultEdge>> G3 = new Subgraph<Point, DefaultEdge, SimpleGraph<Point, DefaultEdge>>(
					G, p3);
			compList.addAll(GraphUtils.components(G3));
			assert (G3.vertexSet().size() == p3.size());
			numComponents = compList.size();			
		}

		//DenseVector partition = part.bestPar;
		double partitionVolume = 0;
		for (int i = 0;i< part.bestPar.size();i++)
			partitionVolume = partitionVolume + part.bestPar.getData()[i];	

		DenseVector y = new DenseVector(part.bestPar.size());
		
		boolean thisQual = part.passesQualityThreshold();

				
		
		if (!checkQuality || thisQual){
			for(int j = 0; j < compList.size();j++){
				ArrayList<Point> ptarray = new ArrayList<Point>(compList.get(j));
				blobLinkedList.add(ptarray);				
			}
		}
		else
			blobLinkedList.add(blob);
		

		//display this blob in new window
		if(displayLevel > 2 && (thisQual || !checkQuality) || displayLevel > 3){
			double thisVolume = Math.min(partitionVolume, part.bestPar.size()-(partitionVolume));
			double thisCut = L.A.mult(part.bestPar,y).dot(part.bestPar);
			System.err.println("This cut: "+ thisCut);
			System.err.println("This volume: "+ thisVolume);
			System.err.println("This quality: "+ part.quality);
			System.err.println("Passes: "+ thisQual);
			System.err.println("Total num nodes: "+ L.vertices.size());

			if (displayLevel > 3){
			Iterator<Set<Point>> it = compList.iterator();
			LinkedList<ArrayList<Point>> ll = new LinkedList<ArrayList<Point>>();
			while(it.hasNext()){
				ll.add(new ArrayList<Point>(it.next()));
			}
			displayBlobPartitions(ll);
			}
		}
		
		return blobLinkedList;
	}
	
		

	public static BoolMatrix findBlobCentroids(BufferedImage blobImage,int highValue,int midValue,int lowValue){
		ArrayList<Point> centroids = findBlobCentroids(ImageGraphUtils.imageDistanceGraph(blobImage),highValue,midValue,lowValue);
		
		
		BoolMatrix centroidMatrix = new BoolMatrix(blobImage.getWidth(),blobImage.getHeight());

		Iterator<Point> it = centroids.iterator();
		while(it.hasNext()){
			Point pt = it.next();
			centroidMatrix.set(pt.x,pt.y,true);
			centroidMatrix.set(pt.x+1,pt.y,true);
			centroidMatrix.set(pt.x-1,pt.y,true);
			centroidMatrix.set(pt.x,pt.y+1,true);
			centroidMatrix.set(pt.x,pt.y-1,true);
		}
		return centroidMatrix;
	}
	
	private static ArrayList<Point> findBlobCentroids(
			SimpleWeightedGraph<Point, DefaultWeightedComparableEdge> G,
			int highValue,int midValue,int lowValue) {
	    JFrame frame = new JFrame("Display Blobs");
	    frame.pack();
	    frame.setVisible(false);

	    if (displayLevel > 0)
	    	System.err.println("Finding components...");
		ArrayList<Set<Point>> cl = GraphUtils.components(G);
		Iterator<Set<Point>> it = cl.iterator();
		LinkedList<ArrayList<Point>> blobs = new LinkedList<ArrayList<Point>>();
		while(it.hasNext()){
			blobs.add(new ArrayList<Point>(it.next()));
		}

		if (displayLevel > 0)
			System.err.println("Splitting blobs...");
		ArrayList<Point> tempCentroids = findBlobCentroids(blobs,highValue,midValue,lowValue);
		if (displayLevel > 0)
			System.err.println("\n...done finding centroids.");
		return tempCentroids;
	}



	public static ArrayList<Point> findBlobCentroids(LinkedList<ArrayList<Point>> blobLinkedList,int highValue,int midValue,int lowValue){
		ArrayList<Point> centroids = new ArrayList<Point>();
				
		int count = 0;
		int displayFreq = 100;
		while(!blobLinkedList.isEmpty()){
			count++;
			if (displayLevel > 0 && (count)%displayFreq == 0)
				System.err.print(".");
			ArrayList<Point> currentBlob = blobLinkedList.poll();

			if(currentBlob.size() <= lowValue)
				continue;
			
			if(currentBlob.size() <= midValue){
				Point ct = getBlobCenter(currentBlob);
				centroids.add(ct);
				continue;
			}

			LinkedList<ArrayList<Point>> brokenBlob = null;
			if(currentBlob.size() <= highValue){
				brokenBlob = breakBlob(currentBlob,true);
			}
			else			
				brokenBlob = breakBlob(currentBlob,false);

			if (brokenBlob.size() == 1){
				Point ct = getBlobCenter(brokenBlob.get(0));
				centroids.add(ct);
			}
			else
				blobLinkedList.addAll(brokenBlob);
		}
		return centroids; 
	}

//	public static ArrayList<Point> findBlobCentroids(LinkedList<ArrayList<Point>> blobLinkedList,int numCenters){
//		
//		
//		ArrayList<Point> centroids = new ArrayList<Point>();
//		
//		int low_threshold = 20;
//
//		while(!blobLinkedList.isEmpty() && centroids.size() < numCenters-1){
//			ArrayList<Point> currentBlob = blobLinkedList.pop();
//
//			//displayBlobPartitions(currentBlob,frame);
//
//			if(currentBlob.size() <= low_threshold)
//				continue;
//			
//			if(currentBlob.size() <= low_threshold*2){
//				Point ct = getBlobCenter(currentBlob);
//				centroids.add(ct);
//				continue;
//			}
//
//
//			/*
//			//ANG -- test
//			SimpleWeightedGraph<Point, DefaultWeightedComparableEdge> G = ImageGraphUtils.imageDistanceGraph(blobPartitionsToImage(currentBlob));
//			ArrayList<Set<Point>> cl = GraphUtils.components(G);
//			LinkedList<ArrayList<Point>> ll = new LinkedList<ArrayList<Point>>();
//			Iterator<Set<Point>> it = cl.iterator();
//			while(it.hasNext()){
//				ll.add(new ArrayList<Point>(it.next()));
//			}
//			displayBlobPartitions(ll,frame);
//			*/
//			
//			LinkedList<ArrayList<Point>> brokenBlob = breakBlob(currentBlob,false);
//			if(brokenBlob != null){
//				//displayBlobPartitions(brokenBlob,frame);
//				blobLinkedList.addAll(brokenBlob);
//				continue;
//			}
//			
//			
//			Point ct = getBlobCenter(currentBlob);
//			centroids.add(ct);
//		}
//
//		ArrayList<Point> remBlob = new ArrayList<Point>();
//		if (!blobLinkedList.isEmpty()){
//			Iterator<ArrayList<Point>> it = blobLinkedList.iterator();
//			while (it.hasNext()){
//				remBlob.addAll(it.next());
//			}
//			centroids.add(getBlobCenter(remBlob));
//		}		
//
//		return centroids; 
//	}
	
	@SuppressWarnings("unused")
	private static void displayBlobPartitions(ArrayList<Point> thisBlob){
		LinkedList<ArrayList<Point>> ll = new LinkedList<ArrayList<Point>>();
		ll.add(thisBlob);
		displayBlobPartitions(ll);
	}

	private static void displayBlobPartitions(LinkedList<ArrayList<Point>> thisBlob){
	    JFrame frame = new JFrame("Display Blobs");
	    frame.pack();
	    frame.setVisible(false);

		BufferedImage tempImage = blobPartitionsToImage(thisBlob);
		int width = tempImage.getWidth();
		int height = tempImage.getHeight();
		int scale = 10;
		
		BufferedImage tempImage2 = new BufferedImage(width*scale,height*scale,BufferedImage.TYPE_INT_RGB);
		Graphics g = tempImage2.getGraphics();
		g.drawImage(tempImage.getScaledInstance(width*scale,height*scale,Image.SCALE_FAST), 0, 0, null);
		g.dispose();
		tempImage = tempImage2;

		
		ImageIcon ic = new ImageIcon(tempImage);
		JOptionPane.showMessageDialog(frame, ic);
	}	
	
	@SuppressWarnings("unused")
	private static BufferedImage blobPartitionsToImage(ArrayList<Point> thisBlob){
		LinkedList<ArrayList<Point>> ll = new LinkedList<ArrayList<Point>>();
		ll.add(thisBlob);
		return blobPartitionsToImage(ll);
	}
	private static BufferedImage blobPartitionsToImage(LinkedList<ArrayList<Point>> thisBlob){
		
		assert(thisBlob.size()!= 0);
		
		int minX = Integer.MAX_VALUE;
		int maxX = 0;
		int minY = Integer.MAX_VALUE;
		int maxY = 0;

		Iterator<ArrayList<Point>> it = thisBlob.iterator();
		while (it.hasNext()){
			ArrayList<Point> zz = it.next();
			int [] tempBoundingBox = blobBoundingBox(zz);
			if (tempBoundingBox[0] < minX) minX = tempBoundingBox[0];
			if (tempBoundingBox[1] > maxX) maxX = tempBoundingBox[1];
			if (tempBoundingBox[2] < minY) minY = tempBoundingBox[2];
			if (tempBoundingBox[3] > maxY) maxY = tempBoundingBox[3];
		}

		int width = maxX - minX + 1;
		int height = maxY - minY + 1;

		ShortMatrix sm = new ShortMatrix(width,height,(short) 0);
		it = thisBlob.iterator();
		short color = 1;
		while (it.hasNext()){
			ArrayList<Point> zz = it.next();
			writeBlobToShortMatrix(zz,sm,color,minX,minY);
			color++;
		}
		
		BufferedImage tempImage = partitionToImage(sm);
		
		return tempImage;
	}
	
	
	private static void writeBlobToShortMatrix(ArrayList<Point> thisBlob, ShortMatrix sm, short color, int xoffset, int yoffset){
		for (int i = 0;i < thisBlob.size();i++){
			Point pt = thisBlob.get(i);
			sm.set(pt.x-xoffset,pt.y-yoffset,color);
		}
	}

	public static int[] blobBoundingBox(ArrayList<Point> thisBlob){
		int maxX = Integer.MIN_VALUE;
		int minX = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;
		
		/** get bounding box */
		for (int i = 0;i < thisBlob.size();i++){
			Point pt = thisBlob.get(i);
			if (pt.x < minX) minX = pt.x;
			if (pt.x > maxX) maxX = pt.x;
			if (pt.y < minY) minY = pt.y;
			if (pt.y > maxY) maxY = pt.y;
		}
		
		//int width = maxX - minX + 1;
		//int height = maxY - minY + 1;
		//ShortMatrix sm = new ShortMatrix(width,height,(short) 0);
		
		int[] retval = new int[4];
		retval[0] = minX;
		retval[1] = maxX;
		retval[2] = minY;
		retval[3] = maxY;
		
		/*
		System.out.print(minX + " ");
		System.out.print(maxX + " ");
		System.out.print(minY + " ");
		System.out.print(maxY + "\n");
		*/
		
		return retval;
	}

	private static Point getBlobCenter(ArrayList<Point> currentBlob) {
		if (currentBlob.size() == 0)
			return new Point(-1,-1);
		Iterator<Point> blobit = currentBlob.iterator();
		
		double x = 0;
		double y = 0;
		int n = 0;
		while(blobit.hasNext()){
			n++;
			Point pt = blobit.next();
			x += pt.x;
			y += pt.y;
		}
		return new Point((int)(x/n),(int)(y/n));
	}

	
	
	public static ShortMatrix recursivePartition(BufferedImage image, int numPartitions, GraphPartitioner partitioner){
		
		if (partitioner == null)
			partitioner = new SpectralPartition();

		if (numPartitions < 2)
			numPartitions = 2;
		
		
		SimpleWeightedGraph<Point,DefaultWeightedComparableEdge> G = ImageGraphUtils.imageDistanceGraph(image);

		Set<Point> partition = partitioner.partition(G);

		
		ShortMatrix partitionMatrix = new ShortMatrix(image.getWidth(),image.getHeight(),(short)0);

		Iterator<Point> it = partition.iterator();
		while (it.hasNext()){
			Point pt = it.next();
			partitionMatrix.set(pt.x,pt.y,1);
		}
		
		return partitionMatrix;
	}
	
	public static ShortMatrix isoperimetricPartition(BufferedImage image, int numPartitions){
		return recursivePartition(image,numPartitions, new IsoperimetricPartition());
	}

	public static ShortMatrix spectralPartition(BufferedImage image,int numPartitions){
		return recursivePartition(image,numPartitions, new SpectralPartition());
	}
	
	public static BufferedImage partitionToImage(ShortMatrix partitionMatrix){
		
		BufferedImage image = new BufferedImage(partitionMatrix.getWidth(), partitionMatrix.getHeight(),BufferedImage.TYPE_INT_RGB);

		int numColors = 0;
		for (int i = 0;i < partitionMatrix.getWidth();i++)
			for (int j = 0;j< partitionMatrix.getHeight();j++)
				if (partitionMatrix.get(i,j) > numColors)
					numColors = partitionMatrix.get(i,j);

		
		Vector<Color> colors = new Vector<Color>(numColors+1);
		//colors.setSize(numColors+1);
		
		colors.add(Color.BLACK);
		colors.add(Color.WHITE);
		colors.add(Color.RED);
		colors.add(Color.BLUE);
		colors.add(Color.GREEN);
		colors.add(Color.YELLOW);
		colors.add(Color.ORANGE);
		colors.add(Color.PINK);
		
		for (int i = 8;i < numColors+1;i++)
		{
			colors.add(ImageUtils.randomColor());
		}
		
		for (int i = 0;i < partitionMatrix.getWidth();i++)
			for (int j = 0;j< partitionMatrix.getHeight();j++)
				image.setRGB(i,j,colors.get(partitionMatrix.get(i,j)).getRGB());
				
		return image;
	}

	public static BufferedImage partitionToImage(BoolMatrix partitionMatrix){
		
		BufferedImage image = new BufferedImage(partitionMatrix.getWidth(), partitionMatrix.getHeight(),BufferedImage.TYPE_INT_RGB);

		int numColors = 1;
		
		Vector<Color> colors = new Vector<Color>();
		colors.setSize(numColors+1);
		
		colors.set(0,Color.BLUE);
		colors.set(1,Color.RED);
		
		for (int i = 0;i < partitionMatrix.getWidth();i++)
			for (int j = 0;j< partitionMatrix.getHeight();j++)
				if (partitionMatrix.get(i,j))
					image.setRGB(i,j,colors.get(1).getRGB());
				else	
					image.setRGB(i,j,colors.get(0).getRGB());
				
		return image;
	}
}