package game.qwop;

import org.jetbrains.annotations.NotNull;

public class StateQWOPDelayEmbedded_Differences extends StateQWOPDelayEmbedded_Poses {

    public StateQWOPDelayEmbedded_Differences(@NotNull StateQWOP[] states) {
        super(states);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public float[] flattenState() {


        float[] flatState = new float[stateSize];

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

    /**
     * Transforms the states. The first returned element is the most recent state snapshot. After that are the
     * snapshots with the previous provided snapshot subtracted out.
     * @param states Time delayed series of states to transform.
     * @return Transformed states. This will have the same dimension as the input.
     */
    private static StateQWOP[] getDifferencedStates(StateQWOP[] states) {
        StateQWOP[] finalDifferences = new StateQWOP[states.length];

        finalDifferences[0] = states[0];
        for (int j = 1; j < finalDifferences.length; j++) {
            finalDifferences[j] = states[j - 1].subtract(states[j]);
        }
        return finalDifferences;
    }

    //    @Override
//    public float[] flattenStateWithRescaling(StateNormalizerQWOP.StateStatistics stateStatistics) {
//        float[] flatState = flattenState();
//        float xOffset = individualStates[0].getCenterX();
//
//        float[] flatRescaled = null;
//        for (int i = 0; i < individualStates.length; i++) {
//            if (i > 0) {
//                flatRescaled = individualStates[i]
//                                .multiply(1f/ QWOPConstants.timestep) // Make the difference like a finite difference
//                        // velocity.
//                                .subtract(stateStatistics.getMean().swapPosAndVel()) // Swaps are because we want to
//                        // use the velocity normalization on a finite difference that is in the position slots of a
//                        // StateQWOP.
//                                .divide(stateStatistics.getStdev().swapPosAndVel())
//                                .extractPositions();
//            } else {
//                flatRescaled =
//                        individualStates[i]
//                                .xOffsetSubtract(xOffset)
//                                .subtract(stateStatistics.getMean())
//                                .divide(stateStatistics.getStdev())
//                                .extractPositions();
//            }
//            System.arraycopy(flatRescaled, 0, flatState, 36 * i, 36);
//        }
//
//        return flatState;
//    }

}
