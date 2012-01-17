package GemIdentStatistics;

import java.io.Serializable;
import java.util.List;


/**
 * A dumb struct to store information about a 
 * node in the decision tree.
 * 
 * @author Adam Kapelner
 */
public class TreeNode implements Cloneable, Serializable {
	private static final long serialVersionUID = 8632232129710601363L;
	
	/** is this node a terminal leaf? */
	public boolean isLeaf;
	/** the left daughter node */
	public TreeNode left;
	/** the right daughter node */
	public TreeNode right;
	/** the attribute this node makes a decision on */
	public int splitAttributeM;
	/** the value this node makes a decision on */
	public int splitValue;
	/** the generation of this node from the top node (only useful for debugging */
	public int generation;
	/** if this is a leaf node, then the result of the classification, otherwise null */
	public Integer klass;
	/** the remaining data records at this point in the tree construction (freed after tree is constructed) */
	public transient List<int[]> data;		
	
	/** initializes default values */
	public TreeNode(){
		splitAttributeM=-99;
		splitValue=-99;
//		generation=1;
	}
	public TreeNode(List<int[]> data){
		this();
		this.data = data;
	}
	/** clones this node */
	public TreeNode clone(){ //"data" element always null in clone
		TreeNode copy=new TreeNode();
		copy.isLeaf=isLeaf;
		if (left != null) //otherwise null
			copy.left=left.clone();
		if (right != null) //otherwise null
			copy.right=right.clone();
		copy.splitAttributeM=splitAttributeM;
		copy.klass=klass;
		copy.splitValue=splitValue;
		return copy;
	}
	public boolean isLeaf() {
		return isLeaf;
	}
	public void setLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
	}
	public TreeNode getLeft() {
		return left;
	}
	public void setLeft(TreeNode left) {
		this.left = left;
	}
	public TreeNode getRight() {
		return right;
	}
	public void setRight(TreeNode right) {
		this.right = right;
	}
	public int getSplitAttributeM() {
		return splitAttributeM;
	}
	public void setSplitAttributeM(int splitAttributeM) {
		this.splitAttributeM = splitAttributeM;
	}
	public int getSplitValue() {
		return splitValue;
	}
	public void setSplitValue(int splitValue) {
		this.splitValue = splitValue;
	}
	public int getGeneration() {
		return generation;
	}
	public void setGeneration(int generation) {
		this.generation = generation;
	}
	public Integer getKlass() {
		return klass;
	}
	public void setKlass(Integer klass) {
		this.klass = klass;
	}
}