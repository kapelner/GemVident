
package GemIdentImageSets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentTools.Rainbow;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentTools.Matrices.ShortMatrix;
import GemIdentView.KFrame;
import GemIdentView.ScrollablePicture;

/**
 * Handles the "sophisticated helper" feature that finds colocalizations in
 * Nuance image sets.
 * 
 * This class is not fully documented since it implements a proprietary feature
 * for a proprietary hardware setup
 * 
 * @author Adam Kapelner
 */
public class NuanceDetailedTrainingHelper {

	private static final String DialogTitle = "Generating the sophisticated image set helper";

	private NuanceImageListInterface nuanceImageList;

	// stores the intensities
	private HashMap<String, Integer> intensity_lower_bound;
	
	//stores the actual data
	private HashMap<String, BoolMatrix> visual_data;
	
	//stores the training helper image
	private BufferedImage training_image;

	private double dim_scale;
	
//	private int xo;
//	private int xf;
//	private int yo;
//	private int yf;
	
	/** the frame to house the progress bar */
	private JFrame dialog;
	/** the progress bar for the entire data collection process */
	private JProgressBar progress;
	/** the amount to update the progress bar by when one image completes */
	private double update;
	private JFrame overview_image_frame;
	private JScrollPaneWithScreenshot viewpane_for_overview_image;
	private Dimension dims_of_visual_data;

	
	//all the amounts to update (all in percentages)
	private static final double ClearImageProgress = 5;
	private static final double DisplayImageProgress = 45;
	private static final double RebuildDateProgress = 50;
//	private static final double CropProgress = 25;
	
	public NuanceDetailedTrainingHelper(NuanceImageListInterface nuanceImageList){
		this.nuanceImageList = nuanceImageList;
		//init some data
		visual_data = new HashMap<String, BoolMatrix>();
		dims_of_visual_data = new Dimension(nuanceImageList.getGlobalWidth(true), nuanceImageList.getGlobalHeight(true));
		dialog = new JFrame();
		overview_image_frame = new JFrame();
		
		ResetLowerBounds();
	}
	
	public void ResetLowerBounds(){
		//copy the intensity ranges
		intensity_lower_bound = new HashMap<String, Integer>();
		for (String wave_name : nuanceImageList.getWaveNames()){
			intensity_lower_bound.put(wave_name, -99); //values that could never exist for true range
//			System.out.println("intensityranges_copy wave_name:" + wave_name);
		}		
	}

	public void trashImageWindow() {
		overview_image_frame.setVisible(false);
		overview_image_frame.dispose();	
		overview_image_frame = new JFrame();
	}
	
	private static final int dialog_width = 500;
	private static final int dialog_height = 50;
	public void display() {
		//kill unsophisticated training helper
		nuanceImageList.TrashOverviewImage();
		
		//kill the old window and dialog
		trashImageWindow();
		dialog = new JFrame();
		
		//generate all the stuff for the progress bar frame
		update = 0; //reset the progress bar value
		progress = new JProgressBar();
		progress.setStringPainted(true);		
		
		Point origin = Run.it.getGuiLoc();
		origin.translate(KFrame.frameSize.width / 2 - dialog_width / 2, KFrame.frameSize.height / 2 - dialog_height / 2);
		dialog.setLocation(origin);
		dialog.setLayout(new BorderLayout());
		dialog.setTitle(DialogTitle);		
		dialog.add(progress, BorderLayout.CENTER);
		dialog.pack();
		dialog.setResizable(true);
		dialog.setVisible(true);
		dialog.setSize(new Dimension(dialog_width, dialog_height));
		dialog.repaint();
		
		RebuildDataIfNecessary();
		RegenerateImage();
		
		//ditch the progress bar frame
	    dialog.dispose();
	    dialog.setVisible(false);
	    
	    //now create the custom display
		// set up scrollable pic with the overview image
		final ScrollablePicture scrollable = IOTools.GenerateScrollablePicElement(training_image);
		// set up listeners
		scrollable.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent e){}
			public void mouseEntered(MouseEvent e){}
			public void mouseExited(MouseEvent e){}
			public void mousePressed(MouseEvent e){}
			public void mouseReleased(MouseEvent e) {
				if (e.getClickCount() == 2) { // ie "double" click
					PossiblyAddNewTrainingSetImage(e.getPoint(), scrollable);
					DrawGlobalThumbnailOverlay(scrollable.getImageIconGraphics());
					scrollable.repaint();
				}					
			}
		});
		
		// now give the frame an optimal size
		Dimension view_size = scrollable.getDimensionOfImage();
		Dimension new_size = new Dimension();
		if (view_size.width > KFrame.frameSize.width) {
			new_size.width = KFrame.frameSize.width;
		} else {
			new_size.width = view_size.width + NuanceImageListInterface.ScrollbarThickness;
		}
		if (view_size.height > KFrame.frameSize.height) {
			new_size.height = KFrame.frameSize.height;
		} else {
			new_size.height = view_size.height + NuanceImageListInterface.ScrollbarThickness;
		}
		
	 // set up the scrollpane inside the frame
	    overview_image_frame.setTitle("Sophisticated training set helper");
		viewpane_for_overview_image = new JScrollPaneWithScreenshot();
		viewpane_for_overview_image.setViewportView(scrollable);
		overview_image_frame.add(viewpane_for_overview_image);
		// now set up the frame to be viewed
		overview_image_frame.setResizable(true);
		overview_image_frame.setSize(new_size);	
		overview_image_frame.setVisible(true);
		overview_image_frame.repaint();
		//center it
		viewpane_for_overview_image.getHorizontalScrollBar().setValue((scrollable.getWidth() - new_size.width) / 2);
		viewpane_for_overview_image.getVerticalScrollBar().setValue((scrollable.getHeight() - new_size.height) / 2);
		//paint image nums
		DrawGlobalThumbnailOverlay(scrollable.getImageIconGraphics());	
		scrollable.repaint();
	}
	
	protected void PossiblyAddNewTrainingSetImage(Point point, ScrollablePicture scrollable) {
		overview_image_frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		//find the stage number
		double pixels_per_x = training_image.getWidth() / (double) (nuanceImageList.width);
		double pixels_per_y = training_image.getHeight() / (double) (nuanceImageList.height);
		int x = (int) Math.round((point.x - pixels_per_x / 2) / pixels_per_x);
		int y = (int) Math.round((point.y - pixels_per_y / 2) / pixels_per_y);
		
		Integer stage = nuanceImageList.GetStageFromThumbnailCoordinates(x, y);
		if (stage != NuanceImageListInterface.BAD_PIC){		
			// now check if the files associated with this stage and the surrounding stages exist
			String filename_if_it_exists = nuanceImageList.CheckIfStageExists(scrollable, stage);
			if (filename_if_it_exists != null){
				JOptionPane.showMessageDialog(overview_image_frame, "the file \"" + filename_if_it_exists + "\" does not exist.\nYou must've deleted it previously . . .");
				return; // we're not going to do anything else
			}
//			System.out.println("stage " + stage + " x " + x + " y " + y + " convname: " + NuanceImageListInterface.ConvertNumToFilename(stage));
			nuanceImageList.trainPanel.AddNewThumbnail(NuanceImageListInterface.ConvertNumToFilename(stage));
		}
		overview_image_frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));		
	}

	private void DrawGlobalThumbnailOverlay(Graphics g) {
		g.setColor(Color.BLACK);
		
		double pixels_per_x = training_image.getWidth() / (double) (nuanceImageList.width);
		double pixels_per_y = training_image.getHeight() / (double) (nuanceImageList.height);
		for (double i = pixels_per_x / 2 - 1; i < training_image.getWidth(); i += pixels_per_x) {
			for (double j = pixels_per_y / 2 - 1 + 10; j < training_image.getHeight(); j += pixels_per_y) {
				int x = (int) Math.round(i);
				int y = (int) Math.round(j);
				
				int adj_x = (int)Math.floor(x / pixels_per_x);
				int adj_y = (int)Math.floor(y / pixels_per_y);
				Integer stage = nuanceImageList.GetStageFromThumbnailCoordinates(adj_x, adj_y);
//				System.out.println("stage: " + stage + " i:" + (int)i + " j:" + (int)j + " adj_x: " + adj_x + " y: " + adj_y);
				if (stage != NuanceImageListInterface.BAD_PIC){
//					if (nuanceImageList instanceof NuanceImageList){
//						stage--; //don't know why we need this correction but we do
//					}
					String display_num = String.valueOf(stage);
					// if it's in the training set, give it some asterisks
					if (nuanceImageList.trainPanel.isInTrainingSet(NuanceImageListInterface.ConvertNumToFilename(stage))) {
						display_num += "****";
					}
					g.setColor(new Color(0, 50, 0));
					g.setFont(new Font("SansSerif", Font.PLAIN, 40));
					g.drawString(display_num, x, y);
				}
			}
		}
	}
	
	private static final int BackgroundColor = Color.WHITE.getRGB();

	private void RegenerateImage() {
		
//		int cropped_width = xf - xo;
//		int cropped_height = yf - yo;

		//immediately establish scale factor
		long total_pixels = (long)dims_of_visual_data.width * (long)dims_of_visual_data.height;
		double area_scale = IOTools.MAX_NUM_PIXELS_BUFFERED_IMAGE_TO_DISPLAY / total_pixels;
//		System.out.println("area_scale: " + area_scale + " total_pixels " + total_pixels);
		
		if (area_scale > 1){
			area_scale = 1;
		}
		dim_scale = Math.sqrt(area_scale);
//		System.out.println("dim_scale: " + dim_scale + " area_scale " + area_scale + " w:" + dims_of_visual_data.width + " h:" + dims_of_visual_data.height);
		int scaled_width = (int)Math.round(dim_scale * dims_of_visual_data.width);
		int scaled_height = (int)Math.round(dim_scale * dims_of_visual_data.height);
		
		ImageAndScoresBank.FlushAllCaches(); //make way for Prince Ali
		
		//init the image and clear it
//		System.out.println("training_image: " + scaled_width + "x" + scaled_height + " now clearing . . .");
		training_image = new BufferedImage(scaled_width, scaled_height, BufferedImage.TYPE_INT_RGB);

		for (int i = 0; i < scaled_width; i++){
			for (int j = 0; j < scaled_height; j++){
				training_image.setRGB(i, j, BackgroundColor);
			}					
		}	
		
		update += ClearImageProgress;
		UpdateProgressBar();
		
		final ArrayList<String> waves_to_build = new ArrayList<String>();
		for (final String wave_name : visual_data.keySet()){
			if (nuanceImageList.getWaveVisible(wave_name)){ //only update those the user has chosen to be visible
				waves_to_build.add(wave_name);
			}
		}
		final double num_waves_to_build = waves_to_build.size();

		ExecutorService builderpool = Executors.newFixedThreadPool(Run.it.num_threads);
		//now build the image from the binary data
		for (final String wave_name : waves_to_build){
			builderpool.execute(new Thread(){
				public void run(){					
//					System.out.println("updating image with " + wave_name);
					BoolMatrix M = visual_data.get(wave_name);
//					System.out.println(wave_name + " loaded: " + M.getWidth() + "x" + M.getHeight());
//					IOTools.WriteIsMatrix(wave_name + "_is.tiff", M);
//					System.out.println(wave_name + " (done)");
					int color = nuanceImageList.getWaveColor(wave_name).getRGB();
					for (int i = 0; i < M.getWidth(); i++){
						for (int j = 0; j < M.getHeight(); j++){
//								int alpha_value = M.get(i, j);
//								if (alpha_value != 0){ //if we're at the point of positivity
							if (M.get(i, j)){
								int scaled_i = (int)Math.floor(i * dim_scale);
								int scaled_j = (int)Math.floor(j * dim_scale);
								try {
									int pixel_color = training_image.getRGB(scaled_i, scaled_j);
									//if there's nothing here or if it's the same, no prob
									if (pixel_color == BackgroundColor || pixel_color == color){
	//										System.out.println(scaled_i + "," + scaled_j + ": " + "set to: true");// + alpha_value);
	//										training_image.setRGB(scaled_i, scaled_j, new Color(r, g, b, alpha_value).getRGB());
										training_image.setRGB(scaled_i, scaled_j, color);
									}
									else { //colocalization - random pixel color
	//										System.out.println(scaled_i + "," + scaled_j + ": " + "set to: ColocalizationColor");
										training_image.setRGB(scaled_i, scaled_j, Rainbow.getRandomColorInt());
									}
								} catch (ArrayIndexOutOfBoundsException e){}
							}
						}					
					}
					update += (1 / num_waves_to_build) * DisplayImageProgress;
					UpdateProgressBar();					
				}
			});
		}
		//shutdown pool and do a join
		builderpool.shutdown();
		try {	         
	         builderpool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
	    update = 100;
//	    IOTools.WriteImage("sophisticated.jpg", "JPEG", training_image);	    
	}
	
	private ArrayList<String> haveTheIntensitiesChangedAndIsItVisible(){
		ArrayList<String> those_changed = new ArrayList<String>();

		for (String wave_name : nuanceImageList.getWaveNames()){
			if (nuanceImageList.getWaveVisible(wave_name)){
				int a = intensity_lower_bound.get(wave_name);			
				int[] range = nuanceImageList.getIntensityranges().get(wave_name);
	//			System.out.println("ranges_copy:" + ranges_copy[0] + " " + ranges_copy[1]);
	//			System.out.println("ranges:" + range[0] + " " + range[1] + " before " + a);
				if (a != range[0]){
					intensity_lower_bound.put(wave_name, range[0]);
					//we must record this wave name that's changed
					those_changed.add(wave_name);
				}
			}
		}		
		return those_changed;
	}	

	private static final double IntensityBumpUpFactor = 1.5;
	private void RebuildDataIfNecessary() {
		//create a pool for building
		ExecutorService builderpool = Executors.newFixedThreadPool(Run.it.num_threads * 2);
	    
		ArrayList<String> those_changed = haveTheIntensitiesChangedAndIsItVisible();
//		if (those_changed.size() > 0){
//			//redo the bounds
//			xo = Integer.MAX_VALUE;
//			xf = Integer.MIN_VALUE;
//			yo = Integer.MAX_VALUE;
//			yf = Integer.MIN_VALUE;
//		}
		final HashSet<Integer> clickedonset = nuanceImageList.GetClickedOnImagesAsIntsAndCreateIfNecessary();
		
		final double num_images_to_build = clickedonset.size() * those_changed.size();
		
//		Collection<String> trained_images = Run.it.getGUI().getTrainingImageSet();
//		final ArrayList<Integer> trained_stages = new ArrayList<Integer>();
//		for (String training_image : trained_images)
//			trained_stages.add(Integer.parseInt(training_image.split("_")[1]));
		
		for (final String wave_name : those_changed){
//			System.out.println("need to rebuild:" + wave_name);
			builderpool.execute(new Thread(){
				public void run(){
					BoolMatrix M = new BoolMatrix(dims_of_visual_data);
					for (int stage : clickedonset){
						//ditch if it's not a real stage:
						if (stage == ImageSetInterface.BAD_PIC){
							continue;
						}
//						if (!trained_stages.contains(stage))
//							continue;						
//						if (new Random().nextDouble() < .1) //only show it sometimes
//							System.out.println("rebuilding stage " + stage + " (" + wave_name + ")");
						ShortMatrix data = nuanceImageList.getNuanceImagePixelDataForOneWave(stage, wave_name, true);
//						BoolMatrix copy = new BoolMatrix(data.getWidth(), data.getHeight()); 
						for (int i = 0; i < data.getWidth(); i++){
							for (int j = 0; j < data.getHeight(); j++){
								//mark the bottom borders:
								if (i == 0 || j == 0){
									M.set(nuanceImageList.getTrueLocation(stage, new Point(i, j), true), true);
								}
								//otherwise look to see if it's a positive pixel
								else {
									//mark the top borders
									if (i == data.getWidth() - 1 || j == data.getHeight() - 1){
										M.set(nuanceImageList.getTrueLocation(stage, new Point(i + 1, j + 1), true), true);
									}									
									int pixel_value = data.get(i, j);
									int lower_bound = intensity_lower_bound.get(wave_name);
									if (lower_bound < NuanceSubImage.BeginningValOnSliderA){
										lower_bound = NuanceSubImage.BeginningValOnSliderA;
									}
									if (pixel_value >= (lower_bound / NuanceSubImage.conversion_factor * IntensityBumpUpFactor)){ //ie it's in the range
	//									int alpha = (int)Math.round((pixel_value - a) / ((double)alpha_range) * 255);
										Point global_point = nuanceImageList.getTrueLocation(stage, new Point(i, j), true);
										M.set(global_point, true);
	//									copy.set(i, j, true);
//										if (global_point.x < xo){
//											xo = global_point.x;
//										}
//										else if (global_point.x > xf){
//											xf = global_point.x;
//										}
//										if (global_point.y < yo){
//											yo = global_point.y;
//										}
//										else if (global_point.y > yf){
//											yf = global_point.y;
//										}									
									}
								}
							}					
						}
//						if (trained_stages.contains(stage)){
//							System.out.println("a: " + (intensity_lower_bound.get(wave_name) / NuanceSubImage.conversion_factor * IntensityBumpUpFactor) + " stage_" + stage + "_wave_" + wave_name);
//							IOTools.WriteScoreImage("stage_" + stage + "_wave_" + wave_name + ".tiff", copy);
//						}
						update += (1 / num_images_to_build) * RebuildDateProgress;
						UpdateProgressBar();
					}
					visual_data.put(wave_name, M);
					
					
				}
			});
		}	
		
		//do a join
		builderpool.shutdown();
		try {	         
	         builderpool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
	    
	    //done with building
	    update = RebuildDateProgress;
	    UpdateProgressBar();
	    
//	    CropImagesToSmallestSize(those_changed);
	}

//	private void CropImagesToSmallestSize(ArrayList<String> those_changed) {
//
//		final double num_images_to_crop = those_changed.size();
//	    System.out.println("crop values: " + xo + "," + xf + "," + yo + "," + yf);
//		//create a pool for cropping
//		ExecutorService cropperpool = Executors.newFixedThreadPool(Run.it.NUM_THREADS);
//	    //now do the cropping
//		for (final String wave_name : those_changed){			
//			cropperpool.execute(new Thread(){
//				public void run(){
//					BoolMatrix M = visual_data.get(wave_name);
//					System.out.println("cropping:" + wave_name);
//					visual_data.put(wave_name, M.crop(xo, xf, yo, yf));
//					
//					update += (1 / num_images_to_crop) * CropProgress;
//					UpdateProgressBar();
//				}
//			});
//		}
//		
//		if (num_images_to_crop == 0){
//			update += CropProgress;
//			UpdateProgressBar();
//		}
//
//		//shutdown and do a join
//		cropperpool.shutdown();
//		try {	         
//			cropperpool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
//	    } catch (InterruptedException ignored){}	    
//	}

	private void UpdateProgressBar() {
		progress.setValue((int)Math.round(update));
	}
}