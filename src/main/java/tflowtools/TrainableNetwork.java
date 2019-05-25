package tflowtools;

import org.tensorflow.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A wrapper for creating, training, and evaluating TensorFlow models. Normally the syntax is a pain in Java, so this
 * just keeps me from having to remember all of it.
 *
 * @author matt
 */
public class TrainableNetwork {

    /**
     * Networks are not created in Java. Java calls a python script defined here to create the network.
     */
    public static final String pythonGraphCreatorScript = "python/java_value_function/create_generic_graph.py";

    /**
     * Default location from which to save/load Tensorflow models.
     */
    public static final String graphPath = "src/main/resources/tflow_models/";

    /**
     * Default location from which to save/load Tensorflow checkpoint files.
     */
    public String checkpointPath = "src/main/resources/tflow_models/checkpoints";

    /**
     * .pb file defining the structure (but not yet weights) of the network.
     */
    private final File graphDefinition;

    /**
     * Loaded Tensorflow graph definition.
     */
    private final Graph graph;

    /**
     * Loaded Tensorflow session.
     */
    private final Session session;

    /**
     * Send Python TensorFlow output to console? Tests don't like this, and it kind of clutters up stuff.
     */
    public static boolean tflowDebugOutput = false;

    /**
     * Create a new wrapper for an existing Tensorflow graph.
     *
     * @param graphDefinition Graph definition file.
     */
    public TrainableNetwork(File graphDefinition) throws FileNotFoundException {

        if (!graphDefinition.exists() || !graphDefinition.isFile())
            throw new FileNotFoundException("Unable to locate the specified model file.");

        this.graphDefinition = graphDefinition;

        // Begin loading the structure of the model as defined by the graph file.
        byte[] graphDef = null;
        try {
            graphDef = Files.readAllBytes(graphDefinition.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Put the graph together and potentially load a previous checkpoint.
        graph = new Graph();
        session = new Session(graph);
        graph.importGraphDef(graphDef);

        // Initialize
        session.runner().addTarget("init").run(); // Could be removed if it proves too slow and we only care
        // about loading rather than initializing from scratch.
    }

    /**
     * Run training on a set of data.
     *
     * @param inputs One or more sets of inputs. First dimension is the example number. Second dimension is the size of
     *               each example.
     * @param desiredOutputs Outputs which we want inputs to produce.
     * @param steps Number of training steps (some form of gradient descent) to use on this set of inputs.
     * @return The loss of the last step performed (smaller is better).
     */
    public float trainingStep(float[][] inputs, float[][] desiredOutputs, int steps) {
        Tensor<Float> input = Tensors.create(inputs);
        Tensor<Float> value_out = Tensors.create(desiredOutputs);
        Tensor<Float> output = null;
        for (int i = 0; i < steps; i++) {
            output = session.runner().feed("input", input).feed("output_target", value_out).addTarget("train").fetch(
                    "loss").run().get(0).expect(Float.class);
        }
        assert output != null;
//        float[] outputStuff = new float[inputs.length];
//        output.copyTo(outputStuff);
        return output.floatValue(); // Could be problematic with softmax which doesn't spit out a single value.
    }

    /**
     * Save training progress (i.e. weights and biases) to specific checkpoint file. Will still be saved to the
     * checkpoint directory. Do not include file extension.
     * @return A list of the files in the save directory matching the given checkpoint name. Note that we aren't
     * doing strict checking on the number of files. If you suspect that some other source is adding similarly-named
     * files, check this.
     */
    public List<File> saveCheckpoint(String checkpointName) {
        assert !checkpointName.isEmpty();

        Tensor<String> checkpointTensor = Tensors.create(Paths.get(checkpointPath, checkpointName).toString());
        session.runner().feed("save/Const", checkpointTensor).addTarget("save/control_dependency").run();

        File checkPointDirectory = new File(checkpointPath);
        assert checkPointDirectory.isDirectory();
        assert checkPointDirectory.exists();

        File[] files = checkPointDirectory.listFiles();

        // Also report the files created.
        List<File> checkpointFiles = new ArrayList<>();
        for (File file : Objects.requireNonNull(files)) {
            if (file.getName().contains(checkpointName)) {
                checkpointFiles.add(file);
            }
        }
        return checkpointFiles;
    }

    /**
     * Load a checkpoint file. Must match the graph loaded. Name does not need to include path or file extension.
     * @param checkpointName Name of the checkpoint to load.
     */
    public void loadCheckpoint(String checkpointName) {
        Tensor<String> checkpointTensor = Tensors.create(Paths.get(checkpointPath, checkpointName).toString());
        session.runner().feed("save/Const", checkpointTensor).addTarget("save/restore_all").run();
    }

    /**
     * Evaluate one or more inputs to the net. Does not perform any training.
     * @param inputs One or more inputs to feed in. For a single example, the first dimension should be 1.
     * @return The evaluated values of the inputs.
     */
    public float[][] evaluateInput(float[][] inputs) {
        Tensor<Float> inputTensor = Tensors.create(inputs);
        Tensor<Float> outputTensor =
                session.runner().feed("input", inputTensor).fetch("output").run().get(0).expect(Float.class);

        long[] outputShape = outputTensor.shape();
        float[][] output = new float[(int) outputShape[0]][(int) outputShape[1]];
        outputTensor.copyTo(output);
        return output;
    }

    /**
     * Print all operations in the graph for debugging.
     */
    public void printGraphOperations() {
        Iterator<Operation> iter = graph.operations();
        while (iter.hasNext()) {
            System.out.println(iter.next().name());
        }
    }

    /**
     * Get the number of outputs from a specified operation. This is NOT the same as the dimension of the output. In
     * most cases, the number of outputs will be 1.
     * @param operationName Name of the operation to check on.
     * @return Number of outputs returned by that operation.
     */
    public int getNumberOfOperationOutputs(String operationName) {
        Operation operation = graph.operation(operationName);

        Objects.requireNonNull(operation, "Tried to retrieve the number of outputs for an operation which does not " +
                "exist in the graph.");
        return operation.numOutputs();
    }

    /**
     * Get the dimensions of an output of an operation. Note that any -1 dimension is one of unknown size (e.g.
     * number of samples being fed in).
     * @param operationName Name of the operation to examine.
     * @param outputIdx Index of the output of the operation to examine (usually 0). Not the same as shape!
     * @return An array of sizes of the dimensions of the output.
     */
    public int[] getShapeOfOperationOutput(String operationName, int outputIdx) {
        if (outputIdx + 1 > getNumberOfOperationOutputs(operationName)) {
            throw new IndexOutOfBoundsException("Tried to retrieve the shape of an output index which exceeds the " +
                    "number of outputs.");
        }

        Shape outputShape = graph.operation(operationName).output(outputIdx).shape();

        int[] outputDimensions = new int[outputShape.numDimensions()];
        for (int i = 0; i < outputShape.numDimensions(); i++) {
            outputDimensions[i] = (int) outputShape.size(i);
        }
        return outputDimensions;
    }

    /**
     * Get the sizes of the fully-connected layers in the net. These should include inputs and outputs.
     * @return Array of the sizes of the inputs/outputs of the fully-connected layers of the net.
     */
    public int[] getLayerSizes() {
        // Collect all the operation names.
        Iterator<Operation> iter = graph.operations();
        Set<String> operationNames = new HashSet<>();
        while (iter.hasNext()) {
            operationNames.add(iter.next().name());
        }

        // Count the number of fully-connected weight operations.
        int count = 0;
        while(operationNames.contains("fully_connected" + count + "/weights/weight")) {
            count++;
        }
        if (count == 0) {
            throw new IllegalStateException("No fully-connected layers found. Something has changed in the Python " +
                    "script, likely.");
        }

        int[] layerSizes = new int[count + 1];
        // Grab input dimensions on the first bunch.
        for (int i = 0; i < count; i++) {
            layerSizes[i] = (int) graph.operation("fully_connected" + i + "/weights/weight").output(0).shape().size(0);
        }
        // Grab the output layer on the last one.
        layerSizes[count] =
                (int) graph.operation("fully_connected" + (count - 1) + "/weights/weight").output(0).shape().size(1);

        return layerSizes;
    }

    /**
     * Get the .pb file which defines the network. Does not include weights.
     * @return The tensorflow network definition file.
     */
    public File getGraphDefinitionFile() {
        return graphDefinition;
    }

    /**
     * Create a new fully-connected neural network structure. This calls a python script to make the net.
     *
     * @param graphName      Name of the graph file (without path or file extension).
     * @param layerSizes     List of layer sizes. Make sure that the first and last layers match the inputs and desired
     *                       output sizes.
     * @param additionalArgs Additional inputs defined by the python script. Make sure that the list is separated out
     *                       by each word in the command line.
     * @return A new TrainableNetwork based on the specifications.
     */
    public static TrainableNetwork makeNewNetwork(String graphName, List<Integer> layerSizes,
                                                  List<String> additionalArgs) throws FileNotFoundException {
        // Add all command line arguments for the python script to a list.
        List<String> commandList = new ArrayList<>();
        commandList.add("python3");
        commandList.add(pythonGraphCreatorScript);
        commandList.add("--layers");
        commandList.addAll(layerSizes.stream().map(String::valueOf).collect(Collectors.toList()));
        commandList.add("--savepath");
        commandList.add(graphPath + graphName + ".pb");
        commandList.addAll(additionalArgs);

        // Make and run the command line process.
        ProcessBuilder pb = new ProcessBuilder(commandList.toArray(new String[commandList.size()]));
        if (tflowDebugOutput) {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT); // Makes sure that error messages and outputs go to console.
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        }

        try {
            Process p = pb.start();
            p.waitFor(); // Make sure it has finished before moving on.
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        // Send the newly-created graph file to a new TrainableNetwork object.
        File graphFile = new File(graphPath + graphName + ".pb");
        if (!graphFile.exists()) {
            throw new FileNotFoundException("Failed. Unable to locate the TensorFlow graph file which was supposedly " +
                    "created.");
        }

        return new TrainableNetwork(graphFile);
    }

    public static TrainableNetwork makeNewNetwork(String graphName, List<Integer> layerSizes) throws FileNotFoundException {
        return makeNewNetwork(graphName, layerSizes, new ArrayList<>());
    }
}
