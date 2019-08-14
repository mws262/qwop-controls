package data;

import game.state.State;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger logger = LogManager.getLogger(LoadStateStatistics.class);

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
        Scanner sc = new Scanner(file, "utf-8");

        List<Float> valuesList = new ArrayList<>();
        while (sc.hasNextDouble())
            valuesList.add(sc.nextFloat());

        float[] valuesArray = new float[valuesList.size()];
        for (int i = 0; i < valuesList.size(); i++) {
            valuesArray[i] = valuesList.get(i);
        }
        logger.info("State statistics loaded from file: " + file.getName());
        return valuesArray;
    }

    /**
     * Container for state statistics information. Can also do the rescaling of states based on the contained values.
     */
    @SuppressWarnings("WeakerAccess")
    public static class StateStatistics {
        private final State max;
        private final State min;
        private final State mean;
        private final State range;
        private final State stdev;

        private StateStatistics(float[] max, float[] min, float[] mean, float[] range, float[] stdev) {
            assert max.length == 72;
            assert min.length == 72;
            assert mean.length == 72;
            assert range.length == 72;
            assert stdev.length == 72;

            this.max = new State(max, false);
            this.min = new State(min, false);
            this.mean = new State(mean, false);
            this.range = new State(range, false);
            this.stdev = new State(stdev, false);
        }

        public State getMax() {
            return max;
        }

        public State getMin() {
            return min;
        }

        public State getMean() {
            return mean;
        }

        public State getRange() {
            return range;
        }

        public State getStdev() {
            return stdev;
        }

    }
}
