package GemIdentCentroidFinding.ViaSegmentation;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.jgrapht.graph.SimpleWeightedGraph;

import GemIdentImageSets.RegularSubImage;
import GemIdentTools.ImageUtils;
import GemIdentTools.ValueSortedMap;
import GemIdentTools.Matrices.ShortMatrix;

import GemIdentCentroidFinding.ViaSegmentation.DefaultWeightedComparableEdge;

public class RegionGrowing {

	private ShortMatrix regionData;
	private Vector<Color> regionColors; 
	private Map<Integer,RegionInfo> regionInfos;
	private LinkedList<BoundaryPoint> boundaryPoints;
	private Set<XY<Integer>> seeds;
	private RegularSubImage regionImage;	

	private int numberSeeds, height, width;
	private RegularSubImage image;
	private LinkedList<BoundaryPoint> rejectedPoints;
	private Map<Set<RegionInfo>,RegionInfo> groupInfos;
	private double threshold;

	private ValueSortedMap<DefaultWeightedComparableEdge, Double> globalEdgeMap;
	public boolean mergeInitialized;
	private SimpleWeightedGraph<Set<RegionInfo>, DefaultWeightedComparableEdge> regionGraph;
	private Map<Integer, Integer> colorMap;	

	public static class XY<T> {
		public T x, y;

		public boolean equals(XY<T> o){
			return x == o.x && y == o.y;
		}

		XY(T _x, T _y) {
			this.x = _x;
			this.y = _y;
		}
	}

	public static class RGB<T> {
		public T r, g, b;
		RGB(T _r, T _g, T _b){
			r = _r;
			g = _g;
			b = _b;		
		}
		@SuppressWarnings("unchecked")
		public RGB(Color col) {
			Integer _r = col.getRed();
			Integer _g = col.getGreen();
			Integer _b = col.getBlue();
			r = (T)_r;
			g = (T)_g;
			b = (T)_b;
			// TODO Auto-generated constructor stub
		}
	}

	public static class Pixel {
		public XY<Integer> xy;
		public RGB<Integer> rgb;
	}


	public static class BoundaryPoint {
		XY<Integer> xy;
		double distFromRegion;
		int closestRegion;

		BoundaryPoint(XY<Integer> _xy) {
			xy = new XY<Integer>(_xy.x, _xy.y);
			distFromRegion = 0;
			closestRegion = 0;
		}

		BoundaryPoint(int _x, int _y) {
			xy = new XY<Integer>(_x,_y);
			distFromRegion = 0;
			closestRegion = 0;
		}

		BoundaryPoint() {
			xy = new XY<Integer>(0,0);
			distFromRegion = 0;
			closestRegion = 0;
		}
	}

	@SuppressWarnings("unchecked")
	private class RegionInfo implements Comparable{
		public XY<Double> xycenter;
		public RGB<Double> rgbcenter;
		public int numPoints;
		public int regionNumber;
		RegionInfo(){
			xycenter = new XY<Double>(0.,0.);
			rgbcenter = new RGB<Double>(0.,0.,0.);
			numPoints = 0;
			regionNumber = -1;
		}
		public int compareTo(Object o) {
			if (!(o instanceof RegionInfo))
				throw new ClassCastException("A RegionInfo object expected.");
			RegionInfo ri = (RegionInfo) o;
			if (numPoints > ri.numPoints)
				return 1;
			if (numPoints < ri.numPoints)
				return -1;
			if (regionNumber > ri.regionNumber)
				return 1;
			if (regionNumber < ri.regionNumber)
				return -1;
			return 0;
		}
	}

	public RegionGrowing(RegularSubImage _image, int _numberSeeds){
		initialize(_image,_numberSeeds);
	}	

	private static Pixel getPixelFromImage(RegularSubImage img, XY<Integer> xy) {
		Pixel pixel = new Pixel();
		pixel.xy = xy;
		int r = img.getR(xy.x, xy.y);
		int g = img.getG(xy.x, xy.y);
		int b = img.getB(xy.x, xy.y);
		RGB<Integer> rgb = new RGB<Integer>(r,g,b);
		pixel.rgb = rgb;

		return pixel;
	}

	private double getPixelIntensity(int x, int y) {
		int r = image.getR(x, y);
		int g = image.getG(x, y);
		int b = image.getB(x, y);
		double intensity = (r + g + b)/3;

		return intensity;
	}



	public ShortMatrix getRegionShortMatrix(){

		return regionData;
	}
	public RegularSubImage getRegionImage(){
		return getRegionImage(true);
	}
	public RegularSubImage getRegionImage(boolean showBoundary){
		if (mergeInitialized)
			recomputeColors();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int reg = getPixelGroup(i,j);
				assert(reg != -1);
				int r = Color.BLACK.getRed();
				int g = Color.BLACK.getGreen();
				int b = Color.BLACK.getBlue();
				if (!isBoundaryPixel(i,j) || !showBoundary){
					r = regionColors.get(reg).getRed();
					g = regionColors.get(reg).getGreen();
					b = regionColors.get(reg).getBlue();
				}
				else if (((i +j) % 2) == 0 ){
					r = Color.WHITE.getRed();
					g = Color.WHITE.getGreen();
					b = Color.WHITE.getBlue();
				}

				if (reg == 0){
					r = image.getR(i,j);
					g = image.getR(i,j);
					b = image.getR(i,j);
				}

				// TODO Auto-generated method stub

				regionImage.setPixel(i,j,r,g,b);
			}
		}

		return regionImage;
	}

	private int getPixelGroup(int i, int j) {
		int reg = regionData.get(i, j);
		if (colorMap != null)
			reg = colorMap.get(reg);
		return reg;
	}

	private boolean isBoundaryPixel(int i, int j) {
		Set<Integer> tempBN = getBoundaryNeighbors(i,j);
		tempBN.remove(getPixelGroup(i,j));
		if (tempBN.size() > 0 ){
			return true;
		}
		else
			return false;
	}

	private void initialize(RegularSubImage _image,int _numberSeeds){

		/*
		 * Initializations/declarations
		 */
		image = _image;
		numberSeeds = _numberSeeds;
		threshold = 1;

		width = image.getWidth();
		height = image.getHeight();

		regionData = new ShortMatrix(width, height, (short) 0);
		regionColors = new Vector<Color>();
		regionInfos = new HashMap<Integer,RegionInfo>();
		boundaryPoints = new LinkedList<BoundaryPoint>();
		rejectedPoints = new LinkedList<BoundaryPoint>();
		groupInfos = new HashMap<Set<RegionInfo>,RegionInfo>();
		seeds = new HashSet<XY<Integer>>();
		regionImage = new RegularSubImage(RegularSubImage.FillImage(width,height,(Color.BLACK).getRGB()));
		globalEdgeMap = new ValueSortedMap<DefaultWeightedComparableEdge, Double>();
		mergeInitialized = false;
		colorMap = null;		

		/*
		 * Apply seed selection -- Have user click on seed points or select them
		 * UAR.
		 */

		/* Random seed selection */

		chooseSeedsAtRandom();
		chooseColorsAtRandom();

		/*
		 * Intialize regions by adding seed colors. Initialize boundaries by
		 * looking at seed areas.
		 */

		initializeSeeds();

		//System.err.println("Max intensity: " + computeMaxIntensity());
	}

	public void grow(){
		if (!boundaryPoints.isEmpty()){
			BoundaryPoint bp = boundaryPoints.pop();
			if( regionData.get(bp.xy.x,bp.xy.y) != 0) return;
			bp = computeBoundaryDistances(bp);
			double aa = bp.distFromRegion;
			aa = aa+1;
			if(bp.distFromRegion < threshold){
				addRegionPoint(bp);
				addBoundaryNeighbors(bp.xy.x,bp.xy.y);
			}
			else{
				rejectedPoints.push(bp);
			}
		}
		else if (!rejectedPoints.isEmpty()){
			boundaryPoints = rejectedPoints;
			rejectedPoints = new LinkedList<BoundaryPoint>();
			threshold = threshold*1.2;
			System.out.print("Region Growing:  New threshold: " + threshold +"\t");
			System.out.print("No. bps: " + boundaryPoints.size() +"\n");
		}
	}

	private void addRegionPoint(BoundaryPoint bp) {
		assert (regionData.get(bp.xy.x, bp.xy.y) == 0);

		int reg = bp.closestRegion;

		regionData.set(bp.xy.x, bp.xy.y, reg);

		updateRegionInfo(reg, bp.xy);
	}



	private void updateRegionInfo(int reg, XY<Integer> xy) {
		RegionInfo tempCenter = regionInfos.get(reg);

		tempCenter.numPoints = tempCenter.numPoints + 1;
		int np = tempCenter.numPoints;
		double tx = tempCenter.xycenter.x;
		double ty = tempCenter.xycenter.y;
		double tr = tempCenter.rgbcenter.r;
		double tg = tempCenter.rgbcenter.g;
		double tb = tempCenter.rgbcenter.b;

		RGB<Integer> rgb = getPixelFromImage(image, xy).rgb;

		double inv_np = 1 / (double) np;

		tempCenter.xycenter.x = (xy.x + tx * (np - 1)) * inv_np;
		tempCenter.xycenter.x = (xy.y + ty * (np - 1)) * inv_np;
		tempCenter.rgbcenter.r = (rgb.r + tr * (np - 1)) * inv_np;
		tempCenter.rgbcenter.g = (rgb.g + tg * (np - 1)) * inv_np;
		tempCenter.rgbcenter.b = (rgb.b + tb * (np - 1)) * inv_np;
		tempCenter.regionNumber = reg;

		regionInfos.put(reg, tempCenter);
	}

	//ANG -- this grow function now defunct
	// public void grow(){
	// /* Cycle through boundary points and compute distances to regions.
	// * Find the point with the smallest distance. */
	// BoundaryPoint bestPoint = computeBoundaryDistances();
	//		
	// /* Color this point the color of its closest region. */
	// regions.set(bestPoint.xy.x,bestPoint.xy.y,bestPoint.closestRegion);
	//		
	// int r = regionColors.get(bestPoint.closestRegion-1).r;
	// int g = regionColors.get(bestPoint.closestRegion-1).g;
	// int b = regionColors.get(bestPoint.closestRegion-1).b;
	// regionImage.setPixel(bestPoint.xy.x,bestPoint.xy.y,r,g,b);
	//		
	// /* Add new boundary points. */
	// addBoundaryNeighbors(bestPoint.xy.x,bestPoint.xy.y);
	//		
	// /* Remove this point from the boundary. */
	// boundaryPoints.remove(bestPoint);
	// //System.err.println(".");
	// }
	//

	/*
	 * private BoundaryPoint computeBoundaryDistances(BoundaryPoint bp) {
	 * Iterator<BoundaryPoint> it2 = boundaryPoints.iterator(); double
	 * bestOverallDist = 99999999; BoundaryPoint bestOverallPoint = new
	 * BoundaryPoint(); while (it2.hasNext()){ BoundaryPoint bp = it2.next();
	 * XY<Integer> tempXY = bp.xy; int x = tempXY.x; int y = tempXY.y;
	 * 
	 * Set<Integer> tempBoundaryRegions = new HashSet<Integer>(); if (x>0)
	 * tempBoundaryRegions.add((int) regions.get(x - 1, y)); if (x < width-1)
	 * tempBoundaryRegions.add((int) regions.get(x + 1, y)); if (y > 0)
	 * tempBoundaryRegions.add((int) regions.get(x, y - 1)); if (y <
	 * height-1)tempBoundaryRegions.add((int) regions.get(x, y + 1));
	 * 
	 * tempBoundaryRegions.remove(0);
	 * 
	 * Iterator<Integer> trb_it = tempBoundaryRegions.iterator(); double
	 * bestDist = 9999999; int bestRegion = 0;
	 * 
	 * while (trb_it.hasNext()) { int tr = trb_it.next(); double thisDist =
	 * computeDistanceToRegion(getPixelFromImage( image, tempXY),
	 * regionCenters.get(tr-1)); if (thisDist < bestDist) { bestDist = thisDist;
	 * bestRegion = tr; } } bp.distFromRegion = bestDist; bp.closestRegion =
	 * bestRegion; if(bestDist < bestOverallDist){ bestOverallDist = bestDist;
	 * bestOverallPoint = bp; } } return bestOverallPoint; }
	 */

	/* Implements variant of region growing algorithm of [Shih and Cheng 2005].
	 * Their algorithm is basically as follows:
	 * 	1) Generate initial seed points
	 * 	2) 
	 * A main difference between this implementation and theirs is that we don't
	 * attempt to find the minimum weight boundary point at each step.  The reason
	 * that this step is slow is that each time we add a pixel to a region, we'd 
	 * have to recompute the distances for each boundary point adjacent to the same
	 * region.  To compensate for this, we instead use a gradually raised threshold,
	 * recompute boundary points at each threshold value, and combine 
	 * is slow).  Instead we  */
	public void segment() {
		while (hasBoundaryPoints()) {
			grow();
		}
	}

	private BoundaryPoint computeBoundaryDistances(BoundaryPoint bp) {
		XY<Integer> tempXY = bp.xy;
		int x = tempXY.x;
		int y = tempXY.y;

		Set<Integer> tempBoundaryRegions = getBoundaryNeighbors(x,y);

		assert (tempBoundaryRegions.size() > 0);

		Iterator<Integer> br_it = tempBoundaryRegions.iterator();
		double bestDist = 9999999;
		int bestRegion = -1;

		while (br_it.hasNext()) {
			int regionIndex = br_it.next();
			double thisDist = computeDistanceToRegion(getPixelFromImage(image,
					tempXY), regionInfos.get(regionIndex));
			if (thisDist < bestDist) {
				bestDist = thisDist;
				bestRegion = regionIndex;
			}
		}
		bp.distFromRegion = bestDist;
		bp.closestRegion = bestRegion;
		return bp;
	}

	private Set<Integer> getBoundaryNeighbors(int x, int y) {
		// TODO Auto-generated method stub
		Set<Integer> tempBoundaryRegions = new HashSet<Integer>();
		if (x > 0)
			tempBoundaryRegions.add(getPixelGroup(x - 1, y));
		if (x < width - 1)
			tempBoundaryRegions.add(getPixelGroup(x + 1, y));
		if (y > 0)
			tempBoundaryRegions.add(getPixelGroup(x, y - 1));
		if (y < height - 1)
			tempBoundaryRegions.add(getPixelGroup(x, y + 1));

		tempBoundaryRegions.remove(0);
		return tempBoundaryRegions;
	}



	private void initializeSeeds() {
		Iterator<XY<Integer>> it = seeds.iterator();

		int seedNumber = 1;
		while (it.hasNext()) {
			XY<Integer> tempPoint = it.next();
			int x = tempPoint.x;
			int y = tempPoint.y;

			RegionInfo tempCenter = new RegionInfo();
			tempCenter.xycenter.x = 0.;
			tempCenter.xycenter.y = 0.;

			tempCenter.rgbcenter.r = 0.;
			tempCenter.rgbcenter.g = 0.;
			tempCenter.rgbcenter.b = 0.;

			regionInfos.put(seedNumber,tempCenter);

			BoundaryPoint bp = new BoundaryPoint();
			bp.xy = tempPoint;
			bp.distFromRegion = 0;
			bp.closestRegion = seedNumber;

			addRegionPoint(bp);

			addBoundaryNeighbors(x, y);
			seedNumber++;
		}

	}

	private void addBoundaryNeighbors(int x, int y) {
		// TODO Auto-generated method stub
		if (x > 0) {
			if (regionData.get(x - 1, y) == 0)
				boundaryPoints.add(new BoundaryPoint(x - 1, y));
		}
		if (x < width - 1)
			if (regionData.get(x + 1, y) == 0)
				boundaryPoints.add(new BoundaryPoint(x + 1, y));
		if (y > 0)
			if (regionData.get(x, y - 1) == 0)
				boundaryPoints.add(new BoundaryPoint(x, y - 1));
		if (y < height - 1)
			if (regionData.get(x, y + 1) == 0)
				boundaryPoints.add(new BoundaryPoint(x, y + 1));
	}

	private void chooseSeedsAtRandom() {
		for (int i = 0; i < numberSeeds; i++) {
			// pick seed at random
			int x = (int) (Math.random() * width);
			int y = (int) (Math.random() * height);
			XY<Integer> thisSeed = new XY<Integer>(x,y);
			while(seeds.contains(thisSeed)){
				//Ensures no duplicate points
				x = (int) (Math.random() * width);
				y = (int) (Math.random() * height);
				thisSeed = new XY<Integer>(x,y);
			}
			assert (thisSeed.x < width);
			assert (thisSeed.y < height);
			seeds.add(thisSeed);			
			assert (seeds.contains(thisSeed));
		}
	}


	private void chooseColorsAtRandom() {
		regionColors.add(Color.BLACK);
		for (int i = 0; i < numberSeeds; i++) {
			// pick color at random
			regionColors.add(ImageUtils.randomColor());
		}
	}


	public <T> double computeRGBDistance(RGB<T> rgb1, RGB<T> rgb2){

		double dist2 = 0;
		dist2 += Math.pow((Double)rgb1.r - (Double)rgb2.r, 2);
		dist2 += Math.pow((Double)rgb1.g - (Double)rgb2.g, 2);
		dist2 += Math.pow((Double)rgb1.b - (Double)rgb2.b, 2);
		return Math.sqrt(dist2);		
	}



	private double computeDistanceToRegion(Pixel pixel, RegionInfo tr) {
		RGB<Integer> rgb = pixel.rgb;

		double dist2 = 0;
		dist2 += Math.pow(rgb.r - tr.rgbcenter.r, 2);
		dist2 += Math.pow(rgb.g - tr.rgbcenter.g, 2);
		dist2 += Math.pow(rgb.b - tr.rgbcenter.b, 2);
		return Math.sqrt(dist2);
	}

	@SuppressWarnings("unused")
	private double computeMaxIntensity() {
		double maxIntensity = 0;
		for(int i = 0;i< width;i++){
			for(int j = 0;j< height;j++){
				maxIntensity = Math.max(maxIntensity,getPixelIntensity(i,j));
			}
		}
		return maxIntensity;
	}

	public boolean hasBoundaryPoints() {
		return !boundaryPoints.isEmpty() | !rejectedPoints.isEmpty();
	}



	public void initMergeRegions() {

		if (mergeInitialized)
			return;
		//SortedSet<RegionInfo> mList  = new TreeSet<RegionInfo>(regionInfos);

		regionGraph = new SimpleWeightedGraph<Set<RegionInfo>, DefaultWeightedComparableEdge>(DefaultWeightedComparableEdge.class);

		Map<Integer,Set<RegionInfo>> tempRegionMap = new HashMap<Integer,Set<RegionInfo>>();
		/* Add regions to graph as vertices. */
		Iterator<RegionInfo> it = regionInfos.values().iterator();
		while (it.hasNext()){
			Set<RegionInfo> tempSet = new HashSet<RegionInfo>();
			RegionInfo tempInfo = it.next();
			tempSet.add(tempInfo);
			tempRegionMap.put(tempInfo.regionNumber,tempSet);

			regionGraph.addVertex(tempSet);
		}

		/* Find region boundaries and add edges */
		for(int i = 0;i < width;i++){
			for(int j=0;j<height;j++){
				Set<Integer> neighbors = getBoundaryNeighbors(i,j);
				int thisRegion = getRegion(i,j);
				neighbors.remove(thisRegion);
				Iterator<Integer> it2 = neighbors.iterator();
				while(it2.hasNext()){
					int neighborRegion = it2.next();
					//System.err.println("r1: "+thisRegion+" r2:"+neighborRegion);
					regionGraph.addEdge(tempRegionMap.get(thisRegion),tempRegionMap.get(neighborRegion));
				}
			}
		}

		/* Compute edge weights */
		Iterator<DefaultWeightedComparableEdge> edge_it = regionGraph.edgeSet().iterator();
		while (edge_it.hasNext()){
			DefaultWeightedComparableEdge tempEdge = edge_it.next();
			double td = computeRegionGroupDistances(regionGraph.getEdgeSource(tempEdge), regionGraph.getEdgeTarget(tempEdge));
			regionGraph.setEdgeWeight(tempEdge,td);
			globalEdgeMap.put(tempEdge,td);
		}
		mergeInitialized = true;
	}

	private double computeRegionGroupDistances(Set<RegionInfo> r1,
			Set<RegionInfo> r2) {

		final double maxNumPixels = width*height;

		RegionInfo tempCenters1 = computeRegionGroupInfo(r1);
		RegionInfo tempCenters2 = computeRegionGroupInfo(r2);		

		double distance = computeRGBDistance(tempCenters1.rgbcenter,tempCenters2.rgbcenter);		

		//add correction for size of smallest of the two region
		double rs = (double) Math.min(tempCenters1.numPoints,tempCenters2.numPoints)/maxNumPixels;

		return distance*rs;
	}

	private RegionInfo combineRegionGroupInfos(Set<RegionInfo> g1, Set<RegionInfo> g2){				
		RegionInfo r1 = groupInfos.get(g1);
		RegionInfo r2 = groupInfos.get(g2);		

		RegionInfo tempInfo = combineRegionInfos(r1,r2);
		assert(r1 != null);
		assert(r2 != null);

		return tempInfo;
	}

	private RegionInfo combineRegionInfos(RegionInfo r1, RegionInfo r2){
		RegionInfo tempRI = new RegionInfo();

		assert(r1 != null);
		assert(r2 != null);

		int numPoints1 = r1.numPoints;
		int numPoints2 = r2.numPoints;

		tempRI.numPoints = numPoints1 + numPoints2;

		double invnp = 1/(double)tempRI.numPoints;

		tempRI.xycenter.x = (r1.xycenter.x * numPoints1 + r2.xycenter.x *numPoints2)*invnp;
		tempRI.xycenter.y = (r1.xycenter.y * numPoints1 + r2.xycenter.y * numPoints2)*invnp;
		tempRI.rgbcenter.r = (r1.rgbcenter.r * numPoints1+ r2.rgbcenter.r * numPoints2)*invnp;
		tempRI.rgbcenter.g = (r1.rgbcenter.g * numPoints1 + r2.rgbcenter.g * numPoints2)*invnp;
		tempRI.rgbcenter.b = (r1.rgbcenter.b * numPoints1+ r2.rgbcenter.b * numPoints2)*invnp;

		return tempRI;
	}

	private RegionInfo computeRegionGroupInfo(Set<RegionInfo> r1) {
		if (groupInfos.containsKey(r1))
			return groupInfos.get(r1);

		RegionInfo tempInfo = new RegionInfo();
		Iterator<RegionInfo> it = r1.iterator();;
		while (it.hasNext()){
			RegionInfo nextInfo = it.next();
			tempInfo = combineRegionInfos(tempInfo,nextInfo);
			//tempInfo
		}
		groupInfos.put(r1,tempInfo);

		return tempInfo;	
	}

	private int getRegion(int i, int j) {
		return regionData.get(i,j);
	}

	private void recomputeColors(){
		colorMap = new HashMap<Integer,Integer>();

		Iterator<Set<RegionInfo>> it = groupInfos.keySet().iterator();
		while (it.hasNext()){
			Set<RegionInfo> tempGroup = it.next();
			//System.out.print("cm: "+ count+ " <- ");

			int biggestSubregion = getBiggestGroupRegionNumber(tempGroup);

			Iterator<RegionInfo> it2 = tempGroup.iterator();

			while (it2.hasNext()){
				int regcol = it2.next().regionNumber;
				//System.out.print(regcol + " ");
				colorMap.put(regcol,biggestSubregion);
			}
			//System.out.println();
		}
	}

	private int getBiggestGroupRegionNumber(Set<RegionInfo> tempGroup) {
		Iterator<RegionInfo> it2 = tempGroup.iterator();

		int biggestGroupRegionNumber = -1;
		int maxRegionSize = 0;

		while (it2.hasNext()){
			RegionInfo tempRegion = it2.next();
			if (tempRegion.numPoints > maxRegionSize){
				biggestGroupRegionNumber = tempRegion.regionNumber;
				maxRegionSize = tempRegion.numPoints;
			}				
		}
		return biggestGroupRegionNumber;
	}

	public void merge() {		
		if (!mergeInitialized)
			return;
		assert (groupInfos.size() > 1);
		assert (globalEdgeMap.size() > 0);
		assert (groupInfos.size() == regionGraph.vertexSet().size());
		assert (globalEdgeMap.size() == regionGraph.edgeSet().size());
		/* Find edge with smallest weight */
		DefaultWeightedComparableEdge edge = globalEdgeMap.firstKey();
		globalEdgeMap.remove(edge);
		Set<RegionInfo> r1;
		Set<RegionInfo> r2;
		r1 = regionGraph.getEdgeSource(edge);
		r2 = regionGraph.getEdgeTarget(edge);
		assert(r1 != null && r2 != null);

		assert(groupInfos.containsKey(r1));
		assert(groupInfos.containsKey(r2));

		/* Contract this edge */
		RegionInfo tempInfo = combineRegionGroupInfos(r1,r2);

		Set<RegionInfo> nr = new HashSet<RegionInfo>();
		nr.addAll(r1);
		nr.addAll(r2);

		groupInfos.put(nr,tempInfo);	
		regionGraph.addVertex(nr);

		groupInfos.remove(r1);
		groupInfos.remove(r2);

		regionGraph.removeEdge(edge);

		/* Remove vertices and rewire obsolete edges */
		Set<DefaultWeightedComparableEdge> edgeSet = new HashSet<DefaultWeightedComparableEdge>();
		edgeSet.addAll(regionGraph.edgesOf(r1));
		edgeSet.addAll(regionGraph.edgesOf(r2));
		Iterator<DefaultWeightedComparableEdge> it = edgeSet.iterator();
		while(it.hasNext()){
			DefaultWeightedComparableEdge tempEdge = it.next();
			Set<RegionInfo> ri = regionGraph.getEdgeSource(tempEdge);
			if (ri.equals(r1) || ri.equals(r2))
				ri = regionGraph.getEdgeTarget(tempEdge);			

			regionGraph.addEdge(nr, ri);
			regionGraph.removeEdge(tempEdge);

			globalEdgeMap.remove(tempEdge);			
		}

		regionGraph.removeVertex(r1);
		regionGraph.removeVertex(r2);


		/* Compute weights for new edges */
		edgeSet = regionGraph.edgesOf(nr);
		it = edgeSet.iterator();
		while(it.hasNext()){
			DefaultWeightedComparableEdge tempEdge = it.next();
			double td = computeRegionGroupDistances(regionGraph.getEdgeSource(tempEdge), regionGraph.getEdgeTarget(tempEdge));
			regionGraph.setEdgeWeight(tempEdge,td);
			globalEdgeMap.put(tempEdge,td);
		}
	}

	public int getNumRegions() {
		// TODO Auto-generated method stub
		return groupInfos.size();
	}
}