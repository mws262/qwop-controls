package data;

import game.State;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Used for loading mean, min, max, range, stdev info for 72 dimension state info. This is from a large (~2.5
 * million) state collection, and is generated from TFRecord files by calculate_normalization.py.
 *
 * @author matt
 */
public class LoadStateStatistics {

    public static StateStatistics loadStatsFromFile() throws FileNotFoundException {
        File fileMax = new File("./src/main/resources/data_stats/state_max.txt");
        File fileMin = new File("./src/main/resources/data_stats/state_min.txt");
        File fileMean = new File("./src/main/resources/data_stats/state_mean.txt");
        File fileRange = new File("./src/main/resources/data_stats/state_range.txt");
        File fileStdev = new File("./src/main/resources/data_stats/state_stdev.txt");

        float[] maxVals = readArrayFromFile(fileMax);
        float[] minVals = readArrayFromFile(fileMin);
        float[] meanVals = readArrayFromFile(fileMean);
        float[] rangeVals = readArrayFromFile(fileRange);
        float[] stdevVals = readArrayFromFile(fileStdev);

        return new StateStatistics(maxVals, minVals, meanVals, rangeVals, stdevVals);
    }

    private static float[] readArrayFromFile(File file) throws FileNotFoundException {
        Scanner sc = new Scanner(file);

        List<Float> valuesList = new ArrayList<>();
        while (sc.hasNextDouble())
            valuesList.add(sc.nextFloat());

        float[] valuesArray = new float[valuesList.size()];
        for (int i = 0; i < valuesList.size(); i++) {
            valuesArray[i] = valuesList.get(i);
        }
        return valuesArray;
    }

    /**
     * Container for state statistics information. Can also do the rescaling of states based on the contained values.
     */
    public static class StateStatistics {
        public final float[] max;
        public final float[] min;
        public final float[] mean;
        public final float[] range;
        public final float[] stdev;

        private StateStatistics(float[] max, float[] min, float[] mean, float[] range, float[] stdev) {
            assert max.length == 72;
            assert min.length == 72;
            assert mean.length == 72;
            assert range.length == 72;
            assert stdev.length == 72;

            this.max = max;
            this.min = min;
            this.mean = mean;
            this.range = range;
            this.stdev = stdev;
        }

        /**
         * Take state data, subtract the mean of each variable and divide by standard deviation. This results in
         * zero-mean, unit-variance in all dimensions. This operation is in place, i.e. stateData input will be
         * modified.
         * @param stateData Array of flattened state data.
         * @return Standardized array of flattened state data.
         */
        public float[] standardizeState(float[] stateData, float xOffset) {

            for (int i = 0; i < stateData.length; i++) {
                if (i%6 == 0) {
                    stateData[i] += xOffset;
                }
                if (stdev[i] > 0) {
                    stateData[i] = (stateData[i] - mean[i]) / stdev[i];
                } else {
                    stateData[i] = (stateData[i] - mean[i]);
                }
            }
            return stateData;
        }

        public float[] standardizeState(State state, float xOffset) {
            return standardizeState(state.flattenState(), xOffset);
        }

        public float[] standardizeState(float[] stateData) {
            return standardizeState(stateData, -stateData[0]); // If no offset given, subtract out the torso x.
        }

        public float[] standardizeState(State state) {
            return standardizeState(state.flattenState());
        }



        /**
         * Take state data, subtract the minimum of each variable and divide by its range. This results in [0,1]
         * range data in all dimensions. This operation is in place, i.e. stateData input will be modified.
         * @param stateData Array of flattened state data.
         * @return Standardized array of flattened state data.
         */
        public float[] rescaleState(float[] stateData) {
            for (int i = 0; i < stateData.length; i++) {
                if (range[i] > 0) {
                    stateData[i] = (stateData[i] - min[i]) / range[i];
                } else {
                    stateData[i] = 0;
                }
            }
            return stateData;
        }

        public float[] rescaleState(State state) {
            return rescaleState(state.flattenState());
        }
    }
}
