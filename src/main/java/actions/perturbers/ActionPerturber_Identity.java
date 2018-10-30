package actions.perturbers;

import actions.ActionQueue;

/**
 * Placeholder {@link IActionPerturber} which just copies the input {@link actions.ActionQueue}.
 *
 * @author matt
 */
public class ActionPerturber_Identity implements IActionPerturber {
    @Override
    public ActionQueue perturb(ActionQueue unperturbedQueue) {
        return unperturbedQueue.getCopyOfUnexecutedQueue();
    }
}
