package game;

import data.LoadStateStatistics;
import tflowtools.TrainableNetwork;
import ui.PanelRunner;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class GameColdStartCorrected implements IGame{
    private static final int STATE_SIZE = 72;
    private static final int COMMAND_SIZE = 4;
    private static final int INPUT_SIZE = STATE_SIZE * 2 + COMMAND_SIZE;
    private static final int OUTPUT_SIZE = STATE_SIZE;
    State currentState;

    private GameUnified game = new GameUnified();
    private TrainableNetwork net;

    private static LoadStateStatistics.StateStatistics stateStats;
    static {
        try {
            stateStats = LoadStateStatistics.loadStatsFromFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private GameColdStartCorrected() {
        game.useWarmStarting = false;
//        game.iterations = 25;
        game.makeNewWorld();
        makeNewWorld();
    }

    public GameColdStartCorrected(String fileName, java.util.List<Integer> hiddenLayerSizes,
                       java.util.List<String> additionalArgs) throws FileNotFoundException {
        this();
        // Supplement the hidden layer sizes with the input and output sizes.
        List<Integer> allLayerSizes = new ArrayList<>(hiddenLayerSizes);
        allLayerSizes.add(0, INPUT_SIZE);
        allLayerSizes.add(OUTPUT_SIZE);

        net = TrainableNetwork.makeNewNetwork(fileName, allLayerSizes, additionalArgs);
    }

    /**
     * Constructor which uses existing model.
     * @param existingFile A .pb file referring to an existing model.
     * @throws FileNotFoundException Occurs when the specified model file is not found.
     */
    public GameColdStartCorrected(File existingFile) throws FileNotFoundException {
        this();
        net = new TrainableNetwork(existingFile);
    }

    /**
     * Step the game forward with the cold-started version of the game.
     */
    private State coldStepGame(State stateBefore, boolean[] transitionCommand) {
        game.setState(stateBefore);
        game.step(transitionCommand);
        return game.getCurrentState();
    }

    /**
     * Find the correction to apply to the cold-started game.
     */
    private float[] getStateCorrection(State stateBefore, boolean[] transitionCommand, State uncorrectedStateAfter) {

        float[][] netInput = new float[1][INPUT_SIZE];
        netInput[0] = makeSingleInput(stateBefore, transitionCommand, uncorrectedStateAfter);
        float[][] predictedCorrection = net.evaluateInput(netInput);
        return predictedCorrection[0];
    }

    /**
     * Given a start state and command, predict the next state with corrections applied.
     */
    private State correctedStepGame(State stateBefore, boolean[] transitionCommand) {
        State uncorrectedState = coldStepGame(stateBefore, transitionCommand);
        float[] correction = getStateCorrection(stateBefore, transitionCommand, uncorrectedState);
        float[] stateWithCorrection = uncorrectedState.flattenState();

        for (int i = 0; i < STATE_SIZE; i++) {
            stateWithCorrection[i] = stateWithCorrection[i] + correction[i];
        }
        if (false) {
            game.fullStatePDController(new State(stateWithCorrection, false));
            return coldStepGame(stateBefore, transitionCommand);
        } else {
            return new State(stateWithCorrection, false);
        }
    }

    /**
     * Assemble the input to the corrector.
     */
    private float[] makeSingleInput(State stateIn, boolean[] commandIn, State uncorrectedStateAfter) {
        float[] input = new float[INPUT_SIZE];
        float[] stateInNormalized = stateStats.standardizeState(stateIn);
        float[] uncorrectedStateNormalized = stateStats.standardizeState(uncorrectedStateAfter, -stateIn.body.getX());
        System.arraycopy(stateInNormalized, 0, input, 0, STATE_SIZE);
        System.arraycopy(uncorrectedStateNormalized, 0, input, STATE_SIZE, STATE_SIZE);
        input[STATE_SIZE * 2] = commandIn[0] ? 1f : 0f;
        input[STATE_SIZE * 2 + 1] = commandIn[1] ? 1f : 0f;
        input[STATE_SIZE * 2 + 2] = commandIn[2] ? 1f : 0f;
        input[STATE_SIZE * 2 + 3] = commandIn[3] ? 1f : 0f;
        return input;
    }

    /**
     * For training, find the correction which the corrector SHOULD produce.
     */
    private float[] getDesiredStateCorrection(State stateBefore, State uncorrectedState, State desiredStateAfter) {

        float[] coldStepState = uncorrectedState.flattenState();
        float[] desiredState = desiredStateAfter.flattenState();

        for (int i = 0; i < STATE_SIZE; i++) {
            desiredState[i] = desiredState[i] - coldStepState[i];
        }
        return desiredState;
    }


    public float doTrainingOnRun(List<State> states, List<boolean[]> commands) {
        float[][] fullInput = new float[states.size() - 1][INPUT_SIZE];
        float[][] fullOutput = new float[states.size() - 1][OUTPUT_SIZE];

        for (int i = 0; i < states.size() - 1; i++) {
            State st = states.get(i);
            State stNext = states.get(i + 1);
            boolean[] command = commands.get(i);

            // Input.
            State coldStepState = coldStepGame(st, command);
            fullInput[i] = makeSingleInput(st, command, coldStepState);

            // Desired output.
            fullOutput[i] = getDesiredStateCorrection(st, coldStepState, stNext);
        }

        float loss = net.trainingStep(fullInput, fullOutput, 1);
        System.out.println("Loss: " + loss);
        return loss;
    }

    public void saveCheckpoint(String name) {
        net.saveCheckpoint(name);
    }

    public void loadCheckpoint(String name) {
        net.loadCheckpoint(name);
    }

    @Override
    public void makeNewWorld() {
        game.makeNewWorld();
        currentState = GameUnified.getInitialState();
    }

    @Override
    public void step(boolean q, boolean w, boolean o, boolean p) {
        step(new boolean[]{q, w, o, p});
    }

    @Override
    public void step(boolean[] commands) {
        currentState = correctedStepGame(currentState, commands);
    }

    @Override
    public State getCurrentState() {
        return currentState; // TODO should I copy this?
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
    public void setState(State st) {

    }

    @Override
    public void applyBodyImpulse(float v, float v1) {

    }

    @Override
    public byte[] getFullState() {
        return new byte[0];
    }
}
