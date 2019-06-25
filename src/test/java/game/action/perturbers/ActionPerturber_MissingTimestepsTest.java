package game.action.perturbers;

import game.action.Action;
import game.action.ActionQueue;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ActionPerturber_MissingTimestepsTest {

    @Test
    public void perturb() {
        ActionQueue originalQueue = makeActionQueue();

        // Single perturbation, no unusual circumstances.
        Map<Integer, Integer> perturbationLocations = new HashMap<>();
        perturbationLocations.put(3, 1);
        ActionPerturber_MissingTimesteps perturber = new ActionPerturber_MissingTimesteps(perturbationLocations);
        ActionQueue perturbedQueue = perturber.perturb(originalQueue);

        Assert.assertEquals(92, perturbedQueue.getTotalQueueLengthTimesteps());
        Action[] perturbedActions = perturbedQueue.getActionsInCurrentRun();
        Assert.assertEquals(5, perturbedActions[0].getTimestepsTotal());
        Assert.assertEquals(23, perturbedActions[1].getTimestepsTotal());
        Assert.assertEquals(4, perturbedActions[2].getTimestepsTotal());
        Assert.assertEquals(44, perturbedActions[3].getTimestepsTotal());
        Assert.assertEquals(16, perturbedActions[4].getTimestepsTotal());

        // Multiple perturbations, no unusual circumstances.
        perturbationLocations = new HashMap<>();
        perturbationLocations.put(3, 3);
        perturbationLocations.put(1, 2);

        perturber = new ActionPerturber_MissingTimesteps(perturbationLocations);
        perturbedQueue = perturber.perturb(originalQueue);
        Assert.assertEquals(88, perturbedQueue.getTotalQueueLengthTimesteps());
        perturbedActions = perturbedQueue.getActionsInCurrentRun();
        Assert.assertEquals(5, perturbedActions[0].getTimestepsTotal());
        Assert.assertEquals(21, perturbedActions[1].getTimestepsTotal());
        Assert.assertEquals(4, perturbedActions[2].getTimestepsTotal());
        Assert.assertEquals(42, perturbedActions[3].getTimestepsTotal());
        Assert.assertEquals(16, perturbedActions[4].getTimestepsTotal());

        // Multiple perturbations, perturbations too big.
        perturbationLocations = new HashMap<>();
        perturbationLocations.put(3, 3);
        perturbationLocations.put(1, 2);
        perturbationLocations.put(0, 5);
        perturbationLocations.put(2, Integer.MAX_VALUE);

        perturber = new ActionPerturber_MissingTimesteps(perturbationLocations);
        perturbedQueue = perturber.perturb(originalQueue);
        Assert.assertEquals(81, perturbedQueue.getTotalQueueLengthTimesteps());
        perturbedActions = perturbedQueue.getActionsInCurrentRun();
        Assert.assertEquals(1, perturbedActions[0].getTimestepsTotal());
        Assert.assertEquals(21, perturbedActions[1].getTimestepsTotal());
        Assert.assertEquals(1, perturbedActions[2].getTimestepsTotal());
        Assert.assertEquals(42, perturbedActions[3].getTimestepsTotal());
        Assert.assertEquals(16, perturbedActions[4].getTimestepsTotal());
    }

    private ActionQueue makeActionQueue() {
        ActionQueue actionQueue = new ActionQueue(); // 93 timesteps total.
        actionQueue.addAction(new Action(5, true, false, true, false));
        actionQueue.addAction(new Action(23, true, true, true, false));
        actionQueue.addAction(new Action(4, false, false, false, true));
        actionQueue.addAction(new Action(45, false, true, false, false));
        actionQueue.addAction(new Action(16, true, false, true, true));
        return actionQueue;
    }
}
