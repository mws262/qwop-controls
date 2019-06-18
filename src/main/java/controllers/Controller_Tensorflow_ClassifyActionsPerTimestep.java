package controllers;

import game.actions.Action;
import game.state.IState;
import game.state.State;
import tflowtools.TensorflowLoader;

import java.util.List;

/**
 * Neural-network-based controller which uses a {@link State state} to classify which key combination should be
 * pressed. Only uses the current state and potentially the previous action to make decisions and is limited to the QP,
 * WO, none/none key combinations.
 *
 * @author matt
 */
public class Controller_Tensorflow_ClassifyActionsPerTimestep extends TensorflowLoader implements IController {

    /**
     * Name of the input in the TensorFlow graph.
     **/
    private final String inputName;

    /**
     * Name of the output in the TensorFlow graph.
     **/
    private final String outputName;

    /**
     * Previous action index.
     */
    private int prevAction = 0;

    /**
     * Print out each prediction.
     **/
    public boolean verbose = true;

    /**
     * Tries to smooth out noise by sticking with the current action if that action's predicted probability is above
     * this threshold, even if another action has a higher probability. 0 means stay with this action forever, 1
     * means immediately switch when a better action is predicted.
     */
    public float actionLatchingThreshold = 1f;

    /**
     * Create a new TensorFlow classifier controller.
     *
     * @param pbFile    Name of the TensorFlow graph file (usually *.pb), with file extension.
     * @param directory Name of the directory containing the saved graph file.
     */
    public Controller_Tensorflow_ClassifyActionsPerTimestep(String pbFile, String directory, String graphInputName,
                                                            String graphOutputName) {
        super(pbFile, directory);
        this.inputName = graphInputName;
        this.outputName = graphOutputName;
    }

    @Override
    public Action policy(IState state) {
        List<Float> keyClassification = sisoFloatPrediction(state, inputName, outputName);

        float probability0 = keyClassification.get(0);
        float probability1 = keyClassification.get(1);
        float probability2 = keyClassification.get(2);

        Action chosenAction;

        // WO
        if ((probability0 > actionLatchingThreshold && prevAction == 0) || probability0 >= probability1 && probability0 >= probability2) {
            chosenAction = new Action(1, false, true, true, false);
            prevAction = 0;
            if (verbose) System.out.println("WO, " + probability0);

            // QP
        } else if ((probability1 > actionLatchingThreshold && prevAction == 1) || probability1 >= probability0 && probability1 >= probability2) {
            chosenAction = new Action(1, true, false, false, true);
            prevAction = 1;
            if (verbose) System.out.println("QP, " + probability1);

            // None
        } else if ((probability2 > actionLatchingThreshold && prevAction == 2) || probability2 >= probability0 && probability2 >= probability1) {

            chosenAction = new Action(1, false, false, false, false);
            prevAction = 2;
            if (verbose) System.out.println("__, " + probability2);
        } else {
            throw new IllegalStateException("TensorFlow action classifier controller did not choose anything. This " +
                    "really shouldn't be possible.");
        }

        return chosenAction;
    }
}
