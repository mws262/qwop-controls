package game.qwop;

import game.state.IState;
import game.state.transform.ITransform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Used for loading mean, min, max, range, stdev info for 72 dimension state info. This is from a large (~2.5
 * million) state collection, and is generated from TFRecord files by calculate_normalization.py.
 *
 * @author matt
 */
public class StateNormalizerQWOP implements ITransform<StateQWOP> {

    public enum NormalizationMethod {
        STDEV, RANGE
    }

    public NormalizationMethod normalizationMethod;

    private static final Logger logger = LogManager.getLogger(StateNormalizerQWOP.class);

    private final StateStatistics stateStats;

    public StateNormalizerQWOP(NormalizationMethod normalizationMethod) {

        this.normalizationMethod = normalizationMethod;

        File fileMax = new File("./src/main/resources/data_stats/state_max.txt");
        File fileMin = new File("./src/main/resources/data_stats/state_min.txt");
        File fileMean = new File("./src/main/resources/data_stats/state_mean.txt");
        File fileRange = new File("./src/main/resources/data_stats/state_range.txt");
        File fileStdev = new File("./src/main/resources/data_stats/state_stdev.txt");

        float[] maxVals = new float[0];
        float[] minVals = new float[0];
        float[] meanVals = new float[0];
        float[] rangeVals = new float[0];
        float[] stdevVals = new float[0];
        try {
            maxVals = readArrayFromFile(fileMax);
            minVals = readArrayFromFile(fileMin);
            meanVals = readArrayFromFile(fileMean);
            rangeVals = readArrayFromFile(fileRange);
            stdevVals = readArrayFromFile(fileStdev);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        stateStats = new StateStatistics(maxVals, minVals, meanVals, rangeVals, stdevVals);
    }

    public StateStatistics getStateStats() {
        return stateStats;
    }

    private static float[] readArrayFromFile(File file) throws FileNotFoundException {
        Scanner sc = new Scanner(file, "utf-8");

        List<Float> valuesList = new ArrayList<>();
        while (sc.hasNextDouble())
            valuesList.add(sc.nextFloat());

        float[] valuesArray = new float[valuesList.size()];
        for (int i = 0; i < valuesList.size(); i++) {
            valuesArray[i] = valuesList.get(i);
        }
        logger.info("StateQWOP statistics loaded from file: " + file.getName());
        return valuesArray;
    }

    @Override
    public void updateTransform(List<StateQWOP> statesToUpdateFrom) {} // May want to add later.

    @Override
    public List<float[]> transform(List<StateQWOP> originalStates) {
        List<float[]> tformedVals = new ArrayList<>(originalStates.size());
        for (StateQWOP s : originalStates) {
            tformedVals.add(transform(s));
        }
        return tformedVals;
    }

    @Override
    public float[] transform(StateQWOP originalState) {

        switch(normalizationMethod) {
            case STDEV:
                return originalState.xOffsetSubtract(originalState.getCenterX())
                        .subtract(stateStats.getMean())
                        .divide(stateStats.getStdev())
                        .flattenState();
            case RANGE:
                return originalState.xOffsetSubtract(originalState.getCenterX())
                        .subtract(stateStats.getMin())
                        .divide(stateStats.getRange())
                        .flattenState();
            default:
                throw new IllegalStateException("Unhandled state normalization case: " + normalizationMethod.toString());
        }

    }

    @Override
    public List<float[]> untransform(List<float[]> transformedStates) {
        List<float[]> untransformedStates = new ArrayList<>(transformedStates.size());

        switch(normalizationMethod) {
            case STDEV:
                for (float[] tformed : transformedStates) {
                    float[] utformed = new float[tformed.length];
                    for (int i = 0; i < tformed.length; i++) {
                        utformed[i] = tformed[i] * stateStats.stdevArray[i] + stateStats.meanArray[i];
                    }
                    untransformedStates.add(utformed);
                }
                return untransformedStates;
            case RANGE:
                for (float[] tformed : transformedStates) {
                    float[] utformed = new float[tformed.length];
                    for (int i = 0; i < tformed.length; i++) {
                        utformed[i] = tformed[i] * stateStats.rangeArray[i] + stateStats.minArray[i];
                    }
                    untransformedStates.add(utformed);
                }
                return untransformedStates;
            default:
                throw new IllegalStateException("Unhandled state normalization case: " + normalizationMethod.toString());
        }
    }

    @Override
    public List<float[]> compressAndDecompress(List<StateQWOP> originalStates) {
        return originalStates.stream().map(IState::flattenState).collect(Collectors.toList());
    }

    @Override
    public int getOutputSize() {
        return StateQWOP.STATE_SIZE;
    }

    @Override
    public String getName() {
        return "QWOPNormalizer";
    }

    /**
     * Container for state statistics information. Can also do the rescaling of states based on the contained values.
     */
    @SuppressWarnings("WeakerAccess")
    public static class StateStatistics {
        private final StateQWOP max;
        private final StateQWOP min;
        private final StateQWOP mean;
        private final StateQWOP range;
        private final StateQWOP stdev;

        final float[] maxArray;
        final float[] minArray;
        final float[] meanArray;
        final float[] rangeArray;
        final float[] stdevArray;

        private StateStatistics(float[] max, float[] min, float[] mean, float[] range, float[] stdev) {
            assert max.length == 72;
            assert min.length == 72;
            assert mean.length == 72;
            assert range.length == 72;
            assert stdev.length == 72;

            this.maxArray = max;
            this.minArray = min;
            this.meanArray = mean;
            this.rangeArray = range;
            this.stdevArray = stdev;

            this.max = new StateQWOP(max, false);
            this.min = new StateQWOP(min, false);
            this.mean = new StateQWOP(mean, false);
            this.range = new StateQWOP(range, false);
            this.stdev = new StateQWOP(stdev, false);
        }

        public StateQWOP getMax() {
            return max;
        }
        public StateQWOP getMin() {
            return min;
        }
        public StateQWOP getMean() {
            return mean;
        }
        public StateQWOP getRange() {
            return range;
        }
        public StateQWOP getStdev() {
            return stdev;
        }

    }
}
