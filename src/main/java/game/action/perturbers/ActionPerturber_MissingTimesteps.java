package game.action.perturbers;

import game.action.Action;
import game.action.ActionQueue;
import game.action.Command;

import java.util.List;
import java.util.Map;

/**
 * Perturbs an {@link ActionQueue} by removing timesteps (shortening {@link game.action.Action}) without replacement at
 * specified locations in the sequence.
 *
 * See {@link ActionPerturber_OffsetActionTransitions} for an implementation which preserves the number of timesteps by moving
 * them from one command to the next.
 *
 * @author matt
 */
public class ActionPerturber_MissingTimesteps<C extends Command<?>> implements IActionPerturber<C> {

    /**
     * Action indices as keys and number of timesteps to move as values. Timesteps are removed from the specified
     * command without replacement anywhere else.
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
    public ActionQueue<C> perturb(ActionQueue<C> unperturbedQueue) {
        List<Action<C>> allActions = unperturbedQueue.getActionsInCurrentRun();

        for (Map.Entry<Integer, Integer> entry : perturbationIndexAndSize.entrySet()) {
            int actionIdx = entry.getKey();
            if (actionIdx < allActions.size()) {
                int durationReduction = Integer.min(allActions.get(actionIdx).getTimestepsTotal() - 1,
                        entry.getValue()); // Must leave at least 1 element.
                Action<C> replacementAction =
                        new Action<>(allActions.get(actionIdx).getTimestepsTotal() - durationReduction,
                        allActions.get(actionIdx).peek());

                allActions.set(actionIdx, replacementAction);
            }
        }

        ActionQueue<C> perturbedActionQueue = new ActionQueue<>();
        perturbedActionQueue.addSequence(allActions);
        return perturbedActionQueue;
    }
}
