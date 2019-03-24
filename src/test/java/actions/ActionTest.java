package actions;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
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
            Assert.assertArrayEquals(action1Copy.poll(), keys1);
            ts++;
        }
        Assert.assertEquals(ts, actTimesteps1);

        // Test action 2
        ts = 0;
        while (action2Copy.hasNext()) {
            Assert.assertArrayEquals(action2Copy.poll(), keys2);
            ts++;
        }
        Assert.assertEquals(ts, actTimesteps2);

        // Test action 3
        ts = 0;
        while (action3Copy.hasNext()) {
            Assert.assertArrayEquals(action3Copy.poll(), keys3);
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
        Assert.assertArrayEquals(validAction1.peek(), keys1);
        Assert.assertArrayEquals(validAction2.peek(), keys2);
        Assert.assertArrayEquals(validAction3.peek(), keys3);

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

        Assert.assertNotEquals(validAction1, String.valueOf(5));
        Assert.assertNotEquals(validAction1, validAction2);

        Assert.assertEquals(validAction1, validAction1);
        Assert.assertEquals(validAction2, validAction2);
        Assert.assertEquals(validAction3, validAction3);

        // Must copy to get a pollable version of the action.
        Action action1Copy = validAction1.getCopy();
        Action action2Copy = validAction2.getCopy();
        Action action3Copy = validAction3.getCopy();

        Assert.assertEquals(validAction1, action1Copy);
        Assert.assertEquals(validAction2, action2Copy);
        Assert.assertEquals(validAction3, action3Copy);

        Action equivAction = new Action(actTimesteps1, keys1).getCopy();
        Assert.assertEquals(equivAction, action1Copy);


        for (int i = 0; i < 5; i++) {
            action1Copy.poll();
            equivAction.poll();
        }
        Assert.assertEquals(equivAction, action1Copy);
    }

    @Test
    public void getCopy() {
        Assert.assertEquals(validAction1, validAction1.getCopy());
        Assert.assertEquals(validAction2, validAction2.getCopy());
        Assert.assertEquals(validAction3, validAction3.getCopy());
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
        Assert.assertArrayEquals(consolidatedActions.get(0).peek(), new boolean[]{false, false, false, false});

        Assert.assertEquals(9, consolidatedActions.get(1).getTimestepsTotal());
        Assert.assertEquals(9, consolidatedActions.get(1).getTimestepsRemaining());
        Assert.assertArrayEquals(consolidatedActions.get(1).peek(), new boolean[]{true, false, false, false});

        // Make a list with a single, nonzero element.
        List<Action> singleActionList = new ArrayList<>();
        singleActionList.add(new Action(10, true, true, true, true));

        List<Action> consolidatedSingleAction = Action.consolidateActions(singleActionList);
        Assert.assertEquals(10, consolidatedSingleAction.get(0).getTimestepsRemaining());
        Assert.assertArrayEquals(consolidatedSingleAction.get(0).peek(), new boolean[]{true, true, true, true});
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

    @Test
    public void keysToOneHot() {
        // I don't care which keys correspond to which one-hot element as long as they are unique.
        float[] sum = new float[9];
        for (Action.Keys keys : Action.Keys.values()) {
            float[] oneHot = Action.keysToOneHot(keys);
            float individualSum = 0;
            for (int i = 0; i < sum.length; i++) {
                sum[i] += oneHot[i];
                individualSum += oneHot[i];
            }
            Assert.assertEquals(1f, individualSum, 1e-15f); // Should only be a single one in the array.
        }

        Assert.assertArrayEquals(new float[] {1, 1, 1, 1, 1, 1, 1, 1, 1}, sum, 1e-15f); // The array should have only
        // one of each element.
    }

    @Test
    public void keysToBooleans() {
        Assert.assertArrayEquals(new boolean[]{true, false, false, false}, Action.keysToBooleans(Action.Keys.q));
        Assert.assertArrayEquals(new boolean[]{false, true, false, false}, Action.keysToBooleans(Action.Keys.w));
        Assert.assertArrayEquals(new boolean[]{false, false, true, false}, Action.keysToBooleans(Action.Keys.o));
        Assert.assertArrayEquals(new boolean[]{false, false, false, true}, Action.keysToBooleans(Action.Keys.p));
        Assert.assertArrayEquals(new boolean[]{true, false, true, false}, Action.keysToBooleans(Action.Keys.qo));
        Assert.assertArrayEquals(new boolean[]{true, false, false, true}, Action.keysToBooleans(Action.Keys.qp));
        Assert.assertArrayEquals(new boolean[]{false, true, true, false}, Action.keysToBooleans(Action.Keys.wo));
        Assert.assertArrayEquals(new boolean[]{false, true, false, true}, Action.keysToBooleans(Action.Keys.wp));
        Assert.assertArrayEquals(new boolean[]{false, false, false, false}, Action.keysToBooleans(Action.Keys.none));
    }

    @Test
    public void booleansToKeys() {
        Assert.assertEquals(Action.Keys.q, Action.booleansToKeys(new boolean[]{true, false, false, false}));
        Assert.assertEquals(Action.Keys.w, Action.booleansToKeys(new boolean[]{false, true, false, false}));
        Assert.assertEquals(Action.Keys.o, Action.booleansToKeys(new boolean[]{false, false, true, false}));
        Assert.assertEquals(Action.Keys.p, Action.booleansToKeys(new boolean[]{false, false, false, true}));
        Assert.assertEquals(Action.Keys.qo, Action.booleansToKeys(new boolean[]{true, false, true, false}));
        Assert.assertEquals(Action.Keys.qp, Action.booleansToKeys(new boolean[]{true, false, false, true}));
        Assert.assertEquals(Action.Keys.wo, Action.booleansToKeys(new boolean[]{false, true, true, false}));
        Assert.assertEquals(Action.Keys.wp, Action.booleansToKeys(new boolean[]{false, true, false, true}));
        Assert.assertEquals(Action.Keys.none, Action.booleansToKeys(new boolean[]{false, false, false, false}));
    }
}