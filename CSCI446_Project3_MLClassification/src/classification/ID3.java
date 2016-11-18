
package classification;

import java.util.ArrayList;
import java.util.Arrays;
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
    ID3Node rootNode;           // The root node of the ID3 Decision tree that is built after Train() runs

    /**
     * Some constructor logic defined in abstract class
     * @param trainingFolds
     * @param testingFold
     */
    public ID3(DataSet[] trainingFolds, DataSet testingFold)
    {
        super(trainingFolds, testingFold);
        categorizerName = "ID3";
        
        // foldResult is an (n x n) matrix where n = number of classifications
        int matrixSize = trainingSet.getNumClassifications();
        foldResult = new int[matrixSize][];
        for (int i = 0; i < foldResult.length; i++)
        {
            foldResult[i] = new int[matrixSize];
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
        
        System.out.format("About to run ID3 on the following training set:%n%s%n", trainingSet.toString());
        
        rootNode = id3_Recursive(new ID3Node(), trainingSet, features);
        printDecisionTree(rootNode);
    }

    /**
     * 
     * @return the classification results as a confusion matrix
     */
    @Override
    public int[][] Test() 
    {
        return new int[3][];
//        throw new UnsupportedOperationException("Not supported yet."); 
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
        System.out.format("%n== Starting recursive method. ==%n== S: %n%s== remainingFeatures: %s%n", S.toString(), remainingFeatures.toString());
        
        // Base Cases //
        // if all the values in S have the same classification, assign root that classification label and return
        if (S.getNumClassifications() == 1)
        {
            System.out.format("All values in S found to be classification %s. Assigning as leaf node.%n", Arrays.toString(S.getClassifcationValues()));
            int leafValue = S.getVectors()[0].classification();
            root.setNodeValue(leafValue);
            return root;
        }
        // if there are no remaining features to branch off of, assign root the majority category 
        // amongst data points still in S 
        if (remainingFeatures.isEmpty())
        {
            int leafValue = S.getMajorityClassification();
            
            System.out.format("No remaining features left. Assigning current node as leaf with majority classificaton %d%n", leafValue);
            
            root.setNodeValue(leafValue);
            return root;
        }
        
        // else begin recursive tree building loop... //
        // calculate the feature that best classifies the data
        int bestFeature = calculateMaxGainFeature(S, remainingFeatures);
        // and assign to root
        root.setNodeValue(bestFeature);
        
        System.out.format("Assigning current node to have best feature value %d%n%n", bestFeature);
        
        // for each value 'bestFeature' can be...
        int[] bestFeatureValues = S.getValuesOfAFeature(bestFeature);
        
        System.out.format("For each value of best feature %d: (%s)%n", bestFeature, Arrays.toString(bestFeatureValues));
        
        for (int value : bestFeatureValues)
        {
            System.out.format("Current value: %d%n", value);
            DataSet featureValuesSubset = S.getSubsetByFeatureValue(bestFeature, value);
            System.out.format("Subset for data of feature %d with value %d:%n%s", bestFeature, value, featureValuesSubset.toString());
            
            // if Examples(V_i) is empty...
            if (featureValuesSubset.getVectors().length == 0)
            {
                System.out.format("There are no values in S with this feature-value.%n");
                
                // make a new branch and node with classification lable of class majority in current set
                int childNodeValue = S.getMajorityClassification();
                root.getChildren().put(value, new ID3Node(childNodeValue));
                
                System.out.format("Made new branch and leaf node with the majority classification. Branch value: %d, leaf node classification: %d%n", value, childNodeValue);
                
                // return because leaf node known
                return root;
            }
            // otherwise if not empty, generate branch and subtree 
            else
            {
                // the branch has the value 'value' (of one the feature-values of current best feature)
                // and child node initially has no data
                root.getChildren().put(value, new ID3Node());
                System.out.format("Added branch to current node (feature value %d) with value %d%n", root.getNodeValue(), value);
                
                remainingFeatures.remove((Integer)bestFeature);
                System.out.format("Removed %d from the remaining features.%n", bestFeature);
                System.out.format("Running next recursive call.%n");
                
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
        System.out.format("- calculating max gain feature...%n");
        
        double maxGain = Double.MIN_VALUE;
        int maxGainFeature = -1;
        
        // for each remaining feature...
        for (int feature : remainingFeatures)
        {
            System.out.format("- current feature: %d%n", feature);
            // calculate the gain of the data set given that feature. 
            double gain = informationGain(S, feature);
            System.out.format("- gain: %.3f%n", gain);
            // and save if max gain thus far
            if (gain > maxGain)
            {
                System.out.format("- gain was greater, assignming to max gain feature%n");
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
     * Prints the tree under any provided root node
     * @param rootNode : Root of tree to print
     */
    public void printDecisionTree(ID3Node rootNode)
    {
        int numDashes = 0;
        System.out.println("\nPrinting Decision Tree. (F) means feature value, (C) means classification, "
                + "\n(B)-> is the branch feature-value taken to get to the node it points to:\n");
        printDecisionTreeRecursion(rootNode, numDashes);
        
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
                    dashes++;
                    System.out.format("%sB(%d)->", printDashes(dashes), entry.getKey());
                    printDecisionTreeRecursion(entry.getValue(), dashes);
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
}
