package goals.value_function;

import game.action.Action;
import flashqwop.FlashGame;
import flashqwop.FlashStateLogger;
import game.GameUnified;
import game.state.IState;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.jblas.util.Random;
import tree.node.NodeQWOP;
import tree.Utility;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;
import vision.VisionDataSaver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Note: with many JVM garbage collectors, the control evaluation time has large spikes. Should use ZGC or Shenandoah
 * . For Linux, ZGC is built-in. For windows, it isn't. Shenandoah is built into Red Hat's Java build for windows
 * though.
 *
 * -XX:+UseEpsilonGC
 */
@SuppressWarnings("Duplicates")
public class MAIN_FlashEvaluation extends FlashGame {
    static{
        Utility.loadLoggerConfiguration();
    }
    private boolean imageCapture = false;
    private boolean addActionNoise = true;
    private float noiseProbability = 0.99f;


    private VisionDataSaver visionSaver;
    private int gameMonitorIndex = 0;
    private File captureDir = new File("vision_capture");

    // Net and execution parameters.
    String valueNetworkName = "small_net.pb"; // "deepnarrow_net.pb";
    String checkpointName = "small329";//698"; //329"; // "med67";

    private static boolean hardware = true;

    Action[] prefix = new Action[]{
            new Action(7, Action.Keys.none),
//            new Action(40, Action.Keys.wo),
//            new Action(20, Action.Keys.qp),
//            new Action(1, Action.Keys.p),
//            new Action(19, Action.Keys.qp),
//            new Action(3, Action.Keys.wo),

    };

    private Logger logger = LogManager.getLogger(MAIN_FlashEvaluation.class);

    private ValueFunction_TensorFlow valueFunction = null;

    public MAIN_FlashEvaluation() {
        super(hardware); // Do hardware commands out?

        if (imageCapture) {
            visionSaver = new VisionDataSaver(captureDir, gameMonitorIndex);
            getServer().addStateListener(visionSaver);
        }

        getServer().addStateListener(new FlashStateLogger());

        loadController();

        // "Warming-up" the JVM. I think that this helps trigger JIT compilation of the correct stuff before time
        // sensitive things need to happen.
        logger.debug("Doing dummy controller evaluations as a hack to prime(?) the caches.");
        // Temporarily disable logging for the value function controller. It's not useful clogging the log with the
        // warm-up iterations.
        Configurator.setLevel(LogManager.getLogger(ValueFunction_TensorFlow_StateOnly.class).getName(), Level.OFF);
        for (int i = 0; i < 50; i++) {
            getControlAction(GameUnified.getInitialState()); // TODO make this better. The first controller evaluation ever
        }
        Configurator.setLevel(LogManager.getLogger(ValueFunction_TensorFlow_StateOnly.class).getName(), Level.INFO);
        logger.debug("Dummy evaluations complete.");

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
    public Action getControlAction(IState state) {
        Action action = valueFunction.getMaximizingAction(new NodeQWOP(state));
        if (addActionNoise && Random.nextFloat() < noiseProbability) {
            if (action.getTimestepsTotal() < 2 || Random.nextFloat() > 0.5f) {
                action = new Action(action.getTimestepsTotal() + 1, action.peek());
                // logger.warn("Action disturbed 1 up.");
            } else {
                action = new Action(action.getTimestepsTotal() - 1, action.peek());
                // logger.warn("Action disturbed 1 down.");
            }
        }
        return action;
    }

    @Override
    public void reportGameStatus(IState state, boolean[] command, int timestep) {}

    private void loadController() {
        // Load a value function controller.
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly(new File("src/main/resources/tflow_models" +
                    "/" + valueNetworkName), new GameUnified(), false); // state_only.pb"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            valueFunction.loadCheckpoint(checkpointName);//"small329"); // "small289"); // _after439");//273");//chk_after1");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MAIN_FlashEvaluation();
    }


}
