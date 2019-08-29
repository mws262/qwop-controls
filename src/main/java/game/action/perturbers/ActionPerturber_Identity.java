package game.action.perturbers;

import game.action.ActionQueue;
import game.action.Command;

/**
 * Placeholder {@link IActionPerturber} which just copies the input {@link game.action.ActionQueue}.
 *
 * @author matt
 */
public class ActionPerturber_Identity<C extends Command<?>> implements IActionPerturber<C> {
    @Override
    public ActionQueue<C> perturb(ActionQueue<C> unperturbedQueue) {
        return unperturbedQueue.getCopyOfUnexecutedQueue();
    }
}
