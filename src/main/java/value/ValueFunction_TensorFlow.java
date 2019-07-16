package value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Iterables;
import data.LoadStateStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tflowtools.TrainableNetwork;
import tree.node.NodeQWOPBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ValueFunction_TensorFlow_StateOnly.class, name = "tflow_state_only"),
})
public abstract class ValueFunction_TensorFlow implements IValueFunction, AutoCloseable {

    /**
     * Input layer size.
     */
    @JsonIgnore
    public final int inputSize;

    /**
     * Output layer size.
     */
    public final int outputSize;

    /**
     * TensorFlow neural network used to predict value.
     */
    TrainableNetwork network;

    public final String fileName;
    public final List<Integer> hiddenLayerSizes;
    public List<String> additionalNetworkArgs;
    /**
     * Number of nodes to run through training in one shot.
     */
    private int trainingBatchSize = 1000;

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
    private int batchCount;

    /**
     * Sum of losses during one update. Exists for logging.
     */
    private float lossSum;

    LoadStateStatistics.StateStatistics stateStats;
    {
        try {
            stateStats = LoadStateStatistics.loadStatsFromFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static Logger logger = LogManager.getLogger(ValueFunction_TensorFlow.class);

    /**
     * Constructor which also creates a new TensorFlow model.
     * @param fileName Name of the file to be created.
     * @param inputSize Defines the input dimension of the net.
     * @param outputSize 1D size of the output layer.
     * @param hiddenLayerSizes Size of the fully-connected interior layers.
     * @param additionalNetworkArgs Additional arguments used when creating the net (see {@link TrainableNetwork}.
     * @throws FileNotFoundException Occurs when the file is not created successfully.
     */
    ValueFunction_TensorFlow(@JsonProperty("fileName") String fileName,
                             int inputSize,
                             @JsonProperty("outputSize") int outputSize,
                             @JsonProperty("hiddenLayerSizes") List<Integer> hiddenLayerSizes,
                             @JsonProperty("additionalNetworkArgs") List<String> additionalNetworkArgs,
                             @JsonProperty("activeCheckpoint") String activeCheckpoint) throws IOException {
        logger.info("Making a new network for the value function.");
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.fileName = fileName;
        this.hiddenLayerSizes = hiddenLayerSizes;
        // Supplement the hidden layer sizes with the input and output sizes.
        List<Integer> allLayerSizes = new ArrayList<>(hiddenLayerSizes);
        allLayerSizes.add(0, inputSize);
        allLayerSizes.add(outputSize);
        this.additionalNetworkArgs = additionalNetworkArgs;

        network = TrainableNetwork.makeNewNetwork(fileName, allLayerSizes, additionalNetworkArgs);
        // Load checkpoint if provided.
        if (activeCheckpoint != null && !activeCheckpoint.isEmpty()) {
            loadCheckpoint(activeCheckpoint);
        }
    }

    /**
     * Constructor which uses existing model.
     * @param existingModel A .pb file referring to an existing model.
     * @throws FileNotFoundException Occurs when the specified model file is not found.
     */
    ValueFunction_TensorFlow(File existingModel) throws FileNotFoundException {
        logger.info("Loading existing network for the value function.");
        fileName = existingModel.getPath();
        network = new TrainableNetwork(existingModel);
        int[] layerSizes = network.getLayerSizes();
        hiddenLayerSizes = new ArrayList<>();
        for (int i = 1; i < layerSizes.length - 1; i++) {
            hiddenLayerSizes.add(layerSizes[i]);
        }
        inputSize = layerSizes[0];
        outputSize = layerSizes[layerSizes.length - 1];
    }

    /**
     * Existing network. It will not be validated. Mostly for use when copying.
     * @param network Existing value network.
     */
    ValueFunction_TensorFlow(TrainableNetwork network) {
        logger.info("Using provided network for the value function.");
        this.fileName = network.getGraphDefinitionFile().getPath();
        int[] layerSizes = network.getLayerSizes();
        assert layerSizes.length >= 2;
        inputSize = layerSizes[0];
        hiddenLayerSizes = new ArrayList<>();
        outputSize = layerSizes[layerSizes.length - 1];
        for (int i = 1; i < layerSizes.length - 1; i++) {
            hiddenLayerSizes.add(layerSizes[i]);
        }
        this.network = network;
    }

    /**
     * Load a checkpoint file for the neural network. This should be in the checkpoints directory, and should not
     * include any file extensions.
     * @param checkpointName Name of the checkpoint to load.
     */
    public void loadCheckpoint(String checkpointName) throws IOException {
        assert !checkpointName.isEmpty();
        network.loadCheckpoint(checkpointName);
    }

    /**
     * Save a checkpoint file for the neural networks (basically all weights and biases). Directory automatically
     * chosen.
     * @param checkpointName Name of the checkpoint file. Do not include file extension or directory.
     * @return A list of checkpoint with that name.
     */
    public List<File> saveCheckpoint(String checkpointName) throws IOException {
        assert !checkpointName.isEmpty();
        return network.saveCheckpoint(checkpointName);
    }

    /**
     * Get the file containing the tensorflow definition of the neural network.
     * @return The .pb file holding the graph definition. Does not include weights.
     */
    @SuppressWarnings("unused")
    @JsonIgnore
    public File getGraphDefinitionFile() {
        return network.getGraphDefinitionFile();
    }

    @JsonProperty("activeCheckpoint")
    public String getActiveCheckpoint() {
        return network.getActiveCheckpoint();
    }

    /**
     * Set the number of examples fed in per batch during training.
     * @param batchSize Size of each batch. In number of examples.
     */
    public void setTrainingBatchSize(int batchSize) {
        logger.info("Training batch size set to " + batchSize + " samples.");
        trainingBatchSize = batchSize;
    }

    /**
     * Set the number of training iterations per batch.
     * @param stepsPerBatch Number of training steps taken per batch fed in.
     */
    public void setTrainingStepsPerBatch(int stepsPerBatch) {
        logger.info("Training iterations per batch set to " + stepsPerBatch + ".");
        trainingStepsPerBatch = stepsPerBatch;
    }

    @Override
    public void update(List<? extends NodeQWOPBase<?>> nodes) {
        assert trainingBatchSize > 0;

        batchCount = 0;
        lossSum = 0f;

        float[][] trainingInput = new float[trainingBatchSize][inputSize];
        float[][] trainingOutput = new float[trainingBatchSize][outputSize];

        long startTime = System.currentTimeMillis();

        logger.info("Beginning value function update containing " + nodes.size() + " samples divided into batches of " + trainingBatchSize);

        // Divide into batches.
        Iterables.partition(nodes, trainingBatchSize).forEach(batch -> {

            // Iterate through the nodes in the batch.
            for (int i = 0; i < batch.size(); i++) {
                NodeQWOPBase<?> n = batch.get(i);

                // Don't include root node since it doesn't have a parent.
                if (n.getParent() == null) {
                    continue;
                }
                // Get state.
                float[] input = assembleInputFromNode(n);
                assert input.length == inputSize;

                float[] output = assembleOutputFromNode(n);
                assert output.length == outputSize;

                trainingInput[i] = input;
                trainingOutput[i] = output;
            }
            lossSum += network.trainingStep(trainingInput, trainingOutput, trainingStepsPerBatch);
            batchCount++;
        });
        logger.info("Update complete. Epoch: " + epochCount + ". Batches this epoch: " + batchCount + ". Average " +
                "loss: " + (lossSum / (float) batchCount) + ". Total time elapsed: " + ((System.currentTimeMillis() - startTime)/100)/10f);
        epochCount++;
    }

    @Override
    public float evaluate(NodeQWOPBase<?> node) {
        float[][] input = new float[1][inputSize];
        input[0] = assembleInputFromNode(node);
        float[][] result = network.evaluateInput(input);
        return result[0][0];
    }

    abstract float[] assembleInputFromNode(NodeQWOPBase<?> node);

    abstract float[] assembleOutputFromNode(NodeQWOPBase<?> node);

    @Override
    public void close() {
        network.close();
    }
}
