package game;

public class GameColdStartCorrected {
    private static final int STATE_SIZE = 72;
    private static final int COMMAND_SIZE = 4;

    State statePrevious;
    State stateNext;
    boolean[] command;

    private GameUnified game = new GameUnified();

    public GameColdStartCorrected() {
        game.useWarmStarting = false;
        game.makeNewWorld();
    }

    /**
     * @param stateBefore
     * @param transitionCommand
     * @return Uncorrected State after one timestep cold-started.
     */
    private State coldStepGame(State stateBefore, boolean[] transitionCommand) {
        game.setState(stateBefore);
        game.step(transitionCommand);
        return game.getCurrentState();
    }

    private float[] getStateCorrection(State stateBefore, boolean[] transitionCommand, State uncorrectedStateAfter) {

        makeSingleInput(stateBefore, transitionCommand);
        return null;
    }

    private State correctedStepGame(State stateBefore, boolean[] transitionCommand) {
        State uncorrectedState = coldStepGame(stateBefore, transitionCommand);
        float[] correction = getStateCorrection(stateBefore, transitionCommand, uncorrectedState);
        float[] stateWithCorrection = uncorrectedState.flattenState();

        for (int i = 0; i < STATE_SIZE; i++) {
            stateWithCorrection[i] = stateWithCorrection[i] + correction[i];
        }
        return new State(stateWithCorrection, false);
    }

    private float[] makeSingleInput(State stateIn, boolean[] commandIn) {
        float[] input = new float[STATE_SIZE + COMMAND_SIZE];

        System.arraycopy(stateIn.flattenState(), 0, input, 0, STATE_SIZE);
        input[STATE_SIZE] = commandIn[0] ? 1f : 0f;
        input[STATE_SIZE + 1] = commandIn[1] ? 1f : 0f;
        input[STATE_SIZE + 2] = commandIn[2] ? 1f : 0f;
        input[STATE_SIZE + 3] = commandIn[3] ? 1f : 0f;
        return input;
    }

    // For training.
    // This is what the corrector should spit out.
    private float[] getDesiredStateCorrection(State stateBefore, boolean[] transitionCommand, State desiredStateAfter) {

        float[] coldStepState = coldStepGame(stateBefore, transitionCommand).flattenState();
        float[] desiredState = desiredStateAfter.flattenState();

        for (int i = 0; i < STATE_SIZE; i++) {
            desiredState[i] = desiredState[i] - coldStepState[i];
        }
        return desiredState;
    }
}
