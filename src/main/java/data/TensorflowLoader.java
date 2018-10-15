package data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import com.google.common.primitives.Floats;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import game.State;

/**
 * Basic utilities and loaders for getting TensorFlow models in here and working. Users should extend this class.
 * Specifically applies to networks which take a game state and return a single list of numbers.
 *
 * @author matt
 */
public abstract class TensorflowLoader {

    /**
     * Current TensorFlow session.
     **/
    private final Session session;

    /**
     * Computational graph loaded from TensorFlow saves.
     **/
    private final Graph graph;

    /**
     * Load the computational graph from a .pb file and also make a new session.
     *
     * @param pbFile    Name of the graph save file (usually *.pb), including the file extension.
     * @param directory Directory name containing the graph save file.
     */
    public TensorflowLoader(String pbFile, String directory) {
        byte[] graphDef = null;
        try {
            graphDef = Files.readAllBytes(Paths.get(directory, pbFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (graphDef == null)
            throw new NullPointerException("TensorFlow graph was not successfully loaded.");

        graph = new Graph();
        graph.importGraphDef(graphDef);
        session = new Session(graph);
    }

    /**
     * Simple prediction from the model where we give a state and expect a list of floats out.
     * Only applies when a single output (could be multi-element though) is required from the graph.
     *
     * @param state      State to feed into the computational graph.
     * @param inputName  Name of the graph input to shove the state into.
     * @param outputName Name of the graph output to fetch.
     * @return List of values returned by the specified graph output.
     */
    protected List<Float> sisoFloatPrediction(State state, String inputName, String outputName) {
        Tensor<Float> inputTensor = Tensor.create(flattenState(state), Float.class);
        Tensor<Float> result =
                session.runner().feed(inputName + ":0", inputTensor)
                        .fetch(outputName + ":0")
                        .run().get(0).expect(Float.class);
        long[] outputShape = result.shape();

        float[] reshapedResult = result.copyTo(new float[(int) outputShape[0]][(int) outputShape[1]])[0];

        return Floats.asList(reshapedResult);
    }

    /**
     * Print out all operations in the TensorFlow graph to help determine which ones we want to use. There can be an
     * incredible number, and sometimes TensorBoard is the best way to sort this out.
     **/
    @SuppressWarnings("unused")
    public void printTensorflowGraphOperations() {
        Iterator<Operation> iter = graph.operations();
        while (iter.hasNext()) {
            System.out.println(iter.next().name());
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

    /**
     * Make a State object into a 72-element array the way TensorFlow wants it. This method also subtracts the torso
     * x-component out of all body parts.
     *
     * @param state Input state to flatten into an array.
     * @return A 1x72 element array containing all the concatenated state variable values, with the torso x-component
     * subtracted out.
     */
    private static float[][] flattenState(State state) {
        float[][] flatState = new float[1][72];
        float bodyX = state.body.getX();

        // Body
        flatState[0][0] = 0; //TODO this ordering is different from that used in State. Make sure that's ok...
        flatState[0][1] = state.body.getY();
        flatState[0][2] = state.body.getTh();
        flatState[0][3] = state.body.getDx();
        flatState[0][4] = state.body.getDy();
        flatState[0][5] = state.body.getDth();

        // rthigh
        flatState[0][6] = state.rthigh.getX() - bodyX;
        flatState[0][7] = state.rthigh.getY();
        flatState[0][8] = state.rthigh.getTh();
        flatState[0][9] = state.rthigh.getDx();
        flatState[0][10] = state.rthigh.getDy();
        flatState[0][11] = state.rthigh.getDth();

        // rcalf
        flatState[0][12] = state.rcalf.getX() - bodyX;
        flatState[0][13] = state.rcalf.getY();
        flatState[0][14] = state.rcalf.getTh();
        flatState[0][15] = state.rcalf.getDx();
        flatState[0][16] = state.rcalf.getDy();
        flatState[0][17] = state.rcalf.getDth();

        // rfoot
        flatState[0][18] = state.rfoot.getX() - bodyX;
        flatState[0][19] = state.rfoot.getY();
        flatState[0][20] = state.rfoot.getTh();
        flatState[0][21] = state.rfoot.getDx();
        flatState[0][22] = state.rfoot.getDy();
        flatState[0][23] = state.rfoot.getDth();

        // lthigh
        flatState[0][24] = state.lthigh.getX() - bodyX;
        flatState[0][25] = state.lthigh.getY();
        flatState[0][26] = state.lthigh.getTh();
        flatState[0][27] = state.lthigh.getDx();
        flatState[0][28] = state.lthigh.getDy();
        flatState[0][29] = state.lthigh.getDth();

        // lcalf
        flatState[0][30] = state.lcalf.getX() - bodyX;
        flatState[0][31] = state.lcalf.getY();
        flatState[0][32] = state.lcalf.getTh();
        flatState[0][33] = state.lcalf.getDx();
        flatState[0][34] = state.lcalf.getDy();
        flatState[0][35] = state.lcalf.getDth();

        // lfoot
        flatState[0][36] = state.lfoot.getX() - bodyX;
        flatState[0][37] = state.lfoot.getY();
        flatState[0][38] = state.lfoot.getTh();
        flatState[0][39] = state.lfoot.getDx();
        flatState[0][40] = state.lfoot.getDy();
        flatState[0][41] = state.lfoot.getDth();

        // ruarm
        flatState[0][42] = state.ruarm.getX() - bodyX;
        flatState[0][43] = state.ruarm.getY();
        flatState[0][44] = state.ruarm.getTh();
        flatState[0][45] = state.ruarm.getDx();
        flatState[0][46] = state.ruarm.getDy();
        flatState[0][47] = state.ruarm.getDth();

        // rlarm
        flatState[0][48] = state.rlarm.getX() - bodyX;
        flatState[0][49] = state.rlarm.getY();
        flatState[0][50] = state.rlarm.getTh();
        flatState[0][51] = state.rlarm.getDx();
        flatState[0][52] = state.rlarm.getDy();
        flatState[0][53] = state.rlarm.getDth();

        // luarm
        flatState[0][54] = state.luarm.getX() - bodyX;
        flatState[0][55] = state.luarm.getY();
        flatState[0][56] = state.luarm.getTh();
        flatState[0][57] = state.luarm.getDx();
        flatState[0][58] = state.luarm.getDy();
        flatState[0][59] = state.luarm.getDth();

        // llarm
        flatState[0][60] = state.llarm.getX() - bodyX;
        flatState[0][61] = state.llarm.getY();
        flatState[0][62] = state.llarm.getTh();
        flatState[0][63] = state.llarm.getDx();
        flatState[0][64] = state.llarm.getDy();
        flatState[0][65] = state.llarm.getDth();

        // head
        flatState[0][66] = state.head.getX() - bodyX;
        flatState[0][67] = state.head.getY();
        flatState[0][68] = state.head.getTh();
        flatState[0][69] = state.head.getDx();
        flatState[0][70] = state.head.getDy();
        flatState[0][71] = state.head.getDth();

        return flatState;
    }
}
