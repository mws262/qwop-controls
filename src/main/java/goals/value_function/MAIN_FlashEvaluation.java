package goals.value_function;

import flashqwop.FlashGame;
import flashqwop.FlashStateLogger;
import game.action.Action;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.IStateQWOP;
import game.qwop.StateQWOP;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.jblas.util.Random;
import tree.node.NodeGame;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;
import vision.VisionDataSaver;

import java.io.File;
<<<<<<< HEAD
import java.io.FileNotFoundException;
=======
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Note: with many JVM garbage collectors, the control evaluation time has large spikes. Should use ZGC or Shenandoah
 * . For Linux, ZGC is built-in. For windows, it isn't. Shenandoah is built into Red Hat's Java build for windows
 * though.
 *
 * -XX:+UseEpsilonGC
 */
@SuppressWarnings("Duplicates")
public class MAIN_FlashEvaluation extends FlashGame {

    private final boolean imageCapture = false;
<<<<<<< HEAD
    private final boolean addActionNoise = false;
=======
    private final boolean addActionNoise = true;
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
    private final float noiseProbability = 0.99f;


    private VisionDataSaver visionSaver;
    private int gameMonitorIndex = 0;
    private File captureDir = new File("vision_capture");

    // Net and execution parameters.
<<<<<<< HEAD
    String valueNetworkName = "src/main/resources/tflow_models/tuesday.pb";
    String checkpointName = "src/main/resources/tflow_models/checkpoints/small329";
//    private final String valueNetworkName = "tuesday.pb"; // "deepnarrow_net.pb";
//    private final String checkpointName = "small329";//698"; //329"; // "med67";

    private static boolean hardware = true;
=======
    private final String valueNetworkName = "small_net.pb"; // "deepnarrow_net.pb";
    private final String checkpointName = "small329";//698"; //329"; // "med67";

    private static boolean hardware = false;
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449


    private final List<Action<CommandQWOP>> prefix = new ArrayList<>();


    private static final Logger logger = LogManager.getLogger(MAIN_FlashEvaluation.class);

    private ValueFunction_TensorFlow<CommandQWOP, StateQWOP> valueFunction = null;

    private MAIN_FlashEvaluation() {
        super(hardware); // Do hardware commands out?

        prefix.add(new Action<>(7, CommandQWOP.NONE));
        //            new Action(40, Action.Keys.wo),
        //            new Action(20, Action.Keys.qp),
        //            new Action(1, Action.Keys.p),
        //            new Action(19, Action.Keys.qp),
        //            new Action(3, Action.Keys.wo),

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
            getControlAction(GameQWOP.getInitialState()); // TODO make this better. The first controller evaluation ever
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
    public List<Action<CommandQWOP>> getActionSequenceFromBeginning() {
        return prefix;
    }

    @Override
    public Action<CommandQWOP> getControlAction(IStateQWOP state) {// TODO unsafe
        Action<CommandQWOP> action = valueFunction.getMaximizingAction(new NodeGame(state));
        if (addActionNoise && Random.nextFloat() < noiseProbability) {
            if (action.getTimestepsTotal() < 2 || Random.nextFloat() > 0.5f) {
                action = new Action<>(action.getTimestepsTotal() + 1, action.peek());
                // logger.warn("Action disturbed 1 up.");
            } else {
                action = new Action<>(action.getTimestepsTotal() - 1, action.peek());
                // logger.warn("Action disturbed 1 down.");
            }
        }
        return action;
    }

    @Override
    public void reportGameStatus(IStateQWOP state, CommandQWOP command, int timestep) {}

    private void loadController() {
        // Load a value function controller.
        try {
            valueFunction = new ValueFunction_TensorFlow_StateOnly<>(
<<<<<<< HEAD
                    new File(valueNetworkName),
                    new GameQWOP(),
                    new StateQWOP.Normalizer(StateQWOP.Normalizer.NormalizationMethod.STDEV),
=======
                    new File("src/main/resources/tflow_models/" + valueNetworkName),
                    new GameQWOP(),
                    new StateQWOP.Normalizer(StateQWOP.Normalizer.NormalizationMethod.STDEV),
                    "src/main/resources/tflow_models/special_checkpoints/" + checkpointName,
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
                    1f,
                    false);

            // .pb"));
<<<<<<< HEAD
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            valueFunction.loadCheckpoint(checkpointName);//"small329"); // "small289"); // _after439");//273");//chk_after1");
=======
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MAIN_FlashEvaluation();
    }


}
