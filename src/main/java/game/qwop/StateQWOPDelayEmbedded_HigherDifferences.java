package game.qwop;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.state.transform.ITransform;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

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

    public static class Normalizer implements ITransform<StateQWOPDelayEmbedded_HigherDifferences> {

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
        public void updateTransform(List<StateQWOPDelayEmbedded_HigherDifferences> statesToUpdateFrom) {}

        @Override
        public List<float[]> transform(List<StateQWOPDelayEmbedded_HigherDifferences> originalStates) {
            List<float[]> transformedStates = new ArrayList<>(originalStates.size());
            for (StateQWOPDelayEmbedded_HigherDifferences st : originalStates) {
                transformedStates.add(transform(st));
            }
            return transformedStates;
        }

        @Override
        public float[] transform(StateQWOPDelayEmbedded_HigherDifferences originalState) {
            float[] flatState = originalState.flattenState();

            // Just normalize the newest state. Others are made into higher differences, and I'm going to leave them
            // alone until I have a more reasonable way to scale them. The Differences one will normalize velocity,
            // but I didn't do that here.
            float xOffset = originalState.getCenterX();
            float[] flatRescaled =
                    originalState.getIndividualStates()[0]
                            .xOffsetSubtract(xOffset)
                            .subtract(
                                    normalizationMethod == NormalizationMethod.STDEV ?
                                    stateStats.getMean()
                                    : stateStats.getMin())
                            .divide(normalizationMethod == NormalizationMethod.STDEV ?
                                    stateStats.getStdev()
                                    : stateStats.getRange())
                            .extractPositions();
            System.arraycopy(flatRescaled, 0, flatState, 0, 36);

            return flatState;
        }
        @Override
        public List<float[]> untransform(List<float[]> transformedStates) {
            // TODO
            throw new RuntimeException("Not implemented yet.");
        }

        @Override
        public List<float[]> compressAndDecompress(List<StateQWOPDelayEmbedded_HigherDifferences> originalStates) {
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
            return "StatePoseNormalizer";
        }
    }
}
