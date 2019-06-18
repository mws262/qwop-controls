package game.state;

import data.LoadStateStatistics;

import java.util.Arrays;

public class StateDelayEmbedded implements IState {

    private State[] individualStates;

    private final int stateVariableCount;


    public static boolean useFiniteDifferences = false;

    // Order is NEWEST to OLDEST
    public StateDelayEmbedded(State[] states) {
        individualStates = Arrays.copyOf(states, states.length);
        // Configurations variables (3), number of individual States making up this composite, number of body parts.
        stateVariableCount = individualStates.length * individualStates[0].getStateVariableCount();
    }

    @Override
    public float[] flattenState() {

        float[] flatState = new float[3 * stateVariableCount];

        // Subtract out the current body x from all other x coordinates, even the time delayed ones.
        float xOffset = individualStates[0].getCenterX();

        if (!useFiniteDifferences) {
            for (int i = 0; i < individualStates.length; i++) {
                float[] flatPositions = individualStates[i].extractPositions(xOffset);
                System.arraycopy(flatPositions, 0, flatState, i * 36, 36);
            }
        } else {
            State[] differencedStates = getDifferencedStates(individualStates);
            for (int i = 0; i < individualStates.length; i++) {
                if (i > 0) {
                    xOffset = 0;
                }
                System.arraycopy(differencedStates[i].extractPositions(xOffset), 0, flatState, i * 36, 36);
            }

        }

        return flatState;
    }

    State[] getDifferencedStates(State[] states) {
        State[] finalDifferences = new State[states.length];

        finalDifferences[0] = states[0];
        for (int j = 1; j < finalDifferences.length; j++) {
            State[] stateDiff = new State[states.length - 1];

            for (int i = 0; i < states.length - 1; i++) {
                stateDiff[i] = states[i + 1].subtract(states[i]);
            }
            finalDifferences[j] = stateDiff[0];
            states = stateDiff;
        }
        return finalDifferences;
    }

    @Override
    public float[] flattenStateWithRescaling(LoadStateStatistics.StateStatistics stateStatistics) {
        float[] flatState = new float[3 * stateVariableCount];
        float xOffset = individualStates[0].getCenterX();

        for (int i = 0; i < individualStates.length; i++) {
            float[] flatRescaled =
                    individualStates[i]
                            .xOffsetSubtract(xOffset)
                            .subtract(stateStatistics.getMean())
                            .divide(stateStatistics.getStdev())
                            .extractPositions();
            System.arraycopy(flatRescaled, 0, flatState, i * 36, 36);
        }

        return flatState;
    }

    // Just applies to the most recent state for now.
    @Override
    public StateVariable getStateVariableFromName(ObjectName obj) {
        return individualStates[0].getStateVariableFromName(obj);
    }

    @Override
    public StateVariable[] getAllStateVariables() {
        return individualStates[0].getAllStateVariables();
    }

    @Override
    public float getCenterX() {
        return individualStates[0].getCenterX();
    }

    @Override
    public boolean isFailed() {
        return individualStates[0].isFailed();
    }

    @Override
    public int getStateVariableCount() {
        return stateVariableCount;
    }
}
