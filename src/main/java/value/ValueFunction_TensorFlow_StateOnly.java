package value;

import actions.Action;
import com.google.common.collect.Iterables;
import data.LoadStateStatistics;
import tflowtools.TrainableNetwork;
import tree.Node;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class ValueFunction_TensorFlow_StateOnly implements IValueFunction {

    private static final int STATE_SIZE = 72;
    private static final int VALUE_SIZE = 1;

    /**
     * TensorFlow neural network used to predict value.
     */
    private TrainableNetwork network;

    /**
     * Number of nodes to run through training in one shot.
     */
    private int trainingBatchSize = 100;

    /**
     * Number of training epochs completed since the creation of this object.
     */
    private int epochCount = 0;

    /**
     * Number of training batches completed since the start of the last update.
     */
    private int batchCount = 0;

    /**
     * Should we spit out info after training iterations?
     */
    public boolean verbose = true;

    /**
     * How many training steps to take per batch of the update data.
     */
    private int trainingStepsPerBatch = 1;

    public LoadStateStatistics.StateStatistics stateStats;
    {
        try {
            stateStats = LoadStateStatistics.loadStatsFromFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private ValueFunction_TensorFlow_StateOnly() {}


    @Override
    public Action getMaximizingAction(Node currentNode) {
        return null;
    }

    @Override
    public float evaluate(Node currentNode) {

        if (currentNode.getTreeDepth() <= 0) {
            throw new IllegalArgumentException("Cannot evaluate a node at depth 0 or less.");
        }

        float[][] input = new float[1][STATE_SIZE];
        System.arraycopy(stateStats.standardizeState(currentNode.getState()), 0, input[0], 0, STATE_SIZE);
        float[][] result = network.evaluateInput(input);

        return result[0][0];
    }

    @Override
    public void update(List<Node> nodes) {
        assert trainingBatchSize > 0;

        batchCount = 0;

        float[][] trainingStateArray = new float[trainingBatchSize][STATE_SIZE];
        float[][] value = new float[trainingBatchSize][VALUE_SIZE];

        // Divide into batches.
        Iterables.partition(nodes, trainingBatchSize).forEach(batch -> {

            // Iterate through the nodes in the batch.
            for (int i = 0; i < batch.size(); i++) {
                Node n = batch.get(i);

                // Don't include root node since it doesn't have a parent.
                if (n.getTreeDepth() == 0) {
                    continue;
                }
                // Get state.
                float[] state = stateStats.standardizeState(n.getState());
                System.arraycopy(state, 0, trainingStateArray[i], 0, STATE_SIZE);

                // Tack the action duration onto the end.
                value[i][0] = n.getValue()/n.visitCount.floatValue();

                assert !Float.isNaN(value[i][0]);
            }
            float loss = network.trainingStep(trainingStateArray, value, trainingStepsPerBatch);
            if (verbose)
                System.out.println("Loss: " + loss + " Epoch: " + epochCount + ", Batch: " + (++batchCount));
        });

        epochCount++;
    }

    /**
     * Set the number of examples fed in per batch during training.
     * @param batchSize Size of each batch. In number of examples.
     */
    public void setTrainingBatchSize(int batchSize) {
        trainingBatchSize = batchSize;
    }

    /**
     * Set the number of training iterations per batch.
     * @param stepsPerBatch Number of training steps taken per batch fed in.
     */
    public void setTrainingStepsPerBatch(int stepsPerBatch) {
        trainingStepsPerBatch = stepsPerBatch;
    }

    /**
     * Decide whether loss reports will be printed out during training.
     * @param verbose True -- verbose print out. False -- silent.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }


    public static ValueFunction_TensorFlow_StateOnly makeNew(String fileName, List<Integer> hiddenLayerSizes,
                                                            List<String> additionalArgs) throws FileNotFoundException {
        // Supplement the hidden layer sizes with the input and output sizes.
        List<Integer> allLayerSizes = new ArrayList<>(hiddenLayerSizes);
        allLayerSizes.add(0, STATE_SIZE);
        allLayerSizes.add(VALUE_SIZE);

        TrainableNetwork newNetwork = TrainableNetwork.makeNewNetwork(fileName, allLayerSizes, additionalArgs);
        ValueFunction_TensorFlow_StateOnly valFun = new ValueFunction_TensorFlow_StateOnly();
        valFun.network = newNetwork;

        return valFun;
    }

    public static ValueFunction_TensorFlow_StateOnly makeNew(String fileName, List<Integer> hiddenLayerSizes) throws FileNotFoundException {
        return makeNew(fileName, hiddenLayerSizes, new ArrayList<>()); // No additional network command line arguments.
    }
}
