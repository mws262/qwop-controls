package tflowtools;

import com.google.common.base.Preconditions;
import data.TFRecordWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tensorflow.*;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.Summary;
import org.tensorflow.util.Event;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A wrapper for creating, training, and evaluating TensorFlow models. Normally the syntax is a pain in Java, so this
 * just keeps me from having to remember all of it.
 *
 * @author matt
 */
public class TrainableNetwork implements AutoCloseable {

    /**
     * Networks are not created in Java. Java calls a python script defined here to create the network.
     */
    public static final String pythonGraphCreatorScript = "python/java_value_function/create_generic_graph.py";

    /**
     * .pb file defining the structure (but not yet weights) of the network.
     */
    private final File graphDefinition;

    private String activeCheckpoint;

    /**
     * Loaded Tensorflow graph definition.
     */
    private final Graph graph;

    /**
     * Loaded Tensorflow session.
     */
    public final Session session;

    private final int[] layerSizes;

    /**
     * Send Python TensorFlow output to console? Tests don't like this, and it kind of clutters up stuff.
     */
    private static boolean tflowDebugOutput = false;

    /**
     * For logger message output.
     */
    private static Logger logger = LogManager.getLogger(TrainableNetwork.class);

    // Tensorboard
    public final boolean useTensorboard;
    public File tensorboardLogFile;
    public int trainingStepCount = 0;

    private boolean haveResourcesBeenReleased = false;
    public static AtomicInteger openCount = new AtomicInteger();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (openCount.get() > 0) {
                    logger.warn("Networks opened but not explicitly closed: " + openCount.get());
                } else {
                    logger.info("All networks opened were closed properly.");
                }
            }
        });
    }

    /**
     * Create a new wrapper for an existing Tensorflow graph.
     *
     * @param graphDefinition Graph definition file.
     */
    public TrainableNetwork(File graphDefinition, boolean useTensorboard) throws FileNotFoundException {

        if (!graphDefinition.exists() || !graphDefinition.isFile())
            throw new FileNotFoundException("Unable to locate the specified model file.");

        this.graphDefinition = graphDefinition;
        this.useTensorboard = useTensorboard;

        // Begin loading the structure of the model as defined by the graph file.
        byte[] graphDef = null;
        try {
            graphDef = Files.readAllBytes(graphDefinition.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Put the graph together.
        graph = new Graph();
        session = new Session(graph);
        graph.importGraphDef(graphDef);

        // Initialize
        session.runner().addTarget("init").run(); // Could be removed if it proves too slow and we only care
        // about loading rather than initializing from scratch.

        layerSizes = getLayerSizes();

        logger.info("Created a network from a saved model file: " + graphDefinition.toString() + ".");
        openCount.incrementAndGet();

        // Tensorboard initialization, if used.
        if (useTensorboard) {
            // Create the log file and location. Tboard categorizes runs by directory name, so each run will get its
            // own directory
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("MM-dd_HH-mm-ss");
            try {
                tensorboardLogFile =
                        new File("./logs/run" + dateFormat.format(new Date()) + "/tboardlog.tfevents");
                if ( !(tensorboardLogFile.getParentFile().mkdirs() && tensorboardLogFile.createNewFile()) ) {
                    logger.warn(("Tensorboard logger was unable to create its log file. It might already exist."));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Write serialized graph information to the TensorBoard log. This will let it display the full
            // visualization of the graph. Note that this will overwrite any existing file by this name, although
            // that shouldn't happen anyway.
            try (FileOutputStream os = new FileOutputStream(tensorboardLogFile, false)) {
                Event.Builder eventBuilder = Event.newBuilder();
                eventBuilder.setGraphDef(GraphDef.parseFrom(graph.toGraphDef()).toByteString());
                TFRecordWriter.writeToStream(eventBuilder.build().toByteArray(), os);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        float loss = 0;
        for (int i = 0; i < steps; i++) {
            Session.Runner sess = session.runner()
                    .feed("input", input)
                    .feed("output_target", value_out)
                    .addTarget("train")
                    .fetch("loss");

            if (useTensorboard) {
                sess = sess.fetch("summary/summary");
            }
            List<Tensor<?>> out = sess.run();
            loss = out.get(0).expect(Float.class).floatValue();

            if (useTensorboard) {
                toTensorBoardOutput(out.get(1));
            }
            out.forEach(Tensor::close);
        }

        input.close();
        value_out.close();
        return loss; // Could be problematic with softmax which doesn't spit out a single value.
    }

    protected void toTensorBoardOutput(Tensor<?> summaryTensor) {
        byte[] summaryMessage = summaryTensor.bytesValue();
        try (FileOutputStream os = new FileOutputStream(tensorboardLogFile, true)) {
            // Need to convert the summary protobuf into an event protobuf.
            Summary summary = Summary.parseFrom(summaryMessage);

            Event.Builder eventBuilder = Event.newBuilder();
            eventBuilder.setSummary(summary);
            eventBuilder.setStep(trainingStepCount++);
            eventBuilder.setWallTime(eventBuilder.getWallTime());
            Event event = eventBuilder.build();

            TFRecordWriter.writeToStream(event.toByteArray(), os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save training progress (i.e. weights and biases) to specific checkpoint file. Will still be saved to the
     * checkpoint directory. Do not include file extension.
     * @return A list of the files in the save directory matching the given checkpoint name. Note that we aren't
     * doing strict checking on the number of files. If you suspect that some other source is adding similarly-named
     * files, check this.
     */
    public List<File> saveCheckpoint(String checkpointName) throws IOException {
        if (checkpointName == null || checkpointName.isEmpty()) {
            throw new IllegalArgumentException("Back checkpoint save name given. Was: " + checkpointName);
        }

        Tensor<String> checkpointTensor = Tensors.create(checkpointName);
        session.runner().feed("save/Const", checkpointTensor).addTarget("save/control_dependency").run();
        checkpointTensor.close();

        File checkPointDirectory = new File(checkpointName).getParentFile();
        if (!checkPointDirectory.exists() && !checkPointDirectory.mkdirs()) {
            throw new IOException("Checkpoint directory did not exist and could not be created for: " + checkpointName);
        }

        File[] files = checkPointDirectory.listFiles();

        // Also report the files created.
        List<File> checkpointFiles = new ArrayList<>();
        for (File file : Objects.requireNonNull(files)) {
            if (file.getPath().contains(checkpointName + ".")) { // Period keeps others with additional numbers from
                // appearing too.
                checkpointFiles.add(file);
            }
        }

        activeCheckpoint = checkpointName;
        logger.info("Saved checkpoint files:");
        checkpointFiles.forEach(f -> logger.info(f.getName()));
        return checkpointFiles;
    }

    /**
     * Load a checkpoint file. Must match the graph loaded. Name does not need to include path or file extension.
     * @param checkpointName Name of the checkpoint to load.
     */
    public void loadCheckpoint(String checkpointName) throws IOException {
        if (checkpointName == null || checkpointName.isEmpty()) {
            throw new IllegalArgumentException("Back checkpoint load name given. Was: " + checkpointName);
        }

        try {
            Tensor<String> checkpointTensor = Tensors.create(checkpointName);
            session.runner().feed("save/Const", checkpointTensor).addTarget("save/restore_all").run();
            checkpointTensor.close();
            logger.info("Loaded checkpoint from: " + checkpointName);
            activeCheckpoint = checkpointName;
        } catch (IllegalArgumentException | TensorFlowException e) {
            throw new IOException("Checkpoint file did not match the inputs of the selected model. " + e.getMessage());
        }
    }

    /**
     * Evaluate one or more inputs to the net. Does not perform any training.
     * @param inputs One or more inputs to feed in. For a single example, the first dimension should be 1.
     * @return The evaluated values of the inputs.
     */
    public float[][] evaluateInput(float[][] inputs) {
        Tensor<Float> inputTensor = Tensors.create(inputs);
        List<Tensor<?>> result =
                session.runner().feed("input", inputTensor).fetch("output").run();
        Tensor<Float> outputTensor = result.get(0).expect(Float.class);

        long[] outputShape = outputTensor.shape();
        float[][] output = new float[(int) outputShape[0]][(int) outputShape[1]];
        outputTensor.copyTo(output);

        inputTensor.close();
        result.forEach(Tensor::close);
        return output;
    }

    /**
     * Print all operations in the graph for debugging.
     */
    public void printGraphOperations() {
        Iterator<Operation> iter = graph.operations();
        while (iter.hasNext()) {
            logger.info(iter.next().name());
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


    public float[][] getLayerWeights(int layerIndex) {
        Preconditions.checkArgument(layerIndex < layerSizes.length - 1 && layerIndex >= 0, "Invalid layer index.",
                layerIndex);

        float[][] layer = new float[layerSizes[layerIndex]][layerSizes[layerIndex + 1]];
        Tensor<?> tensor = session.runner().fetch("fully_connected" + layerIndex + "/weights/Variable/read").run().get(0);
        tensor.copyTo(layer);
        tensor.close();
        return layer;
    }

    public float[] getLayerBiases(int layerIndex) {
        Preconditions.checkArgument(layerIndex < layerSizes.length - 1 && layerIndex >= 0, "Invalid layer index.",
                layerIndex);
        float[] layer = new float[layerSizes[layerIndex + 1]];
        Tensor<?> tensor = session.runner().fetch("fully_connected" + layerIndex + "/biases/Variable/read").run().get(0);
        tensor.copyTo(layer);
        tensor.close();
        return layer;
    }

    public void setLayerWeights(int layerIndex, float[][] newWeights) {
        Preconditions.checkArgument(layerIndex < layerSizes.length - 1 && layerIndex >= 0, "Invalid layer index.",
                layerIndex);

        Preconditions.checkArgument(newWeights.length == layerSizes[layerIndex], "First dimension of provided weights" +
                " did not match the actual dimension at that index.", newWeights.length, layerSizes[layerIndex]);

        Preconditions.checkArgument(newWeights.length == layerSizes[layerIndex], "First dimension of provided weights" +
                " did not match the actual dimension at that index.", newWeights[0].length, layerSizes[layerIndex + 1]);

        Tensor<Float> replacementTensor = Tensors.create(newWeights);
        session.runner().feed("fully_connected" + layerIndex + "/weights/weight", replacementTensor).addTarget(
                "fully_connected" + layerIndex + "/weights/Variable/Assign").run();

        replacementTensor.close(); // TODO is it still in use?
    }

    public void setLayerBiases(int layerIndex, float[] newBiases) {
        Preconditions.checkArgument(layerIndex < layerSizes.length - 1 && layerIndex >= 0, "Invalid layer index.",
                layerIndex);

        Preconditions.checkArgument(newBiases.length == layerSizes[layerIndex + 1], "Biases " +
                " did not match the actual dimension at that index.", newBiases.length, layerSizes[layerIndex + 1]);

        Tensor<Float> replacementTensor = Tensors.create(newBiases);
        session.runner().feed("fully_connected" + layerIndex + "/biases/bias", replacementTensor).addTarget(
                "fully_connected" + layerIndex + "/biases/Variable/Assign").run();
        replacementTensor.close(); // TODO is it still in use?
    }

    /**
     * Get the .pb file which defines the network. Does not include weights.
     * @return The tensorflow network definition file.
     */
    public File getGraphDefinitionFile() {
        return graphDefinition;
    }

    public String getActiveCheckpoint() {
        return activeCheckpoint;
    }

    @Override
    public synchronized void close() {
        if (!haveResourcesBeenReleased)
            openCount.decrementAndGet();

        session.close();
        graph.close();
        haveResourcesBeenReleased = true;
    }

    @Override
    public void finalize() {
        if (!haveResourcesBeenReleased) {
            logger.error("This object was garbage collected without close() having been called. This means there are" +
                    " resources still open in the background.");
        }
    }

    /**
     * Create a new fully-connected neural network structure. This calls a python script to make the net.
     *
     * @param graphFileName  Name of the graph file.
     * @param layerSizes     List of layer sizes. Make sure that the first and last layers match the inputs and desired
     *                       output sizes.
     * @param additionalArgs Additional inputs defined by the python script. Make sure that the list is separated out
     *                       by each word in the command line.
     * @return A new TrainableNetwork based on the specifications.
     */
    public static TrainableNetwork makeNewNetwork(String graphFileName, List<Integer> layerSizes,
                                                  List<String> additionalArgs, boolean tensorboardLogging) throws FileNotFoundException {

        return new TrainableNetwork(makeGraphFile(graphFileName, layerSizes, additionalArgs),
                tensorboardLogging);
    }

    public static TrainableNetwork makeNewNetwork(String graphName, List<Integer> layerSizes, boolean tensorboardLogging) throws FileNotFoundException {
        return makeNewNetwork(graphName, layerSizes, new ArrayList<>(), tensorboardLogging);
    }

    public static File makeGraphFile(String graphFileName, List<Integer> layerSizes,
                List<String> additionalArgs) throws FileNotFoundException {

        for (Integer layerSize : layerSizes) {
            if (layerSize <= 0 ) {
                throw new IllegalArgumentException("No network layer sizes may be less than or equal to zero. A layer" +
                        " was specified as: " + layerSize);
            }
        }

        if (graphFileName.isEmpty()) {
            throw new IllegalArgumentException("Graph name may not be empty.");
        }

        // Add all command line arguments for the python script to a list.
        List<String> commandList = new ArrayList<>();
        commandList.add("python3");
        commandList.add(pythonGraphCreatorScript);
        commandList.add("--layers");
        commandList.addAll(layerSizes.stream().map(String::valueOf).collect(Collectors.toList()));
        commandList.add("--savepath");
        commandList.add(graphFileName);
        commandList.addAll(additionalArgs);

        StringBuilder sb = new StringBuilder();
        commandList.forEach(c -> sb.append(c).append(" "));
        logger.info("Python network-creator script called with arguments:\n" + sb.toString());

        // Make and run the command line process.
        ProcessBuilder pb = new ProcessBuilder(commandList.toArray(new String[0]));
        if (tflowDebugOutput) {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT); // Makes sure that error messages and outputs go to console.
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        }

        try {
            Process p = pb.start();
            p.waitFor(); // Make sure it has finished before moving on.
            p.destroy(); //todo necessary?
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        // Send the newly-created graph file to a new TrainableNetwork object.
        File graphFile = new File(graphFileName);
        if (!graphFile.exists()) {
            throw new FileNotFoundException("Failed. Unable to locate the TensorFlow graph file which was supposedly " +
                    "created.");
        }

        return graphFile;
    }
}
