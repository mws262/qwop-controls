package actions.perturbers;

import actions.Action;
import actions.ActionQueue;

import java.util.Map;

/**
 * Perturbs an {@link ActionQueue} by removing timesteps (shortening {@link actions.Action}) without replacement at
 * specified locations in the sequence.
 *
 * See {@link ActionPerturber_SwitchTooSoon} for an implementation which preserves the number of timesteps by moving
 * them from one action to the next.
 *
 * @author matt
 */
public class ActionPerturber_MissingTimesteps implements IActionPerturber{

    /**
     * Action indices as keys and number of timesteps to move as values. Timesteps are removed from the specified
     * action without replacement anywhere else.
     */
    private Map<Integer, Integer> perturbationIndexAndSize;


    public ActionPerturber_MissingTimesteps(Map<Integer, Integer> perturbationIndexAndSize) {
        for (Integer size : perturbationIndexAndSize.values()) {
            if (size <= 0) {
                throw new IllegalArgumentException("Perturbations sizes should be positive integers. Given was: " + size + ".");
            }
        }
        for (Integer size : perturbationIndexAndSize.keySet()) {
            if (size < 0) {
                throw new IllegalArgumentException("Perturbations indices should be non-negative integers. Given was:" +
                        " " + size + ".");
            }
        }
        this.perturbationIndexAndSize = perturbationIndexAndSize;
    }

    @Override
    public ActionQueue perturb(ActionQueue unperturbedQueue) {
        Action[] allActions = unperturbedQueue.getActionsInCurrentRun();

        for (Integer actionIdx : perturbationIndexAndSize.keySet()) {
            if (actionIdx < allActions.length) {
                int durationReduction = Integer.min(allActions[actionIdx].getTimestepsTotal() - 1,
                        perturbationIndexAndSize.get(actionIdx)); // Must leave at least 1 element.
                Action replacementAction = new Action(allActions[actionIdx].getTimestepsTotal() - durationReduction,
                        allActions[actionIdx].peek());
                allActions[actionIdx] = replacementAction;
            }
        }
        ActionQueue perturbedActionQueue = new ActionQueue();
        perturbedActionQueue.addSequence(allActions);
        return perturbedActionQueue;
    }
}
