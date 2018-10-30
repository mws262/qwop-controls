package actions.perturbers;

import actions.Action;
import actions.ActionQueue;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ActionPerturber_SwitchTooSoonTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    @Test
    public void perturb() {

        // Single perturbation.
        ActionQueue aq = makeActionQueue();
        Action[] originalActions = aq.getActionsInCurrentRun();
        Map<Integer, Integer> perturbMap = new HashMap<>();
        perturbMap.put(1, 1);
        ActionPerturber_SwitchTooSoon actionPerturber1 = new ActionPerturber_SwitchTooSoon(perturbMap);
        ActionQueue queuePerturbed1 = actionPerturber1.perturb(aq);
        Action[] actions = queuePerturbed1.getActionsInCurrentRun();
        Assert.assertEquals(5, actions.length);
        Assert.assertEquals(aq.getTotalQueueLengthTimesteps(), queuePerturbed1.getTotalQueueLengthTimesteps());
        Assert.assertEquals(11, actions[0].getTimestepsTotal());
        Assert.assertEquals(14, actions[1].getTimestepsTotal());
        Assert.assertEquals(14, actions[2].getTimestepsTotal());
        Assert.assertEquals(15, actions[3].getTimestepsTotal());
        Assert.assertEquals(16, actions[4].getTimestepsTotal());

        Assert.assertTrue(Arrays.equals(actions[0].peek(), originalActions[0].peek()));
        Assert.assertTrue(Arrays.equals(actions[1].peek(), originalActions[1].peek()));
        Assert.assertTrue(Arrays.equals(actions[2].peek(), originalActions[2].peek()));
        Assert.assertTrue(Arrays.equals(actions[3].peek(), originalActions[3].peek()));
        Assert.assertTrue(Arrays.equals(actions[4].peek(), originalActions[4].peek()));

        // Single perturbation, too big.
        aq = makeActionQueue();
        originalActions = aq.getActionsInCurrentRun();
        perturbMap = new HashMap<>();
        perturbMap.put(2, 13); // Can't subtract 13 from action 2, should do 12.
        actionPerturber1 = new ActionPerturber_SwitchTooSoon(perturbMap);
        queuePerturbed1 = actionPerturber1.perturb(aq);
        actions = queuePerturbed1.getActionsInCurrentRun();
        Assert.assertEquals(5, actions.length);

        Assert.assertEquals(aq.getTotalQueueLengthTimesteps(), queuePerturbed1.getTotalQueueLengthTimesteps());
        Assert.assertEquals(12, actions[0].getTimestepsTotal());
        Assert.assertEquals(1, actions[1].getTimestepsTotal());
        Assert.assertEquals(26, actions[2].getTimestepsTotal());
        Assert.assertEquals(15, actions[3].getTimestepsTotal());
        Assert.assertEquals(16, actions[4].getTimestepsTotal());

        Assert.assertTrue(Arrays.equals(actions[0].peek(), originalActions[0].peek()));
        Assert.assertTrue(Arrays.equals(actions[1].peek(), originalActions[1].peek()));
        Assert.assertTrue(Arrays.equals(actions[2].peek(), originalActions[2].peek()));
        Assert.assertTrue(Arrays.equals(actions[3].peek(), originalActions[3].peek()));
        Assert.assertTrue(Arrays.equals(actions[4].peek(), originalActions[4].peek()));

        // Multiple perturbations, adjacent.
        aq = makeActionQueue();
        originalActions = aq.getActionsInCurrentRun();
        perturbMap = new HashMap<>();
        perturbMap.put(3, 5); // Can't subtract 13 from action 2, should do 12.
        perturbMap.put(4, 6); // Can't subtract 13 from action 2, should do 12.
        actionPerturber1 = new ActionPerturber_SwitchTooSoon(perturbMap);
        queuePerturbed1 = actionPerturber1.perturb(aq);
        actions = queuePerturbed1.getActionsInCurrentRun();
        Assert.assertEquals(5, actions.length);

        Assert.assertEquals(aq.getTotalQueueLengthTimesteps(), queuePerturbed1.getTotalQueueLengthTimesteps());
        Assert.assertEquals(12, actions[0].getTimestepsTotal());
        Assert.assertEquals(13, actions[1].getTimestepsTotal());
        Assert.assertEquals(9, actions[2].getTimestepsTotal());
        Assert.assertEquals(14, actions[3].getTimestepsTotal());
        Assert.assertEquals(22, actions[4].getTimestepsTotal());

        Assert.assertTrue(Arrays.equals(actions[0].peek(), originalActions[0].peek()));
        Assert.assertTrue(Arrays.equals(actions[1].peek(), originalActions[1].peek()));
        Assert.assertTrue(Arrays.equals(actions[2].peek(), originalActions[2].peek()));
        Assert.assertTrue(Arrays.equals(actions[3].peek(), originalActions[3].peek()));
        Assert.assertTrue(Arrays.equals(actions[4].peek(), originalActions[4].peek()));

        // Exception cases.
        exception.expect(IllegalArgumentException.class);
        perturbMap = new HashMap<>();
        perturbMap.put(0, 1);
        new ActionPerturber_SwitchTooSoon(perturbMap);

        perturbMap = new HashMap<>();
        perturbMap.put(2, 0);
        new ActionPerturber_SwitchTooSoon(perturbMap);
    }

    private ActionQueue makeActionQueue() {
        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addAction(new Action(12, false, false, true, false));
        actionQueue.addAction(new Action(13, true, false, true, false));
        actionQueue.addAction(new Action(14, false, false, false, true));
        actionQueue.addAction(new Action(15, false, true, false, false));
        actionQueue.addAction(new Action(16, true, false, true, true));
        return actionQueue;
    }
}
