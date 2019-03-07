package value;

import actions.Action;
import actions.ActionSet;
import com.google.common.collect.Iterables;
import data.LoadStateStatistics;
import distributions.Distribution_Equal;
import tflowtools.TrainableNetwork;
import tree.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private int trainingBatchSize = 100;

    /**
     * How many training steps to take per batch of the update data.
     */
    private int trainingStepsPerBatch = 1;

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

    public LoadStateStatistics.StateStatistics stateStats;
    {
        try {
            stateStats = LoadStateStatistics.loadStatsFromFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

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
        ActionSet actionChoices;

        // If no action generator is assigned, just use the actions of this node's children.
        if (Node.potentialActionGenerator == null) {
            if (currentNode.getChildCount() == 0) {
                throw new IllegalStateException("Tried to get a maximizing action using a node with no action " +
                        "generator or existing children.");
            }
            Node[] children = currentNode.getChildren();
            List<Action> childActions = Arrays.stream(children).map(Node::getAction).collect(Collectors.toList());
            actionChoices = new ActionSet(new Distribution_Equal());
            actionChoices.addAll(childActions);
        } else {
            actionChoices = Node.potentialActionGenerator.getPotentialChildActionSet(currentNode);
        }

        if (actionChoices.isEmpty() || actionChoices.contains(null)) {
            throw new IllegalStateException("Node has no action generator and this node has no children with actions.");
        }


        float[][] input = new float[1][STATE_SIZE + ACTION_SIZE];
        System.arraycopy(stateStats.standardizeState(currentNode.getState()), 0, input[0], 0, STATE_SIZE);

        // Find the action which maximizes the value from this state.
        float maxValue = -Float.MAX_VALUE;
        Action bestAction = null;
        for (Action action : actionChoices) {
            input[0][STATE_SIZE + ACTION_SIZE - 1] = action.getTimestepsTotal();
            float value = network.evaluateInput(input)[0][0];
            if (value > maxValue) {
                maxValue = value;
                bestAction = action;
            }
        }

        Objects.requireNonNull(bestAction);
        return bestAction;
    }

    @Override
    public float evaluate(Node currentNode) {

        if (currentNode.getTreeDepth() <= 0) {
            throw new IllegalArgumentException("Cannot evaluate a node at depth 0 or less.");
        }

        float[][] input = new float[1][STATE_SIZE + ACTION_SIZE];
        System.arraycopy(stateStats.standardizeState(currentNode.getParent().getState()), 0, input[0], 0, STATE_SIZE);
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

                // Don't include root node since it doesn't have a parent.
                if (n.getTreeDepth() == 0) {
                    continue;
                }
                // Get state.
                float[] state = stateStats.standardizeState(n.getParent().getState());
                System.arraycopy(state, 0, trainingStateArray[i], 0, STATE_SIZE);

                // Tack the action duration onto the end.
                trainingStateArray[i][STATE_SIZE + ACTION_SIZE - 1] = n.getAction().getTimestepsTotal();
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
     * @return A list of checkpoint with that name.
     */
    public List<File> saveCheckpoint(String checkpointName) {
        assert !checkpointName.isEmpty();
        List<File> checkpointFiles = network.saveCheckpoint(checkpointName);
        return checkpointFiles;
    }

    public File getCheckpointPath() {
        return new File(network.checkpointPath);
    }

    /**
     * Get the file containing the tensorflow definition of the neural network.
     * @return The .pb file holding the graph definition. Does not include weights.
     */
    public File getGraphDefinitionFile() {
        return network.getGraphDefinitionFile();
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

    /**
     * Make a new ValueFunction backed by a new TensorFlow neural network.
     * @param fileName Name of the .pb file (without extension) which stores the model definition.
     * @param hiddenLayerSizes Sizes of the fully connected layers in the neural network. Do not include the input
     *                         and output layer sizes, as these are determined by the type of value function.
     * @param additionalArgs Any additional neural network options to be passed along when creating the net.
     * @return A new ValueFunction.
     */
    public static ValueFunction_TensorFlow_ActionIn makeNew(String fileName, List<Integer> hiddenLayerSizes,
                                                     List<String> additionalArgs) throws FileNotFoundException {
        // Supplement the hidden layer sizes with the input and output sizes.
        List<Integer> allLayerSizes = new ArrayList<>(hiddenLayerSizes);
        allLayerSizes.add(0, STATE_SIZE + ACTION_SIZE);
        allLayerSizes.add(VALUE_SIZE);

        TrainableNetwork newNetwork = TrainableNetwork.makeNewNetwork(fileName, allLayerSizes, additionalArgs);
        ValueFunction_TensorFlow_ActionIn valFun = new ValueFunction_TensorFlow_ActionIn();
        valFun.network = newNetwork;

        return valFun;
    }

    public static ValueFunction_TensorFlow_ActionIn makeNew(String fileName, List<Integer> hiddenLayerSizes) throws FileNotFoundException {
        return makeNew(fileName, hiddenLayerSizes, new ArrayList<>()); // No additional network command line arguments.
    }
}
