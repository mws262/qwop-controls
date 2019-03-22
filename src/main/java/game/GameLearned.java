package game;

import data.LoadStateStatistics;
import tflowtools.TrainableNetwork;
import ui.PanelRunner;

import java.awt.*;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("Duplicates")
public class GameLearned implements IGame {
    /**
     * Potential key combinations.
     */
    public enum Keys {
        q, w, o, p, qp, wo, qo, wp, none
    }

    /**
     * Association between the key labels to the q, w, o, p boolean buttons pressed.
     */
//    private final static Map<boolean[], GameLearned.Keys> buttonsToLabels = new HashMap<>();
//    static {
//        buttonsToLabels.put(new boolean[]{true, false, false, false}, Keys.q);
//        buttonsToLabels.put(new boolean[]{false, true, false, false}, Keys.w);
//        buttonsToLabels.put(new boolean[]{false, false, true, false}, Keys.o);
//        buttonsToLabels.put(new boolean[]{false, false, false, true}, Keys.p);
//        buttonsToLabels.put(new boolean[]{true, false, false, true}, Keys.qp);
//        buttonsToLabels.put(new boolean[]{false, true, true, false}, Keys.wo);
//        buttonsToLabels.put(new boolean[]{true, false, true, false}, Keys.qo);
//        buttonsToLabels.put(new boolean[]{false, true, false, true}, Keys.wp);
//        buttonsToLabels.put(new boolean[]{false, false, false, false}, Keys.none);
//    }

    private final static Map<GameLearned.Keys, float[]> labelsToOneHot = new HashMap<>();
    static {
        labelsToOneHot.put(GameLearned.Keys.q, new float[] {1, 0, 0, 0, 0, 0, 0, 0, 0});
        labelsToOneHot.put(GameLearned.Keys.w, new float[] {0, 1, 0, 0, 0, 0, 0, 0, 0});
        labelsToOneHot.put(GameLearned.Keys.o, new float[] {0, 0, 1, 0, 0, 0, 0, 0, 0});
        labelsToOneHot.put(GameLearned.Keys.p, new float[] {0, 0, 0, 1, 0, 0, 0, 0, 0});
        labelsToOneHot.put(GameLearned.Keys.qp, new float[] {0, 0, 0, 0, 1, 0, 0, 0, 0});
        labelsToOneHot.put(GameLearned.Keys.wo, new float[] {0, 0, 0, 0, 0, 1, 0, 0, 0});
        labelsToOneHot.put(GameLearned.Keys.qo, new float[] {0, 0, 0, 0, 0, 0, 1, 0, 0});
        labelsToOneHot.put(GameLearned.Keys.wp, new float[] {0, 0, 0, 0, 0, 0, 0, 1, 0});
        labelsToOneHot.put(GameLearned.Keys.none, new float[] {0, 0, 0, 0, 0, 0, 0, 0, 1});
    }


    private static final int numActions = Keys.values().length;
    private static final int outputSize = 72; // TODO
    private static final int positionStateSize = 36;
    private static final int inputSize = 3 * (positionStateSize + numActions); // TODO


    private TrainableNetwork net;

    private Keys commandTwoAgo = Keys.none;
    private Keys commandOneAgo = Keys.none;
    private Keys commandCurrent = Keys.none;

    private float[] poseTwoAgo = new float[positionStateSize];
    private float[] poseOneAgo = new float[positionStateSize];
    private float[] poseCurrent = new float[positionStateSize];


    public LoadStateStatistics.StateStatistics stateStats;

    State currentPredictedState;

    private float currentTorsoX;

    public GameLearned() {
        try {
            stateStats = LoadStateStatistics.loadStatsFromFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(inputSize);
        layerSizes.add(144);
        layerSizes.add(64);
        layerSizes.add(outputSize);
        try {
            net = TrainableNetwork.makeNewNetwork("simulator_graph", layerSizes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Does not assume that previous timesteps are assigned. This basically wipes the "memory" of the game.
    public void giveAllStates(State current, State oneAgo, State twoAgo, Keys commandOneAgo, Keys commandTwoAgo) {
        currentTorsoX = current.body.getX();

        int idx = 0;
        for (StateVariable state : current.getStates()) {
            poseCurrent[idx++] = state.getX() - currentTorsoX;
            poseCurrent[idx++] = state.getY();
            poseCurrent[idx++] = state.getTh();
        }

        idx = 0;
        for (StateVariable state : oneAgo.getStates()) {
            poseOneAgo[idx++] = state.getX() - currentTorsoX; // Still torso x from current, so will not be zero for one ago.
            poseOneAgo[idx++] = state.getY();
            poseOneAgo[idx++] = state.getTh();
        }

        idx = 0;
        for (StateVariable state : twoAgo.getStates()) {
            poseTwoAgo[idx++] = state.getX() - currentTorsoX; // Still torso x from current, so will not be zero for two ago.
            poseTwoAgo[idx++] = state.getY();
            poseTwoAgo[idx++] = state.getTh();
        }

        this.commandOneAgo = commandOneAgo;
        this.commandTwoAgo = commandTwoAgo;
    }

    // Assumes that previous two timesteps are set already.
    public void updateStates(State newCurrentState) {
        float torsoXDiff = currentTorsoX - newCurrentState.body.getX();
        currentTorsoX = newCurrentState.body.getX();

        for (int i = 0; i < poseOneAgo.length; i += 3) { // Every third is an x coordinate.
            poseTwoAgo[i] = poseOneAgo[i] + torsoXDiff;
            poseTwoAgo[i + 1] = poseOneAgo[i + 1];
            poseTwoAgo[i + 2] = poseOneAgo[i + 2];
        }

        for (int i = 0; i < poseCurrent.length; i += 3) { // Every third is an x coordinate.
            poseOneAgo[i] = poseCurrent[i] + torsoXDiff;
            poseOneAgo[i + 1] = poseCurrent[i + 1];
            poseOneAgo[i + 2] = poseCurrent[i + 2];
        }

        int idx = 0;
        for (StateVariable state : newCurrentState.getStates()) {
            poseCurrent[idx++] = state.getX() - currentTorsoX;
            poseCurrent[idx++] = state.getY();
            poseCurrent[idx++] = state.getTh();
        }
        commandTwoAgo = commandOneAgo;
        commandOneAgo = commandCurrent;
        commandCurrent = null; // Awaiting new command.

    }


    // TODO: seriously refactor once things are working.
    // Normalizes the 3 poses, this one, one ago, and two ago, then finds first and second differences. Flattens
    // these to a single array with the past three actions tacked onto the end in one-hot representation.
    private float[] assembleInput() {
        float[] normalizedTwoAgo = new float[positionStateSize];
        float[] normalizedOneAgo = new float[positionStateSize];
        float[] normalizedCurrent = new float[positionStateSize];

        // Normalize the poses.
        for (int statIdx = 0, poseIdx = 0; statIdx < outputSize; statIdx += 6, poseIdx += 3) {
            // Normalize poses from two ago
            normalizedTwoAgo[poseIdx] = stateStats.stdev[statIdx] == 0 ?
                    0 : (poseTwoAgo[poseIdx] - stateStats.mean[statIdx]) / stateStats.stdev[statIdx];
            normalizedTwoAgo[poseIdx + 1] =
                    (poseTwoAgo[poseIdx + 1] - stateStats.mean[statIdx + 1]) / stateStats.stdev[statIdx + 1];
            normalizedTwoAgo[poseIdx + 2] =
                    (poseTwoAgo[poseIdx + 2] - stateStats.mean[statIdx + 2]) / stateStats.stdev[statIdx + 2];

            // From one timestep past.
            normalizedOneAgo[poseIdx] = stateStats.stdev[statIdx] == 0 ?
                    0 : (poseOneAgo[poseIdx] - stateStats.mean[statIdx]) / stateStats.stdev[statIdx];
            normalizedOneAgo[poseIdx + 1] =
                    (poseOneAgo[poseIdx + 1] - stateStats.mean[statIdx + 1]) / stateStats.stdev[statIdx + 1];
            normalizedOneAgo[poseIdx + 2] =
                    (poseOneAgo[poseIdx + 2] - stateStats.mean[statIdx + 2]) / stateStats.stdev[statIdx + 2];

            // From current timestep.
            normalizedCurrent[poseIdx] = stateStats.stdev[statIdx] == 0 ?
                    0 : (poseCurrent[poseIdx] - stateStats.mean[statIdx]) / stateStats.stdev[statIdx];
            normalizedCurrent[poseIdx + 1] =
                    (poseCurrent[poseIdx + 1] - stateStats.mean[statIdx + 1]) / stateStats.stdev[statIdx + 1];
            normalizedCurrent[poseIdx + 2] =
                    (poseCurrent[poseIdx + 2] - stateStats.mean[statIdx + 2]) / stateStats.stdev[statIdx + 2];
        }

        // Calculate first and second difference.
        float[] firstDifference1 = new float[positionStateSize]; // Most recent two.
        float[] firstDifference2 = new float[positionStateSize]; // Ones before that.
        float[] secondDifference = new float[positionStateSize]; // Ones before that.

        for (int i = 0; i < positionStateSize; i++) {
            firstDifference1[i] = normalizedCurrent[i] - normalizedOneAgo[i];
            firstDifference2[i] = normalizedOneAgo[i] - normalizedTwoAgo[i];
            secondDifference[i] = firstDifference1[i] - firstDifference2[i];
        }

        // Assemble.
        float[] currentNetInput = new float[inputSize];
        // Current pose.
        System.arraycopy(normalizedCurrent,0, currentNetInput, 0, positionStateSize);
        // First difference pose (velocity-ish)
        System.arraycopy(firstDifference1,0, currentNetInput, positionStateSize, positionStateSize);
        // Second difference pose (acceleration-ish)
        System.arraycopy(secondDifference,0, currentNetInput, positionStateSize * 2, positionStateSize);
        // 9-element, one hot representation of the current command being pressed.
        System.arraycopy(labelsToOneHot.get(commandCurrent), 0, currentNetInput, positionStateSize * 3, numActions);
        // One hot for previous command.
        System.arraycopy(labelsToOneHot.get(commandOneAgo), 0, currentNetInput, positionStateSize * 3 + numActions,
                numActions);
        // One hot for command two timesteps ago.
        System.arraycopy(labelsToOneHot.get(commandTwoAgo), 0, currentNetInput, positionStateSize * 3 + numActions * 2,
                numActions);

        return currentNetInput;
    }

    @Override
    public void makeNewWorld() {

    }

    @Override
    public void step(boolean q, boolean w, boolean o, boolean p) {
            step(new boolean[] {q, w, o, p});
    }

    @Override
    public void step(boolean[] commands) {
        commandCurrent = matchBooleansToKeys(commands); //buttonsToLabels.get(commands);

        float[] assembledInput = assembleInput();
        float[][] singleInput = new float[1][inputSize]; // Could potentially feed more than one evaluation at a
        // time, but we don't want to here, so one dimension is singleton.
        singleInput[0] = assembledInput;

        float[][] netOutput = net.evaluateInput(singleInput);
        currentPredictedState = new State(netOutput[0], false);
        updateStates(currentPredictedState);
    }

    @Override
    public State getCurrentState() {
        return currentPredictedState;
    }

    @Override
    public boolean getFailureStatus() {
        return false;
    }

    @Override
    public long getTimestepsSimulatedThisGame() {
        return 0;
    }

    @Override
    public void draw(Graphics g, float runnerScaling, int xOffsetPixels, int yOffsetPixels) {

        GameUnified realGame = new GameUnified();
        realGame.drawExtraRunner((Graphics2D) g, getCurrentState(), "", runnerScaling, xOffsetPixels, yOffsetPixels, Color.RED, PanelRunner.normalStroke);
    }

    @Override
    public void setState(State st) {

    }

    @Override
    public void applyBodyImpulse(float v, float v1) {

    }

    @Override
    public byte[] getFullState() {
        return new byte[0];
    }

    private Keys matchBooleansToKeys(boolean[] commands) {
        if (commands[0]) {
            if (commands[2]) {
                return Keys.qo;
            }else if (commands[3]) {
                return Keys.qp;
            }else {
                return Keys.q;
            }
        } else if (commands[1]) {
            if (commands[2]) {
                return Keys.wo;
            }else if (commands[3]) {
                return Keys.wp;
            } else {
                return Keys.w;
            }
        } else if (commands[2]) {
            return Keys.o;
        } else if (commands[3]) {
            return Keys.p;
        } else {
            return Keys.none;
        }
    }
}
