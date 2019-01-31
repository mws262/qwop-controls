package actions;

import game.GameThreadSafe;
import game.State;
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

        Assert.assertEquals(5, a1.getTimestepsTotal());
        Assert.assertEquals(6, a2.getTimestepsTotal());
        Assert.assertEquals(7, a3.getTimestepsTotal());
        Assert.assertEquals(8, a4.getTimestepsTotal());


        Action[] acts = new Action[]{a1, a2, a3, a4};

        actQueue.addSequence(acts);

        // Make sure that as we poll the added sequence we get out what we put in.
        for (int i = 0; i < a1.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, new boolean[]{false, false, false, false}));
        }

        for (int i = 0; i < a2.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, new boolean[]{true, false, false, false}));
        }

        for (int i = 0; i < a3.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, new boolean[]{false, true, false, false}));
        }

        for (int i = 0; i < a4.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(command, new boolean[]{false, false, true, false}));
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

    @Test
    public void integrateWithGame() {
        // This is super-thorough because I'm an idiot who can't tell the difference between true false true false
        // and false false true false.

        // Try simulating a game using an ActionQueue
        GameThreadSafe game = new GameThreadSafe();

        game.step(false, false, true, false);
        float[] s1 = game.getCurrentState().flattenState();

        game.makeNewWorld();
        game.step(false, false, true, false);
        float[] s2 = game.getCurrentState().flattenState();

        for (int i = 0; i < s1.length; i++) {
            Assert.assertEquals(s1[i], s2[i], 1e-10);
        }

        ActionQueue actionQueue = makeTestQueue();

        game.makeNewWorld();
        float[] initialState1 = game.getCurrentState().flattenState();
        int counter = 0;
        while (!actionQueue.isEmpty()) {
            boolean[] commands = actionQueue.pollCommand();
            game.step(commands[0], commands[1], commands[2], commands[3]);
            counter++;
        }
        Assert.assertEquals(26, counter);

        State finalStateWithQueue = game.getCurrentState();

        // Try simulating the game by manually sending a bunch of commands.
        game.makeNewWorld();
        float[] initialState2 = game.getCurrentState().flattenState();
        counter = 0;
        for (int i = 0; i < 5; i++) {
            game.step(false, false, false, false);
            counter++;
        }

        for (int i = 0; i < 6; i++) {
            game.step(true, false, false, false);
            counter++;
        }

        for (int i = 0; i < 7; i++) {
            game.step(false, true, false, false);
            counter++;
        }

        for (int i = 0; i < 8; i++) {
            game.step(false, false, true, false);
            counter++;
        }
        Assert.assertEquals(26, counter);

        // Assert that we started at the same state.
        for (int i = 0; i < initialState1.length; i++) {
            Assert.assertEquals(initialState1[i], initialState2[i], 1e-10f);
        }

        State finalStateManualActions = game.getCurrentState();

        float[] autoStateVals = finalStateWithQueue.flattenState();
        float[] manualStateVals = finalStateManualActions.flattenState();

        for (int i = 0; i < autoStateVals.length; i++) {
            Assert.assertEquals(autoStateVals[i], manualStateVals[i], 1e-10);
        }
    }

    @Test
    public void getCopyOfUnexecutedQueue() {
        ActionQueue baseTestQueue = makeTestQueue();
        ActionQueue copyTestQueue = baseTestQueue.getCopyOfUnexecutedQueue();

        // Both copy and original are started at the same point.
        while (!baseTestQueue.isEmpty()) {
            boolean[] baseCommand = baseTestQueue.pollCommand();
            boolean[] testCommand = copyTestQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(baseCommand, testCommand));
        }
        Assert.assertTrue(copyTestQueue.isEmpty());
        baseTestQueue.addAction(new Action(34, false, false, false, false));
        Assert.assertTrue(copyTestQueue.isEmpty());

        // Give the original a head start and make sure that the copy starts at the beginning.
        baseTestQueue = makeTestQueue();
        for (int i = 0; i < 10; i++){
            baseTestQueue.pollCommand();
        }
        copyTestQueue = baseTestQueue.getCopyOfUnexecutedQueue();
        int counter = 0;
        while (!copyTestQueue.isEmpty()) { // Copy should still have all timesteps remaining.
            copyTestQueue.pollCommand();
            counter++;
        }
        Assert.assertEquals(26, counter);
    }

    @Test
    public void getCopyOfQueueAtExecutionPoint() {
        ActionQueue baseTestQueue = makeTestQueue();
        ActionQueue copyTestQueue = baseTestQueue.getCopyOfUnexecutedQueue();

        // Both copy and original are started at the beginning (trivial case).
        while (!baseTestQueue.isEmpty()) {
            boolean[] baseCommand = baseTestQueue.pollCommand();
            boolean[] testCommand = copyTestQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(baseCommand, testCommand));
        }
        Assert.assertTrue(copyTestQueue.isEmpty());
        baseTestQueue.addAction(new Action(34, false, false, false, false));
        Assert.assertTrue(copyTestQueue.isEmpty());

        // Original makes some progress before making a copy.
        baseTestQueue = makeTestQueue();
        for (int i = 0; i < 7; i++) {
            baseTestQueue.pollCommand();
        }

        copyTestQueue = baseTestQueue.getCopyOfQueueAtExecutionPoint();
        while (!baseTestQueue.isEmpty()) {
            boolean[] baseCommand = baseTestQueue.pollCommand();
            boolean[] testCommand = copyTestQueue.pollCommand();
            Assert.assertTrue(Arrays.equals(baseCommand, testCommand));
        }
        Assert.assertTrue(copyTestQueue.isEmpty());
        baseTestQueue.addAction(new Action(34, false, false, false, false));
        Assert.assertTrue(copyTestQueue.isEmpty());
    }

    @Test
    public void getTotalQueueLengthTimesteps() {
        ActionQueue actQueue = makeTestQueue();
        Assert.assertEquals(26, actQueue.getTotalQueueLengthTimesteps());

        for (int i = 0; i < 3; i++) {
            actQueue.pollCommand(); // Should not change even after actions have been polled.
        }
        Assert.assertEquals(26, actQueue.getTotalQueueLengthTimesteps());
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