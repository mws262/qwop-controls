package goals.value_function;

import actions.Action;
import actions.ActionQueue;
import flashqwop.FlashGame;
import flashqwop.FlashQWOPServer;
import flashqwop.QWOPStateListener;
import game.GameUnified;
import game.State;
import game.StateVariable;
import tree.Node;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;
import vision.CaptureQWOPWindow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

@SuppressWarnings("Duplicates")
public class MAIN_FlashEvaluation extends FlashGame {

    private boolean imageCapture = true;
    private CaptureQWOPWindow capture;
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
            capture = new CaptureQWOPWindow(1); // Whichever screen has toolbar is 0.
            if (!captureDir.exists() || !captureDir.isDirectory()) {
                captureDir.mkdirs();
            }
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
//        printGameInfo();
        restart();
    }

    @Override
    public Action[] getActionSequenceFromBeginning() {
        return prefix;
    }

    @Override
    public Action getControlAction(State state) {
        placeholderNode.setState(state);
        return valueFunction.getMaximizingAction(placeholderNode);
    }

    int runCounter = 0;
    File runFile;
    boolean hasFirstResetHappened = false;
    @Override
    public void reportGameStatus(State state, boolean[] command, int timestep) {

        timestep--;

        if (imageCapture && !state.isFailed() && timestep >= 0) {
            System.out.println(timestep);

            if (timestep == 0) {
                runFile = new File(captureDir.getPath() + "/run" + runCounter++);
                runFile.mkdirs();
                hasFirstResetHappened = true;
            }
            if (hasFirstResetHappened) {
                try {
                    capture.saveImageToPNG(new File(runFile.getPath() + "/ts" + timestep + ".png"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadController() {
        // Load a value function controller.
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(new File("src/main/resources/tflow_models" +
                    "/small_net.pb")); // state_only.pb"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        valueFunction.loadCheckpoint("small289"); // _after439");//273");//chk_after1");
    }

    public static void main(String[] args) {
        MAIN_FlashEvaluation controller = new MAIN_FlashEvaluation();
    }

}
