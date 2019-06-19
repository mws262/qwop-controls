package game.action.perturbers;

import game.action.Action;
import game.action.ActionQueue;

import java.util.Map;

/**
 * Perturbs {@link ActionQueue} by making some or all of the transitions between game.action happen too early. Total
 * number of timesteps should be preserved.
 */
public class ActionPerturber_SwitchTooSoon implements IActionPerturber {

    /**
     * Action indices as keys and number of timesteps to move as values. Timesteps are moved from the previous action
     * to the specified one.
     */
    private Map<Integer, Integer> perturbationIndexAndSize;

    /**
     * Make a new perturber by defining which game.action should be changed and by how much.
     *
     * @param perturbationIndexAndSize Map specifying which action should be started too soon, and how early (in
     *                                 timesteps) it should be started. Only the second action onward (indices 1+)
     *                                 can be started early. "How early" are specified as positive integers.
     */
    public ActionPerturber_SwitchTooSoon(Map<Integer, Integer> perturbationIndexAndSize) {
        for (Integer size : perturbationIndexAndSize.values()) {
            if (size <= 0) {
                throw new IllegalArgumentException("Perturbations sizes should be positive integers. Given was: " + size + ".");
            }
        }
        if (perturbationIndexAndSize.containsKey(0)) {
            throw new IllegalArgumentException("A SwitchTooSoon perturbation cannot happen on the first");
        }
        for (Integer size : perturbationIndexAndSize.keySet()) {
            if (size < 0) {
                throw new IllegalArgumentException("Perturbations indices should be positive integers. Given was: " + size + ".");
            }
        }
        this.perturbationIndexAndSize = perturbationIndexAndSize;
    }

    @Override
    public ActionQueue perturb(ActionQueue unperturbedQueue) {
        Action[] allActions = unperturbedQueue.getActionsInCurrentRun();

        for (Map.Entry<Integer, Integer> entry : perturbationIndexAndSize.entrySet()) {
            int actionIdx = entry.getKey();
            if (actionIdx < allActions.length) {

                int perturbationSize = Integer.min(entry.getValue(),
                        allActions[actionIdx - 1].getTimestepsTotal() - 1); // Perturbations must leave at least one
                // command in the previous Action.

                Action endedEarlyAction =
                        new Action(allActions[actionIdx-1].getTimestepsTotal() - perturbationSize,
                                allActions[actionIdx - 1].peek());
                Action tooSoonAction =
                        new Action(allActions[actionIdx].getTimestepsTotal() + perturbationSize,
                                allActions[actionIdx].peek());


                allActions[actionIdx - 1] = endedEarlyAction;
                allActions[actionIdx] = tooSoonAction;
            }
        }
        ActionQueue perturbedActionQueue = new ActionQueue();
        perturbedActionQueue.addSequence(allActions);
        return perturbedActionQueue;
    }
}
