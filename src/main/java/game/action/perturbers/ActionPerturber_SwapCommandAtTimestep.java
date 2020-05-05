package game.action.perturbers;

import game.action.ActionQueue;
import game.action.Command;

public class ActionPerturber_SwapCommandAtTimestep<C extends Command<?>> implements IActionPerturber<C> {

    private int perturbedTimestep = 0;
    private C commandToSwapIn;

    @Override
    public ActionQueue<C> perturb(ActionQueue<C> unperturbedQueue) {

        if(perturbedTimestep > unperturbedQueue.getTotalQueueLengthTimesteps() - 1) {
            throw new IllegalArgumentException("Specified perturbation timestep is beyond the length of the provided " +
                    "queue.");
        }

        ActionQueue<C> workingQueue = unperturbedQueue.getCopyOfUnexecutedQueue();

        int count = 0;
        while (count++ < perturbedTimestep) {
            workingQueue.pollCommand();
        }

        workingQueue.getCurrentActionIdx();
        return null;
    }
}
