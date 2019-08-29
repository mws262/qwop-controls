package game.qwop;

import game.state.transform.ITransform;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

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

    public static class Normalizer implements ITransform<StateQWOPDelayEmbedded_Poses> {

        public enum NormalizationMethod {
            STDEV, RANGE
        }

        private NormalizationMethod normType;

        private StatisticsQWOP stateStats = new StatisticsQWOP();

        public Normalizer(NormalizationMethod normalizationMethod) throws FileNotFoundException {
            normType = normalizationMethod;
        }

        @Override
        public void updateTransform(List<StateQWOPDelayEmbedded_Poses> statesToUpdateFrom) {}

        @Override
        public List<float[]> transform(List<StateQWOPDelayEmbedded_Poses> originalStates) {
            List<float[]> transformedStates = new ArrayList<>(originalStates.size());
            for (StateQWOPDelayEmbedded_Poses st : originalStates) {
                transformedStates.add(transform(st));
            }
            return transformedStates;
        }

        @Override
        public float[] transform(StateQWOPDelayEmbedded_Poses originalState) {
            float[] flatState = new float[originalState.stateSize];
            float xOffset = originalState.getCenterX();

            StateQWOP[] individualStates = originalState.getIndividualStates();
            float[] flatRescaled;
            for (int i = 0; i < individualStates.length; i++) {
                flatRescaled =
                        individualStates[i]
                                .xOffsetSubtract(xOffset)
                                .subtract(
                                        normType == NormalizationMethod.STDEV ?
                                                stateStats.getMean()
                                                : stateStats.getMin()
                                )
                                .divide(
                                        normType == NormalizationMethod.RANGE ?
                                                stateStats.getStdev()
                                                : stateStats.getRange())
                                .extractPositions();

                System.arraycopy(flatRescaled, 0, flatState, 36 * i, 36);
            }
            return flatState;
        }

        @Override
        public List<float[]> untransform(List<float[]> transformedStates) {
            // TODO
            throw new RuntimeException("Not implemented yet.");
        }

        @Override
        public List<float[]> compressAndDecompress(List<StateQWOPDelayEmbedded_Poses> originalStates) {
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
