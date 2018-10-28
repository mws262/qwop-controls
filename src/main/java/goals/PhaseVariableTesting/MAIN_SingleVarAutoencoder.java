package goals.PhaseVariableTesting;

import actions.Action;
import actions.ActionQueue;
import game.GameLoader;
import game.State;
import transformations.Transform_Autoencoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Trying to see what sorts of things would work as a phase variable to indicate what part of the gait cycle we are
 * in. One logical choice is a neural network which compresses the full 72 state values to a single 1. This runs a
 * sample set of actions through and spits out what the network says.
 *
 * @author matt
 */
public class MAIN_SingleVarAutoencoder {

    public static void main(String[] args) {
        new MAIN_SingleVarAutoencoder().run();
    }
    public void run() {
        GameLoader game = new GameLoader();

        String modelDir = "src/main/resources/tflow_models/";
        Transform_Autoencoder autoencoder = new Transform_Autoencoder(modelDir + "AutoEnc_72to1_6layer.pb", 1);

        ActionQueue actionQueue = getSampleActions();

        List<State> stateList = new ArrayList<>();
        stateList.add(GameLoader.getInitialState());
        while (!actionQueue.isEmpty()) {
            game.stepGame(actionQueue.pollCommand());
            State st = game.getCurrentState();
//            System.out.println(st.body.getTh());
            stateList.add(st);
        }

        List<float[]> tformedSt = autoencoder.transform(stateList);
        for (float[] tf : tformedSt) {
            System.out.println(tf[0]);
        }

    }

    static ActionQueue getSampleActions() {
        // Ran MAIN_Search_LongRun to get these. 19 steps.
        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addAction(new Action(5, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(38, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(35, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(45, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(6, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(45, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(6, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(9, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(38, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(17, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(30, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(18, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(31, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(19, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(37, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(46, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(17, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(25, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(12, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(31, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(12, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(22, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(16, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(21, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(15, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(24, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(21, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(46, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(3, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(46, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(17, new boolean[]{false, false, false, false}));
        return actionQueue;
    }
}
