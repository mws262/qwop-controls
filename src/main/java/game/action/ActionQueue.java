package game.action;

import game.qwop.CommandQWOP;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * All things related to storing and going through sequences of game.command. {@link ActionQueue} itself acts like a
 * {@link Queue} of {@link Action game.command}, while game.command act like queues of keypresses (commands). When
 * calling {@link ActionQueue#pollCommand()}, this will return the next set of keypresses from the current command,
 * while automatically advancing through game.command when one's duration is complete.
 *
 * @author Matt
 *
 * @see Queue
 * @see Action
 * @see ActionList
 */
public class ActionQueue<C extends Command<?>> {

    /**
     * Actions are the delays between keypresses.
     */
    @NotNull
    private final Queue<Action<C>> actionQueue = new LinkedList<>();

    /**
     * All game.command done or queued since the last resetGame. Unlike the queue, things aren't removed until resetGame.
     */
    @NotNull
    private final ArrayList<Action<C>> actionListFull = new ArrayList<>();

    /**
     * Integer command currently in progress. If the command is 20, this will be 20 even when 15 commands have been
     * issued.
     */
    private Action<C> currentAction;

    /**
     * Is there anything at all queued up to execute? Includes both the currentAction and the actionQueue.
     */
    @NotNull
    private final AtomicBoolean isEmpty = new AtomicBoolean(true);

    /**
     * Number of commands polled from the ActionQueue during its life.
     */
    private int commandsPolled = 0;

    public ActionQueue() {}

    /**
     * See the command we are currently executing. Does not change the queue.
     *
     * @return Action which is currently being executed (i.e. timings and keypresses).
     */
    public Action<C> peekThisAction() {
        return currentAction;
    }

    /**
     * See the next command we will execute. Does not change the queue.
     *
     * @return Next full command that will run (i.e. timings and keys). Returns null if no future game.command remain.
     */
    public synchronized Action<C> peekNextAction() {
        if (isEmpty()) throw new IndexOutOfBoundsException("No game.command have been added to this queue. " +
                "Cannot peek.");
        return actionQueue.peek();
    }

    /**
     * See the next keypresses.
     *
     * @return Next QWOP keypresses as a boolean array. True is pressed, false is not pressed.
     */
    public synchronized C peekCommand() {
        if (currentAction == null) throw new IndexOutOfBoundsException("No current command in the queue for us to peek" +
                " at.");

        if (currentAction.getTimestepsRemaining() == 0) {
            if (actionQueue.isEmpty()) {
                return null;
            } else {
                return actionQueue.peek().peek();
            }
        } else {
            return currentAction.peek();
        }
    }

    /**
     * Adds a new command to the end of the queue. If this is the first command to be added, it is loaded up as the
     * current command. All added game.command are copied internally.
     *
     * @param action Action to add to the end of the queue as a copy. Does not influence current polling of the queue
     *               elements.
     */
    public synchronized void addAction(@NotNull Action<C> action) {
        if (action.getTimestepsTotal() == 0) return; // Zero-duration game.command are tolerated, but not added to the queue.

        Action<C> localCopy = action.getCopy();
        actionQueue.add(localCopy);
        actionListFull.add(localCopy);

        // If it's the first command, load it up.
        if (currentAction == null) {
            currentAction = actionQueue.poll();
        }

        isEmpty.set(false);
    }

//    /**
//     * Add a sequence of game.command. All added game.command are copied.
//     *
//     * @param actions Array of game.command to add to the end of the queue. They are copied, and adding does not influence
//     *                polling of the existing queue.
//     */
//    public synchronized void addSequence(@NotNull Action<C>[] actions) {
//        if (actions.length == 0)
//            throw new IllegalArgumentException("Tried to add an empty array of game.command to a queue.");
//
//        for (Action<C> action : actions) {
//            addAction(action); // Copy happens in addAction. No need to duplicate here.
//        }
//    }

    /**
     * Add a sequence of game.command. All added game.command are copied.
     *
     * @param actions List of game.command to add to the end of the queue. They are copied, and adding does not influence
     *                polling of the existing queue.
     */
    public synchronized void addSequence(@NotNull List<Action<C>> actions) {
        if (actions.size() == 0)
            throw new IllegalArgumentException("Tried to add an empty array of game.command to a queue.");

        for (Action<C> action : actions) {
            addAction(action); // Copy happens in addAction. No need to duplicate here.
        }
    }

    @SafeVarargs
    public final synchronized void addSequence(Action<C>... actionsInOrder) {
        if (actionsInOrder.length == 0)
            throw new IllegalArgumentException("Tried to add an empty array of game.command to a queue.");

        for (Action<C> action : actionsInOrder) {
            addAction(action); // Copy happens in addAction. No need to duplicate here.
        }
    }

    /**
     * Request the next QWOP keypress commands from the added sequence. Automatically advances between game.command.
     *
     * @return Get the next command (QWOP true/false array) on the queue.
     */
    public synchronized C pollCommand() {
        if (actionQueue.isEmpty() && !currentAction.hasNext()) {
            throw new IndexOutOfBoundsException("Tried to get a command off the queue when nothing is queued up.");
        }

        // If the current command has no more keypresses, load up the next one.
        if (!currentAction.hasNext()) {
            currentAction.reset(); // Reset the previous current command so it can be polled again in the future.
            currentAction = actionQueue.poll();
            Objects.requireNonNull(currentAction);
            assert currentAction.getTimestepsTotal() == currentAction.getTimestepsRemaining(); // If the newly loaded
            // command doesn't have all of its timesteps remaining, we have issues.
        }

        // Get next command from the current command.
        C nextCommand = currentAction.poll();

        // If this empties the queue, then mark this as empty.
        if (!currentAction.hasNext() && actionQueue.isEmpty()) {
            isEmpty.set(true);
        }
<<<<<<< HEAD
        commandsPolled ++;
=======
        commandsPolled++;
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
        return nextCommand;
    }

    /**
     * Remove everything from the queues and resetGame the sequence.
     */
    public synchronized void clearAll() {
        actionQueue.clear();
        actionListFull.clear();
        if (currentAction != null) currentAction.reset();
        currentAction = null;

        isEmpty.set(true);
    }

    /**
     * Check if the queue has anything in it.
     *
     * @return Whether this queue has more commands left to poll.
     */
    public synchronized boolean isEmpty() {
        return isEmpty.get();
    }

    /**
     * Get all the game.command in this queue.
     *
     * @return All game.command in this queue, including ones which have already been executed.
     */
    public List<Action<C>> getActionsInCurrentRun() {
        return new ArrayList<>(actionListFull);
    }

    /**
     * Index of the current command. 0 is the first {@link Action}.
     *
     * @return Index of the current command.
     */
    public int getCurrentActionIdx() {
        if (isEmpty())
            throw new IndexOutOfBoundsException("Cannot ask for the current command index for an empty command queue.");
        int currIdx = actionListFull.size() - actionQueue.size() - 1;
        assert currIdx >= 0;
        return currIdx;
    }

    /**
     * Resets all progress on the queue making it ready to execute the same game.command again. Note that to actually
     * remove game.command, you should use {@link ActionQueue#clearAll()}.
     */
    public void resetQueue() {
        List<Action<C>> actions = getActionsInCurrentRun();
        clearAll();
        this.addSequence(actions);
    }

    /**
<<<<<<< HEAD
=======
     * Split the queue (without altering the original) into two queues, one before, and one after the specified
     * timestep.
     * @param timestep
     * @return
     */
    public List<ActionQueue<C>> splitQueueAtTimestep(int timestep) {

        if (timestep > getTotalQueueLengthTimesteps() - 1) {
            throw new IllegalArgumentException("Queue is too short to be split in chunks at the specified timestep.");
        }

        // Make a copy, and get to the location of the split in this copy.
        ActionQueue<C> queueCopy = getCopyOfUnexecutedQueue();
        int count = 0;
        while (count++ < timestep) {
            queueCopy.pollCommand();
        }

        int currentActionIdx = queueCopy.getCurrentActionIdx();

        // Get another copy so none of the actions have been polled at all.
        ActionQueue<C> unusedQueue = getCopyOfUnexecutedQueue();
        List<Action<C>> actions = unusedQueue.getActionsInCurrentRun();


        Action<C> dividedAction = queueCopy.getActionsInCurrentRun().get(currentActionIdx);

        // I don't think this can happen. When the next action becomes the current one, it is immediately polled.
        assert(dividedAction.getTimestepsRemaining() != dividedAction.getTimestepsTotal());

        List<Action<C>> actionsBefore;
        List<Action<C>> actionsAfter;
        if (dividedAction.getTimestepsRemaining() == 0) {
            // Note -- subList is inclusive low bound and exclusive high bound. Also, sublists are backed by original
            // list, meaning that it behaves more like an array of values than references.
            actionsBefore = new ArrayList<>(actions.subList(0, currentActionIdx + 1));
            actionsAfter = new ArrayList<>(actions.subList(currentActionIdx + 1, actions.size()));
        } else {
            actionsBefore = new ArrayList<>(actions.subList(0, currentActionIdx + 1));
            actionsAfter = new ArrayList<>(actions.subList(currentActionIdx, actions.size()));
            Action<C> borderAction = queueCopy.peekThisAction();
            Action<C> firstHalfAction = new Action<>(borderAction.getTimestepsTotal() - borderAction.getTimestepsRemaining(),
                    borderAction.getCommand());
            Action<C> secondHalfAction =
                    new Action<>(borderAction.getTimestepsRemaining(),
                    borderAction.getCommand());

            actionsBefore.set(actionsBefore.size() - 1, firstHalfAction);
            actionsAfter.set(0, secondHalfAction);
        }

        ActionQueue<C> queue1 = new ActionQueue<>();
        ActionQueue<C> queue2 = new ActionQueue<>();
        queue1.addSequence(actionsBefore);
        queue2.addSequence(actionsAfter);
        List<ActionQueue<C>> dividedQueues = new ArrayList<>();
        dividedQueues.add(queue1);
        dividedQueues.add(queue2);
        return dividedQueues;
    }

    /**
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
     * Get a copy of this ActionQueue, with none of the game.command performed yet.
     *
     * @return An ActionQueue with all the same game.command, but no progress in them done yet.
     */
    public ActionQueue<C> getCopyOfUnexecutedQueue() {
        ActionQueue<C> actionQueueCopy = new ActionQueue<>();
        actionQueueCopy.addSequence(getActionsInCurrentRun());
        return actionQueueCopy;
    }

    /**
     * Get a copy of this ActionQueue, with the same game.command, and the same progress made on those game.command.
     *
     * @return An ActionQueue which should behave identically to the original.
     */
    public ActionQueue<C> getCopyOfQueueAtExecutionPoint() {
        ActionQueue<C> actionQueueCopy = getCopyOfUnexecutedQueue();
        for (int i = 0; i < commandsPolled; i++) {
            actionQueueCopy.pollCommand();
        }
        return actionQueueCopy;
    }

    /**
     * Gives the total duration of this command queue in timesteps. This does not depend on the number of timesteps
     * already executed on this queue.
     *
     * @return Total duration of this queue in timesteps.
     */
    public int getTotalQueueLengthTimesteps() {
        int totalTS = 0;
        for (Action a : actionListFull) {
            totalTS += a.getTimestepsTotal();
        }
        return totalTS;
    }

    /**
     * Get some sample game.command for use in tests. This is a successful short run found by tree search.
     * @return A successful queue of game.command.
     */
    public static ActionQueue<CommandQWOP> getSampleActions() {
        // Ran MAIN_Search_LongRun to get these.
        ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();
        actionQueue.addAction(new Action<>(27, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(12, CommandQWOP.WO));
        actionQueue.addAction(new Action<>(10, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(44, CommandQWOP.QP));

        actionQueue.addAction(new Action<>(16, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(18, CommandQWOP.WO));
        actionQueue.addAction(new Action<>(23, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(12, CommandQWOP.QP));

        actionQueue.addAction(new Action<>(21, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(12, CommandQWOP.WO));
        actionQueue.addAction(new Action<>(17, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(13, CommandQWOP.QP));

        actionQueue.addAction(new Action<>(13, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(10, CommandQWOP.WO));
        actionQueue.addAction(new Action<>(16, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(8, CommandQWOP.QP));

        actionQueue.addAction(new Action<>(24, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(10, CommandQWOP.WO));
        actionQueue.addAction(new Action<>(23, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(10, CommandQWOP.QP));

        actionQueue.addAction(new Action<>(24, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(34, CommandQWOP.WO));
        actionQueue.addAction(new Action<>(5, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(18, CommandQWOP.QP));

        actionQueue.addAction(new Action<>(20, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(12, CommandQWOP.WO));
        actionQueue.addAction(new Action<>(3, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(8, CommandQWOP.QP));

        actionQueue.addAction(new Action<>(11, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(9, CommandQWOP.WO));
        actionQueue.addAction(new Action<>(3, CommandQWOP.NONE));
        actionQueue.addAction(new Action<>(9, CommandQWOP.QP));

        return actionQueue;
    }
}
