package game;

import data.LoadStateStatistics;
import tflowtools.TrainableNetwork;
import ui.PanelRunner;

import java.awt.*;
import java.io.File;
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


    private static LoadStateStatistics.StateStatistics stateStats;
    static {
        try {
            stateStats = LoadStateStatistics.loadStatsFromFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    State currentPredictedState;

    private float currentTorsoX;

    public GameLearned(String fileName, List<Integer> hiddenLayerSizes,
                             List<String> additionalArgs) throws FileNotFoundException {

        // Supplement the hidden layer sizes with the input and output sizes.
        List<Integer> allLayerSizes = new ArrayList<>(hiddenLayerSizes);
        allLayerSizes.add(0, inputSize);
        allLayerSizes.add(outputSize);

        net = TrainableNetwork.makeNewNetwork(fileName, allLayerSizes, additionalArgs);
    }

    /**
     * Constructor which uses existing model.
     * @param existingFile A .pb file referring to an existing model.
     * @throws FileNotFoundException Occurs when the specified model file is not found.
     */
    public GameLearned(File existingFile) throws FileNotFoundException {
        net = new TrainableNetwork(existingFile);
    }

    /**
     * Load a checkpoint file for the neural network. This should be in the checkpoints directory, and should not
     * include any file extensions.
     * @param checkpointName Name of the checkpoint to load.
     */
    public void loadCheckpoint(String checkpointName) {
        assert !checkpointName.isEmpty();
        net.loadCheckpoint(checkpointName);
    }

    /**
     * Save a checkpoint file for the neural networks (basically all weights and biases). Directory automatically
     * chosen.
     * @param checkpointName Name of the checkpoint file. Do not include file extension or directory.
     * @return A list of checkpoint with that name.
     */
    public List<File> saveCheckpoint(String checkpointName) {
        assert !checkpointName.isEmpty();
        return net.saveCheckpoint(checkpointName);
    }

    public void assembleWholeRunForTraining(List<State> states, List<boolean[]> commands) {
        float[][] fullInput = new float[states.size() - 3][inputSize];
        float[][] fullOutput = new float[states.size() - 3][outputSize];

        for (int i = 2; i < states.size() - 1; i++) {

            // Assemble input.
            State stateTwoAgo = states.get(i - 2);
            State stateOneAgo = states.get(i - 1);
            State stateCurrent = states.get(i);

            boolean[] commandTwoAgo = commands.get(i - 2);
            boolean[] commandOneAgo = commands.get(i - 1);
            boolean[] commandCurrent = commands.get(i);

            float torsoX = stateCurrent.body.getX();

            int idx = 0;
            for (StateVariable state : stateCurrent.getStates()) {
                poseCurrent[idx++] = state.getX() - torsoX;
                poseCurrent[idx++] = state.getY();
                poseCurrent[idx++] = state.getTh();
            }

            idx = 0;
            for (StateVariable state : stateOneAgo.getStates()) {
                poseOneAgo[idx++] = state.getX() - torsoX; // Still torso x from current, so will not be zero for one ago.
                poseOneAgo[idx++] = state.getY();
                poseOneAgo[idx++] = state.getTh();
            }

            idx = 0;
            for (StateVariable state : stateTwoAgo.getStates()) {
                poseTwoAgo[idx++] = state.getX() - torsoX; // Still torso x from current, so will not be zero for two ago.
                poseTwoAgo[idx++] = state.getY();
                poseTwoAgo[idx++] = state.getTh();
            }

            this.commandTwoAgo = matchBooleansToKeys(commandTwoAgo);
            this.commandOneAgo = matchBooleansToKeys(commandOneAgo);
            this.commandCurrent = matchBooleansToKeys(commandCurrent);

            fullInput[i - 2] = assembleInput(poseCurrent, poseOneAgo, poseTwoAgo, this.commandCurrent,
                    this.commandOneAgo,
                    this.commandTwoAgo);

            // Assemble output for training.
            State nextState = states.get(i + 1);
            idx = 0;
            for (StateVariable state : nextState.getStates()) {
                fullOutput[i - 2][idx++] = state.getX() - torsoX; // Still torso x from current, hopefully this result
                // will be positive.
                fullOutput[i - 2][idx++] = state.getY();
                fullOutput[i - 2][idx++] = state.getTh();
            }
        }
        net.trainingStep(fullInput, fullOutput, 2);
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
    private static float[] assembleInput(float[] poseCurrent, float[] poseOneAgo, float[] poseTwoAgo,
                                         Keys commandCurrent, Keys commandOneAgo, Keys commandTwoAgo) {
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
        giveAllStates(GameUnified.getInitialState(), GameUnified.getInitialState(),
                GameUnified.getInitialState(), GameLearned.Keys.none, GameLearned.Keys.none);
    }

    @Override
    public void step(boolean q, boolean w, boolean o, boolean p) {
            step(new boolean[] {q, w, o, p});
    }

    @Override
    public void step(boolean[] commands) {
        commandCurrent = matchBooleansToKeys(commands); //buttonsToLabels.get(commands);

        float[] assembledInput = assembleInput(poseCurrent, poseOneAgo, poseTwoAgo, commandCurrent, commandOneAgo,
                commandTwoAgo);
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
        GameUnified.drawExtraRunner((Graphics2D) g, getCurrentState(), "", runnerScaling, xOffsetPixels, yOffsetPixels, Color.RED, PanelRunner.normalStroke);
    }

    @Override
    public void setState(State st) {}

    @Override
    public void applyBodyImpulse(float v, float v1) {}

    @Override
    public byte[] getFullState() {
        return new byte[0];
    }

    private static Keys matchBooleansToKeys(boolean[] commands) {
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
