package game.action;

import game.qwop.CommandQWOP;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionTest {

    private int actTimesteps1 = 10;
    private int actTimesteps2 = 50;
    private int actTimesteps3 = 0;

    private CommandQWOP command1 = CommandQWOP.WO;
    private CommandQWOP command2 = CommandQWOP.QP;
    private CommandQWOP command3 = CommandQWOP.NONE;

    private Action<CommandQWOP> validAction1 = new Action<>(actTimesteps1, command1);
    private Action<CommandQWOP> validAction2 = new Action<>(actTimesteps2, command2);
    private Action<CommandQWOP> validAction3 = new Action<>(actTimesteps3, command3);

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    @Test
    public void poll() {
        // Must copy to get a pollable version of the command.
        Action action1Copy = validAction1.getCopy();
        Action action2Copy = validAction2.getCopy();
        Action action3Copy = validAction3.getCopy();

        // Make sure that polling evaluates to the correct keys and continues to have next until the expected number
        // of timesteps.
        // Test command 1
        int ts = 0;
        while (action1Copy.hasNext()) {
            Assert.assertEquals(action1Copy.poll(), command1);
            ts++;
        }
        Assert.assertEquals(ts, actTimesteps1);

        // Test command 2
        ts = 0;
        while (action2Copy.hasNext()) {
            Assert.assertEquals(action2Copy.poll(), command2);
            ts++;
        }
        Assert.assertEquals(ts, actTimesteps2);

        // Test command 3
        ts = 0;
        while (action3Copy.hasNext()) {
            Assert.assertEquals(action3Copy.poll(), command3);
            ts++;
        }
        Assert.assertEquals(ts, actTimesteps3);
    }

    @Test
    public void pollRuntimeException() {
        // Cannot poll the base copy of this command. Must copy before polling.
        exception.expect(RuntimeException.class);
        validAction1.poll();
    }

    @Test
    public void pollIndexOutOfBoundsException() {
        exception.expect(IndexOutOfBoundsException.class);
        // Must copy to get a pollable version of the command.
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

        // Should be able to peek the base version of an command (it's effectively a const method).
        Assert.assertEquals(validAction1.peek(), command1);
        Assert.assertEquals(validAction2.peek(), command2);
        Assert.assertEquals(validAction3.peek(), command3);

        // Must copy to get a pollable version of the command.
        Action action1Copy = validAction1.getCopy();
        Action action2Copy = validAction2.getCopy();
        Action action3Copy = validAction3.getCopy();

        // Peeking should not alter the number of timesteps remaining -- even if it is a mutable command.
        action1Copy.peek();
        action2Copy.peek();
        action3Copy.peek();

        Assert.assertEquals(action1Copy.getTimestepsRemaining(), action1Copy.getTimestepsRemaining());
        Assert.assertEquals(action2Copy.getTimestepsRemaining(), action2Copy.getTimestepsRemaining());
        Assert.assertEquals(action3Copy.getTimestepsRemaining(), action3Copy.getTimestepsRemaining());
    }

    @Test
    public void hasNext() {
        // Must copy to get a pollable version of the command.
        Action action1Copy = validAction1.getCopy();
        Action action2Copy = validAction2.getCopy();
        Action action3Copy = validAction3.getCopy();

        // If our command has greater than 0 timesteps, then initially hasNext should be true.
        if (action1Copy.getTimestepsTotal() > 0)
            Assert.assertTrue(action1Copy.hasNext());
        // If we drain away all the timesteps, hasNext should become false.
        for (int i = 0; i < action1Copy.getTimestepsTotal(); i++) {
            action1Copy.poll();
        }
        Assert.assertFalse(action1Copy.hasNext());

        // If our command has greater than 0 timesteps, then initially hasNext should be true.
        if (action2Copy.getTimestepsTotal() > 0)
            Assert.assertTrue(action2Copy.hasNext());
        // If we drain away all the timesteps, hasNext should become false.
        for (int i = 0; i < action2Copy.getTimestepsTotal(); i++) {
            action2Copy.poll();
        }
        Assert.assertFalse(action2Copy.hasNext());

        // If our command has greater than 0 timesteps, then initially hasNext should be true.
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
        // Must copy to get a pollable version of the command.
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
    public void equalsAndHash() {

        Assert.assertNotEquals(validAction1, String.valueOf(5));
        Assert.assertNotEquals(validAction1, validAction2);
        Assert.assertNotEquals(validAction1.hashCode(), validAction2.hashCode());

        Assert.assertEquals(validAction1, validAction1);
        Assert.assertEquals(validAction2, validAction2);
        Assert.assertEquals(validAction3, validAction3);
        Assert.assertEquals(validAction1.hashCode(), validAction1.hashCode());
        Assert.assertEquals(validAction2.hashCode(), validAction2.hashCode());
        Assert.assertEquals(validAction3.hashCode(), validAction3.hashCode());

        // Must copy to get a pollable version of the command.
        Action<CommandQWOP> action1Copy = validAction1.getCopy();
        Action<CommandQWOP> action2Copy = validAction2.getCopy();
        Action<CommandQWOP> action3Copy = validAction3.getCopy();

        Assert.assertEquals(validAction1, action1Copy);
        Assert.assertEquals(validAction1.hashCode(), action1Copy.hashCode());
        Assert.assertEquals(validAction2, action2Copy);
        Assert.assertEquals(validAction2.hashCode(), action2Copy.hashCode());
        Assert.assertEquals(validAction3, action3Copy);
        Assert.assertEquals(validAction3.hashCode(), action3Copy.hashCode());

        Action<CommandQWOP> equivAction = new Action<>(actTimesteps1, command1).getCopy();
        Assert.assertEquals(equivAction, action1Copy);
        Assert.assertEquals(equivAction.hashCode(), action1Copy.hashCode());


        for (int i = 0; i < 5; i++) {
            action1Copy.poll();
            equivAction.poll();
        }
        Assert.assertEquals(equivAction, action1Copy);
        Assert.assertEquals(equivAction.hashCode(), action1Copy.hashCode());
    }

    @Test
    public void comparing() {
        Action<CommandQWOP> a1 = new Action<>(5, CommandQWOP.QP),
                a2 = new Action<>(5, CommandQWOP.QO),
                a3 = new Action<>(5, CommandQWOP.QO),
                a4 = new Action<>(7, CommandQWOP.O),
                a5 = new Action<>(50, CommandQWOP.NONE),
                a6 = new Action<>(3, CommandQWOP.NONE),
                a7 = new Action<>(8, CommandQWOP.QO),
                a8 = new Action<>(11, CommandQWOP.QP),
                a9 = new Action<>(12, CommandQWOP.Q),
                a10 = new Action<>(30, CommandQWOP.WO),
                a11 = new Action<>(18, CommandQWOP.WO);
        List<Action<CommandQWOP>> alist = new ArrayList<>();
        alist.add(a1);
        alist.add(a2);
        alist.add(a3);
        alist.add(a4);
        alist.add(a5);
        alist.add(a6);
        alist.add(a7);
        alist.add(a8);
        alist.add(a9);
        alist.add(a10);
        alist.add(a11);

        Collections.sort(alist);
        Assert.assertEquals(a6, alist.get(0));
        Assert.assertEquals(a5, alist.get(1));
        Assert.assertEquals(a9, alist.get(2));
        Assert.assertEquals(a4, alist.get(3));
        Assert.assertEquals(a2, alist.get(4));
        Assert.assertEquals(a3, alist.get(5));
        Assert.assertEquals(a7, alist.get(6));
        Assert.assertEquals(a1, alist.get(7));
        Assert.assertEquals(a8, alist.get(8));
        Assert.assertEquals(a11, alist.get(9));
        Assert.assertEquals(a10, alist.get(10));
    }

    @Test
    public void getCopy() {
        Assert.assertEquals(validAction1, validAction1.getCopy());
        Assert.assertEquals(validAction2, validAction2.getCopy());
        Assert.assertEquals(validAction3, validAction3.getCopy());
    }

    @Test
    public void isMutable() {
        Action<CommandQWOP> act = new Action<>(14, CommandQWOP.QO);
        Assert.assertFalse(act.isMutable());

        Action act_copy = act.getCopy();
        Assert.assertTrue(act_copy.isMutable());
    }

    @Test
    public void consolidateActions() {
        // General list of game.command with weird ordering and some zero-duration game.command.
        List<Action<CommandQWOP>> actions = new ArrayList<>();
        actions.add(new Action<>(4, CommandQWOP.NONE));
        actions.add(new Action<>(1, CommandQWOP.NONE));
        actions.add(new Action<>(0, CommandQWOP.NONE));
        actions.add(new Action<>(3, CommandQWOP.Q));
        actions.add(new Action<>(0, CommandQWOP.NONE));
        actions.add(new Action<>(1, CommandQWOP.Q));
        actions.add(new Action<>(5, CommandQWOP.Q));
        actions.add(new Action<>(0, CommandQWOP.QP));

        List<Action<CommandQWOP>> consolidatedActions = Action.consolidateActions(actions);

        Assert.assertEquals(2, consolidatedActions.size());

        Assert.assertEquals(5, consolidatedActions.get(0).getTimestepsTotal());
        Assert.assertEquals(5, consolidatedActions.get(0).getTimestepsRemaining());
        Assert.assertEquals(consolidatedActions.get(0).peek(), CommandQWOP.NONE);

        Assert.assertEquals(9, consolidatedActions.get(1).getTimestepsTotal());
        Assert.assertEquals(9, consolidatedActions.get(1).getTimestepsRemaining());
        Assert.assertEquals(consolidatedActions.get(1).peek(), CommandQWOP.Q);

        // Make a list with a single, nonzero element.
        List<Action<CommandQWOP>> singleActionList = new ArrayList<>();
        singleActionList.add(new Action<>(10, CommandQWOP.QO));

        List<Action<CommandQWOP>> consolidatedSingleAction = Action.consolidateActions(singleActionList);
        Assert.assertEquals(10, consolidatedSingleAction.get(0).getTimestepsRemaining());
        Assert.assertEquals(consolidatedSingleAction.get(0).peek(), CommandQWOP.QO);
    }

    @Test
    public void consolidateActionsIllegalArgumentExceptionSingleElement() {
        exception.expect(IllegalArgumentException.class);

        // Make a list with a single, 0-duration element.
        List<Action<CommandQWOP>> singleActionList = new ArrayList<>();
        singleActionList.add(new Action<>(0, CommandQWOP.QP));
        List<Action<CommandQWOP>> consolidatedSingleAction = Action.consolidateActions(singleActionList);
    }

    @Test
    public void consolidateActionsIllegalArgumentExceptionMultiElement() {
        exception.expect(IllegalArgumentException.class);

        // Make a list with several, 0-duration element.
        List<Action<CommandQWOP>> actionList = new ArrayList<>();
        actionList.add(new Action<>(0, CommandQWOP.QO));
        actionList.add(new Action<>(0, CommandQWOP.WP));
        actionList.add(new Action<>(0, CommandQWOP.WO));
        actionList.add(new Action<>(0, CommandQWOP.NONE));

        List<Action<CommandQWOP>> consolidateActions = Action.consolidateActions(actionList);
    }

    @Test
    public void constructorThrowsIllegalArgumentException() {
        exception.expect(IllegalArgumentException.class);
        new Action<>(-1, CommandQWOP.NONE);
    }

    @Test
    public void keysToOneHot() {
        // I don't care which keys correspond to which one-hot element as long as they are unique.
        float[] sum = new float[9];
        for (CommandQWOP.Keys keys : CommandQWOP.Keys.values()) {
            float[] oneHot = CommandQWOP.keysToOneHot(keys).get();
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
}