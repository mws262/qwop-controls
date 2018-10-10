package distributions;

import java.util.List;

import main.Action;

/**
 * Most basic action sampling distribution. This will just randomly pick one of the provided possible actions with
 * equal probability. Note that this is not precisely the same as a uniform distribution. If the provided possible
 * actions are heavily skewed in some way, then the outputs of this distribution will be similarly skewed since each
 * possible sample has equal probability.
 *
 * @author matt
 */
public class Distribution_Equal extends Distribution<Action> {

    /**
     * Get a random sample from the provided set of actions.
     *
     * @param set A List of possible actions to sample from.
     * @return An {@link main.Action action} sampled from the input set according to this distribution.
     */
    @Override
    public Action randOnDistribution(List<Action> set) {
        return randSample(set);
    }
}
