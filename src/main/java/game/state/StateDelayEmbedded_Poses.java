package game.state;

import data.LoadStateStatistics;

public class StateDelayEmbedded_Poses extends StateDelayEmbedded {

    public StateDelayEmbedded_Poses(State[] states) {
        super(states);
    }

    @Override
    public float[] flattenState() {

        float[] flatState = new float[3 * stateVariableCount];

        // Subtract out the current body x from all other x coordinates, even the time delayed ones.
        float xOffset = individualStates[0].getCenterX();
        for (int i = 0; i < individualStates.length; i++) {
            float[] flatPositions = individualStates[i].extractPositions(xOffset);
            System.arraycopy(flatPositions, 0, flatState, i * 36, 36);
        }
        return flatState;
    }

    @Override
    public float[] flattenStateWithRescaling(LoadStateStatistics.StateStatistics stateStatistics) {
        float[] flatState = flattenState();
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
}
