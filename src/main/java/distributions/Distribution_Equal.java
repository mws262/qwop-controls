package distributions;

import game.action.Action;
import game.action.Command;

import java.util.List;

/**
 * Most basic command sampling distribution. This will just randomly pick one of the provided possible game.command with
 * equal probability. Note that this is not precisely the same as a uniform distribution. If the provided possible
 * game.command are heavily skewed in some way, then the outputs of this distribution will be similarly skewed since each
 * possible sample has equal probability.
 *
 * @author matt
 */
public class Distribution_Equal <C extends Command<?>> extends Distribution<Action<C>> {

    /**
     * Get a random sample from the provided set of game.command.
     *
     * @param set A List of possible game.command to sample from.
     * @return An {@link Action command} sampled from the input set according to this distribution.
     */
    @Override
    public Action<C> randOnDistribution(List<Action<C>> set) {
        return randSample(set);
    }

    @Override
    public boolean equals(Object other) {
        return other != null && this.getClass() == other.getClass();
    }

    @Override
    public int hashCode() {
        return 1084739;
    }
}
