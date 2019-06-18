package game.actions.perturbers;

import game.actions.ActionQueue;

/**
 * Placeholder {@link IActionPerturber} which just copies the input {@link game.actions.ActionQueue}.
 *
 * @author matt
 */
public class ActionPerturber_Identity implements IActionPerturber {
    @Override
    public ActionQueue perturb(ActionQueue unperturbedQueue) {
        return unperturbedQueue.getCopyOfUnexecutedQueue();
    }
}
