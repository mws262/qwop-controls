package game.state;

import data.LoadStateStatistics;

/**
 * For each additional normal state given, this will use the poses from the first, use the first two poses to to get
 * a first difference, use the first three to get a third difference, and so on.
 *
 * [36 poses, 36 first differences, 36 second differences, ...]
 *
 * @author matt
 */
public class StateDelayEmbedded_HigherDifferences extends StateDelayEmbedded {
    public StateDelayEmbedded_HigherDifferences(State[] states) {
        super(states);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public float[] flattenState() {

        float[] flatState = new float[3 * stateVariableCount];

        // Subtract out the current body x from all other x coordinates, even the time delayed ones.
        float xOffset = individualStates[0].getCenterX();

        State[] differencedStates = getDifferencedStates(individualStates);
        for (int i = 0; i < individualStates.length; i++) {
            if (i > 0) {
                xOffset = 0;
            }
            System.arraycopy(differencedStates[i].extractPositions(xOffset), 0, flatState, i * 36, 36);
        }
        return flatState;
    }

    @Override
    public float[] flattenStateWithRescaling(LoadStateStatistics.StateStatistics stateStatistics) {
        float[] flatState = flattenState();
        float xOffset = individualStates[0].getCenterX();
        float[] flatRescaled =
                individualStates[0]
                        .xOffsetSubtract(xOffset)
                        .subtract(stateStatistics.getMean())
                        .divide(stateStatistics.getStdev())
                        .extractPositions();
        System.arraycopy(flatRescaled, 0, flatState, 0, 36);

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

}
