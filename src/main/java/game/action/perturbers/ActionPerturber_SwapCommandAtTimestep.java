package game.action.perturbers;

import game.action.Action;
import game.action.ActionQueue;
import game.action.Command;

import java.util.List;

/**
 * Change out a single timestep from an {@link ActionQueue} at a specified location, in timesteps, along the queue.
 *
 * @param <C> Command type of the perturber.
 */
public class ActionPerturber_SwapCommandAtTimestep<C extends Command<?>> implements IActionPerturber<C> {

    private final int perturbedTimestep;
    private final C commandToSwapIn;

    public ActionPerturber_SwapCommandAtTimestep(int perturbedTimestep, C commandToSwapIn) {
        this.perturbedTimestep = perturbedTimestep;
        this.commandToSwapIn = commandToSwapIn;
    }

    @Override
    public ActionQueue<C> perturb(ActionQueue<C> unperturbedQueue) {

        if (perturbedTimestep > unperturbedQueue.getTotalQueueLengthTimesteps() - 1) {
            throw new IllegalArgumentException("Specified perturbation timestep is beyond the length of the provided " +
                    "queue.");
        }

        // Split the existing queue into 3 chunks, the middle one being only a single timestep. The replace the
        // single-timestep one with the swapped-in action.
        List<ActionQueue<C>> splitQueues = unperturbedQueue.splitQueueAtTimestep(perturbedTimestep);
        ActionQueue<C> qStart = splitQueues.get(0);
        ActionQueue<C> qEnd = splitQueues.get(1);

        List<ActionQueue<C>> qEndSplit = qEnd.splitQueueAtTimestep(1);

        ActionQueue<C> qSwapAction = qEndSplit.get(0);
        ActionQueue<C> qAfterSwap = qEndSplit.get(1);


        qSwapAction.clearAll();
        qSwapAction.addAction(new Action<>(1, commandToSwapIn));

        //noinspection unchecked
        return ActionQueue.concatenateQueues(qStart, qSwapAction, qAfterSwap);
    }
}
