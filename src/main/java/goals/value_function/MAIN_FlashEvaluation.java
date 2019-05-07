package goals.value_function;

import actions.Action;
import flashqwop.FlashGame;
import game.GameUnified;
import game.State;
import org.jblas.util.Random;
import tree.Node;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;
import vision.VisionDataSaver;

import java.io.File;
import java.io.FileNotFoundException;

@SuppressWarnings("Duplicates")
public class MAIN_FlashEvaluation extends FlashGame {

    private boolean imageCapture = false;
    private boolean addActionNoise = false;
    private float noiseProbability = 0.3f;


    private VisionDataSaver visionSaver;
    private int gameMonitorIndex = 0;
    private File captureDir = new File("vision_capture");

    Action[] prefix = new Action[]{
            new Action(7, Action.Keys.none),
//            new Action(40, Action.Keys.wo),
//            new Action(20, Action.Keys.qp),
//            new Action(1, Action.Keys.p),
//            new Action(19, Action.Keys.qp),
//            new Action(3, Action.Keys.wo),

    };

    private ValueFunction_TensorFlow valueFunction = null;
    private Node placeholderNode = new Node(); // TODO only really needs the state. This is just acting as a container.

    public MAIN_FlashEvaluation() {
        if (imageCapture) {
            visionSaver = new VisionDataSaver(captureDir, gameMonitorIndex);
            getServer().addStateListener(visionSaver);
        }

        loadController();

        getControlAction(GameUnified.getInitialState()); // TODO make this better. The first controller evaluation ever
        // takes 8 times longer than the rest. I don't know why. In the meantime, just do the first evaluation in a
        // non-time-critical section of the code. In the long term, the controller should be an anytime approach
        // anyway.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        restart();
    }

    @Override
    public Action[] getActionSequenceFromBeginning() {
        return prefix;
    }

    @Override
    public Action getControlAction(State state) {
        placeholderNode.setState(state);
        Action action = valueFunction.getMaximizingAction(placeholderNode);
        if (addActionNoise && Random.nextFloat() < noiseProbability) {
            if (action.getTimestepsTotal() < 2 || Random.nextFloat() > 0.5f) {
                action = new Action(action.getTimestepsTotal() + 1, action.peek());
            } else {
                action = new Action(action.getTimestepsTotal() - 1, action.peek());
            }
        }
        return action;
    }

    @Override
    public void reportGameStatus(State state, boolean[] command, int timestep) {}

    public void loadController() {
        // Load a value function controller.
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(new File("src/main/resources/tflow_models" +
                    "/small_net.pb")); // state_only.pb"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        valueFunction.loadCheckpoint("small329"); // "small289"); // _after439");//273");//chk_after1");
    }

    public static void main(String[] args) {
        MAIN_FlashEvaluation controller = new MAIN_FlashEvaluation();
    }


}
