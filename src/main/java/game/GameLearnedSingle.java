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
public class GameLearnedSingle implements IGame {

    private static final int numActions = 4;//Keys.values().length;
    private static final int outputSize = 72; // TODO
    private static final int positionStateSize = 36;
    private static final int inputSize = 72 + 4;

    private TrainableNetwork net;

    private boolean[] commandCurrent;

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

    public GameLearnedSingle(String fileName, List<Integer> hiddenLayerSizes,
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
    public GameLearnedSingle(File existingFile) throws FileNotFoundException {
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
        float[][] fullInput = new float[states.size() - 1][inputSize];
        float[][] fullOutput = new float[states.size() - 1][outputSize];

        for (int i = 0; i < states.size() - 1; i++) {
            // Assemble input.
            State stateCurrent = states.get(i);
            float torsoX = stateCurrent.body.getX();

            fullInput[i] = assembleInput(stateCurrent, commands.get(i));
            float[] stateOutRescaled = stateStats.rescaleState(states.get(i + 1));
            float xdiff = states.get(i + 1).body.getX() - torsoX;

            for (int j = 0; j < stateOutRescaled.length; j += 6) {
                stateOutRescaled[j] += xdiff;
            }

            fullOutput[i] = stateOutRescaled;

        }
        float loss = net.trainingStep(fullInput, fullOutput, 1);
        System.out.println("Loss: " + loss);
    }

    private float[] assembleInput(State state, boolean[] command) {

        float[] input = new float[inputSize];

        float[] stateInRescaled = stateStats.rescaleState(state);

        System.arraycopy(stateInRescaled, 0, input, 0, stateInRescaled.length);
        input[72] = command[0] ? 1f : 0f;
        input[73] = command[1] ? 1f : 0f;
        input[74] = command[2] ? 1f : 0f;
        input[75] = command[3] ? 1f : 0f;
        return input;
    }

    @Override
    public void makeNewWorld() {
        currentPredictedState = GameUnified.getInitialState();
    }

    @Override
    public void step(boolean q, boolean w, boolean o, boolean p) {
        step(new boolean[] {q, w, o, p});
    }

    @Override
    public void step(boolean[] commands) {
        commandCurrent = commands;
//        commandCurrent = Action.booleansToKeys(commands);

        float[][] input = new float[1][inputSize];
        input[0] = assembleInput(currentPredictedState, commands);

        float[][] netOutput = net.evaluateInput(input);

        for (int i = 0; i < outputSize; i++) {

            if (i % 6 == 0) {
                netOutput[0][i] = netOutput[0][i] * (stateStats.range[i] > 0 ?
                        stateStats.range[i] : 1) + currentPredictedState.body.getX() + stateStats.min[i];
            } else {
                netOutput[0][i] = netOutput[0][i] * stateStats.range[i] + stateStats.min[i];
            }
        }
        currentPredictedState = new State(netOutput[0], false);
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
