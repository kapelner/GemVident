package GemIdentCentroidFinding.ViaSegmentation;

import org.jgrapht.graph.DefaultWeightedEdge;


@SuppressWarnings("unchecked")
public class DefaultWeightedComparableEdge extends DefaultWeightedEdge implements Comparable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5983244925821746513L;

	public int compareTo(Object o) {
		int val = ((Integer) hashCode()).compareTo((Integer) o.hashCode());
		assert(!(hashCode() == o.hashCode() && this != o));

		return val;
	}

}
