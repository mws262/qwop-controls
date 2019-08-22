package distributions;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Action;
import game.action.Command;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Action sampling distribution implementation which is weighted according to a normal distribution. Once the mean
 * and standard deviation of the distribution have been set in the constructor, they cannot be changed. New
 * distributions should be created rather than editing existing ones.
 *
 * @author matt
 */

public class Distribution_Normal<C extends Command<?>> extends Distribution<Action<C>> {

    /**
     * Mean of this Gaussian distribution.
     */
    private final float mean;

    /**
     * Standard deviation of this Gaussian distribution.
     */
    private final float standardDeviation;

    /**
     * Create a new sampling distribution which is Gaussian, with specified center and standard deviation.
     *
     * @param mean  Center of created distribution.
     * @param stdev Standard deviation of the created distribution.
     */
    public Distribution_Normal(@JsonProperty("mean") float mean,
                               @JsonProperty("standardDeviation") float stdev) {
        if (stdev < 0)
            throw new IllegalArgumentException("Standard deviation argument may not be negative.");
        this.mean = mean;
        this.standardDeviation = stdev;
    }

    /**
     * Get an action based on duration sampled from a normal distribution with mean and standardDeviation. This
     * method will choose the nearest duration action to whatever value is sampled from the continuous Gaussian
     * distribution.
     *
     * @param set A list of game.action to sample from.
     * @return An {@link Action action} sampled from the input set according to this distribution.
     */
    @Override
    public Action<C> randOnDistribution(List<Action<C>> set) {
        if (set.size() < 1)
            throw new IllegalArgumentException("Argument action set must have at least 1 element.");

        double r = rand.nextGaussian(); // Gets a new value on bell curve. 0 mean, 1 stddev.
        double rScaled = r * standardDeviation + mean; // Scale to our possible action range.

        final Comparator<Action> comp = Comparator.comparingDouble(p -> Math.abs(p.getTimestepsTotal() - rScaled));

        return set.stream().min(comp).orElse(null); // Returns value (or null if something goes WEIRD).
    }

    public float getMean() {
        return mean;
    }

    public float getStandardDeviation() {
        return standardDeviation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Distribution_Normal that = (Distribution_Normal) o;
        return Float.compare(that.mean, mean) == 0 &&
                Float.compare(that.standardDeviation, standardDeviation) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mean, standardDeviation);
    }
}
