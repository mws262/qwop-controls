package main;

import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActionTest {

    private int actTimesteps1 = 10;
    private int actTimesteps2 = 50;
    private int actTimesteps3 = 0;

    private boolean[] keys1 = {false, true, true, false};
    private boolean[] keys2 = {true, false, false, true};
    private boolean[] keys3 = {false, false, false, false};

    private Action validAction1 = new Action(actTimesteps1, keys1[0], keys1[1], keys1[2], keys1[3]);
    private Action validAction2 = new Action(actTimesteps2, keys2);
    private Action validAction3 = new Action(actTimesteps3, keys3);

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    @Test
    public void poll() {
        // Must copy to get a pollable version of the action.
        Action action1Copy = validAction1.getCopy();
        Action action2Copy = validAction2.getCopy();
        Action action3Copy = validAction3.getCopy();

        // Make sure that polling evaluates to the correct keys and continues to have next until the expected number
        // of timesteps.
        // Test action 1
        int ts = 0;
        while (action1Copy.hasNext()) {
            Assert.assertTrue(Arrays.equals(action1Copy.poll(), keys1));
            ts++;
        }
        Assert.assertEquals(ts, actTimesteps1);

        // Test action 2
        ts = 0;
        while (action2Copy.hasNext()) {
            Assert.assertTrue(Arrays.equals(action2Copy.poll(), keys2));
            ts++;
        }
        Assert.assertEquals(ts, actTimesteps2);

        // Test action 3
        ts = 0;
        while (action3Copy.hasNext()) {
            Assert.assertTrue(Arrays.equals(action3Copy.poll(), keys3));
            ts++;
        }
        Assert.assertEquals(ts, actTimesteps3);
    }

    @Test
    public void pollRuntimeException() {
        // Cannot poll the base copy of this action. Must copy before polling.
        exception.expect(RuntimeException.class);
        validAction1.poll();
    }

    @Test
    public void pollIndexOutOfBoundsException() {
        exception.expect(IndexOutOfBoundsException.class);
        // Must copy to get a pollable version of the action.
        Action action1Copy = validAction1.getCopy();
        Action action2Copy = validAction2.getCopy();
        Action action3Copy = validAction3.getCopy();

        // Poll away all valid timesteps.
        while (action1Copy.hasNext())
            action1Copy.poll();
        while (action2Copy.hasNext())
            action1Copy.poll();
        while (action3Copy.hasNext())
            action1Copy.poll();

        // Polling one too many times should throw.
        action1Copy.poll();
        action2Copy.poll();
        action3Copy.poll();
    }

    @Test
    public void peek() {

        // Should be able to peek the base version of an action (it's effectively a const method).
        Assert.assertTrue(Arrays.equals(validAction1.peek(), keys1));
        Assert.assertTrue(Arrays.equals(validAction2.peek(), keys2));
        Assert.assertTrue(Arrays.equals(validAction3.peek(), keys3));

        // Must copy to get a pollable version of the action.
        Action action1Copy = validAction1.getCopy();
        Action action2Copy = validAction2.getCopy();
        Action action3Copy = validAction3.getCopy();

        // Peeking should not alter the number of timesteps remaining -- even if it is a mutable action.
        action1Copy.peek();
        action2Copy.peek();
        action3Copy.peek();

        Assert.assertEquals(action1Copy.getTimestepsRemaining(), action1Copy.getTimestepsRemaining());
        Assert.assertEquals(action2Copy.getTimestepsRemaining(), action2Copy.getTimestepsRemaining());
        Assert.assertEquals(action3Copy.getTimestepsRemaining(), action3Copy.getTimestepsRemaining());
    }

    @Test
    public void hasNext() {
        // Must copy to get a pollable version of the action.
        Action action1Copy = validAction1.getCopy();
        Action action2Copy = validAction2.getCopy();
        Action action3Copy = validAction3.getCopy();

        // If our action has greater than 0 timesteps, then initially hasNext should be true.
        if (action1Copy.getTimestepsTotal() > 0)
            Assert.assertTrue(action1Copy.hasNext());
        // If we drain away all the timesteps, hasNext should become false.
        for (int i = 0; i < action1Copy.getTimestepsTotal(); i++) {
            action1Copy.poll();
        }
        Assert.assertFalse(action1Copy.hasNext());

        // If our action has greater than 0 timesteps, then initially hasNext should be true.
        if (action2Copy.getTimestepsTotal() > 0)
            Assert.assertTrue(action2Copy.hasNext());
        // If we drain away all the timesteps, hasNext should become false.
        for (int i = 0; i < action2Copy.getTimestepsTotal(); i++) {
            action2Copy.poll();
        }
        Assert.assertFalse(action2Copy.hasNext());

        // If our action has greater than 0 timesteps, then initially hasNext should be true.
        if (action3Copy.getTimestepsTotal() > 0)
            Assert.assertTrue(action3Copy.hasNext());
        // If we drain away all the timesteps, hasNext should become false.
        for (int i = 0; i < action3Copy.getTimestepsTotal(); i++) {
            action3Copy.poll();
        }
        Assert.assertFalse(action3Copy.hasNext());
    }

    @Test
    public void reset() {
        // Must copy to get a pollable version of the action.
        Action action1Copy = validAction1.getCopy();
        Action action2Copy = validAction2.getCopy();
        Action action3Copy = validAction3.getCopy();

        if (action1Copy.getTimestepsTotal() > 0)
            action1Copy.poll();
        if (action2Copy.getTimestepsTotal() > 0)
            action2Copy.poll();
        if (action3Copy.getTimestepsTotal() > 0)
            action3Copy.poll();

        action1Copy.reset();
        action2Copy.reset();
        action3Copy.reset();

        // Resetting should bring the total timesteps remaining back to the total number.
        Assert.assertEquals(action1Copy.getTimestepsRemaining(), action1Copy.getTimestepsTotal());
        Assert.assertEquals(action2Copy.getTimestepsRemaining(), action2Copy.getTimestepsTotal());
        Assert.assertEquals(action3Copy.getTimestepsRemaining(), action3Copy.getTimestepsTotal());
    }

    @Test
    public void resetRuntimeException() {
        exception.expect(RuntimeException.class);
        validAction1.reset();
    }

    @Test
    public void getTimestepsTotal() {
        Assert.assertEquals(actTimesteps1, validAction1.getTimestepsTotal());
        Assert.assertEquals(actTimesteps2, validAction2.getTimestepsTotal());
        Assert.assertEquals(actTimesteps3, validAction3.getTimestepsTotal());
    }

    @Test
    public void equals() {

        Assert.assertFalse(validAction1.equals(String.valueOf(5)));
        Assert.assertFalse(validAction1.equals(validAction2));

        Assert.assertTrue(validAction1.equals(validAction1));
        Assert.assertTrue(validAction2.equals(validAction2));
        Assert.assertTrue(validAction3.equals(validAction3));

        // Must copy to get a pollable version of the action.
        Action action1Copy = validAction1.getCopy();
        Action action2Copy = validAction2.getCopy();
        Action action3Copy = validAction3.getCopy();

        Assert.assertTrue(validAction1.equals(action1Copy));
        Assert.assertTrue(validAction2.equals(action2Copy));
        Assert.assertTrue(validAction3.equals(action3Copy));

        Action equivAction = new Action(actTimesteps1, keys1).getCopy();
        Assert.assertTrue(equivAction.equals(action1Copy));


        for (int i = 0; i < 5; i++) {
            action1Copy.poll();
            equivAction.poll();
        }
        Assert.assertTrue(equivAction.equals(action1Copy));
    }

    @Test
    public void getCopy() {
        Assert.assertTrue(validAction1.equals(validAction1.getCopy()));
        Assert.assertTrue(validAction2.equals(validAction2.getCopy()));
        Assert.assertTrue(validAction3.equals(validAction3.getCopy()));
    }

    @Test
    public void isMutable() {
        Action act = new Action(14, true, true, true, true);
        Assert.assertFalse(act.isMutable());

        Action act_copy = act.getCopy();
        Assert.assertTrue(act_copy.isMutable());
    }

    @Test
    public void consolidateActions() {
        // General list of actions with weird ordering and some zero-duration actions.
        List<Action> actions = new ArrayList<>();
        actions.add(new Action(4, false, false, false, false));
        actions.add(new Action(1, false, false, false, false));
        actions.add(new Action(0, false, false, false, false));
        actions.add(new Action(3, true, false, false, false));
        actions.add(new Action(0, false, false, false, false));
        actions.add(new Action(1, true, false, false, false));
        actions.add(new Action(5, true, false, false, false));
        actions.add(new Action(0, true, false, false, true));

        List<Action> consolidatedActions = Action.consolidateActions(actions);

        Assert.assertEquals(2, consolidatedActions.size());

        Assert.assertEquals(5, consolidatedActions.get(0).getTimestepsTotal());
        Assert.assertEquals(5, consolidatedActions.get(0).getTimestepsRemaining());
        Assert.assertTrue(Arrays.equals(consolidatedActions.get(0).peek(), new boolean[]{false, false, false, false}));

        Assert.assertEquals(9, consolidatedActions.get(1).getTimestepsTotal());
        Assert.assertEquals(9, consolidatedActions.get(1).getTimestepsRemaining());
        Assert.assertTrue(Arrays.equals(consolidatedActions.get(1).peek(), new boolean[]{true, false, false, false}));

        // Make a list with a single, nonzero element.
        List<Action> singleActionList = new ArrayList<>();
        singleActionList.add(new Action(10, true, true, true, true));

        List<Action> consolidatedSingleAction = Action.consolidateActions(singleActionList);
        Assert.assertEquals(10, consolidatedSingleAction.get(0).getTimestepsRemaining());
        Assert.assertTrue(Arrays.equals(consolidatedSingleAction.get(0).peek(), new boolean[]{true, true, true, true}));
    }

    @Test
    public void consolidateActionsIllegalArgumentExceptionSingleElement() {
        exception.expect(IllegalArgumentException.class);

        // Make a list with a single, 0-duration element.
        List<Action> singleActionList = new ArrayList<>();
        singleActionList.add(new Action(0, true, true, true, true));
        List<Action> consolidatedSingleAction = Action.consolidateActions(singleActionList);
    }

    @Test
    public void consolidateActionsIllegalArgumentExceptionMultiElement() {
        exception.expect(IllegalArgumentException.class);

        // Make a list with several, 0-duration element.
        List<Action> actionList = new ArrayList<>();
        actionList.add(new Action(0, true, true, true, true));
        actionList.add(new Action(0, true, false, true, true));
        actionList.add(new Action(0, true, true, false, true));
        actionList.add(new Action(0, true, true, true, false));

        List<Action> consolidateActions = Action.consolidateActions(actionList);
    }

    @Test
    public void constructorThrowsIllegalArgumentException() {
        exception.expect(IllegalArgumentException.class);
        new Action(-1, false, false, false, false);
    }
}