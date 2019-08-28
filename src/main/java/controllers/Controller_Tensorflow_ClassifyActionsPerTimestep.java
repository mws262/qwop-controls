package controllers;

import game.IGameSerializable;
import game.action.Action;
import game.qwop.CommandQWOP;
import game.qwop.StateQWOP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tflowtools.TensorflowLoader;
import tree.node.NodeQWOPExplorableBase;

import java.util.List;

/**
 * Neural-network-based controller which uses a {@link StateQWOP state} to classify which key combination should be
 * pressed. Only uses the current state and potentially the previous command to make decisions and is limited to the QP,
 * WO, none/none key combinations.
 *
 * @author matt
 */
public class Controller_Tensorflow_ClassifyActionsPerTimestep extends TensorflowLoader implements IController<CommandQWOP> {

    /**
     * Name of the input in the TensorFlow graph.
     **/
    private final String inputName;

    /**
     * Name of the output in the TensorFlow graph.
     **/
    private final String outputName;

    /**
     * Previous command index.
     */
    private int prevAction = 0;

    /**
     * Tries to smooth out noise by sticking with the current command if that command's predicted probability is above
     * this threshold, even if another command has a higher probability. 0 means stay with this command forever, 1
     * means immediately switch when a better command is predicted.
     */
    public float actionLatchingThreshold = 1f;

    private static final Logger logger = LogManager.getLogger(Controller_Tensorflow_ClassifyActionsPerTimestep.class);

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
    public Action<CommandQWOP> policy(NodeQWOPExplorableBase<?, CommandQWOP> state) {
        List<Float> keyClassification = sisoFloatPrediction(state.getState(), inputName, outputName);
        float probability0 = keyClassification.get(0);
        float probability1 = keyClassification.get(1);
        float probability2 = keyClassification.get(2);
        Action<CommandQWOP> chosenAction;

        // WO
        if ((probability0 > actionLatchingThreshold && prevAction == 0) || probability0 >= probability1 && probability0 >= probability2) {
            chosenAction = new Action<>(1, CommandQWOP.WO);
            prevAction = 0;
            logger.debug("WO, " + probability0);

            // QP
        } else if ((probability1 > actionLatchingThreshold && prevAction == 1) || probability1 >= probability0 && probability1 >= probability2) {
            chosenAction = new Action<>(1, CommandQWOP.QP);
            prevAction = 1;
            logger.debug("QP, " + probability1);

            // None
        } else if ((probability2 > actionLatchingThreshold && prevAction == 2) || probability2 >= probability0 && probability2 >= probability1) {

            chosenAction = new Action<>(1, CommandQWOP.NONE);
            prevAction = 2;
            logger.debug("__, " + probability2);
        } else {
            throw new IllegalStateException("TensorFlow command classifier controller did not choose anything. This " +
                    "really shouldn't be possible.");
        }
        return chosenAction;
    }

    @Override
    public Action<CommandQWOP> policy(NodeQWOPExplorableBase<?, CommandQWOP> state,
                                     IGameSerializable<CommandQWOP> game) {
        return policy(state);
    }

    @Override
    public IController<CommandQWOP> getCopy() {
        throw new RuntimeException("Haven't implemented copy on this controller yet!");
    }
}
