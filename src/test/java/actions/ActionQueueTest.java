package actions;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ActionQueueTest {

    @Test
    public void peekThisAction() {
        ActionQueue actQueue = new ActionQueue();
        Action a1 = new Action(5, false, false, false, false);
        Action a2 = new Action(6, true, false, false, false);
        Action a3 = new Action(7, false, false, false, false);
        Action a4 = new Action(8, false, false, false, false);

        Action[] acts = new Action[]{a1, a2, a3, a4};
        actQueue.addSequence(acts);

        Assert.assertEquals(actQueue.peekThisAction(), a1);

        for (int i = 0; i < a1.getTimestepsTotal(); i++) {
            actQueue.pollCommand();
            Assert.assertEquals(actQueue.peekThisAction(), a1);
        }

        for (int i = 0; i < a2.getTimestepsTotal(); i++) {
            actQueue.pollCommand();
            Assert.assertEquals(actQueue.peekThisAction(), a2);
        }

        for (int i = 0; i < a3.getTimestepsTotal(); i++) {
            actQueue.pollCommand();
            Assert.assertEquals(actQueue.peekThisAction(), a3);
        }

        for (int i = 0; i < a4.getTimestepsTotal(); i++) {
            actQueue.pollCommand();
            if (!actQueue.isEmpty()) {
                Assert.assertEquals(actQueue.peekThisAction(), a4);
            }
        }
    }

    @Test
    public void peekNextAction() {
        ActionQueue actQueue = new ActionQueue();
        Action a1 = new Action(5, false, false, false, false);
        Action a2 = new Action(6, true, false, false, false);
        Action a3 = new Action(7, false, false, false, false);
        Action a4 = new Action(8, false, false, false, false);

        Action[] acts = new Action[]{a1, a2, a3, a4};
        actQueue.addSequence(acts);

        Assert.assertEquals(actQueue.peekNextAction(), a2);

        for (int i = 0; i < a1.getTimestepsTotal(); i++) {
            actQueue.pollCommand();
            Assert.assertEquals(actQueue.peekNextAction(), a2);
        }

        for (int i = 0; i < a2.getTimestepsTotal(); i++) {
            actQueue.pollCommand();
            Assert.assertEquals(actQueue.peekNextAction(), a3);
        }

        for (int i = 0; i < a3.getTimestepsTotal(); i++) {
            actQueue.pollCommand();
            Assert.assertEquals(actQueue.peekNextAction(), a4);
        }

        actQueue.pollCommand();
        Assert.assertNull(actQueue.peekNextAction()); // Once on the last action we should get null for the next.

    }

    @Test
    public void peekCommand() {
        ActionQueue actQueue = makeTestQueue();

        for (int i = 0; i < 5; i++) {
            Assert.assertTrue(Arrays.equals(new boolean[]{false, false, false, false}, actQueue.peekCommand()));
            actQueue.pollCommand();
        }

        for (int i = 0; i < 6; i++) {
            Assert.assertTrue(Arrays.equals(new boolean[]{true, false, false, false}, actQueue.peekCommand()));
            actQueue.pollCommand();
        }

        for (int i = 0; i < 7; i++) {
            Assert.assertTrue(Arrays.equals(new boolean[]{false, true, false, false}, actQueue.peekCommand()));
            actQueue.pollCommand();
        }

        for (int i = 0; i < 8; i++) {
            Assert.assertTrue(Arrays.equals(new boolean[]{false, false, true, false}, actQueue.peekCommand()));
            actQueue.pollCommand();
        }

        Assert.assertNull(actQueue.peekCommand()); // Once on the last action we should get null for the next.
    }

    @Test
    public void addAction() {
        ActionQueue actQueue = new ActionQueue();
        Assert.assertTrue(actQueue.isEmpty());

        Action a1 = new Action(5, false, false, false, false);
        Action a2 = new Action(6, true, false, false, false);
        Action a3 = new Action(7, false, true, false, false);
        Action a4 = new Action(8, false, false, true, false);

        // Add first action and verify
        actQueue.addAction(a1);
        Assert.assertFalse(actQueue.isEmpty());

        Assert.assertEquals(1, actQueue.getActionsInCurrentRun().length);
        Assert.assertEquals(5, actQueue.peekThisAction().getTimestepsRemaining());

        Assert.assertTrue(Arrays.equals(actQueue.pollCommand(), a1.peek()));

        // Second action
        actQueue.addAction(a2);
        Assert.assertEquals(2, actQueue.getActionsInCurrentRun().length);
        Assert.assertEquals(4, actQueue.peekThisAction().getTimestepsRemaining());
        Assert.assertEquals(6, actQueue.getActionsInCurrentRun()[1].getTimestepsRemaining());

        Assert.assertTrue(Arrays.equals(actQueue.pollCommand(), a1.peek()));

        // Third action
        actQueue.addAction(a3);
        Assert.assertEquals(3, actQueue.getActionsInCurrentRun().length);
        Assert.assertEquals(3, actQueue.peekThisAction().getTimestepsRemaining());
        Assert.assertEquals(7, actQueue.getActionsInCurrentRun()[2].getTimestepsRemaining());

        // Poll away the rest in this action.
        Assert.assertTrue(Arrays.equals(actQueue.pollCommand(), a1.peek()));
        Assert.assertTrue(Arrays.equals(actQueue.pollCommand(), a1.peek()));
        Assert.assertTrue(Arrays.equals(actQueue.pollCommand(), a1.peek()));

        // Third action
        actQueue.addAction(a4);
        Assert.assertEquals(4, actQueue.getActionsInCurrentRun().length);
        Assert.assertEquals(0, actQueue.peekThisAction().getTimestepsRemaining());
        Assert.assertEquals(8, actQueue.getActionsInCurrentRun()[3].getTimestepsRemaining());

        Assert.assertTrue(Arrays.equals(actQueue.pollCommand(), a2.peek()));
    }

    @Test
    public void addSequence() {
        ActionQueue actQueue = new ActionQueue();
        Action a1 = new Action(5, false, false, false, false);
        Action a2 = new Action(6, true, false, false, false);
        Action a3 = new Action(7, false, true, false, false);
        Action a4 = new Action(8, false, false, true, false);

        Action[] acts = new Action[]{a1, a2, a3, a4};

        actQueue.addSequence(acts);
        Assert.assertEquals(4, actQueue.getActionsInCurrentRun().length);

        // Make sure that as we poll the added sequence we get out what we put in.
        for (int i = 0; i < a1.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, a1.peek()));
        }

        for (int i = 0; i < a2.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, a2.peek()));
        }

        for (int i = 0; i < a3.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, a3.peek()));
        }

        for (int i = 0; i < a4.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, a4.peek()));
        }
        Assert.assertTrue(actQueue.isEmpty());
    }

    @Test
    public void pollCommand() {
        ActionQueue actQueue = new ActionQueue();
        Action a1 = new Action(5, false, false, false, false);
        Action a2 = new Action(6, true, false, false, false);
        Action a3 = new Action(7, false, true, false, false);
        Action a4 = new Action(8, false, false, true, false);

        Action[] acts = new Action[]{a1, a2, a3, a4};

        actQueue.addSequence(acts);

        // Make sure that as we poll the added sequence we get out what we put in.
        for (int i = 0; i < a1.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, a1.peek()));
        }

        for (int i = 0; i < a2.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, a2.peek()));
        }

        for (int i = 0; i < a3.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, a3.peek()));
        }

        for (int i = 0; i < a4.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, a4.peek()));
        }
        Assert.assertTrue(actQueue.isEmpty());
    }

    @Test
    public void clearAll() {
        ActionQueue actQueue = makeTestQueue();
        Assert.assertFalse(actQueue.isEmpty());

        actQueue.clearAll();
        Assert.assertTrue(actQueue.isEmpty());
        Assert.assertEquals(0, actQueue.getActionsInCurrentRun().length);
    }

    @Test
    public void isEmpty() {
        ActionQueue actQueue = new ActionQueue();
        // Should be empty before adding anything.
        Assert.assertTrue(actQueue.isEmpty());
        Assert.assertEquals(0, actQueue.getActionsInCurrentRun().length);

        // Should not be empty after adding.
        Action a1 = new Action(5, false, false, false, false);
        actQueue.addAction(a1);
        Assert.assertFalse(actQueue.isEmpty());
        Assert.assertEquals(1, actQueue.getActionsInCurrentRun().length);

        // Drain the queue.
        for (int i = 0; i < 5; i++) {
            actQueue.pollCommand();
        }

        // Should be back to empty.
        Assert.assertTrue(actQueue.isEmpty());

        // Not empty again after adding another.
        actQueue.addAction(a1);
        Assert.assertFalse(actQueue.isEmpty());
    }

    @Test
    public void getActionsInCurrentRun() {
        ActionQueue actQueue = new ActionQueue();
        Action a1 = new Action(5, false, false, false, false);
        Action a2 = new Action(6, true, false, false, false);
        Action a3 = new Action(7, false, true, false, false);
        Action a4 = new Action(8, false, false, true, false);

        Action[] acts = new Action[]{a1, a2, a3, a4};
        actQueue.addSequence(acts);
        Action[] retrievedActions = actQueue.getActionsInCurrentRun();
        Assert.assertTrue(Arrays.equals(acts, retrievedActions));
    }

    @Test
    public void getCurrentActionIdx() {
        ActionQueue actQueue = makeTestQueue();
        Assert.assertEquals(0, actQueue.getCurrentActionIdx());

        for (int i = 0; i < 5; i++) {
            actQueue.pollCommand();
            Assert.assertEquals(0, actQueue.getCurrentActionIdx());
        }

        for (int i = 0; i < 6; i++) {
            actQueue.pollCommand();
            Assert.assertEquals(1, actQueue.getCurrentActionIdx());
        }

        for (int i = 0; i < 7; i++) {
            actQueue.pollCommand();
            Assert.assertEquals(2, actQueue.getCurrentActionIdx());
        }

        for (int i = 0; i < 8; i++) {
            actQueue.pollCommand();
            Assert.assertEquals(3, actQueue.getCurrentActionIdx());
        }
    }

    /**
     * Generic 4-action queue for testing here.
     *
     * @return Test action queue.
     */
    private ActionQueue makeTestQueue() {
        ActionQueue actQueue = new ActionQueue();
        Action a1 = new Action(5, false, false, false, false);
        Action a2 = new Action(6, true, false, false, false);
        Action a3 = new Action(7, false, true, false, false);
        Action a4 = new Action(8, false, false, true, false);

        Action[] acts = new Action[]{a1, a2, a3, a4};
        actQueue.addSequence(acts);
        return actQueue;
    }
}