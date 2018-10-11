package distributions;

import java.util.Comparator;
import java.util.List;

import main.Action;

/**
 * Action sampling distribution implementation which is weighted according to a normal distribution. Once the mean
 * and standard deviation of the distribution have been set in the constructor, they cannot be changed. New
 * distributions should be created rather than editing existing ones.
 *
 * @author matt
 */

public class Distribution_Normal extends Distribution<Action> {

    /**
     * Mean of this Gaussian distribution.
     **/
    private final float mean;

    /**
     * Standard deviation of this Gaussian distribution.
     **/
    private final float standardDeviation;

    /**
     * Create a new sampling distribution which is Gaussian, with specified center and standard deviation.
     *
     * @param mean  Center of created distribution.
     * @param stdev Standard deviation of the created distribution.
     */
    public Distribution_Normal(float mean, float stdev) {
        this.mean = mean;
        this.standardDeviation = stdev;
    }

    /**
     * Get an action based on duration sampled from a normal distribution with mean and standardDeviation. This
     * method will choose the nearest duration action to whatever value is sampled from the continuous Gaussian
     * distribution.
     *
     * @param set A list of actions to sample from.
     * @return An {@link main.Action action} sampled from the input set according to this distribution.
     */
    @Override
    public Action randOnDistribution(List<Action> set) {
        double r = rand.nextGaussian(); // Gets a new value on bell curve. 0 mean, 1 stddev.
        double rScaled = r * standardDeviation + mean; // Scale to our possible action range.
        Action best = set.get(0);
        float diff = Float.MAX_VALUE;

        final Comparator<Action> comp = Comparator.comparingDouble(p -> Math.abs(p.getTimestepsTotal() - rScaled));

        set.stream().min(comp); // TODO FINISH THIS


        for (Action a : set) { // Find the closest action value to the generated one.
            float candidate = (float) Math.abs(a.getTimestepsTotal() - rScaled);
            if (candidate < diff) {
                diff = candidate;
                best = a;
            }
        }
        return best;
    }

}
