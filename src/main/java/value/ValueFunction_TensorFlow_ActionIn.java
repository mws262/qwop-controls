package value;

import actions.Action;
import actions.ActionSet;
import com.google.common.collect.Iterables;
import tflowtools.TrainableNetwork;
import tree.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * [state, action duration] ----> |NEURAL NET| ----> single scalar value
 *
 * This value function evaluates a Node like this:
 * 1. Get the state from the parent Node.
 * 2. Get the action leading to this node (i.e. its action).
 * 3. Concatenate [state, action] and pass to TensorFlow network.
 * 4. A single scalar value pops out.
 *
 * When using it as a controller, given a node:
 * 1. Take the state at the current node.
 * 2. Get all possible actions leaving this node.
 * 3. Concatenate and pass to TensorFlow.
 * 4. Pick the Action which maximizes the resulting value.
 *
 * @author matt
 */
public class ValueFunction_TensorFlow_ActionIn implements IValueFunction {

    private static final int STATE_SIZE = 72;
    private static final int VALUE_SIZE = 1;
    private static final int ACTION_SIZE = 1;

    /**
     * TensorFlow neural network used to predict value.
     */
    private TrainableNetwork network;

    /**
     * Number of nodes to run through training in one shot.
     */
    public int trainingBatchSize = 100;

    /**
     * How many training steps to take per batch of the update data.
     */
    public int trainingStepsPerBatch = 1;

    /**
     * Number of training epochs completed since the creation of this object.
     */
    private int epochCount = 0;

    /**
     * Number of training batches completed since the start of the last update.
     */
    private int batchCount = 0;

    /**
     * Create a value function from an existing neural network save file. If a new network is needed, call
     * {@link ValueFunction_TensorFlow_ActionIn#makeNew(String, List, List)}. If we want to also load a checkpoint
     * with network weights, call {@link ValueFunction_TensorFlow_ActionIn#loadCheckpoint}
     * @param file
     */
    public ValueFunction_TensorFlow_ActionIn(File file) {
        network = new TrainableNetwork(file);
    }

    private ValueFunction_TensorFlow_ActionIn() {}

    @Override
    public Action getMaximizingAction(Node currentNode) {
        ActionSet actionChoices = Node.potentialActionGenerator.getPotentialChildActionSet(currentNode);

        float[][] input = new float[1][STATE_SIZE + ACTION_SIZE];
        System.arraycopy(currentNode.getState().flattenState(), 0, input[0], 0, STATE_SIZE);

        // Find the action which maximizes the value from this state.
        float maxValue = -Float.MAX_VALUE;
        int maxIndex = -1;

        for (int i = 0; i < actionChoices.size(); i++) {
            input[0][STATE_SIZE + ACTION_SIZE - 1] = actionChoices.get(i).getTimestepsTotal();
            float value = network.evaluateInput(input)[0][0];
            if (value > maxValue) {
                maxValue = value;
                maxIndex = i;
            }
        }

        assert maxIndex >= 0;
        return actionChoices.get(maxIndex);
    }

    @Override
    public float evaluate(Node currentNode) {
        assert currentNode.getTreeDepth() > 0;

        float[][] input = new float[1][STATE_SIZE + ACTION_SIZE];
        System.arraycopy(currentNode.getParent().getState().flattenState(), 0, input[0], 0, STATE_SIZE);
        input[0][STATE_SIZE + ACTION_SIZE - 1] = currentNode.getAction().getTimestepsTotal();
        float[][] result = network.evaluateInput(input);

        return result[0][0];
    }

    @Override
    public void update(List<Node> nodes) {
        assert trainingBatchSize > 0;

        batchCount = 0;

        float[][] trainingStateArray = new float[trainingBatchSize][STATE_SIZE + ACTION_SIZE];
        float[][] value = new float[trainingBatchSize][VALUE_SIZE];

        // Divide into batches.
        Iterables.partition(nodes, trainingBatchSize).forEach(batch -> {

            // Iterate through the nodes in the batch.
            for (int i = 0; i < batch.size(); i++) {
                Node n = batch.get(i);

                // Get state.
                float[] state = n.getParent().getState().flattenState();
                System.arraycopy(state, 0, trainingStateArray[i], 0, STATE_SIZE);

                // Tack the action duration onto the end.
                trainingStateArray[i][STATE_SIZE + ACTION_SIZE - 1] = n.getAction().getTimestepsTotal();
                value[i][0] = n.getValue()/n.visitCount.floatValue();

                assert !Float.isNaN(value[i][0]);
            }
            float loss = network.trainingStep(trainingStateArray, value, trainingStepsPerBatch);
            System.out.println("Loss: " + loss + " Epoch: " + epochCount + ", Batch: " + (++batchCount));
        });

        epochCount++;
    }

    /**
     * Load a checkpoint file for the neural network. This should be in the checkpoints directory, and should not
     * include any file extensions.
     * @param checkpointName Name of the checkpoint to load.
     */
    public void loadCheckpoint(String checkpointName) {
        assert !checkpointName.isEmpty();
        network.loadCheckpoint(checkpointName);
    }

    /**
     * Save a checkpoint file for the neural networks (basically all weights and biases). Directory automatically
     * chosen.
     * @param checkpointName Name of the checkpoint file. Do not include file extension or directory.
     */
    public void saveCheckpoint(String checkpointName) {
        assert !checkpointName.isEmpty();
        network.saveCheckpoint(checkpointName);
    }
    /**
     * Make a new ValueFunction backed by a new TensorFlow neural network.
     * @param fileName Name of the .pb file (without extension) which stores the model definition.
     * @param hiddenLayerSizes Sizes of the fully connected layers in the neural network. Do not include the input
     *                         and output layer sizes, as these are determined by the type of value function.
     * @param additionalArgs Any additional neural network options to be passed along when creating the net.
     * @return A new ValueFunction.
     */
    public static ValueFunction_TensorFlow_ActionIn makeNew(String fileName, List<Integer> hiddenLayerSizes,
                                                     List<String> additionalArgs) {
        // Supplement the hidden layer sizes with the input and output sizes.
        List<Integer> allLayerSizes = new ArrayList<>(hiddenLayerSizes);
        allLayerSizes.add(0, STATE_SIZE + ACTION_SIZE);
        allLayerSizes.add(VALUE_SIZE);

        TrainableNetwork newNetwork = TrainableNetwork.makeNewNetwork(fileName, allLayerSizes, additionalArgs);
        ValueFunction_TensorFlow_ActionIn valFun = new ValueFunction_TensorFlow_ActionIn();
        valFun.network = newNetwork;

        return valFun;
    }

    public static ValueFunction_TensorFlow_ActionIn makeNew(String fileName, List<Integer> hiddenLayerSizes) {
        return makeNew(fileName, hiddenLayerSizes, new ArrayList<>()); // No additional network command line arguments.
    }
}
