package game.state.transform;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import game.qwop.StateQWOP;
import game.state.IState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Transform based around a TensorFlow neural network. The neural network takes all the values in a {@link StateQWOP} and
 * feeds them through several layers which (usually) reduce the state to fewer numbers (e.g. 72 to 12 numbers). This
 * is the encoder. The network has layers which expand the reduced state back out to the full number of values. This
 * is the decoder.
 *
 * Note that working with the networks requires the names of the inputs/outputs. TODO these should be standardized in
 * some way.
 *
 * The neural networks are trained using the Python interface with TensorFlow and exported to .pb models.
 *
 * @author matt
 */
public class Transform_Autoencoder<S extends IState> implements ITransform<S> {
    /**
     * TensorFlow session. Data can be fed into the neural network and outputs retrieved.
     */
    private Session tensorflowSession;

    /**
     * Dimension that a state input is changed (usually reduced) to.
     */
    private final int outputSize;

    private static final Logger logger = LogManager.getLogger(Transform_Autoencoder.class);

    public final String graphFile;

    /**
     * Make a transform which uses a neural network autoencoder to transform state data into a reduced state.
     *
     * @param outputSize Dimension of the output (i.e. how many numbers the state is reduced to.
     */
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_EXCEPTION", justification = "Null pointer is caught.")
    public Transform_Autoencoder(@JsonProperty("graphFile") String graphFile,
                                 @JsonProperty("outputSize") int outputSize) {
        this.graphFile = graphFile;
        this.outputSize = outputSize;
        byte[] graphDef = null;
        try {
            graphDef = Files.readAllBytes(Paths.get(graphFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (graphDef == null) {
            throw new NullPointerException("TensorFlow graph description was not successfully loaded.");
        }
        Graph g = new Graph();
        g.importGraphDef(graphDef);
        tensorflowSession = new Session(g);
    }

    @Override
    public void updateTransform(List<S> nodesToUpdateFrom) {} // Nothing is adaptive about this transform.

    @Override
    public List<float[]> transform(List<S> originalStates) {
        List<float[]> transformedStates = new ArrayList<>();

        for (S st : originalStates) {
            transformedStates.add(transform(st));
        }
        return transformedStates;
    }

    @Override
    public float[] transform(S originalState) {
        Tensor<Float> inputTensor = assembleNetInputFromState(originalState);
        Tensor<Float> result =
                tensorflowSession.runner().feed("Squeeze:0", inputTensor)
                        .fetch("decoder/decoder_input:0")
                        .run().get(0).expect(Float.class);

        float[][] res = result.copyTo(new float[1][outputSize]);

        inputTensor.close();
        result.close();
        return res[0];
    }

    @Override
    public List<float[]> untransform(List<float[]> transformedStates) {
        return null; // TODO figure out how to get the correct layers in/out for this.
    }

    @Override
    public List<float[]> compressAndDecompress(List<S> originalStates) {
        List<float[]> transformedStates = new ArrayList<>();

        for (S st : originalStates) {
            Tensor<Float> inputTensor = assembleNetInputFromState(st);
            Tensor<Float> result =
                    tensorflowSession.runner().feed("Squeeze:0", inputTensor)
                            .fetch("transform_out/Add_1:0")
                            .run().get(0).expect(Float.class);

            float[][] res = result.copyTo(new float[1][st.getStateSize()]);
            transformedStates.add(res[0]);
        }
        return transformedStates;
    }

    private Tensor<Float> assembleNetInputFromState(S st) {
        float[][] flatState = new float[][] {st.flattenState()};
        return Tensor.create(flatState, Float.class);
    }

    @Override
    public int getOutputSize() {
        return outputSize;
    }

    @Override
    public String getName() {
        return "AutoEnc " + getOutputSize();
    }
}
