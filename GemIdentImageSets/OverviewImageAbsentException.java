
package GemIdentImageSets;

import java.io.FileNotFoundException;

/**
 * If the thumbnail image of the overall global context is missing,
 * throw this error. No new functionality, just want to distinguish
 * between this error and a vanilla FileNotFoundException
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class OverviewImageAbsentException extends FileNotFoundException {}