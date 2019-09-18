package tree.sampler;

import game.action.Command;
import game.state.IState;

/**
 * Workers can end up fighting over tree areas to expand. If a worker can't find anything to do, wait for an
 * increasing length of time. When things open up again, reset the delay back to zero.
 */
public abstract class Sampler_DeadlockDelay<C extends Command<?>, S extends IState> implements ISampler<C, S> {

    private int deadlockDelayCurrent = 0;
    private int deadlockMax = 5000; // No delays larger than 5 seconds.


    void deadlockDelay() {
        try {
            Thread.sleep(deadlockDelayCurrent);
            deadlockDelayCurrent = Math.min(deadlockDelayCurrent * 2 + 1, deadlockMax);
            if (deadlockDelayCurrent == deadlockMax)
                System.out.println("MAX DELAY");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    void resetDeadlockDelay() {
        deadlockDelayCurrent = 0;
    }
}
