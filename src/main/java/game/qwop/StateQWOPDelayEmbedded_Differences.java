package game.qwop;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.state.transform.ITransform;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * An agglomeration of time delayed
 */
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

    public static class Normalizer implements ITransform<StateQWOPDelayEmbedded_Differences> {

        public enum NormalizationMethod {
            STDEV, RANGE
        }

        @JsonProperty("normalizationMethod")
        public final NormalizationMethod normalizationMethod;

        private StatisticsQWOP stateStats = new StatisticsQWOP();

        public Normalizer(@JsonProperty("normalizationMethod") NormalizationMethod normalizationMethod) throws FileNotFoundException {
            this.normalizationMethod = normalizationMethod;
        }

        @Override
        public void updateTransform(List<StateQWOPDelayEmbedded_Differences> statesToUpdateFrom) {}

        @Override
        public List<float[]> transform(List<StateQWOPDelayEmbedded_Differences> originalStates) {
            List<float[]> transformedStates = new ArrayList<>(originalStates.size());
            for (StateQWOPDelayEmbedded_Differences st : originalStates) {
                transformedStates.add(transform(st));
            }
            return transformedStates;
        }

        @Override
        public float[] transform(StateQWOPDelayEmbedded_Differences originalState) {
            float xOffset = originalState.getCenterX();

            StateQWOP[] individualStates = getDifferencedStates(originalState.getIndividualStates());
            float[] flatRescaled;
            float[] fullFlatRescaled = new float[originalState.stateSize];
            for (int i = 0; i < individualStates.length; i++) {
                if (i > 0) {
                    flatRescaled = individualStates[i]
                            // TODO update so this works better if the states are spaced out by more than 1 timestep.
                            .multiply(1f/ QWOPConstants.timestep) // Make the difference like a finite difference
                            // velocity.
                            .subtract(
                                    normalizationMethod == NormalizationMethod.STDEV ?
                                    stateStats.getMean().swapPosAndVel()
                                    : stateStats.getMin().swapPosAndVel()) // Swaps are because we want to
                            // use the velocity normalization on a finite difference that is in the position slots of a
                            // StateQWOP.
                            .divide(
                                    normalizationMethod == NormalizationMethod.STDEV ?
                                    stateStats.getStdev().swapPosAndVel()
                                    : stateStats.getRange())
                            .extractPositions();
                } else {
                    flatRescaled =
                            individualStates[i]
                                    .xOffsetSubtract(xOffset)
                                    .subtract(
                                            normalizationMethod == NormalizationMethod.STDEV ?
                                                    stateStats.getMean()
                                                    : stateStats.getMin()
                                    )
                                    .divide(
                                            normalizationMethod == NormalizationMethod.RANGE ?
                                            stateStats.getStdev()
                                            : stateStats.getRange())
                                    .extractPositions();
                }
                System.arraycopy(flatRescaled, 0, fullFlatRescaled, 36 * i, 36);
            }
            return fullFlatRescaled;
        }

        @Override
        public List<float[]> untransform(List<float[]> transformedStates) {
            // TODO
            throw new RuntimeException("Not implemented yet.");
        }

        @Override
        public List<float[]> compressAndDecompress(List<StateQWOPDelayEmbedded_Differences> originalStates) {
            // TODO
            throw new RuntimeException("Not implemented yet.");
        }

        @Override
        public int getOutputSize() {
            // It's not nailed down anywhere here. Can vary. Figure it out later TODO
            throw new RuntimeException("Not implemented yet.");
        }

        @Override
        public String getName() {
            return "StateDiffNormalizer";
        }
    }
}
