
package GemIdentTools;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import GemIdentCentroidFinding.ViaSegmentation.DefaultWeightedComparableEdge;
import GemIdentImageSets.RegularSubImage;
import GemIdentTools.Matrices.ShortMatrix;



public class ImageGraphUtils{

	private static final double threshold = 0;
	private static final double sigma2 = 10;

	public static double computeColorRGBDistance(Color c1, Color c2){
		
		double dist2 = 0;
		dist2 += Math.pow((double)c1.getRed() - (double)c2.getRed(), 2);
		dist2 += Math.pow((double)c1.getBlue() - (double)c2.getBlue(), 2);
		dist2 += Math.pow((double)c1.getGreen() - (double)c2.getGreen(), 2);
		return Math.sqrt(dist2);		
	}

	public static double computeColorHSBDistance(Color c1, Color c2){
		float [] hsb1 = Color.RGBtoHSB(c1.getRed(),c1.getBlue(),c1.getGreen(),null);
		float [] hsb2 = Color.RGBtoHSB(c2.getRed(),c2.getBlue(),c2.getGreen(),null);
		
		double dist2 = 0;
		dist2 += Math.pow(hsb1[0] - hsb2[0], 2);
		dist2 += Math.pow(hsb1[1] - hsb2[1], 2);
		dist2 += Math.pow(hsb1[2] - hsb2[2], 2);
		return Math.sqrt(dist2);		
	}

	
    public static SimpleWeightedGraph<Point,DefaultWeightedComparableEdge> imageDistanceGraph(BufferedImage _image){   
    	RegularSubImage image = new RegularSubImage(_image);
		int width = image.getWidth();
		int height = image.getHeight();
		SimpleWeightedGraph<Point, DefaultWeightedComparableEdge> G;
		G = new SimpleWeightedGraph<Point,DefaultWeightedComparableEdge>(DefaultWeightedComparableEdge.class);

		//add vertices to graph
		for (int i = 0;i < width;i++)
		{
			for (int j = 0;j < height;j++){
				if (image.getIntensity(i,j) > threshold){
					Point p = new Point(i,j);
					G.addVertex(p);						
				}
			}
		}

		//add edges to graph
		Iterator<Point> vSetIterator = G.vertexSet().iterator();
		while (vSetIterator.hasNext()){
			Point thisPt = vSetIterator.next();
			int x = thisPt.x;
			int y = thisPt.y;
			if (G.containsVertex(new Point(x+1,y))){
				G.addEdge(thisPt, new Point(x+1,y));
			}
			if (G.containsVertex(new Point(x,y+1))){
				G.addEdge(thisPt, new Point(x,y+1));
			}
			if (G.containsVertex(new Point(x+1,y+1))){
				G.addEdge(thisPt, new Point(x+1,y+1));
			}
			if (G.containsVertex(new Point(x-1,y+1))){
				G.addEdge(thisPt, new Point(x-1,y+1));
			}
			/*
			if (G.containsVertex(new Point(x-1,y))){
				G.addEdge(thisPt, new Point(x-1,y));
			}
			if (G.containsVertex(new Point(x-1,y-1))){
				G.addEdge(thisPt, new Point(x-1,y-1));
			}
			if (G.containsVertex(new Point(x,y-1))){
				G.addEdge(thisPt, new Point(x,y-1));
			}
			if (G.containsVertex(new Point(x+1,y-1))){
				G.addEdge(thisPt, new Point(x+1,y-1));
			}
			*/
		}
		
		//compute edge weights
		Iterator<DefaultWeightedComparableEdge> edgeIt = G.edgeSet().iterator();
		while (edgeIt.hasNext()){
			DefaultWeightedComparableEdge thisEdge = edgeIt.next();
			Point p1 = G.getEdgeSource(thisEdge);
			Point p2 = G.getEdgeTarget(thisEdge);
			Color c1 = new Color(image.getRGB(p1.x,p1.y));
			Color c2 = new Color(image.getRGB(p2.x,p2.y));
			G.setEdgeWeight(thisEdge,Math.exp(-computeColorRGBDistance(c1,c2)/sigma2));
			//System.out.println(G.getEdgeWeight(thisEdge));
		}
		
		//System.out.println("Number of nodes: " + G.vertexSet().size());
		//System.out.println("Number of edges: " + G.edgeSet().size());
		return G;
	}
    public static SimpleGraph<Point,DefaultEdge> imageDistanceGraph(ArrayList<Point> points){   
    	

		SimpleGraph<Point, DefaultEdge> G;
		G = new SimpleGraph<Point,DefaultEdge>(DefaultEdge.class);


		Iterator<Point> it = points.iterator();
		while(it.hasNext()){
			G.addVertex(it.next());						
		}

		//add edges to graph
		Iterator<Point> vSetIterator = G.vertexSet().iterator();
		while (vSetIterator.hasNext()){
			Point thisPt = vSetIterator.next();
			int x = thisPt.x;
			int y = thisPt.y;
			if (G.containsVertex(new Point(x+1,y))){
				G.addEdge(thisPt, new Point(x+1,y));
			}
			if (G.containsVertex(new Point(x,y+1))){
				G.addEdge(thisPt, new Point(x,y+1));
			}
			if (G.containsVertex(new Point(x+1,y+1))){
				G.addEdge(thisPt, new Point(x+1,y+1));
			}
			if (G.containsVertex(new Point(x-1,y+1))){
				G.addEdge(thisPt, new Point(x-1,y+1));
			}
			/*
			if (G.containsVertex(new Point(x-1,y))){
				G.addEdge(thisPt, new Point(x-1,y));
			}
			if (G.containsVertex(new Point(x-1,y-1))){
				G.addEdge(thisPt, new Point(x-1,y-1));
			}
			if (G.containsVertex(new Point(x,y-1))){
				G.addEdge(thisPt, new Point(x,y-1));
			}
			if (G.containsVertex(new Point(x+1,y-1))){
				G.addEdge(thisPt, new Point(x+1,y-1));
			}
			*/
		}
		
		//System.out.println("Number of nodes: " + G.vertexSet().size());
		//System.out.println("Number of edges: " + G.edgeSet().size());
		return G;
	}
	public static RegularSubImage shortMatrixToRegularSubImage(ShortMatrix image){
		
		RegularSubImage newimg = new RegularSubImage(IOTools.InitializeImage(image.getWidth(), image.getHeight(), null, null));
		for(int i = 0;i<image.getWidth();i++){
			for(int j = 0;j<image.getHeight();j++){
				if (image.get(i,j) == 1)
					newimg.setPixel(i,j,Color.BLUE);
				else if (image.get(i,j) == 2)
					newimg.setPixel(i,j,Color.RED);
			}
		}
		
		return newimg;
	}

}