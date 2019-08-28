package data;

import game.qwop.StateNormalizerQWOP;
import game.qwop.StateQWOP;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class StateNormalizerQWOPTest {

    @Test
    public void checkStateStats() {
        // Sanity check for loaded state statistics and to make sure that no errors occur. This is based not just on
        // the code in the class, but also the text files containing the stats.

        StateNormalizerQWOP normalizer = new StateNormalizerQWOP(StateNormalizerQWOP.NormalizationMethod.STDEV);
        StateNormalizerQWOP.StateStatistics stateStats = normalizer.getStateStats();


        Assert.assertNotNull(stateStats);

        StateQWOP mean = stateStats.getMean();
        StateQWOP range = stateStats.getRange();
        StateQWOP min = stateStats.getMin();
        StateQWOP max = stateStats.getMax();
        StateQWOP stdev = stateStats.getStdev();

        Assert.assertEquals(0f, mean.getCenterX(), 1e-12f); // Should have body x subtracted off.
        Assert.assertEquals(0f, range.getCenterX(), 1e-12f); // Should have body x subtracted off.
        Assert.assertEquals(0f, min.getCenterX(), 1e-12f); // Should have body x subtracted off.
        Assert.assertEquals(0f, max.getCenterX(), 1e-12f); // Should have body x subtracted off.
        Assert.assertEquals(0f, stdev.getCenterX(), 1e-12f); // Should have body x subtracted off.

        Assert.assertArrayEquals(range.flattenState(), max.subtract(min).flattenState(), 1e-5f); // Range should be
        // consistent with min and max.

        // No negative values in these quantities.
        // Range
        float[] flatRange = range.flattenState();
        Arrays.sort(flatRange);
        Assert.assertTrue(flatRange[0] >= 0);

        // Max - min
        float[] flatDiff = max.subtract(min).flattenState();
        Arrays.sort(flatDiff);
        Assert.assertTrue(flatDiff[0] >= 0);

        // Standard deviation
        float[] flatStdev = stdev.flattenState();
        Arrays.sort(flatStdev);
        Assert.assertTrue(flatStdev[0] >= 0);

        // mean - min
        float[] meanMinusMin = mean.subtract(min).flattenState();
        Arrays.sort(meanMinusMin);
        Assert.assertTrue(meanMinusMin[0] >= 0);

        // max - mean
        float[] maxMinusMean = max.subtract(mean).flattenState();
        Arrays.sort(maxMinusMean);
        Assert.assertTrue(maxMinusMean[0] >= 0);

        // range - stdev
        float[] rangeMinusStdev = range.subtract(stdev).flattenState();
        Arrays.sort(rangeMinusStdev);
        Assert.assertTrue(rangeMinusStdev[0] >= 0);

        // All values besides the body x should not be zero (or it's very unlikely).
        float[] flatMean = mean.flattenState();
        flatRange = range.flattenState();
        float[] flatMax = max.flattenState();
        float[] flatMin = min.flattenState();
        flatStdev = stdev.flattenState();

        for (int i = 1; i < flatMean.length; i++) {
            Assert.assertNotEquals(0f, flatMean[i]);
            Assert.assertNotEquals(0f, flatRange[i]);
            Assert.assertNotEquals(0f, flatMax[i]);
            Assert.assertNotEquals(0f, flatMin[i]);
            Assert.assertNotEquals(0f, flatStdev[i]);
        }
    }
}
