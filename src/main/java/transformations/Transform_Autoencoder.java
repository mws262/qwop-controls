package transformations;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import game.IState;
import game.State;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Transform based around a TensorFlow neural network. The neural network takes all the values in a {@link State} and
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
public class Transform_Autoencoder implements ITransform {

    /**
     * TensorFlow session. Data can be fed into the neural network and outputs retrieved.
     */
    private Session tensorflowSession;

    /**
     * Dimension that a state input is changed (usually reduced) to.
     */
    private final int outputSize;

    private final int stateDimension = 72;

    /**
     * State needs to be turned into a 1x72 float array to be fed into the network.
     */
    private float[][] flatSt = new float[1][stateDimension];

    private final Logger logger = LogManager.getLogger(Transform_Autoencoder.class);

    /**
     * Make a transform which uses a neural network autoencoder to transform state data into a reduced state.
     *
     * @param filename Full path/filename of the TensorFlow model
     * @param outputSize Dimension of the output (i.e. how many numbers the state is reduced to.
     */
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_EXCEPTION", justification = "Null pointer is caught.")
    public Transform_Autoencoder(String filename, int outputSize) {
        this.outputSize = outputSize;
        byte[] graphDef = null;
        try {
            graphDef = Files.readAllBytes(Paths.get(filename));
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
    public void updateTransform(List<IState> nodesToUpdateFrom) {} // Nothing is adaptive about this transform.

    @Override
    public List<float[]> transform(List<IState> originalStates) {
        List<float[]> transformedStates = new ArrayList<>();

        for (IState st : originalStates) {
            float[] flattenedState = st.flattenState();
            if (flattenedState.length != this.stateDimension) {
                flatSt[0] = Arrays.copyOf(flattenedState, stateDimension); // Will truncate state if too big. Beware!
                logger.warn("Dimension of state input is longer than expected (" + stateDimension + "). Was " + flattenedState.length + ". Truncating down to size.");
            } else {
                flatSt[0] = flattenedState;
            }

            Tensor<Float> inputTensor = Tensor.create(flatSt, Float.class);
            Tensor<Float> result =
                    tensorflowSession.runner().feed("Squeeze:0", inputTensor)
                            .fetch("decoder/decoder_input:0")
                            .run().get(0).expect(Float.class);

            float[][] res = result.copyTo(new float[1][outputSize]);
            transformedStates.add(res[0]);
        }
        return transformedStates;
    }

    @Override
    public List<IState> untransform(List<float[]> transformedStates) {
        return null; // TODO figure out how to get the correct layers in/out for this.
    }

    @Override
    public List<IState> compressAndDecompress(List<IState> originalStates) {
        List<IState> transformedStates = new ArrayList<>();

        for (IState st : originalStates) {
            float[] flattenedState = st.flattenState();
            if (flattenedState.length != this.stateDimension) {
                flatSt[0] = Arrays.copyOf(flattenedState, stateDimension); // Will truncate state if too big. Beware!
                logger.warn("Dimension of state input is longer than expected (" + stateDimension + "). Was " + flattenedState.length + ". Truncating down to size.");
            } else {
                flatSt[0] = flattenedState;
            }
            Tensor<Float> inputTensor = Tensor.create(flatSt, Float.class);
            Tensor<Float> result =
                    tensorflowSession.runner().feed("Squeeze:0", inputTensor)
                            .fetch("transform_out/Add_1:0")
                            .run().get(0).expect(Float.class);

            float[][] res = result.copyTo(new float[1][72]);
            transformedStates.add(new State(res[0], false));
        }
        return transformedStates;
    }

    @Override
    public int getOutputStateSize() {
        return outputSize;
    }

    @Override
    public String getName() {
        return "AutoEnc " + getOutputStateSize();
    }

    //TODO move to unit test.
    // Example usage:
    //	public static void goals(String[] args) {
    //		TensorflowAutoencoder enc = new TensorflowAutoencoder();
    //
    //		float[][] dummy = new float[1][72];
    //		for (int i = 0; i < dummy[0].length; i++) {
    //			dummy[0][i] = 0.1f;
    //		}
    //
    //		Tensor<Float> inputTensor = Tensor.create(dummy, Float.class);
    //		long init = System.currentTimeMillis();
    //		Tensor<Float> result =
    //				enc.tensorflowSession.runner().feed("Squeeze:0", inputTensor)
    //				.fetch("transform_out/Add_1:0")
    //				.run().get(0).expect(Float.class);
    //
    //
    //		float[][] res = result.copyTo(new float[1][72]);
    //
    //		for (int i = 0; i < res[0].length; i++) {
    //			System.out.println(res[0][i]);
    //		}
    //	}
}
