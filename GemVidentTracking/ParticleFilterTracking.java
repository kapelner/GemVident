
package GemVidentTracking;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import GemIdentCentroidFinding.PostProcess;
import GemIdentCentroidFinding.ViaSegmentation.GraphUtils;
import GemIdentClassificationEngine.Classify;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentModel.Phenotype;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentTools.ImageUtils;
import GemIdentTools.Geometry.Solids;
import GemIdentView.JProgressBarAndLabel;

/**
 *	 Perform tracking of multiple objects using particle filtering.
 *
 */
public abstract class ParticleFilterTracking implements Serializable{

	private static final long serialVersionUID = 1L;

	public static class ObsVertex extends GraphUtils.Vertex{};
	public static class PathVertex extends GraphUtils.Vertex{};

	protected int numParticles=0;
	protected static final PathVertex falsePositive = new PathVertex();
	protected static final ObsVertex falseNegative = new ObsVertex();
	private static final Color tentativeColor = Color.darkGray;
	private static final Color strongColor = Color.green;
	public static final String trackingDir = "tracking";
	private static final boolean drawIndividualColors = true;
	private static final int MIN_CLICK_DISTANCE = 10;

	/** Tracking variables **/
	protected ArrayList<Particle> particles;
	protected ArrayList<PathCloud> clouds;
	protected HashMap<PathCloud,Color> pathColors;
	protected ArrayList<String> trackList;


	/** All the tuning parameters **/
	public double logProbThreshold = -100;
	public double falsePositiveLogProb = -100;
	public double falseNegativeLogProb = -100;
	public double sigma2ant = 20;
	public double sigma2obs = 5;
	public double tentativeThreshold = -45;
	public double deadThreshold = -90;	
	public double initialLogProb = tentativeThreshold -30;
	public int shortPathLengthThreshold = 20;
	public boolean dontShowShortPaths = false;
	public double newPathThreshold = .99;
	public double alivePercentDisplayThreshold = 0;
	public double tentativePercentDisplayThreshold = .5;
	public int numImagesTracked=0;
	public String phenotypeName = null;
	public boolean memReducedPathWriter = false;
	public int memReducedPathWritingFreq = 3;
	private HashMap<PathCloud,Integer> indexMap = null;
	private String outputDir = null;
	private int pathCloudOutputIdx;
	private HashMap<Integer, Integer> frameNumbers;


	public ParticleFilterTracking(){
		particles = null;
		clouds = null;
		pathColors = null;
	}


	public void InitializeTracking(ArrayList<Point> pts,int t){

		particles = new ArrayList<Particle >(); 
		clouds = new ArrayList<PathCloud>();
		pathColors = new HashMap<PathCloud,Color>();
		indexMap = null;
		for(int j=0;j<numParticles;j++){
			Particle trackedAnts = new Particle();
			particles.add(trackedAnts);
		}
		createNewPathClouds(pts,t);		
	}


	public void drawPathCloudCenters(BufferedImage I, int t){
		int r = 3;
		synchronized(clouds){
			for (PathCloud cld: clouds){
				Color center=Color.white;
				Color around=strongColor;
				if ((cld.getNumAlive(t)/(double) numParticles) < alivePercentDisplayThreshold)
					continue;

				if(cld.getNumAlive(t) == 0)
					continue;

				Point pos = cld.getCenter(t);

				if (cld.getNumTentative(t)/(double) numParticles > tentativePercentDisplayThreshold){
					around=tentativeColor;
				}

				int i = pos.x;
				int j = pos.y;
				if (r >0){
					for (Point ptt:Solids.GetPointsInSolidUsingCenter(r,new Point(i,j))){
						try {I.setRGB(ptt.x,ptt.y,center.getRGB());} catch (Exception e){}}
					for (Point ptt:Solids.GetPointsInSolidUsingCenter(r-1,new Point(i,j))){
						try {I.setRGB(ptt.x,ptt.y,around.getRGB());} catch (Exception e){}
						try {I.setRGB(i,j,center.getRGB());} catch (Exception e){}}
				}	
				else{
					for (Point ptt:Solids.GetPointsInSolidUsingCenter(r,new Point(i,j))){
						try {I.setRGB(ptt.x,ptt.y,around.getRGB());} catch (Exception e){}}
				}
			}		
		}
	}

	/**
	 * For each particle, moves simulation forward one step based on next observations.
	 * 	
	 * For each particle: <UL>
	 * 		<LI> Sample next step given current step.
	 * 		<LI> Update importance weights.
	 * 		<LI> Re-sample if necessary.
	 * 		</UL> 
	 * @param nextPts
	 * @param t 
	 */
	public void updateTracking(ArrayList<Point> nextPts, int t){
		/*
		 * Advance the current particle one step.
		 */

//		System.out.println("\tafter gc: "+Runtime.getRuntime().freeMemory());

		HashMap<Point, List<Particle> > obsMap = new HashMap<Point,List<Particle>>();

		for(int i = 0; i < particles.size();i++){
			Particle thisParticle=particles.get(i);

			/**
			 * Advance current paths
			 */
			simulateForwardStep(nextPts,thisParticle);

			/**
			 * Add unexplained observations as (tentative) 
			 */
			for (Point obs: thisParticle.currentFalsePositives){
				List<Particle> zz = obsMap.get(obs);
				if (zz == null)
					zz = new ArrayList<Particle>();
				zz.add(thisParticle);
				obsMap.put(obs,zz);
			}

		}

		ArrayList<Point> newPaths = new ArrayList<Point>();
		/**
		 * Add unexplained observations as new path clouds.
		 */
		for (Entry<Point, List<Particle>> zz: obsMap.entrySet()){
			List<Particle> aa = zz.getValue();
			Point obs = zz.getKey();
			if ((aa.size()/(double) particles.size()) > newPathThreshold){
				newPaths.add(obs);
			}
		}
		synchronized(clouds){
			createNewPathClouds(newPaths,t);

			/*
			 * Re-sample paths
			 */
			for (PathCloud cp: clouds)
				cp.resamplePaths(t,this);
		}

	}
	
	private void writeAndClearDeadClouds(int t){
		synchronized(clouds){

			if (clouds == null)
				return;
			ArrayList<PathCloud> removals = new ArrayList<PathCloud>();
			for (PathCloud cp: clouds){
				if (!cp.isAlive(t)){
					this.memReducedWritePathCloudsToFile_write(cp);
					removals.add(cp);
				}
			}
			for (Particle pt : particles){
				ArrayList<AntPath> apRemovals = new ArrayList<AntPath>();
				for (AntPath ap : pt.getPaths())
					if (!ap.isAlive(t))
						apRemovals.add(ap);
				for (AntPath ap : apRemovals)
					pt.getPaths().remove(ap);
			}
//			int numAntPaths = 0;
//			for (Particle pt : particles)
//				numAntPaths += pt.getPaths().size();
//			System.out.println(numAntPaths);
			for (PathCloud cp:removals){
				clouds.remove(cp);
				pathColors.remove(cp);
				indexMap.remove(cp);
			}

		}
	}

	private void writeAndClearAllClouds(){
		synchronized(clouds){
		for (PathCloud cp: clouds){
			this.memReducedWritePathCloudsToFile_write(cp);
		}	
		clouds.clear();
		}
		
	}

	private void createNewPathClouds(ArrayList<Point> newPaths,int t) {
		int idx = clouds.size()+1;
		if (indexMap == null)
			indexMap = new HashMap<PathCloud,Integer>();
		for (Point obs: newPaths){
			PathCloud pc = new PathCloud(obs,t,particles);
			indexMap.put(pc,idx);
			idx++;
			for (Particle thisParticle : particles){
				AntPath ap = new AntPath(t,obs,initialLogProb,this);
				thisParticle.getPaths().add(ap);
				pc.addPath(ap, thisParticle);
			}
			clouds.add(pc);
			pathColors.put(pc,ImageUtils.randomColor());
		}
	}


	protected abstract void simulateForwardStep(ArrayList<Point> nextPts,Particle thisParticle);

	/**
	 * 	Computes the trackings for ants for the specified range of image files.
	 * @param trackingProgress 
	 * @param stopButton 
	 * @param j 
	 * 
	 */
	public void computeTrackings(JProgressBarAndLabel trackingProgress, int startTime, int endTime, int numParticles, JButton stopButton,String phenotypeName){

//		Runtime.getRuntime().gc();
//		System.out.println(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());		

		if (!IOTools.DoesDirectoryExist(trackingDir))
			(new File(Run.it.imageset.getFilenameWithHomePath(trackingDir))).mkdir();

		if (phenotypeName == null){
			Collection<Phenotype> phenotypes = Run.it.getPhenotypesSaveNON();
			Iterator<Phenotype> it = phenotypes.iterator();
			Phenotype ph = it.next();
			while(it.hasNext()){
				ph = it.next();
			}
			this.phenotypeName = ph.getName();
		}
		else
			this.phenotypeName = phenotypeName;	


		numImagesTracked = 0;

		this.numParticles = numParticles;

		trackingProgress.setValue(0);

		trackList=Classify.AllCentroids();
		
		frameNumbers = new HashMap<Integer,Integer>();
		for (int i=0;i < trackList.size();i++){
			String[] pt = trackList.get(i).split("_");
			String[] pt2 = pt[1].split("\\.");
			frameNumbers.put(i+1,Integer.parseInt(pt2[0]));
		}

		ArrayList<Point> pts = getCentroidPoints(startTime-1);
		if (pts == null)
			return;

		int numImages = Math.min(trackList.size(),endTime-startTime+1);
		trackingProgress.setText("Computing trackings for " + numImages + " images...");

		InitializeTracking(pts,startTime);

		numImagesTracked++;

		long ptTime = 0;
		long udTime = 0;
		
		if (memReducedPathWriter)
			memReducedWritePathCloudsToFile_init();
		
		for (int i=1;i< numImages;i++){
			if (!stopButton.isEnabled()){
				trackingProgress.setValue(0);
				break;
			}
			trackingProgress.setValue((int) (i*(trackingProgress.getMaximum()/(double)numImages)));
//			System.out.println(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());		

			long t1 = System.currentTimeMillis();
			pts = getCentroidPoints(i+startTime-1);
			long t2 = System.currentTimeMillis();
			updateTracking(pts,startTime+i);
			long t3 = System.currentTimeMillis();
			
			ptTime += t2-t1;
			udTime += t3-t2;
			numImagesTracked++;
//			System.out.print(ptTime);
//			System.out.println("\t"+udTime);
			
			if (memReducedPathWriter && (startTime+i) % this.memReducedPathWritingFreq == 0){
				writeAndClearDeadClouds(startTime+i);
				Runtime.getRuntime().gc();
			}
		}

		if (memReducedPathWriter)
			writeAndClearAllClouds();
		
//		Runtime.getRuntime().gc();
//		System.out.println(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());		

		trackingProgress.setValue(trackingProgress.getMaximum());
	}	


	public static class AntFileFilter implements FilenameFilter{
		/**
		 * Given a file, returns true if it is an ant file.
		 * 
		 * @param dir		the directory the file is located in
		 * @param name		the file itself
`		 * @return			whether or not the file is an image
		 */
		public boolean accept(File dir, String name) {
			String[] fileparts=name.split("\\.");
			if (fileparts.length >= 2){
				//String first = fileparts[0].toLowerCase();
				String ext=fileparts[fileparts.length - 1].toLowerCase();
				if ((ext.equals("data")))
					return true;
				else 
					return false;
			}
			else return false;
		}

		public boolean accept(File pathname) {
			return accept(null, pathname.getName());
		}		
	}
	protected static class CloudPathFileFilter implements FilenameFilter{
		/**
		 * Given a file, returns true if it is an ant file.
		 * 
		 * @param dir		the directory the file is located in
		 * @param name		the file itself
		 * @return			whether or not the file is an image
		 */
		public boolean accept(File dir, String name) {
			String[] fileparts=name.split("\\.");
			if (fileparts.length >= 2){
				String first = fileparts[0].toLowerCase();
				String ext=fileparts[fileparts.length - 1].toLowerCase();
				if (ext.equals("csv")  && first.contains("cloudcenters"))
					return true;
				else 
					return false;
			}
			else return false;
		}		
	}


	public void writePathCloudsToFile() {
		if (clouds == null)
			return;

		String td = Run.it.imageset.getFilenameWithHomePath(trackingDir);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date date = new Date();

		String outputDir = td + File.separator + phenotypeName+"."+dateFormat.format(date);		

		if (!(new File(outputDir)).mkdir()){
			System.err.println("Problem creating output directory: \n\t"+outputDir );
			return;
		}
		PrintWriter hout=null;		
		String headerFilename = outputDir + File.separator + "header.txt";
		try {
			hout=new PrintWriter(new BufferedWriter(new FileWriter(headerFilename)));
		} catch (IOException e) {
			System.err.println(headerFilename+" cannot be edited in CSV appending");
		}

		writeHeader(hout);
		hout.close();

		for (int i = 0;i < clouds.size();i++){
			PathCloud cld = clouds.get(i);
			if (cld.getLength() < shortPathLengthThreshold && dontShowShortPaths)
				continue;
			String filename = outputDir + File.separatorChar + String.format("cloudCenters_%05d.csv",i+1);

			PrintWriter out=null;
			try {
				out=new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			} catch (IOException e) {
				System.err.println(filename+" cannot be edited in CSV appending");
			}
			cld.print(out,frameNumbers);
			out.close();
		}
	}

	
	public void memReducedWritePathCloudsToFile_init() {
		String td = Run.it.imageset.getFilenameWithHomePath(trackingDir);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date date = new Date();

		outputDir = td + File.separator + phenotypeName+"."+dateFormat.format(date);
		pathCloudOutputIdx = 0;

		if (!(new File(outputDir)).mkdir()){
			System.err.println("Problem creating output directory: \n\t"+outputDir );
			return;
		}

		PrintWriter hout=null;		
		String headerFilename = outputDir + File.separator + "header.txt";
		try {
			hout=new PrintWriter(new BufferedWriter(new FileWriter(headerFilename)));
		} catch (IOException e) {
			System.err.println(headerFilename+" cannot be edited in CSV appending");
		}

		writeHeader(hout);
		hout.close();
		
	}	
	public void memReducedWritePathCloudsToFile_write(PathCloud cld) {
		if (clouds == null || outputDir == null)
			return;


		if (cld.getLength() < shortPathLengthThreshold && dontShowShortPaths)
			return;
		pathCloudOutputIdx = pathCloudOutputIdx + 1;
		String filename = outputDir + File.separatorChar + String.format("cloudCenters_%05d.csv",pathCloudOutputIdx);

		PrintWriter out=null;
		try {
			out=new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		} catch (IOException e) {
			System.err.println(filename+" cannot be edited in CSV appending");
		}
		cld.print(out, frameNumbers);
		out.close();

	}


	private void writeHeader(PrintWriter hout) {
		hout.println("phenotypeName: " + phenotypeName);
		hout.println("numImagesTracked: "+this.numImagesTracked);
		hout.println("logProbThreshold: " + logProbThreshold);
		hout.println("falsePositiveLogProb: " + this.falsePositiveLogProb);
		hout.println("falseNegativeLogProb: " + this.falseNegativeLogProb);
		hout.println("sigma2ant: " + this.sigma2ant);
		hout.println("sigma2obs: " + this.sigma2obs);
		hout.println("tentativeThreshold: " + this.tentativeThreshold);
		hout.println("deadThreshold: " + this.deadThreshold);
		hout.println("initialLogProb: " + this.initialLogProb);
		hout.println("newPathThreshold: " + this.newPathThreshold);		
	}


	public void makeTrackImg(BufferedImage I,int t){

		if (clouds == null)
			return;

		int r = 0;
		if (numParticles <= 50)
			r = 1;

		if (numParticles <= 2)
			r = 2;
		synchronized(clouds){
			for (PathCloud cp : clouds){
				ArrayList<AntPath> paths = cp.getPaths(t);
				if (paths == null)
					continue;
				for (AntPath thisPath: cp.getPaths(t)){
					Color center=Color.white;
					Color around=strongColor;
					if (!thisPath.isAlive(t))
						continue;

					if(dontShowShortPaths && thisPath.getLength() < shortPathLengthThreshold)
						continue;
					Point pos = thisPath.getPosition(t);
					Point obs = thisPath.getObservation(t);

					if (obs == null)
						center=Color.red;
					if (thisPath.isTentative(t)){
						around=tentativeColor;
					}

					int i = pos.x;
					int j = pos.y;
					if (r >1){
						for (Point ptt:Solids.GetPointsInSolidUsingCenter(3,new Point(i,j))){
							try {I.setRGB(ptt.x,ptt.y,center.getRGB());} catch (Exception e){}}
						for (Point ptt:Solids.GetPointsInSolidUsingCenter(2,new Point(i,j))){
							try {I.setRGB(ptt.x,ptt.y,around.getRGB());} catch (Exception e){}
							try {I.setRGB(i,j,center.getRGB());} catch (Exception e){}}
					}
					else{
						for (Point ptt:Solids.GetPointsInSolidUsingCenter(r,new Point(i,j))){
							try {I.setRGB(ptt.x,ptt.y,around.getRGB());} catch (Exception e){}}
					}
				}
			}
		}
	}

	public BufferedImage makeTrackImg(int rows,int cols,int t) {
		BufferedImage I=new BufferedImage(rows,cols,BufferedImage.TYPE_INT_ARGB);
		makeTrackImg(I,t);
		return I;
	}	

	public void drawPaths(BufferedImage I, int t, Integer maxDrawLength) {
		if (clouds == null)
			return;
		Graphics g = I.getGraphics();
		synchronized(clouds){
			for (PathCloud cp : clouds){
				if (!cp.isAlive(t))
					continue;

				if (cp.getLength() < shortPathLengthThreshold && dontShowShortPaths)
					continue;

				Point lastPos = cp.getCenter(t);
				for (int i = 0;i < maxDrawLength;i++){
					if (!cp.isAlive(t-i-1))
						break;
					if (drawIndividualColors)
						g.setColor(pathColors.get(cp));
					else if (cp.isTentative(t-i-1))
						g.setColor(tentativeColor);
					else
						g.setColor(strongColor);
					Point thisPos = cp.getCenter(t-i-1);
					assert(thisPos != null);
					g.drawLine(lastPos.x, lastPos.y, thisPos.x, thisPos.y);
					lastPos = thisPos;
				}
			}
		}
		g.dispose();
	}

	public void drawPaths(BufferedImage I, int t, Integer maxDrawLength, Set<PathCloud> displayClouds) {
		if (clouds == null)
			return;
		Graphics g = I.getGraphics();
		g.setFont(new Font("SansSerif", Font.PLAIN, 20));
		synchronized(clouds){
			for (PathCloud cp : displayClouds){
				if (!cp.isAlive(t))
					continue;

				Point lastPos = cp.getCenter(t);
				if (drawIndividualColors)
					g.setColor(pathColors.get(cp));
				if (indexMap == null)
					makeIndexMap();
				g.drawString(Integer.toString(indexMap.get(cp)),lastPos.x,lastPos.y);
				for (int i = 0;i < maxDrawLength;i++){
					if (!cp.isAlive(t-i-1))
						break;
					if (drawIndividualColors)
						g.setColor(pathColors.get(cp));
					else if (cp.isTentative(t-i-1))
						g.setColor(tentativeColor);
					else
						g.setColor(strongColor);
					Point thisPos = cp.getCenter(t-i-1);
					assert(thisPos != null);
					g.drawLine(lastPos.x, lastPos.y, thisPos.x, thisPos.y);

					lastPos = thisPos;
				}
			}
		}
		g.dispose();
	}


	private void makeIndexMap() {
		// TODO Auto-generated method stub
		indexMap = new HashMap<PathCloud,Integer>();
		int idx = 1;
		for (PathCloud cp: clouds){
			indexMap.put(cp, idx);
			idx++;
		}

	}


	protected ArrayList<Point> getCentroidPoints(int imageNum){
		Collection<Phenotype> phenotypes = Run.it.getPhenotypesSaveNON();

		Iterator<Phenotype> it = phenotypes.iterator();
		Phenotype ph = it.next();
		while(it.hasNext() && ph.getName() != phenotypeName){
			ph = it.next();
		}

		if (imageNum >= trackList.size())
			return null;
		String filename = trackList.get(imageNum);
		String isCentroidName = PostProcess.GetIsCentroidName(filename,ph.getName());
		BufferedImage raw=null;
		try {
			raw = ImageAndScoresBank.getOrAddIs(isCentroidName,null);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ArrayList<Point> pts = new ArrayList<Point>();
		for (int i = 0;i<raw.getWidth();i++){
			for (int j = 0;j<raw.getHeight();j++){
				if (raw.getRGB(i,j) != (Color.BLACK).getRGB()){
					pts.add(new Point(i,j));
				}
			}
		}
		return pts;
	}


	public int getNumImagesTracked() {
		return numImagesTracked;
	}


	public PathCloud getClosestPathCloud(Point local, int t) {		
		int bestDist = MIN_CLICK_DISTANCE; 

		PathCloud bestCloud = null;
		int bestIdx = -2;

		synchronized(clouds){
			for (int i = 0;i< clouds.size();i++){
				PathCloud cp = clouds.get(i);
				if (!cp.isAlive(t))
					continue;

				Point thisPos = cp.getCenter(t);
				int thisDist = Math.abs(local.x-thisPos.x)+Math.abs(local.y-thisPos.y);
				if (thisDist <= bestDist){
					bestDist = thisDist;
					bestCloud = cp;
					bestIdx = i;
				}
			}
			if (bestIdx > -2){
			System.out.print("Selected cloud number: " + (bestIdx+1)+"\t");
			System.out.print("pos: "+bestCloud.getCenter(t).x+","+bestCloud.getCenter(t).y+"\n");
			}
		}
		return bestCloud;
	}	

	public Box makeParametersBox(){
		Box thisBox = Box.createVerticalBox();
		Box line = Box.createHorizontalBox();
		line.add(Box.createHorizontalGlue());
		line.setBorder(LineBorder.createBlackLineBorder());
		thisBox.add(line);
		thisBox.add(makeSigmaSpinners());
		thisBox.add(makeProbabilitySpinners());

		return thisBox;
	}


	private Container makeSigmaSpinners() {
		Container b1 = new Container();
		b1.setLayout(new GridLayout(0,4,0,0));

		final JSpinner sigma2antSpinner = new JSpinner(new SpinnerNumberModel(sigma2ant,0.,100.,.1));
		b1.add(new JLabel("sigma2ant:",JLabel.RIGHT));
		b1.add(sigma2antSpinner);
		final JSpinner sigma2obsSpinner = new JSpinner(new SpinnerNumberModel(sigma2obs,0.,100.,.1));
		b1.add(new JLabel("sigma2obs:",JLabel.RIGHT));
		b1.add(sigma2obsSpinner);

		sigma2antSpinner.addChangeListener(new ChangeListener(){

			public void stateChanged(ChangeEvent e) {
				sigma2ant = (Double) sigma2antSpinner.getValue();
			}});

		sigma2obsSpinner.addChangeListener(new ChangeListener(){

			public void stateChanged(ChangeEvent e) {
				sigma2obs = (Double) sigma2obsSpinner.getValue();

			}});

		return b1;
	}
	
	private Container makeProbabilitySpinners() {
		Container b1 = new Container();
		b1.setLayout(new GridLayout(0,4,0,0));
		Container b2 = new Container();
		b2.setLayout(new GridLayout(0,4,0,0));


		final double reasonableMinValue = -1000;


		final JSpinner falsePositiveLogProbSpinner = new JSpinner(new SpinnerNumberModel(falsePositiveLogProb,reasonableMinValue,0.,.1));			
		falsePositiveLogProbSpinner.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				falsePositiveLogProb = (Double) falsePositiveLogProbSpinner.getValue();

			}});
		b1.add(new JLabel("falsePositiveLogProb:",JLabel.RIGHT));
		b1.add(falsePositiveLogProbSpinner);

		final JSpinner falseNegativeLogProbSpinner = new JSpinner(new SpinnerNumberModel(falseNegativeLogProb,reasonableMinValue,0.,.1));
		falseNegativeLogProbSpinner.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				falseNegativeLogProb = (Double) falseNegativeLogProbSpinner.getValue();

			}});
		b1.add(new JLabel("falseNegativeLogProb:",JLabel.RIGHT));			
		b1.add(falseNegativeLogProbSpinner);

		final JSpinner initialLogProbSpinner = new JSpinner(new SpinnerNumberModel(initialLogProb,reasonableMinValue,0.,.1));
		initialLogProbSpinner.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				initialLogProb = (Double) initialLogProbSpinner.getValue();

			}});
		b1.add(new JLabel("initialLogProb:",JLabel.RIGHT));			
		b1.add(initialLogProbSpinner);

		final JSpinner deadThresholdSpinner = new JSpinner(new SpinnerNumberModel(deadThreshold,reasonableMinValue,0.,.1));
		deadThresholdSpinner.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				deadThreshold = (Double) deadThresholdSpinner.getValue();

			}});
		b2.add(new JLabel("deadThreshold:",JLabel.RIGHT));			
		b2.add(deadThresholdSpinner);

		final JSpinner tentativeThresholdSpinner = new JSpinner(new SpinnerNumberModel(tentativeThreshold,reasonableMinValue,0.,.1));
		tentativeThresholdSpinner.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				tentativeThreshold = (Double) tentativeThresholdSpinner.getValue();

			}});
		b2.add(new JLabel("tentativeThreshold:",JLabel.RIGHT));			
		b2.add(tentativeThresholdSpinner);

		Box b = Box.createVerticalBox();
		b.add(b1);
		b.add(b2);
		return b;
	}
}
