package game.action.perturbers;

import game.action.Action;
import game.action.ActionQueue;
import game.qwop.CommandQWOP;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionPerturber_OffsetActionTransitionsTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    @Test
    public void perturb() {

        // Single perturbation.
        ActionQueue<CommandQWOP> aq = makeActionQueue();
        List<Action<CommandQWOP>> originalActions = aq.getActionsInCurrentRun();
        Map<Integer, Integer> perturbMap = new HashMap<>();
        perturbMap.put(1, 1);
        ActionPerturber_OffsetActionTransitions<CommandQWOP> actionPerturber1 = new ActionPerturber_OffsetActionTransitions<>(perturbMap);
        ActionQueue<CommandQWOP> queuePerturbed1 = actionPerturber1.perturb(aq);
        List<Action<CommandQWOP>> actions = queuePerturbed1.getActionsInCurrentRun();
        Assert.assertEquals(5, actions.size());
        Assert.assertEquals(aq.getTotalQueueLengthTimesteps(), queuePerturbed1.getTotalQueueLengthTimesteps());
        Assert.assertEquals(11, actions.get(0).getTimestepsTotal());
        Assert.assertEquals(14, actions.get(1).getTimestepsTotal());
        Assert.assertEquals(14, actions.get(2).getTimestepsTotal());
        Assert.assertEquals(15, actions.get(3).getTimestepsTotal());
        Assert.assertEquals(16, actions.get(4).getTimestepsTotal());

        Assert.assertEquals(actions.get(0).peek(), originalActions.get(0).peek());
        Assert.assertEquals(actions.get(1).peek(), originalActions.get(1).peek());
        Assert.assertEquals(actions.get(2).peek(), originalActions.get(2).peek());
        Assert.assertEquals(actions.get(3).peek(), originalActions.get(3).peek());
        Assert.assertEquals(actions.get(4).peek(), originalActions.get(4).peek());

        // Single perturbation -- negative.
        aq = makeActionQueue();
        originalActions = aq.getActionsInCurrentRun();
        perturbMap = new HashMap<>();
        perturbMap.put(2, -2);
        actionPerturber1 = new ActionPerturber_OffsetActionTransitions<>(perturbMap);
        queuePerturbed1 = actionPerturber1.perturb(aq);
        actions = queuePerturbed1.getActionsInCurrentRun();
        Assert.assertEquals(5, actions.size());

        Assert.assertEquals(aq.getTotalQueueLengthTimesteps(), queuePerturbed1.getTotalQueueLengthTimesteps());
        Assert.assertEquals(12, actions.get(0).getTimestepsTotal());
        Assert.assertEquals(15, actions.get(1).getTimestepsTotal());
        Assert.assertEquals(12, actions.get(2).getTimestepsTotal());
        Assert.assertEquals(15, actions.get(3).getTimestepsTotal());
        Assert.assertEquals(16, actions.get(4).getTimestepsTotal());

        Assert.assertEquals(actions.get(0).peek(), originalActions.get(0).peek());
        Assert.assertEquals(actions.get(1).peek(), originalActions.get(1).peek());
        Assert.assertEquals(actions.get(2).peek(), originalActions.get(2).peek());
        Assert.assertEquals(actions.get(3).peek(), originalActions.get(3).peek());
        Assert.assertEquals(actions.get(4).peek(), originalActions.get(4).peek());

        // Single perturbation, too big.
        aq = makeActionQueue();
        originalActions = aq.getActionsInCurrentRun();
        perturbMap = new HashMap<>();
        perturbMap.put(2, 13); // Can't subtract 13 from command 2, should do 12.
        actionPerturber1 = new ActionPerturber_OffsetActionTransitions<>(perturbMap);
        queuePerturbed1 = actionPerturber1.perturb(aq);
        actions = queuePerturbed1.getActionsInCurrentRun();
        Assert.assertEquals(5, actions.size());

        Assert.assertEquals(aq.getTotalQueueLengthTimesteps(), queuePerturbed1.getTotalQueueLengthTimesteps());
        Assert.assertEquals(12, actions.get(0).getTimestepsTotal());
        Assert.assertEquals(1, actions.get(1).getTimestepsTotal());
        Assert.assertEquals(26, actions.get(2).getTimestepsTotal());
        Assert.assertEquals(15, actions.get(3).getTimestepsTotal());
        Assert.assertEquals(16, actions.get(4).getTimestepsTotal());

        Assert.assertEquals(actions.get(0).peek(), originalActions.get(0).peek());
        Assert.assertEquals(actions.get(1).peek(), originalActions.get(1).peek());
        Assert.assertEquals(actions.get(2).peek(), originalActions.get(2).peek());
        Assert.assertEquals(actions.get(3).peek(), originalActions.get(3).peek());
        Assert.assertEquals(actions.get(4).peek(), originalActions.get(4).peek());

        // Single perturbation, too small.
        aq = makeActionQueue();
        originalActions = aq.getActionsInCurrentRun();
        perturbMap = new HashMap<>();
        perturbMap.put(2, -14); // Can't subtract 13 from command 2, should do 12.
        actionPerturber1 = new ActionPerturber_OffsetActionTransitions<>(perturbMap);
        queuePerturbed1 = actionPerturber1.perturb(aq);
        actions = queuePerturbed1.getActionsInCurrentRun();
        Assert.assertEquals(5, actions.size());

        Assert.assertEquals(aq.getTotalQueueLengthTimesteps(), queuePerturbed1.getTotalQueueLengthTimesteps());
        Assert.assertEquals(12, actions.get(0).getTimestepsTotal());
        Assert.assertEquals(26, actions.get(1).getTimestepsTotal());
        Assert.assertEquals(1, actions.get(2).getTimestepsTotal());
        Assert.assertEquals(15, actions.get(3).getTimestepsTotal());
        Assert.assertEquals(16, actions.get(4).getTimestepsTotal());

        Assert.assertEquals(actions.get(0).peek(), originalActions.get(0).peek());
        Assert.assertEquals(actions.get(1).peek(), originalActions.get(1).peek());
        Assert.assertEquals(actions.get(2).peek(), originalActions.get(2).peek());
        Assert.assertEquals(actions.get(3).peek(), originalActions.get(3).peek());
        Assert.assertEquals(actions.get(4).peek(), originalActions.get(4).peek());

        // Multiple perturbations, adjacent.
        aq = makeActionQueue();
        originalActions = aq.getActionsInCurrentRun();
        perturbMap = new HashMap<>();
        perturbMap.put(3, 5);
        perturbMap.put(4, 6);
        perturbMap.put(2, -3);
        actionPerturber1 = new ActionPerturber_OffsetActionTransitions<>(perturbMap);
        queuePerturbed1 = actionPerturber1.perturb(aq);
        actions = queuePerturbed1.getActionsInCurrentRun();
        Assert.assertEquals(5, actions.size());

        Assert.assertEquals(aq.getTotalQueueLengthTimesteps(), queuePerturbed1.getTotalQueueLengthTimesteps());
        Assert.assertEquals(12, actions.get(0).getTimestepsTotal());
        Assert.assertEquals(16, actions.get(1).getTimestepsTotal());
        Assert.assertEquals(6, actions.get(2).getTimestepsTotal());
        Assert.assertEquals(14, actions.get(3).getTimestepsTotal());
        Assert.assertEquals(22, actions.get(4).getTimestepsTotal());

        Assert.assertEquals(actions.get(0).peek(), originalActions.get(0).peek());
        Assert.assertEquals(actions.get(1).peek(), originalActions.get(1).peek());
        Assert.assertEquals(actions.get(2).peek(), originalActions.get(2).peek());
        Assert.assertEquals(actions.get(3).peek(), originalActions.get(3).peek());
        Assert.assertEquals(actions.get(4).peek(), originalActions.get(4).peek());

        // Exception cases.
        exception.expect(IllegalArgumentException.class);
        perturbMap = new HashMap<>();
        perturbMap.put(0, 1);
        new ActionPerturber_OffsetActionTransitions(perturbMap);

        perturbMap = new HashMap<>();
        perturbMap.put(2, 0);
        new ActionPerturber_OffsetActionTransitions(perturbMap);
    }

    private ActionQueue<CommandQWOP> makeActionQueue() {
        ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();
        actionQueue.addAction(new Action<>(12, CommandQWOP.O));
        actionQueue.addAction(new Action<>(13, CommandQWOP.QO));
        actionQueue.addAction(new Action<>(14, CommandQWOP.P));
        actionQueue.addAction(new Action<>(15, CommandQWOP.W));
        actionQueue.addAction(new Action<>(16, CommandQWOP.WP));
        return actionQueue;
    }
}
