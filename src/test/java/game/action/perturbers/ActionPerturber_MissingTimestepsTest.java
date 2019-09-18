package game.action.perturbers;

import game.action.Action;
import game.action.ActionQueue;
import game.qwop.CommandQWOP;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionPerturber_MissingTimestepsTest {

    @Test
    public void perturb() {
        ActionQueue<CommandQWOP> originalQueue = makeActionQueue();

        // Single perturbation, no unusual circumstances.
        Map<Integer, Integer> perturbationLocations = new HashMap<>();
        perturbationLocations.put(3, 1);
        ActionPerturber_MissingTimesteps<CommandQWOP> perturber =
                new ActionPerturber_MissingTimesteps<>(perturbationLocations);
        ActionQueue<CommandQWOP> perturbedQueue = perturber.perturb(originalQueue);

        Assert.assertEquals(92, perturbedQueue.getTotalQueueLengthTimesteps());
        List<Action<CommandQWOP>> perturbedActions = perturbedQueue.getActionsInCurrentRun();
        Assert.assertEquals(5, perturbedActions.get(0).getTimestepsTotal());
        Assert.assertEquals(23, perturbedActions.get(1).getTimestepsTotal());
        Assert.assertEquals(4, perturbedActions.get(2).getTimestepsTotal());
        Assert.assertEquals(44, perturbedActions.get(3).getTimestepsTotal());
        Assert.assertEquals(16, perturbedActions.get(4).getTimestepsTotal());

        // Multiple perturbations, no unusual circumstances.
        perturbationLocations = new HashMap<>();
        perturbationLocations.put(3, 3);
        perturbationLocations.put(1, 2);

        perturber = new ActionPerturber_MissingTimesteps<>(perturbationLocations);
        perturbedQueue = perturber.perturb(originalQueue);
        Assert.assertEquals(88, perturbedQueue.getTotalQueueLengthTimesteps());
        perturbedActions = perturbedQueue.getActionsInCurrentRun();
        Assert.assertEquals(5, perturbedActions.get(0).getTimestepsTotal());
        Assert.assertEquals(21, perturbedActions.get(1).getTimestepsTotal());
        Assert.assertEquals(4, perturbedActions.get(2).getTimestepsTotal());
        Assert.assertEquals(42, perturbedActions.get(3).getTimestepsTotal());
        Assert.assertEquals(16, perturbedActions.get(4).getTimestepsTotal());

        // Multiple perturbations, perturbations too big.
        perturbationLocations = new HashMap<>();
        perturbationLocations.put(3, 3);
        perturbationLocations.put(1, 2);
        perturbationLocations.put(0, 5);
        perturbationLocations.put(2, Integer.MAX_VALUE);

        perturber = new ActionPerturber_MissingTimesteps<>(perturbationLocations);
        perturbedQueue = perturber.perturb(originalQueue);
        Assert.assertEquals(81, perturbedQueue.getTotalQueueLengthTimesteps());
        perturbedActions = perturbedQueue.getActionsInCurrentRun();
        Assert.assertEquals(1, perturbedActions.get(0).getTimestepsTotal());
        Assert.assertEquals(21, perturbedActions.get(1).getTimestepsTotal());
        Assert.assertEquals(1, perturbedActions.get(2).getTimestepsTotal());
        Assert.assertEquals(42, perturbedActions.get(3).getTimestepsTotal());
        Assert.assertEquals(16, perturbedActions.get(4).getTimestepsTotal());
    }

    private ActionQueue<CommandQWOP> makeActionQueue() {
        ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>(); // 93 timesteps total.
        actionQueue.addAction(new Action<>(5, CommandQWOP.QO));
        actionQueue.addAction(new Action<>(23, CommandQWOP.W));
        actionQueue.addAction(new Action<>(4, CommandQWOP.P));
        actionQueue.addAction(new Action<>(45, CommandQWOP.O));
        actionQueue.addAction(new Action<>(16, CommandQWOP.Q));
        return actionQueue;
    }
}
