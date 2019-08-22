package game.action.perturbers;

import game.action.ActionQueue;
import game.action.Command;

/**
 * Interface for adding perturbations to {@ActionQueue}. This mutation must happen before executing the queue. Future
 * changes may involve doing the perturbations during a run, if it is useful to perturb in a way related to the game
 * state.
 *
 * @author matt
 */
public interface IActionPerturber<C extends Command<?>> {

    /**
     * Given an unperturbed {@link ActionQueue}, return a copy of this queue with mutations as defined by the
     * implementation of the IActionPerturber.
     *
     * @param unperturbedQueue The queue to perturb. The original is unaltered, and a new altered version is returned.
     * @return
     */
    ActionQueue<C> perturb(ActionQueue<C> unperturbedQueue);
}
