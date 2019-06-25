package game.action;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * All things related to storing and going through sequences of game.action. {@link ActionQueue} itself acts like a
 * {@link Queue} of {@link Action game.action}, while game.action act like queues of keypresses (commands). When
 * calling {@link ActionQueue#pollCommand()}, this will return the next set of keypresses from the current action,
 * while automatically advancing through game.action when one's duration is complete.
 *
 * @author Matt
 *
 * @see Queue
 * @see Action
 * @see ActionList
 */
public class ActionQueue {

    /**
     * Actions are the delays between keypresses.
     */
    private Queue<Action> actionQueue = new LinkedList<>();

    /**
     * All game.action done or queued since the last reset. Unlike the queue, things aren't removed until reset.
     */
    private ArrayList<Action> actionListFull = new ArrayList<>();

    /**
     * Integer action currently in progress. If the action is 20, this will be 20 even when 15 commands have been
     * issued.
     */
    private Action currentAction;

    /**
     * Is there anything at all queued up to execute? Includes both the currentAction and the actionQueue.
     */
    private AtomicBoolean isEmpty = new AtomicBoolean(true);

    /**
     * Number of commands polled from the ActionQueue during its life.
     */
    private int commandsPolled = 0;

    public ActionQueue() {}

    /**
     * See the action we are currently executing. Does not change the queue.
     *
     * @return Action which is currently being executed (i.e. timings and keypresses).
     */
    public Action peekThisAction() {
        return currentAction;
    }

    /**
     * See the next action we will execute. Does not change the queue.
     *
     * @return Next full action that will run (i.e. timings and keys). Returns null if no future game.action remain.
     */
    public synchronized Action peekNextAction() {
        if (isEmpty()) throw new IndexOutOfBoundsException("No game.action have been added to this queue. " +
                "Cannot peek.");
        return actionQueue.peek();
    }

    /**
     * See the next keypresses.
     *
     * @return Next QWOP keypresses as a boolean array. True is pressed, false is not pressed.
     */
    public synchronized boolean[] peekCommand() {
        if (currentAction == null) throw new IndexOutOfBoundsException("No current action in the queue for us to peek" +
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
     * Adds a new action to the end of the queue. If this is the first action to be added, it is loaded up as the
     * current action. All added game.action are copied internally.
     *
     * @param action Action to add to the end of the queue as a copy. Does not influence current polling of the queue
     *               elements.
     */
    public synchronized void addAction(Action action) {
        if (action.getTimestepsTotal() == 0) return; // Zero-duration game.action are tolerated, but not added to the queue.

        Action localCopy = action.getCopy();
        actionQueue.add(localCopy);
        actionListFull.add(localCopy);

        // If it's the first action, load it up.
        if (currentAction == null) {
            currentAction = actionQueue.poll();
        }

        isEmpty.set(false);
    }

    /**
     * Add a sequence of game.action. All added game.action are copied.
     *
     * @param actions Array of game.action to add to the end of the queue. They are copied, and adding does not influence
     *                polling of the existing queue.
     */
    public synchronized void addSequence(Action[] actions) {
        if (actions.length == 0)
            throw new IllegalArgumentException("Tried to add an empty array of game.action to a queue.");

        for (Action action : actions) {
            addAction(action); // Copy happens in addAction. No need to duplicate here.
        }
    }

    /**
     * Add a sequence of game.action. All added game.action are copied.
     *
     * @param actions List of game.action to add to the end of the queue. They are copied, and adding does not influence
     *                polling of the existing queue.
     */
    public synchronized void addSequence(List<Action> actions) {
        if (actions.size() == 0)
            throw new IllegalArgumentException("Tried to add an empty array of game.action to a queue.");

        for (Action action : actions) {
            addAction(action); // Copy happens in addAction. No need to duplicate here.
        }
    }

    /**
     * Request the next QWOP keypress commands from the added sequence. Automatically advances between game.action.
     *
     * @return Get the next command (QWOP true/false array) on the queue.
     */
    public synchronized boolean[] pollCommand() {
        if (actionQueue.isEmpty() && !currentAction.hasNext()) {
            throw new IndexOutOfBoundsException("Tried to get a command off the queue when nothing is queued up.");
        }

        // If the current action has no more keypresses, load up the next one.
        if (!currentAction.hasNext()) {
            currentAction.reset(); // Reset the previous current action so it can be polled again in the future.
            currentAction = actionQueue.poll();
            Objects.requireNonNull(currentAction);
            assert currentAction.getTimestepsTotal() == currentAction.getTimestepsRemaining(); // If the newly loaded
            // action doesn't have all of its timesteps remaining, we have issues.
        }

        // Get next command from the current action.
        boolean[] nextCommand = currentAction.poll();

        // If this empties the queue, then mark this as empty.
        if (!currentAction.hasNext() && actionQueue.isEmpty()) {
            isEmpty.set(true);
        }
        commandsPolled ++;
        return nextCommand;
    }

    /**
     * Remove everything from the queues and reset the sequence.
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
     * Get all the game.action in this queue.
     *
     * @return All game.action in this queue, including ones which have already been executed.
     */
    public Action[] getActionsInCurrentRun() {
        return actionListFull.toArray(new Action[0]);
    }

    /**
     * Index of the current action. 0 is the first {@link Action}.
     *
     * @return Index of the current action.
     */
    public int getCurrentActionIdx() {
        if (isEmpty())
            throw new IndexOutOfBoundsException("Cannot ask for the current action index for an empty action queue.");
        int currIdx = actionListFull.size() - actionQueue.size() - 1;
        assert currIdx >= 0;
        return currIdx;
    }

    /**
     * Resets all progress on the queue making it ready to execute the same game.action again. Note that to actually
     * remove game.action, you should use {@link ActionQueue#clearAll()}.
     */
    public void resetQueue() {
        Action[] actions = getActionsInCurrentRun();
        clearAll();
        this.addSequence(actions);
    }

    /**
     * Get a copy of this ActionQueue, with none of the game.action performed yet.
     *
     * @return An ActionQueue with all the same game.action, but no progress in them done yet.
     */
    public ActionQueue getCopyOfUnexecutedQueue() {
        ActionQueue actionQueueCopy = new ActionQueue();
        actionQueueCopy.addSequence(getActionsInCurrentRun());
        return actionQueueCopy;
    }

    /**
     * Get a copy of this ActionQueue, with the same game.action, and the same progress made on those game.action.
     *
     * @return An ActionQueue which should behave identically to the original.
     */
    public ActionQueue getCopyOfQueueAtExecutionPoint() {
        ActionQueue actionQueueCopy = getCopyOfUnexecutedQueue();
        for (int i = 0; i < commandsPolled; i++) {
            actionQueueCopy.pollCommand();
        }
        return actionQueueCopy;
    }

    /**
     * Gives the total duration of this action queue in timesteps. This does not depend on the number of timesteps
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
     * Get some sample game.action for use in tests. This is a successful short run found by tree search.
     * @return A successful queue of game.action.
     */
    public static ActionQueue getSampleActions() {
        // Ran MAIN_Search_LongRun to get these.
        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addAction(new Action(27, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(12, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(44, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(16, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(18, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(23, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(12, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(21, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(12, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(17, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(13, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(13, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(10, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(16, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(8, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(24, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(10, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(23, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(10, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(24, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(34, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(5, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(18, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(12, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(3, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(8, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(11, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(9, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(3, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(9, new boolean[]{true, false, false, true}));

        return actionQueue;
    }
}
