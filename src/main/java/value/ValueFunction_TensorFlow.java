package value;

import com.google.common.collect.Iterables;
import data.LoadStateStatistics;
import tflowtools.TrainableNetwork;
import tree.INode;
import tree.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public abstract class ValueFunction_TensorFlow implements IValueFunction {

    /**
     * Input layer size.
     */
    private final int inputSize;

    /**
     * Output layer size.
     */
    private final int outputSize;

    /**
     * TensorFlow neural network used to predict value.
     */
    TrainableNetwork network;

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
    boolean verbose = true;

    public LoadStateStatistics.StateStatistics stateStats;
    {
        try {
            stateStats = LoadStateStatistics.loadStatsFromFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructor which also creates a new TensorFlow model.
     * @param fileName Name of the .pb file (without extension) to be created.
     * @param inputSize 1D size of the input layer.
     * @param outputSize 1D size of the output layer.
     * @param hiddenLayerSizes Size of the fully-connected interior layers.
     * @param additionalArgs Additional arguments used when creating the net (see {@link TrainableNetwork}.
     * @throws FileNotFoundException Occurs when the file is not created successfully.
     */
    ValueFunction_TensorFlow(String fileName, int inputSize, int outputSize, List<Integer> hiddenLayerSizes,
                             List<String> additionalArgs) throws FileNotFoundException {
        this.inputSize = inputSize;
        this.outputSize = outputSize;

        // Supplement the hidden layer sizes with the input and output sizes.
        List<Integer> allLayerSizes = new ArrayList<>(hiddenLayerSizes);
        allLayerSizes.add(0, inputSize);
        allLayerSizes.add(outputSize);

        network = TrainableNetwork.makeNewNetwork(fileName, allLayerSizes, additionalArgs);
    }

    /**
     * Constructor which uses existing model.
     * @param existingFile A .pb file referring to an existing model.
     * @throws FileNotFoundException Occurs when the specified model file is not found.
     */
    ValueFunction_TensorFlow(File existingFile) throws FileNotFoundException {

        network = new TrainableNetwork(existingFile);
        int[] layerSizes = network.getLayerSizes();
        inputSize = layerSizes[0];
        outputSize = layerSizes[layerSizes.length - 1];
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
        return network.saveCheckpoint(checkpointName);
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

    @Override
    public void update(List<Node> nodes) {
        assert trainingBatchSize > 0;

        batchCount = 0;

        float[][] trainingInput = new float[trainingBatchSize][inputSize];
        float[][] trainingOutput = new float[trainingBatchSize][outputSize];

        // Divide into batches.
        Iterables.partition(nodes, trainingBatchSize).forEach(batch -> {

            // Iterate through the nodes in the batch.
            for (int i = 0; i < batch.size(); i++) {
                Node n = batch.get(i);

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
            float loss = network.trainingStep(trainingInput, trainingOutput, trainingStepsPerBatch);
            if (verbose)
                System.out.println("Loss: " + loss + " Epoch: " + epochCount + ", Batch: " + (++batchCount));
        });

        epochCount++;
    }

    @Override
    public float evaluate(INode node) {
        float[][] input = new float[1][inputSize];
        input[0] = assembleInputFromNode(node);
        float[][] result = network.evaluateInput(input);
        return result[0][0];
    }


    abstract float[] assembleInputFromNode(INode node);

    abstract float[] assembleOutputFromNode(Node node);
}
