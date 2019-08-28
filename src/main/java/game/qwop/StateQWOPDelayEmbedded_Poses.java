package game.qwop;

public class StateQWOPDelayEmbedded_Poses extends StateQWOPDelayEmbedded {

    final int stateSize;
    static final int INDIVIDUALSTATE_SIZE = StateQWOP.STATEVARIABLE_COUNT * POSE_DIM;

    public StateQWOPDelayEmbedded_Poses(StateQWOP[] states) {
        super(states);
        stateSize = INDIVIDUALSTATE_SIZE * states.length;
    }

    // Extracts the poses (dumps the velocities out). Subtracts the most recent body x from all.
    @Override
    public float[] flattenState() {
        float[] flatState = new float[stateSize];

        // Subtract out the current body x from all other x coordinates, even the time delayed ones.
        float xOffset = individualStates[0].getCenterX();
        for (int i = 0; i < individualStates.length; i++) {
            float[] flatPositions = individualStates[i].extractPositions(xOffset);
            System.arraycopy(flatPositions, 0, flatState, i * INDIVIDUALSTATE_SIZE, INDIVIDUALSTATE_SIZE);
        }
        return flatState;
    }

    @Override
    public int getStateSize() {
        return stateSize;
    }

//    @Override
//    public float[] flattenStateWithRescaling(StateNormalizerQWOP.StateStatistics stateStatistics) {
//        float[] flatState = flattenState();
//        float xOffset = individualStates[0].getCenterX();
//
//        for (int i = 0; i < individualStates.length; i++) {
//            float[] flatRescaled =
//                    individualStates[i]
//                            .xOffsetSubtract(xOffset)
//                            .subtract(stateStatistics.getMean())
//                            .divide(stateStatistics.getStdev())
//                            .extractPositions();
//            System.arraycopy(flatRescaled, 0, flatState, i * 36, 36);
//        }
//        return flatState;
//    }
}
