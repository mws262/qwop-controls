package game.qwop;

/**
 * For each additional normal state given, this will use the poses from the first, use the first two poses to to get
 * a first difference, use the first three to get a third difference, and so on.
 *
 * [36 poses, 36 first differences, 36 second differences, ...]
 *
 * @author matt
 */
public class StateQWOPDelayEmbedded_HigherDifferences extends StateQWOPDelayEmbedded_Poses {
    public StateQWOPDelayEmbedded_HigherDifferences(StateQWOP[] states) {
        super(states);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public float[] flattenState() {

        float[] flatState = new float[stateSize];

        // Subtract out the current body x from all other x coordinates, even the time delayed ones.
        float xOffset = individualStates[0].getCenterX();

        StateQWOP[] differencedStates = getDifferencedStates(individualStates);
        for (int i = 0; i < individualStates.length; i++) {
            if (i > 0) {
                xOffset = 0;
            }
            System.arraycopy(differencedStates[i].extractPositions(xOffset), 0, flatState, i * 36, 36);
        }
        return flatState;
    }
//
//    @Override
//    public float[] flattenStateWithRescaling(StateNormalizerQWOP.StateStatistics stateStatistics) {
//        float[] flatState = flattenState();
//        float xOffset = individualStates[0].getCenterX();
//        float[] flatRescaled =
//                individualStates[0]
//                        .xOffsetSubtract(xOffset)
//                        .subtract(stateStatistics.getMean())
//                        .divide(stateStatistics.getStdev())
//                        .extractPositions();
//        System.arraycopy(flatRescaled, 0, flatState, 0, 36);
//
//        return flatState;
//    }

    private static StateQWOP[] getDifferencedStates(StateQWOP[] states) {
        StateQWOP[] finalDifferences = new StateQWOP[states.length];

        finalDifferences[0] = states[0];
        for (int j = 1; j < finalDifferences.length; j++) {
            StateQWOP[] stateDiff = new StateQWOP[states.length - 1];

            for (int i = 0; i < states.length - 1; i++) {
                stateDiff[i] = states[i + 1].subtract(states[i]);
            }
            finalDifferences[j] = stateDiff[0];
            states = stateDiff;
        }
        return finalDifferences;
    }

}
