package game;

import data.LoadStateStatistics;
import tflowtools.TrainableNetwork;
import ui.PanelRunner;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class GameLearned implements IGame {

    private static final int numActions = 4;//Keys.values().length;
    private static final int outputSize = 72; // TODO
    private static final int positionStateSize = 36;
    private static final int inputSize = 3 * (positionStateSize + numActions); // TODO

    private TrainableNetwork net;

    private boolean[] commandTwoAgo;
    private boolean[] commandOneAgo;
    private boolean[] commandCurrent;

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

    private State currentPredictedState;

    private float currentTorsoX;

    public GameLearned(String fileName, List<Integer> hiddenLayerSizes,
                       List<String> additionalArgs) throws FileNotFoundException {

        // Supplement the hidden layer sizes with the input and output sizes.
        List<Integer> allLayerSizes = new ArrayList<>(hiddenLayerSizes);
        allLayerSizes.add(0, inputSize);
        allLayerSizes.add(outputSize);

        net = TrainableNetwork.makeNewNetwork(fileName, allLayerSizes, additionalArgs);
        makeNewWorld();
    }

    /**
     * Constructor which uses existing model.
     * @param existingFile A .pb file referring to an existing model.
     * @throws FileNotFoundException Occurs when the specified model file is not found.
     */
    public GameLearned(File existingFile) throws FileNotFoundException {
        net = new TrainableNetwork(existingFile);
        makeNewWorld();
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
                poseCurrent[idx++] = state.getX();
                poseCurrent[idx++] = state.getY();
                poseCurrent[idx++] = state.getTh();
            }

            idx = 0;
            for (StateVariable state : stateOneAgo.getStates()) {
                poseOneAgo[idx++] = state.getX(); // Still torso x from current, so will not be zero for one ago.
                poseOneAgo[idx++] = state.getY();
                poseOneAgo[idx++] = state.getTh();
            }

            idx = 0;
            for (StateVariable state : stateTwoAgo.getStates()) {
                poseTwoAgo[idx++] = state.getX(); // Still torso x from current, so will not be zero for two ago.
                poseTwoAgo[idx++] = state.getY();
                poseTwoAgo[idx++] = state.getTh();
            }

            float[] command0 = new float[]{
                    commandCurrent[0] ? 1f : 0f,
                    commandCurrent[1] ? 1f : 0f,
                    commandCurrent[2] ? 1f : 0f,
                    commandCurrent[3] ? 1f : 0f};
            float[] command1 = new float[]{
                    commandOneAgo[0] ? 1f : 0f,
                    commandOneAgo[1] ? 1f : 0f,
                    commandOneAgo[2] ? 1f : 0f,
                    commandOneAgo[3] ? 1f : 0f};
            float[] command2 = new float[]{
                    commandTwoAgo[0] ? 1f : 0f,
                    commandTwoAgo[1] ? 1f : 0f,
                    commandTwoAgo[2] ? 1f : 0f,
                    commandTwoAgo[3] ? 1f : 0f};

            fullInput[i - 2] = assembleInput(poseCurrent, poseOneAgo, poseTwoAgo, command0,
                    command1,
                    command2, torsoX);

            // Assemble output for training.
            State nextState = states.get(i + 1);
            idx = 0;
            for (StateVariable state : nextState.getStates()) {
                fullOutput[i - 2][idx] = (state.getX() - torsoX - stateStats.min[idx])/ (stateStats.range[idx] > 0 ?
                        stateStats.range[idx] : 1);
                idx++;
                fullOutput[i - 2][idx] = (state.getY() - stateStats.min[idx]) / stateStats.range[idx];
                idx++;
                fullOutput[i - 2][idx] = (state.getTh() - stateStats.min[idx]) / stateStats.range[idx];
                idx++;
                fullOutput[i - 2][idx] = (state.getDx() - stateStats.min[idx]) / stateStats.range[idx];
                idx++;
                fullOutput[i - 2][idx] = (state.getDy() - stateStats.min[idx]) / stateStats.range[idx];
                idx++;
                fullOutput[i - 2][idx] = (state.getDth() - stateStats.min[idx]) / stateStats.range[idx];
                idx++;
            }
        }
        float loss = net.trainingStep(fullInput, fullOutput, 1);
        System.out.println("Loss: " + loss);
    }

    // Does not assume that previous timesteps are assigned. This basically wipes the "memory" of the game.
    public void giveAllStates(State current, State oneAgo, State twoAgo, boolean[] commandOneAgo,
                              boolean[] commandTwoAgo) {
        currentTorsoX = current.body.getX();

        int idx = 0;
        for (StateVariable state : current.getStates()) {
            poseCurrent[idx++] = state.getX();
            poseCurrent[idx++] = state.getY();
            poseCurrent[idx++] = state.getTh();
        }

        idx = 0;
        for (StateVariable state : oneAgo.getStates()) {
            poseOneAgo[idx++] = state.getX(); // Still torso x from current, so will not be zero for one ago.
            poseOneAgo[idx++] = state.getY();
            poseOneAgo[idx++] = state.getTh();
        }

        idx = 0;
        for (StateVariable state : twoAgo.getStates()) {
            poseTwoAgo[idx++] = state.getX(); // Still torso x from current, so will not be zero for two ago.
            poseTwoAgo[idx++] = state.getY();
            poseTwoAgo[idx++] = state.getTh();
        }

        this.commandOneAgo = commandOneAgo;
        this.commandTwoAgo = commandTwoAgo;
    }

    // Assumes that previous two timesteps are set already.
    public void updateStates(State newCurrentState) {
//        float normBodyX = newCurrentState.body.getX() - stateStats.mean;
        currentTorsoX = newCurrentState.body.getX();

        poseTwoAgo = poseOneAgo;
        poseOneAgo = poseCurrent;

        int idx = 0;
        for (StateVariable state : newCurrentState.getStates()) {
            poseCurrent[idx++] = state.getX();
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
                                         float[] commandCurrent, float[] commandOneAgo, float[] commandTwoAgo,
                                         float torsoXOffset) {
        float[] normalizedTwoAgo = new float[positionStateSize];
        float[] normalizedOneAgo = new float[positionStateSize];
        float[] normalizedCurrent = new float[positionStateSize];

        // Normalize the poses.
        for (int statIdx = 0, poseIdx = 0; statIdx < outputSize; statIdx += 6, poseIdx += 3) {
            // Normalize poses from two ago
            normalizedTwoAgo[poseIdx] =
                    (poseTwoAgo[poseIdx] - torsoXOffset - stateStats.min[statIdx])/(stateStats.range[statIdx] > 0 ?
                    stateStats.range[statIdx] : 1);
            normalizedTwoAgo[poseIdx + 1] =
                    (poseTwoAgo[poseIdx + 1] - stateStats.min[statIdx + 1]) / stateStats.range[statIdx + 1];
            normalizedTwoAgo[poseIdx + 2] =
                    (poseTwoAgo[poseIdx + 2] - stateStats.min[statIdx + 2]) / stateStats.range[statIdx + 2];

            // From one timestep past.
            normalizedOneAgo[poseIdx] = (poseOneAgo[poseIdx] - torsoXOffset - stateStats.min[statIdx])/(stateStats.range[statIdx] > 0 ?
                    stateStats.range[statIdx] : 1);
            normalizedOneAgo[poseIdx + 1] =
                    (poseOneAgo[poseIdx + 1] - stateStats.min[statIdx + 1]) / stateStats.range[statIdx + 1];
            normalizedOneAgo[poseIdx + 2] =
                    (poseOneAgo[poseIdx + 2] - stateStats.min[statIdx + 2]) / stateStats.range[statIdx + 2];

            // From current timestep.
            normalizedCurrent[poseIdx] = (poseCurrent[poseIdx] - torsoXOffset - stateStats.min[statIdx])/(stateStats.range[statIdx] > 0 ?
                    stateStats.range[statIdx] : 1);
            normalizedCurrent[poseIdx + 1] =
                    (poseCurrent[poseIdx + 1] - stateStats.min[statIdx + 1]) / stateStats.range[statIdx + 1];
            normalizedCurrent[poseIdx + 2] =
                    (poseCurrent[poseIdx + 2] - stateStats.min[statIdx + 2]) / stateStats.range[statIdx + 2];
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
        System.arraycopy(commandCurrent, 0, currentNetInput, positionStateSize * 3,
                numActions);
        // One hot for previous command.
        System.arraycopy(commandOneAgo, 0, currentNetInput, positionStateSize * 3 + numActions,
                numActions);
        // One hot for command two timesteps ago.
        System.arraycopy(commandTwoAgo, 0, currentNetInput, positionStateSize * 3 + numActions * 2,
                numActions);

        return currentNetInput;
    }

    @Override
    public void makeNewWorld() {
        giveAllStates(GameUnified.getInitialState(), GameUnified.getInitialState(),
                GameUnified.getInitialState(), new boolean[]{false, false, false, false}, new boolean[]{false, false, false, false});
    }

    @Override
    public void step(boolean q, boolean w, boolean o, boolean p) {
        step(new boolean[] {q, w, o, p});
    }

    @Override
    public void step(boolean[] commands) {
        commandCurrent = commands;
//        commandCurrent = Action.booleansToKeys(commands);
        float[] command0 = new float[]{
                commandCurrent[0] ? 1f : 0f,
                commandCurrent[1] ? 1f : 0f,
                commandCurrent[2] ? 1f : 0f,
                commandCurrent[3] ? 1f : 0f};
        float[] command1 = new float[]{
                commandOneAgo[0] ? 1f : 0f,
                commandOneAgo[1] ? 1f : 0f,
                commandOneAgo[2] ? 1f : 0f,
                commandOneAgo[3] ? 1f : 0f};
        float[] command2 = new float[]{
                commandTwoAgo[0] ? 1f : 0f,
                commandTwoAgo[1] ? 1f : 0f,
                commandTwoAgo[2] ? 1f : 0f,
                commandTwoAgo[3] ? 1f : 0f};


        float[] assembledInput = assembleInput(poseCurrent, poseOneAgo, poseTwoAgo, command0, command1,
                command2, poseCurrent[0]);
        float[][] singleInput = new float[1][inputSize]; // Could potentially feed more than one evaluation at a
        // time, but we don't want to here, so one dimension is singleton.
        singleInput[0] = assembledInput;

        float[][] netOutput = net.evaluateInput(singleInput);

        for (int i = 0; i < outputSize; i++) {

            if (i % 6 == 0) {
                netOutput[0][i] = netOutput[0][i] * (stateStats.range[i] > 0 ?
                        stateStats.range[i] : 1) + poseCurrent[0] + stateStats.min[i];
            } else {
                netOutput[0][i] = netOutput[0][i] * stateStats.range[i] + stateStats.min[i];
            }
        }
        currentPredictedState = new State(netOutput[0], false);
        //updateStates(currentPredictedState);
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
        if (getCurrentState() != null)
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

}
