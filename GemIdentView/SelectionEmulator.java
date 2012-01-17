
package GemIdentView;

/**
 * An interface that simplifies selecting KClassInfo elements
 * in the {@link KColorTrainPanel color train panel} and the
 * {@link KPhenotypeTrainPanel phenotype train panel}
 * 
 * @author Kyle Woodward
 *
 */
public interface SelectionEmulator {
	/** set a KClassInfo element selected */
	public void selectElement( KClassInfo element );
}