
package GemIdentImageSets;

import java.io.Serializable;

/**
 * A dumb struct that holds information about an exclusion rule - 
 * a rule that excludes one phenotype from being a certain distance 
 * from another phenotype
 * 
 * @author Adam Kapelner
 */
public class ExclusionRuleStruct implements Serializable {
	private static final long serialVersionUID = -2796466806558208962L;
	
	/** the phenotype that if found ... */
	public String if_phenotype;
	/** ... then exclude this phenotype if ... */
	public String exclude_phenotype;
	/** ... this distance away or less */
	public int num_pixels;
	
	public String getIf_phenotype() {
		return if_phenotype;
	}
	public void setIf_phenotype(String if_phenotype) {
		this.if_phenotype = if_phenotype;
	}
	public String getExclude_phenotype() {
		return exclude_phenotype;
	}
	public void setExclude_phenotype(String exclude_phenotype) {
		this.exclude_phenotype = exclude_phenotype;
	}
	public int getNum_pixels() {
		return num_pixels;
	}
	public void setNum_pixels(int num_pixels) {
		this.num_pixels = num_pixels;
	}
}