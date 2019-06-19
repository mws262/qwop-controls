package game.action.perturbers;

import game.action.ActionQueue;

/**
 * Placeholder {@link IActionPerturber} which just copies the input {@link game.action.ActionQueue}.
 *
 * @author matt
 */
public class ActionPerturber_Identity implements IActionPerturber {
    @Override
    public ActionQueue perturb(ActionQueue unperturbedQueue) {
        return unperturbedQueue.getCopyOfUnexecutedQueue();
    }
}
