package game.action.perturbers;

import game.action.ActionQueue;
import game.qwop.ActionQueuesQWOP;
import game.qwop.CommandQWOP;
import org.junit.Assert;
import org.junit.Test;

public class ActionPerturber_SwapCommandAtTimestepTest {

    @Test
    public void perturb() {

        ActionQueue<CommandQWOP> queue = ActionQueuesQWOP.makeShortQueue();
        for (int timestep = 0; timestep < queue.getTotalQueueLengthTimesteps() - 1; timestep++) {
            queue.resetQueue();
            CommandQWOP command = CommandQWOP.WP;

            ActionPerturber_SwapCommandAtTimestep<CommandQWOP> perturber =
                    new ActionPerturber_SwapCommandAtTimestep<>(timestep, command);

            ActionQueue<CommandQWOP> queueAfter = perturber.perturb(queue);

            Assert.assertEquals(queue.getTotalQueueLengthTimesteps(), queueAfter.getTotalQueueLengthTimesteps());

            int count = 0;
            while (!queue.isEmpty()) {
                if (count != timestep) {
                    Assert.assertEquals(queue.pollCommand(), queueAfter.pollCommand());
                } else {
                    Assert.assertEquals(command, queueAfter.pollCommand());
                    queue.pollCommand();
                }
                count++;
            }
        }
    }
}