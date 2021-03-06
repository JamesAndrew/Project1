
package classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Solver using Decision Tree Classification, specifically the Iterative Dichotomiser 3
 * Leaf Nodes:      Classification to give the query data point
 * Non-leaf Nodes:  Decision node (attribute test at each branch)
 * 
 * @author David
 */
public class ID3 extends Categorizer
{
    // other class properties
    int[][] foldResult;         // stores the confusion matrix for current run
    int[][] pruneResult;        // confusion matrix for deciding to keep a prune or not
    ID3Node rootNode;           // The root node of the ID3 Decision tree that is built after Train() runs
    int[] numClassificationsFound;

    /**
     * Some constructor logic defined in abstract class
     * @param trainingFolds
     * @param testingFold
     */
    public ID3(DataSet[] trainingFolds, DataSet testingFold)
    {
        super(trainingFolds, testingFold);
        categorizerName = "ID3";
        
        // foldResult and prune result is an (n x n) matrix where n = number of classifications
        int matrixSize = trainingSet.getNumClassifications();
        
        foldResult = new int[matrixSize][];
        for (int i = 0; i < foldResult.length; i++)
        {
            foldResult[i] = new int[matrixSize];
        }
        pruneResult = new int[matrixSize][];
        for (int i = 0; i < foldResult.length; i++)
        {
            pruneResult[i] = new int[matrixSize];
        }
    }
    
    /**
     * Training builds the tree from fixed, known examples.
     * This method sets up the initial state needed and then called
     * the recursive ID3 procedure.
     * 
     * 
     */
    @Override
    public void Train() 
    {
        ArrayList<Integer> features = trainingSet.getFeaturesList();
        
        // build tree until it cannot be built any further
        rootNode = id3_Recursive(new ID3Node(), trainingSet, features);
        
        // then prune
        rootNode.setIsMasterRoot(true);
        reducedErrorPruning(rootNode, null, -1);
    }

    /**
     * Run each testingFold data point and assign it a classification by running it through the decision tree
     * 
     * @return the classification results as a confusion matrix
     */
    @Override
    public int[][] Test() 
    {
        // for each point in the testing fold
        for (int i = 0; i < testingFold.getVectors().length; i++)
        {
            Vector currentPoint = testingFold.getVectors()[i];
            
            // traverse the decision tree until a value is assigned
            int classification = traverseTree(currentPoint);
            
            // send the classification result to the foldResult statistic array
            addResult(currentPoint.classification(), classification);            
        }
        
        return foldResult;
    }
    private int[][] pruningTest() 
    {
        // clear result array
        for (int i = 0; i < pruneResult.length; i++)
        {
            for (int j = 0; j < pruneResult[i].length; j++)
            {
                pruneResult[i][j] = 0;
            }
        }
        
        // for each point in the testing fold
        for (int i = 0; i < testingFold.getVectors().length; i++)
        {
            Vector currentPoint = testingFold.getVectors()[i];
            
            // traverse the decision tree until a value is assigned
            int classification = traverseTree(currentPoint);
            
            // send the classification result to the foldResult statistic array
            addPruneResult(currentPoint.classification(), classification);            
        }
        
        return pruneResult;
    }
    
    /**
     * ID3 recursive algorithm
     * 
     * @param node : the current subtree root node
     * @param S : The current data (sub)set
     * @param remainingFeatures : Set of allowed features to interact with
     * @return : the current node as a root
     */
    private ID3Node id3_Recursive(ID3Node root, DataSet S, ArrayList<Integer> remainingFeatures)
    {
        // Base Cases //
        // if all the values in S have the same classification, assign root that classification label and return
        if (S.getNumClassifications() == 1)
        {
            int leafValue = S.getVectors()[0].classification();
            root.setNodeValue(leafValue);
            return root;
        }
        // if there are no remaining features to branch off of, assign root the majority category 
        // amongst data points still in S 
        if (remainingFeatures.isEmpty())
        {
            int leafValue = S.getMajorityClassification();
            root.setNodeValue(leafValue);
            return root;
        }
        
        // else begin recursive tree building loop... //
        // calculate the feature that best classifies the data
        int bestFeature = calculateMaxGainFeature(S, remainingFeatures);
        // and assign to root
        root.setNodeValue(bestFeature);
        // for each value 'bestFeature' can be...
        int[] bestFeatureValues = S.getValuesOfAFeature(bestFeature);
        for (int value : bestFeatureValues)
        {
            DataSet featureValuesSubset = S.getSubsetByFeatureValue(bestFeature, value);
            
            // if Examples(V_i) is empty...
            if (featureValuesSubset.getVectors().length == 0)
            {
                // make a new branch and node with classification lable of class majority in current set
                int childNodeValue = S.getMajorityClassification();
                root.getChildren().put(value, new ID3Node(childNodeValue));
                
                // return because leaf node known
                return root;
            }
            // otherwise if not empty, generate branch and subtree 
            else
            {
                // the branch has the value 'value' (of one the feature-values of current best feature)
                // and child node initially has no data
                root.getChildren().put(value, new ID3Node());
                remainingFeatures.remove((Integer)bestFeature);
                id3_Recursive(root.getChildren().get(value), featureValuesSubset, remainingFeatures);
            }
        }
        
        return root;
    }
    
    /**
     * Calculates the information gain for each feature in S and returns
     * the feature value with the highest gain
     * 
     * @param S : the current data set 
     * @return : the feature value with the highest gain
     */
    private int calculateMaxGainFeature(DataSet S, ArrayList<Integer> remainingFeatures)
    {
        double maxGain = -100.0;
        int maxGainFeature = -1;
        
        // for each remaining feature...
        for (int feature : remainingFeatures)
        {
            // calculate the gain of the data set given that feature. 
            double gain = informationGain(S, feature);
            // and save if max gain thus far
            if (gain > maxGain)
            {
                maxGain = gain;
                maxGainFeature = feature;
            }
        }
        
        if (maxGainFeature == -1) throw new RuntimeException("maxGainFeature was never set to an actual feature");
        return maxGainFeature;
    }
    
    /**
     * Gain(S, A) = Entropy(S) - SUM((|Sv| / |S|) * Entropy(Sv))
     * Sums over each subset of S where S was split by each feature value of feature A
     * 
     * @param S : the current data set
     * @param feature : the feature (value and index in the data set will match)
     *                  used as a parameter in the gain equation 
     * @return : The information gain of example S on feature A
     */
    private double informationGain(DataSet S, int feature)
    {
        double sum = 0.0;
        double totalEntropy = entropy(S);
        int[] featureValues = S.getValuesOfAFeature(feature);
        ArrayList<DataSet> subsetsByFeatureValue = new ArrayList<>();
        
        // put each subset (by feature-value) into the array list
        for (int entry : featureValues)
        {
            DataSet subset = S.getSubsetByFeatureValue(feature, entry);
            subsetsByFeatureValue.add(subset);
        }
        
        // run the summation formula
        for (DataSet subset : subsetsByFeatureValue)
        {
            double proportion = (double)subset.getVectors().length / (double)S.getVectors().length;
            sum += -1 * proportion * entropy(subset);
        }
        
        double gain = totalEntropy + sum;
        return gain;
    }
    
    /**
     * Entropy(S) = SUM(-p(I) log2 p(I))
     * Sums over each classification in S
     * 
     * @param S : the current data set
     * @return : the entropy of S
     */
    private double entropy(DataSet S)
    {
        double entropy = 0.0;
        int[] classifications = S.getClassifcationValues();
        ArrayList<DataSet> subsetsByClassification = new ArrayList<>();
        
        // put each classification subset into the array list
        for (int classification : classifications)
        {
            DataSet subset = S.getSubsetByClassification(classification);
            subsetsByClassification.add(subset);
        }
        
        // run the summation formula
        for (DataSet subset : subsetsByClassification)
        {
            double proportion = (double)subset.getVectors().length / (double)S.getVectors().length;
            entropy += -1 * proportion * (Math.log10(proportion) / Math.log10(2));
        }
        
        return entropy;
    }
    
    /**
     * Prunes the decision tree following the REP method
     * @param node : root of the decision (sub)tree
     * @param nodeParentKey : the key in the parentNode 'children' set that the current node belongs to
     */
    private void reducedErrorPruning(ID3Node node, ID3Node parentNode, int nodeParentKey)
    {
        int timesReturned = 0;
        
        if (node.isLeaf())
        {
            // do nothing
        }
        else
        {
            // have a way to iterate through each child to avoid concurrnet modification exception 
            int[] keySet = new int[node.getChildren().keySet().size()];
            int keySetItr = 0;
            for (int key : node.getChildren().keySet())
            {
                keySet[keySetItr] = key;
                keySetItr++;
            }
            
            // for each child node of the rootNode (sub)tree
            for (int i = 0; i < keySet.length; i++)
            {
                int numChildren = node.getChildren().size();
                int childKey = keySet[i];
                ID3Node child = node.getChildren().get(childKey); 
                
                // iterate depth-first
                reducedErrorPruning(child, node, childKey);
                timesReturned++;
                
                // prune given node isn't the master root node and all children have been visited (this avoids concurrent modification exception)
                if (!(node.isIsMasterRoot()) && timesReturned == numChildren)
                {
                    // get accuracy before prune
                    int[][] beforePrecisionConfMatrix = pruningTest();
                    double beforeAccuracy = Statistics.calculateMatrixTPR(beforePrecisionConfMatrix);

                    // prune (remove children and assign the most common classification value)
                    HashMap<Integer, ID3Node> tempChildren = node.getChildren();
                    HashMap<Integer, ID3Node> tempEmpty = new HashMap<>();
                    int tempFeatValue = node.getNodeValue();
                    int newClassification = calculateSubtreeBestClassification(node);
                    
                    node.setChildren(tempEmpty); 
                    node.setNodeValue(newClassification);
                    
                    // get accuracy after prune
                    int[][] afterPrecisionConfMatrix = pruningTest();
                    double afterAccuracy = Statistics.calculateMatrixTPR(afterPrecisionConfMatrix);

                    // if accuracy is better, keep, otherwise revert
                    if (afterAccuracy > beforeAccuracy)
                    {
                        // keep pruned state
                    }
                    else
                    {
                        node.setNodeValue(tempFeatValue);
                        node.setChildren(tempChildren);
                    }
                }
            }
        }
    }
    
    /**
     * traverse the subtree and return the value of the most common classification found
     * @param node
     * @return 
     */
    private int calculateSubtreeBestClassification(ID3Node node)
    {
        numClassificationsFound = new int[trainingSet.getNumClassifications()];
        
        treeBestClassSubroutine(node);
        int bestClassSize = -1;
        int bestClassIndex = -1;
        for (int i = 0; i < numClassificationsFound.length; i++)
        {
            if (numClassificationsFound[i] > bestClassSize)
            {
                bestClassSize = numClassificationsFound[i];
                bestClassIndex = i;
            }
        }
        if (bestClassSize == -1 || bestClassIndex == -1)
        {
            throw new RuntimeException("best classification never assigned during pruning");
        }
        
        return bestClassIndex;
    }
    private void treeBestClassSubroutine(ID3Node node)
    {
        if (node.isLeaf())
        {
            numClassificationsFound[node.getNodeValue()]++;
        }
        else
        {
            for (Map.Entry<Integer, ID3Node> entry : node.getChildren().entrySet())
            {
                ID3Node child = entry.getValue();
                treeBestClassSubroutine(child);
            }
        }
    }
    
    /**
     * Follows the appropriate path through the decision tree until a leaf node classification is reached
     * 
     * @param currentPoint : The training data vector point (does have the known classification as its first entry)
     * @return : the classification to assign to the input vector as a result of tree traversal 
     */
    private int traverseTree(Vector currentPoint)
    {
        ID3Node currentNode = rootNode;
        
        // traverse until current node is a leaf node
        while (!(currentNode.isLeaf()))
        {
            // the branch path to take is the feature-value of currentPoint for the feature that currentNode is
            int currentFeature = currentNode.getNodeValue();
            int branchPath = currentPoint.getFeatureValue(currentFeature);
            
            if (!(currentNode.getChildren().containsKey(branchPath)))
            {
                branchPath = (int)currentNode.getChildren().keySet().toArray()[0];
            }
            
            currentNode = currentNode.getChildren().get(branchPath);
        }
        
        // once at a leaf node, return the classification value that that node contains
        if (!(currentNode.isLeaf())) throw new RuntimeException("Did not traverse to a leaf node");
        return currentNode.getNodeValue();
    }
    
    /**
     * Prints the tree under any provided root node
     * @param rootNode : Root of tree to print
     */
    public void printDecisionTree(ID3Node rootNode)
    {
        int numDashes = 0;
        printDecisionTreeRecursion(rootNode, numDashes);
        System.out.println();
        
    }
    private void printDecisionTreeRecursion(ID3Node node, int dashes)
    {
        System.out.format("%s", node.printValue());
        System.out.println();
        
        if (node.getChildren().isEmpty())
        {
            // do nothing
        }
        else
        {
            for (Map.Entry<Integer, ID3Node> entry : node.getChildren().entrySet())
            {
                if (entry.getValue().getChildren().isEmpty())
                {
                    System.out.format("%sB(%d)->%s%n", printDashes(dashes+1), entry.getKey(), entry.getValue().printValue());
                }
                else
                {
                    System.out.format("%sB(%d)->", printDashes(dashes+1), entry.getKey());
                    printDecisionTreeRecursion(entry.getValue(), dashes+1);
                }
            }
        }
    }
    private String printDashes(int num)
    {
        String dashes = "";
        for (int i = 0; i < num; i++)
        {
            dashes = dashes + "-";
        }
        return dashes;
    }

    /**
     * Adds a classification result to the confusion matrix
     * @param expected : expected class
     * @param actual  : actual class
     */
    private void addResult(int expected, int actual)
    {
        foldResult[expected][actual]++;
    }
    private void addPruneResult(int expected, int actual)
    {
        pruneResult[expected][actual]++;
    }
}
