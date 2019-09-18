package game.qwop;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class StatisticsQWOP {

    private static final Logger logger = LogManager.getLogger(StatisticsQWOP.class);

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


    public StatisticsQWOP() throws FileNotFoundException {
        File fileMax = new File("./src/main/resources/data_stats/state_max.txt");
        File fileMin = new File("./src/main/resources/data_stats/state_min.txt");
        File fileMean = new File("./src/main/resources/data_stats/state_mean.txt");
        File fileRange = new File("./src/main/resources/data_stats/state_range.txt");
        File fileStdev = new File("./src/main/resources/data_stats/state_stdev.txt");

        maxArray = readArrayFromFile(fileMax);
        minArray = readArrayFromFile(fileMin);
        meanArray = readArrayFromFile(fileMean);
        rangeArray = readArrayFromFile(fileRange);
        stdevArray = readArrayFromFile(fileStdev);

        this.max = new StateQWOP(maxArray, false);
        this.min = new StateQWOP(minArray, false);
        this.mean = new StateQWOP(meanArray, false);
        this.range = new StateQWOP(rangeArray, false);
        this.stdev = new StateQWOP(stdevArray, false);
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
}
