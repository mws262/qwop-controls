package game.action;

import game.qwop.ActionQueuesQWOP;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.state.IState;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ActionQueueTest {

    @Test
    public void peekThisAction() {
        ActionQueue<CommandQWOP> actQueue = new ActionQueue<>();
        Action<CommandQWOP> a1 = new Action<>(5, CommandQWOP.NONE),
                a2 = new Action<>(6, CommandQWOP.Q),
                a3 = new Action<>(7, CommandQWOP.NONE),
                a4 = new Action<>(8, CommandQWOP.NONE);

        actQueue.addSequence(a1, a2, a3, a4);

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
        ActionQueue<CommandQWOP> actQueue = new ActionQueue<>();
        Action<CommandQWOP> a1 = new Action<>(5, CommandQWOP.NONE),
                a2 = new Action<>(6, CommandQWOP.Q),
                a3 = new Action<>(7, CommandQWOP.NONE),
                a4 = new Action<>(8, CommandQWOP.NONE);

        actQueue.addSequence(a1, a2, a3, a4);

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
        Assert.assertNull(actQueue.peekNextAction()); // Once on the last command we should get null for the next.

    }

    @Test
    public void peekCommand() {
        ActionQueue<CommandQWOP> actQueue = makeTestQueue();

        for (int i = 0; i < 5; i++) {
            Assert.assertArrayEquals(new boolean[]{false, false, false, false}, actQueue.peekCommand().get());
            actQueue.pollCommand();
        }

        for (int i = 0; i < 6; i++) {
            Assert.assertArrayEquals(new boolean[]{true, false, false, false}, actQueue.peekCommand().get());
            actQueue.pollCommand();
        }

        for (int i = 0; i < 7; i++) {
            Assert.assertArrayEquals(new boolean[]{false, true, false, false}, actQueue.peekCommand().get());
            actQueue.pollCommand();
        }

        for (int i = 0; i < 8; i++) {
            Assert.assertArrayEquals(new boolean[]{false, false, true, false}, actQueue.peekCommand().get());
            actQueue.pollCommand();
        }

        Assert.assertNull(actQueue.peekCommand()); // Once on the last command we should get null for the next.
    }

    @Test
    public void addAction() {
        ActionQueue<CommandQWOP> actQueue = new ActionQueue<>();
        Assert.assertTrue(actQueue.isEmpty());

        Action<CommandQWOP> a1 = new Action<>(5, CommandQWOP.NONE),
                a2 = new Action<>(6, CommandQWOP.Q),
                a3 = new Action<>(7, CommandQWOP.W),
                a4 = new Action<>(8, CommandQWOP.O);

        // Add first command and verify
        actQueue.addAction(a1);
        Assert.assertFalse(actQueue.isEmpty());

        Assert.assertEquals(1, actQueue.getActionsInCurrentRun().size());
        Assert.assertEquals(5, actQueue.peekThisAction().getTimestepsRemaining());

        Assert.assertEquals(actQueue.pollCommand(), a1.peek());

        // Second command
        actQueue.addAction(a2);
        Assert.assertEquals(2, actQueue.getActionsInCurrentRun().size());
        Assert.assertEquals(4, actQueue.peekThisAction().getTimestepsRemaining());
        Assert.assertEquals(6, actQueue.getActionsInCurrentRun().get(1).getTimestepsRemaining());

        Assert.assertEquals(actQueue.pollCommand(), a1.peek());

        // Third command
        actQueue.addAction(a3);
        Assert.assertEquals(3, actQueue.getActionsInCurrentRun().size());
        Assert.assertEquals(3, actQueue.peekThisAction().getTimestepsRemaining());
        Assert.assertEquals(7, actQueue.getActionsInCurrentRun().get(2).getTimestepsRemaining());

        // Poll away the rest in this command.
        Assert.assertEquals(actQueue.pollCommand(), a1.peek());
        Assert.assertEquals(actQueue.pollCommand(), a1.peek());
        Assert.assertEquals(actQueue.pollCommand(), a1.peek());

        // Third command
        actQueue.addAction(a4);
        Assert.assertEquals(4, actQueue.getActionsInCurrentRun().size());
        Assert.assertEquals(0, actQueue.peekThisAction().getTimestepsRemaining());
        Assert.assertEquals(8, actQueue.getActionsInCurrentRun().get(3).getTimestepsRemaining());

        Assert.assertEquals(actQueue.pollCommand(), a2.peek());
    }

    @Test
    public void addSequence() {
        ActionQueue<CommandQWOP> actQueue = new ActionQueue<>();
        Action<CommandQWOP> a1 = new Action<>(5, CommandQWOP.NONE),
                a2 = new Action<>(6, CommandQWOP.Q),
                a3 = new Action<>(7, CommandQWOP.W),
                a4 = new Action<>(8, CommandQWOP.O);

        actQueue.addSequence(a1, a2, a3, a4);
        Assert.assertEquals(4, actQueue.getActionsInCurrentRun().size());

        // Make sure that as we poll the added sequence we get out what we put in.
        for (int i = 0; i < a1.getTimestepsTotal(); i++) {
            CommandQWOP command = actQueue.pollCommand();
            Assert.assertEquals(command, a1.peek());
        }

        for (int i = 0; i < a2.getTimestepsTotal(); i++) {
            CommandQWOP command = actQueue.pollCommand();
            Assert.assertEquals(command, a2.peek());
        }

        for (int i = 0; i < a3.getTimestepsTotal(); i++) {
            CommandQWOP command = actQueue.pollCommand();
            Assert.assertEquals(command, a3.peek());
        }

        for (int i = 0; i < a4.getTimestepsTotal(); i++) {
            CommandQWOP command = actQueue.pollCommand();
            Assert.assertEquals(command, a4.peek());
        }
        Assert.assertTrue(actQueue.isEmpty());
    }

    @Test
    public void pollCommand() {
        ActionQueue<CommandQWOP> actQueue = new ActionQueue<>();
        Action<CommandQWOP> a1 = new Action<>(5, CommandQWOP.NONE),
                a2 = new Action<>(6, CommandQWOP.Q),
                a3 = new Action<>(7, CommandQWOP.W),
                a4 = new Action<>(8, CommandQWOP.O);

        Assert.assertEquals(5, a1.getTimestepsTotal());
        Assert.assertEquals(6, a2.getTimestepsTotal());
        Assert.assertEquals(7, a3.getTimestepsTotal());
        Assert.assertEquals(8, a4.getTimestepsTotal());

        actQueue.addSequence(a1, a2, a3, a4);

        // Make sure that as we poll the added sequence we get out what we put in.
        for (int i = 0; i < a1.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand().get();
            Assert.assertArrayEquals(command, new boolean[]{false, false, false, false});
        }

        for (int i = 0; i < a2.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand().get();
            Assert.assertArrayEquals(command, new boolean[]{true, false, false, false});
        }

        for (int i = 0; i < a3.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand().get();
            Assert.assertArrayEquals(command, new boolean[]{false, true, false, false});
        }

        for (int i = 0; i < a4.getTimestepsTotal(); i++) {
            boolean[] command = actQueue.pollCommand().get();
            Assert.assertArrayEquals(command, new boolean[]{false, false, true, false});
        }
        Assert.assertTrue(actQueue.isEmpty());
    }

    @Test
    public void clearAll() {
        ActionQueue actQueue = makeTestQueue();
        Assert.assertFalse(actQueue.isEmpty());

        actQueue.clearAll();
        Assert.assertTrue(actQueue.isEmpty());
        Assert.assertEquals(0, actQueue.getActionsInCurrentRun().size());
    }

    @Test
    public void isEmpty() {
        ActionQueue<CommandQWOP> actQueue = new ActionQueue<>();
        // Should be empty before adding anything.
        Assert.assertTrue(actQueue.isEmpty());
        Assert.assertEquals(0, actQueue.getActionsInCurrentRun().size());

        // Should not be empty after adding.
        Action<CommandQWOP> a1 = new Action<>(5, CommandQWOP.NONE);
        actQueue.addAction(a1);
        Assert.assertFalse(actQueue.isEmpty());
        Assert.assertEquals(1, actQueue.getActionsInCurrentRun().size());

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
        ActionQueue<CommandQWOP> actQueue = new ActionQueue<>();
        Action<CommandQWOP> a1 = new Action<>(5, CommandQWOP.NONE),
                a2 = new Action<>(6, CommandQWOP.Q),
                a3 = new Action<>(7, CommandQWOP.W),
                a4 = new Action<>(8, CommandQWOP.O);

        List<Action<CommandQWOP>> acts = new ArrayList<>();
        acts.add(a1);
        acts.add(a2);
        acts.add(a3);
        acts.add(a4);
        actQueue.addSequence(a1, a2, a3, a4);
        List<Action<CommandQWOP>> retrievedActions = actQueue.getActionsInCurrentRun();
        Assert.assertEquals(acts, retrievedActions);
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

        for (int i = 0; i < 7; i++) {
            actQueue.pollCommand();
            Assert.assertEquals(3, actQueue.getCurrentActionIdx());
        }
        actQueue.pollCommand();
        Assert.assertTrue(actQueue.isEmpty());
    }

    @Test
    public void splitQueueAtTimestep() {
        ActionQueue<CommandQWOP> actQueue = makeTestQueue();

        for (int i = 0; i < actQueue.getTotalQueueLengthTimesteps(); i++) {
            actQueue.resetQueue();
            List<ActionQueue<CommandQWOP>> splitQueues = actQueue.splitQueueAtTimestep(i);

            Assert.assertEquals(2, splitQueues.size()); // Should only be split into 2 pieces.
            ActionQueue<CommandQWOP> q1 = splitQueues.get(0);
            ActionQueue<CommandQWOP> q2 = splitQueues.get(1);

            // Make sure they come out to the same number of timesteps before and after the split.
            Assert.assertEquals(actQueue.getTotalQueueLengthTimesteps(), q1.getTotalQueueLengthTimesteps() + q2.getTotalQueueLengthTimesteps());
            Assert.assertEquals(i, q1.getTotalQueueLengthTimesteps());

            // Poll timesteps one at a time to make sure the returned commands match.
            while (!actQueue.isEmpty()) {
                CommandQWOP realCommand = actQueue.pollCommand();
                CommandQWOP splitCommand;
                if (!q1.isEmpty()) {
                    splitCommand = q1.pollCommand();
                } else {
                    splitCommand = q2.pollCommand();
                }
                Assert.assertEquals(realCommand, splitCommand);
            }
            // All queues should be empty at the same time as the original.
            Assert.assertTrue(q1.isEmpty());
            Assert.assertTrue(q2.isEmpty());
        }
    }

    @Test
    public void concatenateQueues() {
        ActionQueue<CommandQWOP> a1 = ActionQueuesQWOP.makeShortQueue();
        ActionQueue<CommandQWOP> a2 = makeTestQueue();
        ActionQueue<CommandQWOP> a3 = new ActionQueue<>();
        a3.addAction(new Action<>(20, CommandQWOP.QP));

        //noinspection unchecked
        ActionQueue<CommandQWOP> r1 = ActionQueue.concatenateQueues(a1, a2, a3);

        Assert.assertEquals(a1.getTotalQueueLengthTimesteps()
                + a2.getTotalQueueLengthTimesteps()
                + a3.getTotalQueueLengthTimesteps(),
                r1.getTotalQueueLengthTimesteps());

        while (!r1.isEmpty()) {
            CommandQWOP c;
            if (a2.isEmpty()) {
                c = a3.pollCommand();
            } else if (a1.isEmpty()) {
                c = a2.pollCommand();
            } else {
                c = a1.pollCommand();
            }

            Assert.assertEquals(c, r1.pollCommand());
        }

        Assert.assertTrue(a3.isEmpty());

        a1.resetQueue();
        ActionQueue<CommandQWOP> r2 = ActionQueue.concatenateQueues(a1);
        Assert.assertEquals(a1.getTotalQueueLengthTimesteps(), r2.getTotalQueueLengthTimesteps());

        while (!r2.isEmpty()) {
            Assert.assertEquals(a1.pollCommand(), r2.pollCommand());
        }

        Assert.assertTrue(a1.isEmpty());

    }

    @Test
    public void integrateWithGame() {
        // This is super-thorough because I'm an idiot who can't tell the difference between true false true false
        // and false false true false.

        // Try simulating a game using an ActionQueue
        GameQWOP game = new GameQWOP();

        game.step(false, false, true, false);
        float[] s1 = game.getCurrentState().flattenState();

        game.resetGame();
        game.step(false, false, true, false);
        float[] s2 = game.getCurrentState().flattenState();

        for (int i = 0; i < s1.length; i++) {
            Assert.assertEquals(s1[i], s2[i], 1e-10);
        }

        ActionQueue<CommandQWOP> actionQueue = makeTestQueue();

        game.resetGame();
        float[] initialState1 = game.getCurrentState().flattenState();
        int counter = 0;
        while (!actionQueue.isEmpty()) {
            CommandQWOP command = actionQueue.pollCommand();
            game.step(command);
            counter++;
        }
        Assert.assertEquals(26, counter);

        IState finalStateWithQueue = game.getCurrentState();

        // Try simulating the game by manually sending a bunch of commands.
        game.resetGame();
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

        IState finalStateManualActions = game.getCurrentState();

        float[] autoStateVals = finalStateWithQueue.flattenState();
        float[] manualStateVals = finalStateManualActions.flattenState();

        for (int i = 0; i < autoStateVals.length; i++) {
            Assert.assertEquals(autoStateVals[i], manualStateVals[i], 1e-10);
        }
    }

    @Test
    public void getCopyOfUnexecutedQueue() {
        ActionQueue<CommandQWOP> baseTestQueue = makeTestQueue();
        ActionQueue<CommandQWOP> copyTestQueue = baseTestQueue.getCopyOfUnexecutedQueue();

        // Both copy and original are started at the same point.
        while (!baseTestQueue.isEmpty()) {
            CommandQWOP baseCommand = baseTestQueue.pollCommand();
            CommandQWOP testCommand = copyTestQueue.pollCommand();
            Assert.assertEquals(baseCommand, testCommand);
        }
        Assert.assertTrue(copyTestQueue.isEmpty());
        baseTestQueue.addAction(new Action<>(34, CommandQWOP.NONE));
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
        ActionQueue<CommandQWOP> baseTestQueue = makeTestQueue();
        ActionQueue<CommandQWOP> copyTestQueue = baseTestQueue.getCopyOfUnexecutedQueue();

        // Both copy and original are started at the beginning (trivial case).
        while (!baseTestQueue.isEmpty()) {
            CommandQWOP baseCommand = baseTestQueue.pollCommand();
            CommandQWOP testCommand = copyTestQueue.pollCommand();
            Assert.assertEquals(baseCommand, testCommand);
        }
        Assert.assertTrue(copyTestQueue.isEmpty());
        baseTestQueue.addAction(new Action<>(34, CommandQWOP.NONE));
        Assert.assertTrue(copyTestQueue.isEmpty());

        // Original makes some progress before making a copy.
        baseTestQueue = makeTestQueue();
        for (int i = 0; i < 7; i++) {
            baseTestQueue.pollCommand();
        }

        copyTestQueue = baseTestQueue.getCopyOfQueueAtExecutionPoint();
        while (!baseTestQueue.isEmpty()) {
            CommandQWOP baseCommand = baseTestQueue.pollCommand();
            CommandQWOP testCommand = copyTestQueue.pollCommand();
            Assert.assertEquals(baseCommand, testCommand);
        }
        Assert.assertTrue(copyTestQueue.isEmpty());
        baseTestQueue.addAction(new Action<>(34, CommandQWOP.NONE));
        Assert.assertTrue(copyTestQueue.isEmpty());
    }

    @Test
    public void getTotalQueueLengthTimesteps() {
        ActionQueue actQueue = makeTestQueue();
        Assert.assertEquals(26, actQueue.getTotalQueueLengthTimesteps());

        for (int i = 0; i < 3; i++) {
            actQueue.pollCommand(); // Should not change even after game.command have been polled.
        }
        Assert.assertEquals(26, actQueue.getTotalQueueLengthTimesteps());
    }

    /**
     * Generic 4-command queue for testing here.
     *
     * @return Test command queue.
     */
    private ActionQueue<CommandQWOP> makeTestQueue() {
        ActionQueue<CommandQWOP> actQueue = new ActionQueue<>();
        Action<CommandQWOP> a1 = new Action<>(5, CommandQWOP.NONE),
                a2 = new Action<>(6, CommandQWOP.Q),
                a3 = new Action<>(7, CommandQWOP.W),
                a4 = new Action<>(8, CommandQWOP.O);

        actQueue.addSequence(a1, a2, a3, a4);
        return actQueue;
    }
}