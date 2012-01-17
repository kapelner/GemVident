
package GemIdentImageSets;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import GemIdentClassificationEngine.Classify;
import GemIdentClassificationEngine.FilmSphereColor.FilmSphereColorDatum;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentTools.Thumbnails;
import GemIdentTools.Matrices.ShortMatrix;

/**
 * Provides a cache for:
 * <ul>
 * <li>{@link DataImage images}</li>
 * <li>{@link SuperImage superimages}</li>
 * <li>raw Nuance images</li> 
 * <li>{@link Thumbnails thumbnail images}</li>
 * <li>scores objects</li>
 * <li>Binary images</li>
 * </ul>
 * 
 * @author Adam Kapelner
 */
public class ImageAndScoresBank {

	/** 
	 * The maximum number of SuperImages to be cached, and the max number of Scores objects to be cached
	 * The only reason we cache these at all is when user classifies only those trained. In
	 * this case, training data is built from the trained images, and hence superimages and scores have to be built,
	 * then those same images are classified and superimages and scores would have to be built again (without 
	 * the caching)
	 */ 
//	private static final int SUPER_AND_SCORES_MAX_SIZE = 50; //large training sets?
//	/** The maximum number of DataImages to be cached */
//	private static final int DATAIMAGES_MAX_SIZE=10; //those of you with low RAM - beware
//	/** The maximum number of DataImages to be cached */
//	private static final int NUANCE_RAW_IMAGES_MAX_SIZE=108; //SUPER_AND_SCORES_MAX_SIZE * 9, beware of low ram	
//	/** The maximum number of SuperImages to be cached */
//	private static final int IS_IMAGES_MAX_SIZE=50; //ditto	
//	/** The maximum number of Confusion Images to be cached */
//	private static final int CONFUSION_IMAGES_MAX_SIZE = 10; //ditto
//	/** The maximum number of frame score objects to be cached */
//	private static final int NUM_FRAME_SCORE_OBJECTS = 1;
	

	/**
	 * A class that combines the functionality of a map and a list
	 * 
	 * @param <E>	The object stored in the MapAndList referenced by Strings
	 * 
	 * @author Adam Kapelner
	 */
	private static class MapAndList<K, E> {
		private LinkedHashMap<K, E> map;
		public MapAndList(){
			map=new LinkedHashMap<K, E>();
		}
		public MapAndList(int initial_capacity){
			map=new LinkedHashMap<K, E>(initial_capacity);
		}
		public E get(K key){
			return map.get(key);
		}
		public void put(K key,E value){
			map.put(key,value);			
		}
		/**
		 * Ditch elements until this MapAndList is at its proper size
		 * 
		 * @param max_size
		 */
		public void flushToSize(int max_size) {
			synchronized (this){
				for (int i=0; i < this.size() - max_size; i++){
					this.removeFirstElement();
				}
			}
		}		
		public void removeFirstElement(){
			map.remove(getKeyAtIndex(0));
		}
		public K getKeyAtIndex(int i_o){
			int i = 0;
			for (K key : map.keySet()){
				if (i == i_o)
					return key;
				i++;
			}
			return null;
		}
		public E getValueAtIndex(int i_o){
			return map.get(getKeyAtIndex(i_o));
		}
		public int size(){
			return map.size();
		}
	}
	
	//the cache vars
	private static MapAndList<String, SuperImage> allSuperImages;
	private static MapAndList<String, DataImage> allImages;
	private static MapAndList<String, BufferedImage> allNuanceRawImages;
	private static MapAndList<String, HashMap<String,ShortMatrix>> allScores;
	private static MapAndList<String, BufferedImage> thumbnailImages;	
	private static MapAndList<String, DataImage> isImages;
	private static MapAndList<String, ShortMatrix> confusionImages;
	private static MapAndList<Integer, LinkedHashMap<Integer, HashMap<String, ShortMatrix>>> allFrameScoreObjects;

	/** Initialization is the same as flushing */
	static {
		FlushAllCaches();
	}
	/** reinitialize all caches to free memory */
	public static void FlushAllCaches(){  //just initialize all the ivars:
		allSuperImages = new MapAndList<String, SuperImage>(Run.it.SUPER_AND_SCORES_MAX_SIZE);
		allImages = new MapAndList<String, DataImage>(Run.it.DATAIMAGES_MAX_SIZE);
		allScores = new MapAndList<String, HashMap<String,ShortMatrix>>(Run.it.SUPER_AND_SCORES_MAX_SIZE);
		thumbnailImages = new MapAndList<String, BufferedImage>(); //no max!
		isImages = new MapAndList<String, DataImage>(Run.it.IS_IMAGES_MAX_SIZE);
		allNuanceRawImages = new MapAndList<String, BufferedImage>(Run.it.NUANCE_RAW_IMAGES_MAX_SIZE);
		confusionImages = new MapAndList<String, ShortMatrix>(Run.it.CONFUSION_IMAGES_MAX_SIZE);
		allFrameScoreObjects = new MapAndList<Integer, LinkedHashMap<Integer, HashMap<String, ShortMatrix>>>(Run.it.NUM_FRAME_SCORE_OBJECTS);
	}
	/**
	 * gets a binary image from the cache. If it
	 * doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the binary image
	 * @param dir			the directory where the binary image resides
	 * @return				the binary image
	 * @throws FileNotFoundException 
	 */
	public static BufferedImage getOrAddIs(String filename,String dir) throws FileNotFoundException{
		if (dir != null)
			filename=dir+"//"+filename;
		
		DataImage dataImage=isImages.get(filename);
		if (dataImage == null){
//			System.out.println("Data Image...");
//			System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
			dataImage=new RegularSubImage(filename,false);
//			System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
//			isImages.put(filename,dataImage);
		}
		if (isImages.size() > Run.it.IS_IMAGES_MAX_SIZE)
			isImages.flushToSize(Run.it.IS_IMAGES_MAX_SIZE);
		return dataImage.getAsBufferedImage();
	}	
	/**
	 * gets a {@link SuperImage SuperImage} from the cache. If it
	 * doesn't exist, load it and cache it. If the cache has reached
	 * its limit, flush it to avoid taking too much memory
	 * 
	 * @param filename 		the filename of the superimage
	 * @return				the superimage
	 * @throws FileNotFoundException 
	 */
	public static SuperImage getOrAddSuperImage(String filename) throws FileNotFoundException {		
		SuperImage superImage=allSuperImages.get(filename);
		if (superImage == null){	
			//System.out.println("getOrAddSuperImage add filename: " + filename);
//			System.out.println("Super Image...");
//			System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
			if (Run.it.imageset instanceof NuanceImageListInterface)
				superImage=new SuperNuanceImage((NuanceImageListInterface)Run.it.imageset, filename);
			else
				superImage=new SuperRegularImage(filename);
//			System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
			synchronized (allSuperImages){
				allSuperImages.put(filename,superImage);
			}
//			System.out.println("AddSuperImage: " + filename + " (" + allSuperImages.size() + ")");
		}
		if (allSuperImages.size() > Run.it.SUPER_AND_SCORES_MAX_SIZE)
			allSuperImages.flushToSize(Run.it.SUPER_AND_SCORES_MAX_SIZE);
//		IOTools.WriteImage("super_"+filename+".jpg","JPEG",superImage.getAsBufferedImage()); //debug purposes only
		return superImage;
	}
	/**
	 * gets a Nuance raw image (tiff file now as a BufferedImage) from 
	 * the cache. If it doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the TIFF image
	 * @return				the TIFF as a BufferedImage
	 */
	public static BufferedImage getOrAddRawNuanceImage(String filename){		
		BufferedImage image=allNuanceRawImages.get(filename);
		if (image == null){	
//			System.out.println("AddRawNuanceImage: " + filename + " (" + allNuanceRawImages.size() + ")");
			try {
//				System.out.println("Raw Nuance Image...");
//				System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
				image = IOTools.OpenImage(filename);
//				System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			synchronized (allNuanceRawImages){
				allNuanceRawImages.put(filename, image);
			}
		}
		if (allNuanceRawImages.size() > Run.it.NUANCE_RAW_IMAGES_MAX_SIZE)
			allNuanceRawImages.flushToSize(Run.it.NUANCE_RAW_IMAGES_MAX_SIZE);
		return image;
	}	
	/**
	 * gets a scores object from the cache. If it
	 * doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the scores object
	 * @return				the scores object
	 * @throws FileNotFoundException 
	 */
	public static HashMap<String,ShortMatrix> getOrAddScores(String filename) throws FileNotFoundException {
		HashMap<String,ShortMatrix> scores=allScores.get(filename);
		if (scores == null){
			//System.out.println("getOrAddScores add filename: " + filename);
//			System.out.println("Stain Scores...");
//			System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
			scores = Run.it.imageset.GenerateStainScores(getOrAddSuperImage(filename));
//			System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
			synchronized (allScores){
				allScores.put(filename,scores);
			}
		}
		if (allScores.size() > Run.it.SUPER_AND_SCORES_MAX_SIZE)
			allScores.flushToSize(Run.it.SUPER_AND_SCORES_MAX_SIZE);
		return scores;
	}
	/**
	 * gets a thumbnail (as a BufferedImage) from the cache. If it
	 * doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the thumbnail
	 * @return				the thumbnail
	 */
	public static BufferedImage getOrAddThumbnail(String filename) {
		
		BufferedImage image = thumbnailImages.get(filename);
		if (image == null){
			try {
				image = IOTools.OpenImage(Thumbnails.getThumbnailFilename(filename));
			} catch (FileNotFoundException e) {
				image = null;
			} 
			thumbnailImages.put(filename, image);
		}
		return image;
	}
	/**
	 * gets a {@link DataImage DataImage} from the cache. If it
	 * doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the DataImage
	 * @return				the DataImage
	 * @throws FileNotFoundException 
	 */
	public static DataImage getOrAddDataImage(String filename) throws FileNotFoundException{		
		DataImage dataImage=allImages.get(filename);
		if (dataImage == null){	
//			System.out.println("AddDataImage: " + filename + " (" + allImages.size() + ")");
//			System.out.println("Data Image...");
//			System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
			dataImage = Run.it.imageset.getDataImageFromFilename(filename);
//			System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
			synchronized (allImages){
				allImages.put(filename, dataImage);
			}
		}
		if (allImages.size() > Run.it.DATAIMAGES_MAX_SIZE){
			FlushOne(allImages, Run.it.DATAIMAGES_MAX_SIZE);
		}
		return dataImage;
	}
	/**
	 * gets a Confusion Image (a {@link ShortMatrix ShortMatrix})
	 * from the cache. If it doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the Confusion Image
	 * @return				the DataImage
	 */
	public static ShortMatrix getOrAddConfusionImage(String filename){		
		ShortMatrix confusion_image=confusionImages.get(filename);
		if (confusion_image == null){
			try {
//				System.out.println("Confusion Image...");
//				System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
				confusion_image = new ShortMatrix(Classify.GetConfusionName(filename));
//				System.out.println("\tFree Memory"+Runtime.getRuntime().freeMemory());
			} catch (FileNotFoundException e){ //ie file not found!
				return null;
			}
			synchronized (confusionImages){
				confusionImages.put(filename, confusion_image);
			}
		}
		if (confusionImages.size() > Run.it.CONFUSION_IMAGES_MAX_SIZE){
			FlushOne(confusionImages, Run.it.CONFUSION_IMAGES_MAX_SIZE);
		}
		return confusion_image;
	}
	
	/**
	 * gets a {@link SuperImage SuperImage} from the cache. If it
	 * doesn't exist, load it and cache it. If the cache has reached
	 * its limit, flush it to avoid taking too much memory
	 * @param filmSphereColorDatum 
	 * 
	 * @param filename 		the filename of the superimage
	 * @return				the superimage
	 */
	public static LinkedHashMap<Integer, HashMap<String, ShortMatrix>> getOrAddFrameScoreObject(FilmSphereColorDatum filmSphereColorDatum, int f) {		
		LinkedHashMap<Integer, HashMap<String, ShortMatrix>> frame_score_object = allFrameScoreObjects.get(f);
		if (frame_score_object == null){	
			System.out.println("getOrAddFrameScoreObject add f: " + f);
			frame_score_object = filmSphereColorDatum.generateFrameScoreObject();
			allFrameScoreObjects.put(f, frame_score_object);
		}
		if (frame_score_object.size() > Run.it.NUM_FRAME_SCORE_OBJECTS)
			allFrameScoreObjects.flushToSize(Run.it.NUM_FRAME_SCORE_OBJECTS);
		return frame_score_object;
	}	

	@SuppressWarnings("unchecked")
	private static void FlushOne(MapAndList cache, int max_size) {
		synchronized (cache){
			for (int i=0; i < cache.size() - max_size; i++){
				cache.removeFirstElement();
			}
		}
	}
	
	/** flush all SuperImages from the cache */
	public static void FlushAllSuperImages(){
		allSuperImages=new MapAndList<String, SuperImage>(Run.it.SUPER_AND_SCORES_MAX_SIZE);
	}
	/** flush all scores objects from the cache */
	public static void FlushAllScores(){
		allScores=new MapAndList<String, HashMap<String,ShortMatrix>>(Run.it.SUPER_AND_SCORES_MAX_SIZE);
	}
	/** flush binary images from the cache */
	public static void FlushAllIsImages() {
		isImages=new MapAndList<String, DataImage>(Run.it.IS_IMAGES_MAX_SIZE);	
	}
	/** flush all confusion images from the cache */
	public static void FlushAllConfusionImages() {
		confusionImages = new MapAndList<String, ShortMatrix>(Run.it.CONFUSION_IMAGES_MAX_SIZE);
	}
}