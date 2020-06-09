package game.action.perturbers;

import game.action.Action;
import game.action.ActionQueue;
import game.action.Command;

import java.util.List;
import java.util.Map;

/**
 * Perturbs {@link ActionQueue} by making some or all of the transitions between game.command happen too early/late. Total
 * number of timesteps should be preserved.
 */
public class ActionPerturber_OffsetActionTransitions<C extends Command<?>> implements IActionPerturber<C> {

    /**
     * Action indices as keys and number of timesteps to move as values. Timesteps are moved from the previous command
     * to the specified one.
     */
    private Map<Integer, Integer> perturbationIndexAndSize;

    /**
     * Make a new perturber by defining which game.command should be changed and by how much.
     *
     * @param perturbationIndexAndSize Map specifying which command should be started too early/late, and how early/late (in
     *                                 timesteps) it should be started. Only the second command onward (indices 1+)
     *                                 can be started early. "How early/late" are specified as positive integers.
     */
    public ActionPerturber_OffsetActionTransitions(Map<Integer, Integer> perturbationIndexAndSize) {
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
    public ActionQueue<C> perturb(ActionQueue<C> unperturbedQueue) {
        unperturbedQueue = unperturbedQueue.getCopyOfUnexecutedQueue();
        List<Action<C>> allActions = unperturbedQueue.getActionsInCurrentRun();

        for (Map.Entry<Integer, Integer> entry : perturbationIndexAndSize.entrySet()) {
            int actionIdx = entry.getKey();
            if (actionIdx < allActions.size()) {

                int perturbationSize = entry.getValue();

                if (perturbationSize > 0) { // Action starts early -- positive.

                    perturbationSize = Integer.min(perturbationSize,
                            Integer.max(allActions.get(actionIdx - 1).getTimestepsTotal() - 1, 1));

                } else if (perturbationSize < 0) { // Action starts late -- negative.

                    perturbationSize = -Integer.min(-perturbationSize,
                            Integer.max(allActions.get(actionIdx).getTimestepsTotal() - 1, 1));
                } else {
                    continue;
                }

                Action<C> endedWrongAction =
                        new Action<>(allActions.get(actionIdx - 1).getTimestepsTotal() - perturbationSize,
                                allActions.get(actionIdx - 1).peek());
                Action<C> startedWrongAction =
                        new Action<>(allActions.get(actionIdx).getTimestepsTotal() + perturbationSize,
                                allActions.get(actionIdx).peek());

                allActions.set(actionIdx - 1, endedWrongAction);
                allActions.set(actionIdx, startedWrongAction);
            }
        }
        ActionQueue<C> perturbedActionQueue = new ActionQueue<>();
        perturbedActionQueue.addSequence(allActions);
        return perturbedActionQueue;
    }
}
