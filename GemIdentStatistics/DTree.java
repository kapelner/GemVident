
package GemIdentStatistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import GemIdentClassificationEngine.DatumSetup;
import GemIdentOperations.Run;
import GemIdentView.JProgressBarAndLabel;

/**
 * Creates a decision tree. This class was also expanded to confrom to 
 * the specifications of random forest trees.
 *
 * @author Adam Kapelner
 * 
 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm">Breiman's Random Forests (UC Berkeley)</a>
 */
public class DTree extends Classifier implements Serializable {
	private static final long serialVersionUID = -3724306076596536141L;
	
	/** Instead of checking each index we'll skip every INDEX_SKIP indices unless there's less than MIN_SIZE_TO_CHECK_EACH*/
	private static final int INDEX_SKIP=3;
	/** If there's less than MIN_SIZE_TO_CHECK_EACH points, we'll check each one */
	private static final int MIN_SIZE_TO_CHECK_EACH=10;
	/** If the number of data points is less than MIN_NODE_SIZE, we won't continue splitting, we'll take the majority vote */
	private static final int MIN_NODE_SIZE=5;

	/** This is the root of the Decision Tree, the only thing that is saved during serialization */
	private TreeNode root;
	/** Of the testN, the number that were correctly identified */
	private transient int correct;
	/** an estimate of the importance of each attribute in the data record
	 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm#varimp>Variable Importance</a>
	 */
	private transient int[] importances;
	/** This is a pointer to the Random Forest this decision tree belongs to */
	private transient RandomForest forest;



	/** Serializable happy */
	public DTree(){}
	

	public DTree(DatumSetup datumSetup, JProgressBarAndLabel buildProgress){
		super(datumSetup, buildProgress);
	}
	
	/**
	 * convenience constructor for when it's called for the service of a random forest.
	 * This will wrap the standard constructor and then
	 * it will take care of the specialized building
	 * 
	 * @param raw_data	the raw training data
	 * @param forest	the random forest this decision tree belongs to
	 */
	public DTree(DatumSetup datumSetup, JProgressBarAndLabel buildProgress, ArrayList<int[]> raw_data, RandomForest forest) {
		super(datumSetup, buildProgress);
		super.setData(raw_data);
		this.forest = forest;
		BuildForRandomForestUsingBootstrapSample();
	}


	/**
	 * This constructs a decision tree from a data matrix.
	 * It first creates a bootstrap sample, the train data matrix, as well as the left out records, 
	 * the test data matrix. Then it creates the tree, then calculates the variable importances (not essential)
	 * and then removes the links to the actual data (to save memory)
	 */		
	protected void BuildForRandomForestUsingBootstrapSample(){
		importances = new int[datumSetup.NumberOfFeatures()];
		
		ArrayList<int[]> train=new ArrayList<int[]>(N); //data becomes the "bootstrap" - that's all it knows
		ArrayList<int[]> test=new ArrayList<int[]>();
		
		BootStrapSample(raw_data, train, test);
		correct=0;
		
		root=CreateTree(train);
		CalcTreeVariableImportanceAndError(test);
		FlushData();		
	}
	
	public void Build(){
		root = CreateTree(raw_data);
		FlushData();
		buildProgress.setValue(100); //we're done
	}
	
	/**
	 * Responsible for gauging the error rate of this tree and 
	 * calculating the importance values of each attribute
	 * 
	 * @param test	The left out data matrix
	 */
	private void CalcTreeVariableImportanceAndError(ArrayList<int[]> test) {
		correct=CalcTreeErrorRate(test);
		
		for (int m=0; m < datumSetup.NumberOfFeatures(); m++){
			ArrayList<int[]> data=RandomlyPermuteAttribute(CloneData(test), m);
			int correctAfterPermute=0;
			for (int[] arr:data){
				int prediction=Evaluate(arr);
				if (prediction == GetKlass(arr))
					correctAfterPermute++;
			}
			int diff = correct-correctAfterPermute;
			if (diff > 0)
				importances[m] += diff;
		}		
	}

	/**
	 * Calculates the tree error rate,
	 * displays the error rate to console,
	 * and updates the total forest error rate
	 * 
	 * @param test	the test data matrix
	 * @return	the number correct
	 */
	private int CalcTreeErrorRate(ArrayList<int[]> test){		
		int correct=0;		
		for (int[] record:test){
			int Class=Evaluate(record);
			forest.UpdateOOBEstimate(record,Class);
			if (Class == GetKlass(record))
				correct++;
		}
		
//		double err=1-correct/((double)test.size());
//		System.out.print("\n");
//		System.out.println("of left out data, error rate:"+err);
		return correct;
	}
	/**
	 * This will classify a new data record by using tree
	 * recursion and testing the relevant variable at each node.
	 * 
	 * This is probably the most-used function in all of <b>GemIdent</b>.
	 * It would make sense to inline this in assembly for optimal performance.
	 * 
	 * @param record 	the data record to be classified
	 * @return			the class the data record was classified into
	 */
	public int Evaluate(int[] record){ //localized RF - where it senses error rates locally (the piece of data you're giving it - the images) then it either creates a new RF in that place if need be. On new localities, it tries each RF to see which one has lowest error rate 
		TreeNode evalNode=root;
		
		while (true){
			if (evalNode.isLeaf)
				return evalNode.klass;
			if (record[evalNode.splitAttributeM] <= evalNode.splitValue)
				evalNode=evalNode.left;
			else
				evalNode=evalNode.right;
		}
	}
	/**
	 * Takes a list of data records, and switches the mth attribute across data records.
	 * This is important in order to test the importance of the attribute. If the attribute 
	 * is randomly permuted and the result of the classification is the same, the attribute is
	 * not important to the classification and vice versa.
	 * 
	 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm#varimp">Variable Importance</a>
	 * @param test		The data matrix to be permuted
	 * @param m			The attribute index to be permuted
	 * @return			The data matrix with the mth column randomly permuted
	 */
	private ArrayList<int[]> RandomlyPermuteAttribute(ArrayList<int[]> test,int m){
		int num=test.size()*2;
		for (int i=0;i<num;i++){
			int a=(int)Math.floor(Math.random()*test.size());
			int b=(int)Math.floor(Math.random()*test.size());
			int[] arrA=test.get(a);
			int[] arrB=test.get(b);
			int temp=arrA[m];
			arrA[m]=arrB[m];
			arrB[m]=temp;
		}
		return test;
	}
	/**
	 * Creates a copy of the data matrix
	 * @param data		the data matrix to be copied
	 * @return			the cloned data matrix
	 */
	private ArrayList<int[]> CloneData(ArrayList<int[]> data){
		ArrayList<int[]> clone=new ArrayList<int[]>(data.size());
		int M=data.get(0).length;
		for (int i=0;i<data.size();i++){
			int[] arr=data.get(i);
			int[] arrClone=new int[M];
			for (int j=0;j<M;j++){
				arrClone[j]=arr[j];
			}
			clone.add(arrClone);
		}
		return clone;
	}
	/**
	 * This creates the decision tree according to the specifications of random forest trees. 
	 * 
	 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm#overview">Overview of random forest decision trees</a>
	 * @param train		the training data matrix (a bootstrap sample of the original data)
	 * @return			the TreeNode object that stores information about the parent node of the created tree
	 */
	private TreeNode CreateTree(ArrayList<int[]> train){
		TreeNode root = new TreeNode(train);
		RecursiveSplit(root);
		return root;
	}

	/**
	 * Because Java never passes by reference, this object
	 * is a hack used in order to pass a double value around
	 * by reference.
	 * 
	 * @author Adam Kapelner
	 */
	private class DoubleWrap implements Serializable {
		private static final long serialVersionUID = -9017618157753329621L;
		private double d;
		public DoubleWrap(){}
		public DoubleWrap(double d){
			this.d=d;
		}
		public double getD() {
			return d;
		}
		public void setD(double d) {
			this.d = d;
		}		
	}
	/**
	 * This is the crucial function in tree creation. 
	 * 
	 * <ul>
	 * <li>Step A
	 * Check if this node is a leaf, if so, it will mark isLeaf true
	 * and mark Class with the leaf's class. The function will not
	 * recurse past this point.
	 * </li>
	 * <li>Step B
	 * Create a left and right node and keep their references in
	 * this node's left and right fields. For debugging purposes,
	 * the generation number is also recorded. The {@link RandomForest#Ms Ms} attributes are
	 * now chosen by the {@link #GetVarsToInclude() GetVarsToInclude} function
	 * </li>
	 * <li>Step C
	 * For all Ms variables, first {@link #SortAtAttribute(List,int) sort} the data records by that attribute 
	 * , then look through the values from lowest to 
	 * highest. If value i is not equal to value i+1, record i in the list of "indicesToCheck."
	 * This speeds up the splitting. If the number of indices in indicesToCheck >  MIN_SIZE_TO_CHECK_EACH
	 * then we will only {@link #CheckPosition check} the
	 * entropy at every {@link #INDEX_SKIP INDEX_SKIP} index otherwise, we {@link #CheckPosition check}
	 * the entropy for all. The "E" variable records the entropy and we are trying to find the minimum in which to split on
	 * </li>
	 * <li>Step D
	 * The newly generated left and right nodes are now checked:
	 * If the node has only one record, we mark it as a leaf and set its class equal to
	 * the class of the record. If it has less than {@link #MIN_NODE_SIZE MIN_NODE_SIZE}
	 * records, then we mark it as a leaf and set its class equal to the {@link #GetMajorityKlass(List) majority class}.
	 * If it has more, then we do a manual check on its data records and if all have the same class, then it
	 * is marked as a leaf. If not, then we run {@link #RecursiveSplit RecursiveSplit} on
	 * that node
	 * </li>
	 * </ul>
	 * 
	 * @param parent	The node of the parent
	 */
	private void RecursiveSplit(TreeNode parent){
		if (!parent.isLeaf){

			//-------------------------------Step A
			Integer Klass=CheckIfLeaf(parent.data);
			if (Klass != null){
				parent.isLeaf=true;
				parent.klass=Klass;
//				System.out.print("parent leaf! Class:"+parent.Class+"  ");
//				PrintOutClasses(parent.data);
				return;
			}
			
			//-------------------------------Step B
			int Nsub=parent.data.size();
//			PrintOutClasses(parent.data);			
			
			parent.left=new TreeNode();
//			parent.left.generation=parent.generation+1;
			parent.right=new TreeNode();
//			parent.right.generation=parent.generation+1;
			
			ArrayList<Integer> vars=GetVarsToInclude();
			
			DoubleWrap lowestE=new DoubleWrap(Double.MAX_VALUE);

			//-------------------------------Step C
			for (int m:vars){
				
				SortAtAttribute(parent.data,m);
				
				ArrayList<Integer> indicesToCheck=new ArrayList<Integer>();
				for (int n=1;n<Nsub;n++){
					int classA=GetKlass(parent.data.get(n-1));
					int classB=GetKlass(parent.data.get(n));
					if (classA != classB)
						indicesToCheck.add(n);
				}
				
				if (indicesToCheck.size() == 0){
					parent.isLeaf=true;
					parent.klass=GetKlass(parent.data.get(0));
					continue;
				}
				if (indicesToCheck.size() > MIN_SIZE_TO_CHECK_EACH){
					for (int i=0;i<indicesToCheck.size();i+=INDEX_SKIP){
						CheckPosition(m,indicesToCheck.get(i),Nsub,lowestE,parent);
						if (lowestE.d == 0)
							break;
					}
				}
				else {
					for (int n:indicesToCheck){
						CheckPosition(m,n,Nsub,lowestE,parent);
						if (lowestE.d == 0)
							break;
					}
				}
//				BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
//				System.out.println("************************* lowest e:"+lowestE.d);
//				try {reader.readLine();} catch (IOException e){}
				if (lowestE.d == 0)
					break;
			}
//			System.out.print("\n");
//			System.out.println("split attrubute num:"+parent.splitAttributeM+" at val:"+parent.splitValue+" n:"+parent.data.size()+" ");
//			PrintOutClasses(parent.data);
			//			System.out.println("\nmadeSplit . . .");
//			PrintOutNode(parent," ");
//			PrintOutNode(parent.left,"    ");
//			PrintOutNode(parent.right,"    ");

			//-------------------------------Step D
			if (parent.left.data.size() == 1){
				parent.left.isLeaf=true;
				parent.left.klass=GetKlass(parent.left.data.get(0));							
			}
			else if (parent.left.data.size() < MIN_NODE_SIZE){
				parent.left.isLeaf=true;
				parent.left.klass=GetMajorityKlass(parent.left.data);	
			}
			else {
				Klass=CheckIfLeaf(parent.left.data);
				if (Klass == null){
					parent.left.isLeaf=false;
					parent.left.klass=null;
//					System.out.println("make branch left: m:"+m);
				}
				else {
					parent.left.isLeaf=true;
					parent.left.klass=Klass;
				}
			}
			if (parent.right.data.size() == 1){
				parent.right.isLeaf=true;
				parent.right.klass=GetKlass(parent.right.data.get(0));								
			}
			else if (parent.right.data.size() < MIN_NODE_SIZE){
				parent.right.isLeaf=true;
				parent.right.klass=GetMajorityKlass(parent.right.data);	
			}
			else {
				Klass=CheckIfLeaf(parent.right.data);
				if (Klass == null){
//					System.out.println("make branch right: m:"+m);
					parent.right.isLeaf=false;
					parent.right.klass=null;
				}
				else {
					parent.right.isLeaf=true;
					parent.right.klass=Klass;
				}
			}
			
			if (!parent.left.isLeaf)
				RecursiveSplit(parent.left);
//			else {				
//				System.out.print("left leaf! Class:"+parent.left.Class+"  ");
//				PrintOutClasses(parent.left.data);
//			}
			if (!parent.right.isLeaf)
				RecursiveSplit(parent.right);
//			else {				
//				System.out.print("leaf right! Class:"+parent.right.Class+"  ");
//				PrintOutClasses(parent.right.data);
//			}
		}
	}
	/**
	 * Given a data matrix, return the most popular Y value (the class)
	 * @param data	The data matrix
	 * @return		The most popular class
	 */
	private int GetMajorityKlass(List<int[]> data){
		int[] counts=new int[Run.it.numPhenotypes()];
		for (int[] record:data){
			int Class=GetKlass(record);
			counts[Class]++;
		}
		int index=-99;
		int max=Integer.MIN_VALUE;
		for (int i=0;i<counts.length;i++){
			if (counts[i] > max){
				max=counts[i];
				index=i;
			}				
		}
		return index;
	}

	/**
	 * Checks the {@link #CalcEntropy(double[]) entropy} of an index in a data matrix at a particular attribute (m)
	 * and returns the entropy. If the entropy is lower than the minimum to date (lowestE), it is set to the minimum.
	 * 
	 * The total entropy is calculated by getting the sub-entropy for below the split point and after the split point.
	 * The sub-entropy is calculated by first getting the {@link #GetClassProbs(List) proportion} of each of the classes
	 * in this sub-data matrix. Then the entropy is {@link #CalcEntropy(double[]) calculated}. The lower sub-entropy
	 * and upper sub-entropy are then weight averaged to obtain the total entropy. 
	 * 
	 * @param m				the attribute to split on
	 * @param n				the index to check
	 * @param Nsub			the number of records in the data matrix
	 * @param lowestE		the minimum entropy to date
	 * @param parent		the parent node
	 * @return				the entropy of this split
	 */
	private double CheckPosition(int m,int n,int Nsub,DoubleWrap lowestE,TreeNode parent){
		if (n < 1) //exit conditions
			return 0;
		if (n > Nsub)
			return 0;
		
		List<int[]> lower=GetLower(parent.data,n);
		List<int[]> upper=GetUpper(parent.data,n);
//		if (lower == null)
//			System.out.println("lower list null");	
//		if (upper == null)
//			System.out.println("upper list null");
		double[] pl=GetClassProbs(lower);
		double[] pu=GetClassProbs(upper);
		double eL=CalcEntropy(pl);
		double eU=CalcEntropy(pu);
	
		double e=(eL*lower.size()+eU*upper.size())/((double)Nsub);
//		System.out.println("g:"+parent.generation+" N:"+parent.data.size()+" M:"+datumSetup.NumberOfFeatures()+" Ms:"+RandomForest.Ms+" n:"+n+" m:"+m+" val:"+parent.data.get(n)[m]+"                                                           e:"+e);
//		out.write(m+","+n+","+parent.data.get(n)[m]+","+e+"\n");
		if (e < lowestE.d){			
			lowestE.d=e;
			parent.splitAttributeM=m;
			parent.splitValue=parent.data.get(n)[m];		
			parent.left.data=lower;	
			parent.right.data=upper;
		}
		return e;
	}

	/**
	 * Given a data matrix, check if all the y values are the same. If so,
	 * return that y value, null if not
	 * 
	 * @param data		the data matrix
	 * @return			the common class (null if not common)
	 */
	private Integer CheckIfLeaf(List<int[]> data){
//		System.out.println("checkIfLeaf");
		boolean isLeaf=true;
		int ClassA=GetKlass(data.get(0));
		for (int i=1;i<data.size();i++){			
			int[] recordB=data.get(i);
			if (ClassA != GetKlass(recordB)){
				isLeaf=false;
				break;
			}
		}
		if (isLeaf)
			return GetKlass(data.get(0));
		else
			return null;
	}
	/**
	 * Split a data matrix and return the upper portion
	 * 
	 * @param data		the data matrix to be split
	 * @param nSplit	return all data records above this index in a sub-data matrix
	 * @return			the upper sub-data matrix
	 */
	private List<int[]> GetUpper(List<int[]> data,int nSplit){
		int N=data.size();
		List<int[]> upper=new ArrayList<int[]>(N-nSplit);
		for (int n=nSplit;n<N;n++)
			upper.add(data.get(n));
		return upper;
	}
	/**
	 * Split a data matrix and return the lower portion
	 * 
	 * @param data		the data matrix to be split
	 * @param nSplit	return all data records below this index in a sub-data matrix
	 * @return			the lower sub-data matrix
	 */
	private List<int[]> GetLower(List<int[]> data,int nSplit){
		List<int[]> lower=new ArrayList<int[]>(nSplit);
		for (int n=0;n<nSplit;n++)
			lower.add(data.get(n));
		return lower;
	}
	/**
	 * This class compares two data records by numerically comparing a specified attribute
	 * 
	 * @author Adam Kapelner
	 */
	@SuppressWarnings("unchecked")
	private class AttributeComparator implements Comparator{
		
		/** the specified attribute */
		private int m;
		/**
		 * Create a new comparator
		 * @param m			the attribute in which to compare on
		 */
		public AttributeComparator(int m){
			this.m=m;
		}
		/**
		 * Compare the two data records. They must be of type int[].
		 * 
		 * @param o1		data record A
		 * @param o2		data record B
		 * @return			-1 if A[m] < B[m], 1 if A[m] > B[m], 0 if equal
		 */
		public int compare(Object o1, Object o2){
			int a=((int[])o1)[m];
			int b=((int[])o2)[m];
			if (a < b)
				return -1;
			if (a > b)
				return 1;
			else
				return 0;
		}		
	}
	/**
	 * Sorts a data matrix by an attribute from lowest record to highest record
	 * 
	 * @param data			the data matrix to be sorted
	 * @param m				the attribute to sort on
	 */
	@SuppressWarnings("unchecked")
	private void SortAtAttribute(List<int[]> data,int m){
		Collections.sort(data,new AttributeComparator(m));
	}
	/**
	 * Given a data matrix, return a probabilty mass function representing 
	 * the frequencies of a class in the matrix (the y values)
	 * 
	 * @param records		the data matrix to be examined
	 * @return				the probability mass function
	 */
	private double[] GetClassProbs(List<int[]> records){
		
		double N=records.size();
		
		int[] counts=new int[Run.it.numPhenotypes()];
//		System.out.println("counts:");
//		for (int i:counts)
//			System.out.println(i+" ");
		
		for (int[] record:records)
			counts[GetKlass(record)]++;

		double[] ps=new double[Run.it.numPhenotypes()];
		for (int c=0;c<Run.it.numPhenotypes();c++)
			ps[c]=counts[c]/N;
//		System.out.print("probs:");
//		for (double p:ps)
//			System.out.print(" "+p);
//		System.out.print("\n");
		return ps;
	}
	/** ln(2) */
	private static final double logoftwo=Math.log(2);
	/**
	 * Given a probability mass function indicating the frequencies of 
	 * class representation, calculate an "entropy" value using the method
	 * in Tan Steinbach Kumar's "Data Mining" textbook
	 * 
	 * @param ps			the probability mass function
	 * @return				the entropy value calculated
	 */
	private double CalcEntropy(double[] ps){
		double e=0;		
		for (double p:ps){
			if (p != 0) //otherwise it will divide by zero - see TSK p159
				e+=p*Math.log(p)/logoftwo;
		}
		return -e; //according to TSK p158
	}
	/**
	 * Of the M attributes, select {@link RandomForest#Ms Ms} at random.
	 * If the DTree is being created independently of a random forest, it's all the variables
	 * 
	 * @return		The list of the Ms attributes' indices
	 */
	private ArrayList<Integer> GetVarsToInclude() {
		if (forest == null){ //vanilla DTree
			//return em all
			ArrayList<Integer> shortRecord=new ArrayList<Integer>(datumSetup.NumberOfFeatures());
			for (int i=0;i<datumSetup.NumberOfFeatures();i++){
				shortRecord.add(i);
			}
			return shortRecord;			
		}
		else {
			boolean[] whichVarsToInclude=new boolean[datumSetup.NumberOfFeatures()];
	
			for (int i=0;i<datumSetup.NumberOfFeatures();i++)
				whichVarsToInclude[i]=false;
			
			while (true){
				int a=(int)Math.floor(Math.random()*datumSetup.NumberOfFeatures());
				whichVarsToInclude[a]=true;
				int N=0;
				for (int i=0;i<datumSetup.NumberOfFeatures();i++)
					if (whichVarsToInclude[i])
						N++;
				if (N == forest.getMs())
					break;
			}
			
			ArrayList<Integer> shortRecord=new ArrayList<Integer>(forest.getMs());
			
			for (int i=0;i<datumSetup.NumberOfFeatures();i++){
				if (whichVarsToInclude[i]){
					shortRecord.add(i);
				}
			}
			return shortRecord;
		}
		
	}

	/**
	 * Create a boostrap sample of a data matrix
	 * 
	 * @param data		the data matrix to be sampled
	 * @param train		the bootstrap sample
	 * @param test		the records that are absent in the bootstrap sample
	 */
	private void BootStrapSample(ArrayList<int[]> data,ArrayList<int[]> train,ArrayList<int[]> test){
		ArrayList<Integer> indices=new ArrayList<Integer>(N);
		for (int n=0;n<N;n++)
			indices.add((int)Math.floor(Math.random()*N));
		ArrayList<Boolean> in=new ArrayList<Boolean>(N);
		for (int n=0;n<N;n++)
			in.add(false); //have to initialize it first
		for (int num:indices){
			train.add((data.get(num)).clone());
			in.set(num,true);
		}
		for (int i=0;i<N;i++)
			if (!in.get(i))
				test.add((data.get(i)).clone());
		
//		System.out.println("bootstrap N:"+N+" size of bootstrap:"+bootstrap.size());
	}
	protected void FlushData(){
		FlushNodeData(root);
	}
	/**
	 * Recursively deletes all data records from the tree. This is run after the tree
	 * has been computed and can stand alone to classify incoming data.
	 * 
	 * @param node		initially, the root node of the tree
	 */
	private void FlushNodeData(TreeNode node){
		node.data=null;
		if (node.left != null)
			FlushNodeData(node.left);
		if (node.right != null)
			FlushNodeData(node.right);
	}
	
//	// possible to clone trees
//	private DTree(){}
//	public DTree clone(){
//		DTree copy=new DTree();
//		copy.root=root.clone();
//		return copy;
//	}

	/**
	 * Get the importance level of attribute m for this tree
	 */
	public int getImportanceLevel(int m){
		return importances[m];
	}
//	private void PrintOutNode(TreeNode parent,String init){
//		try {
//			System.out.println(init+"node: left"+parent.left.toString());
//		} catch (Exception e){
//			System.out.println(init+"node: left null");
//		}
//		try {
//			System.out.println(init+" right:"+parent.right.toString());
//		} catch (Exception e){
//			System.out.println(init+"node: right null");
//		}
//		try {
//			System.out.println(init+" isleaf:"+parent.isLeaf);
//		} catch (Exception e){}
//		try {
//			System.out.println(init+" splitAtrr:"+parent.splitAttributeM);
//		} catch (Exception e){}
//		try {
//			System.out.println(init+" splitval:"+parent.splitValue);
//		} catch (Exception e){}
//		try {
//			System.out.println(init+" class:"+parent.Class);
//		} catch (Exception e){}
//		try {
//			System.out.println(init+" data size:"+parent.data.size());
//			PrintOutClasses(parent.data);
//		} catch (Exception e){
//			System.out.println(init+" data: null");
//		}		
//	}
//	private void PrintOutClasses(List<int[]> data){
//		try {
//			System.out.print(" (n="+data.size()+") ");
//			for (int[] record:data)
//				System.out.print(GetClass(record));
//			System.out.print("\n");		
//		}
//		catch (Exception e){
//			System.out.println("PrintOutClasses: data null");	
//		}
//	}
//	public static void PrintBoolArray(boolean[] b) {
//		System.out.print("vars to include: ");
//		for (int i=0;i<b.length;i++)
//			if (b[i])
//				System.out.print(i+" ");
//		System.out.print("\n\n");		
//	}
//
//	public static void PrintIntArray(List<int[]> lower) {
//		System.out.println("tree");
//		for (int i=0;i<lower.size();i++){
//			int[] record=lower.get(i);
//			for (int j=0;j<record.length;j++){
//				System.out.print(record[j]+" ");
//			}
//			System.out.print("\n");
//		}
//		System.out.print("\n");
//		System.out.print("\n");
//	}

	public TreeNode getRoot() {
		return root;
	}

	public void setRoot(TreeNode root) {
		this.root = root;
	}
	
	public void StopBuilding() {}	
}