package tflowtools;

import com.google.common.base.Preconditions;
import game.qwop.StateQWOP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.tensorflow.*;
import ui.runner.PanelRunner_SimpleState;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;

/**
 * For evaluating a previously-trained model with arbitrary tensor inputs and outputs.
 *
 * @author matt
 */
public class TensorflowGenericEvaluator implements AutoCloseable {

    /**
     * Current TensorFlow session.
     */
    private final Session session;

    /**
     * Computational graph loaded from TensorFlow saves.
     */
    private final Graph graph;

    private static final Logger logger = LogManager.getLogger(TensorflowLoader.class);

    public TensorflowGenericEvaluator(@NotNull File graphFile) {
        Preconditions.checkNotNull(graphFile, "Must not be a null file.");
        Preconditions.checkArgument(graphFile.exists(), "Must be an existing file.");
        Preconditions.checkArgument(!graphFile.isDirectory(), "Must not be a directory.");

        logger.debug("Tensorflow version: " + TensorFlow.version());
        byte[] graphDef = null;
        try {
            graphDef = Files.readAllBytes(graphFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (graphDef == null)
            throw new NullPointerException("TensorFlow graph was not successfully loaded.");

        graph = new Graph();
        graph.importGraphDef(graphDef);
        session = new Session(graph);
    }

    public List<Tensor<?>> evaluate(Map<String, Tensor<?>> inputs, List<String> outputsToFetch) {

        final Session.Runner runner = session.runner();

        inputs.forEach((s, t) -> runner.feed(s + ":0", t));
        outputsToFetch.stream().map(s -> s + ":0").forEach(runner::fetch);

        List<Tensor<?>> outputsComputed = runner.run();
        assert outputsComputed.size() == outputsToFetch.size();
        return outputsComputed;
    }

    protected float[] evaluate(float[] input, String inputName, String outputName) {
        Tensor<Float> inputTensor = Tensor.create(input, Float.class);
        List<Tensor<?>> output = session.runner().feed(inputName + ":0", inputTensor)
                .fetch(outputName + ":0")
                .run();
        Tensor<Float> result = output.get(0).expect(Float.class);
        long[] outputShape = result.shape();

        float[] reshapedResult = result.copyTo(new float[(int) outputShape[0]][(int) outputShape[1]])[0];

        inputTensor.close();
        result.close();
        return reshapedResult;
    }


    /**
     * Load a checkpoint file. Must match the graph loaded. Name does not need to include path or file extension.
     * @param checkpointName Name of the checkpoint to load.
     */
    public void loadCheckpoint(@NotNull String checkpointName) throws IOException {
        if (checkpointName == null || checkpointName.isEmpty()) {
            throw new IllegalArgumentException("Back checkpoint load name given. Was: " + checkpointName);
        }

        try {
            Tensor<String> checkpointTensor = Tensors.create(checkpointName);
            session.runner().feed("save/Const", checkpointTensor).addTarget("save/restore_all").run();
            checkpointTensor.close();
            logger.info("Loaded checkpoint from: " + checkpointName);
        } catch (IllegalArgumentException | TensorFlowException e) {
            throw new IOException("Checkpoint file did not match the inputs of the selected model. " + e.getMessage());
        }
    }

    /**
     * Print out all operations in the TensorFlow graph to help determine which ones we want to use. There can be an
     * incredible number, and sometimes TensorBoard is the best way to sort this out.
     */
    @SuppressWarnings("unused")
    public void printTensorflowGraphOperations() {
        Iterator<Operation> iter = graph.operations();
        while (iter.hasNext()) {
            logger.info(iter.next().name());
        }
    }

    /**
     * Get the current TensorFlow session.
     *
     * @return A TensorFlow session object.
     */
    public Session getSession() {
        return session;
    }

    /**
     * Get the TensorFlow computational graph (ie model).
     *
     * @return The TensorFlow graph object.
     */
    public Graph getGraph() {
        return graph;
    }

    @Override
    public void close() {
        session.close();
        graph.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        JFrame frame = new JFrame();
        PanelRunner_SimpleState<StateQWOP> runnerPanel = new PanelRunner_SimpleState<>("Runner");
        runnerPanel.activateTab();
        frame.getContentPane().add(runnerPanel);
        frame.setPreferredSize(new Dimension(1000, 1000));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);


        BufferedReader br = new BufferedReader(new FileReader(new File("./python/mins.txt")));
        float[] mins = new float[35];
        String line;
        int count = 0;
        while ((line = br.readLine()) != null)
            mins[count++] = Float.parseFloat(line);
        br.close();

        br = new BufferedReader(new FileReader(new File("./python/maxes.txt")));
        float[] maxes = new float[35];
        count = 0;
        while ((line = br.readLine()) != null)
            maxes[count++] = Float.parseFloat(line);
        br.close();

        TensorflowGenericEvaluator tflow = new TensorflowGenericEvaluator(new File("./python" +
                "/backup_medres_backgroundincl/modeldef.pb"));
        tflow.loadCheckpoint("./python/backup_medres_backgroundincl/model.ckpt");

        tflow.printTensorflowGraphOperations();

        //CaptureQWOPWindow locator = new CaptureQWOPWindow(1);
        for (int j = 0; j < 500; j++) {

            //locator.saveImageToPNG(new File("tmp.png"));

            /////
            Map<String, Tensor<?>> in = new HashMap<>();
            in.put("img_filename", Tensors.create( "./vision_capture/run1/ts" + j + ".png")); //"tmp.png"));
            List<String> out = new ArrayList<>();
            out.add("processed_img");
            List<Tensor<?>> output = tflow.evaluate(in, out);

            in.clear();
            in.put("img_in", output.get(0));
            out.clear();
            out.add("prediction");
            output = tflow.evaluate(in, out);

            Tensor<Float> result = output.get(0).expect(Float.class);
            long[] outputShape = result.shape();

            float[] reshapedResult = result.copyTo(new float[(int) outputShape[0]][(int) outputShape[1]])[0];

            float[] stateVals = new float[36];
            stateVals[0] = 0;
            for (int i = 0; i < reshapedResult.length; i++) {
                stateVals[i + 1] = reshapedResult[i] * (maxes[i] - mins[i]) + mins[i];
            }

            StateQWOP st = StateQWOP.makeFromPositionArrayOnly(stateVals);

            runnerPanel.updateState(st);
            Thread.sleep(30);
        }

        tflow.close();
    }
}
