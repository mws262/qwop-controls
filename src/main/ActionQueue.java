package main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * All things related to queueing actions should happen in here. Actions themselves act like queues,
 * so this action queue decides when to switch actions when one is depleted.
 *
 * @author Matt
 */
public class ActionQueue {

    /**
     * Actions are the delays between keypresses.
     **/
    private Queue<Action> actionQueue = new LinkedList<>();

    /**
     * All actions done or queued since the last reset. Unlike the queue, things aren't removed until reset.
     **/
    private ArrayList<Action> actionListFull = new ArrayList<>();

    /**
     * Integer action currently in progress. If the action is 20, this will be 20 even when 15 commands have been
     * issued.
     **/
    private Action currentAction;

    /**
     * Is there anything at all queued up to execute? Includes both the currentAction and the actionQueue.
     **/
    private AtomicBoolean isEmpty = new AtomicBoolean(true);

    public ActionQueue() {
    }

    /**
     * See the action we are currently executing. Does not change the queue.
     *
     * @return Action which is currently being executed (i.e. timings and keypresses).
     */
    public Action peekThisAction() {
        if (isEmpty()) throw new IndexOutOfBoundsException("No actions have been added to this queue (or remain).");
        return currentAction;
    }

    /**
     * See the next action we will execute. Does not change the queue.
     *
     * @return Next full action that will run (i.e. timings and keys). Returns null if no future actions remain.
     */
    public Action peekNextAction() {
        if (isEmpty()) throw new IndexOutOfBoundsException("No actions have been added to this queue. " +
                "Cannot peek.");
        return actionQueue.peek();
    }

    /**
     * See the next keypresses.
     *
     * @return Next QWOP keypresses as a boolean array. True is pressed, false is not pressed.
     */
    public boolean[] peekCommand() {
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
     * current action. All added actions are copied internally.
     *
     * @param action Action to add to the end of the queue as a copy. Does not influence current polling of the queue
     *               elements.
     */
    public synchronized void addAction(Action action) {
        if (action.getTimestepsTotal() == 0) return; // Zero-duration actions are tolerated, but not added to the queue.

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
     * Add a sequence of actions. All added actions are copied.
     *
     * @param actions Array of actions to add to the end of the queue. They are copied, and adding does not influence
     *                polling of the existing queue.
     */
    public synchronized void addSequence(Action[] actions) {
        if (actions.length == 0)
            throw new IllegalArgumentException("Tried to add an empty array of actions to a queue.");

        for (Action action : actions) {
            addAction(action); // Copy happens in addAction. No need to duplicate here.
        }
    }

    /**
     * Request the next QWOP keypress commands from the added sequence. Automatically advances between actions.
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
            assert currentAction.getTimestepsTotal() == currentAction.getTimestepsRemaining(); // If the newly loaded
            // action doesn't have all of its timesteps remaining, we have issues.
        }

        // Get next command from the current action.
        boolean[] nextCommand = currentAction.poll();

        // If this empties the queue, then mark this as empty.
        if (!currentAction.hasNext() && actionQueue.isEmpty()) {
            isEmpty.set(true);
        }
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
     * Get all the actions in this queue.
     *
     * @return All actions in this queue, including ones which have already been executed.
     */
    public Action[] getActionsInCurrentRun() {
        return actionListFull.toArray(new Action[0]);
    }

    /**
     * Index of the current action. 0 is the first action.
     *
     * @return Index of the current action.
     */
    public int getCurrentActionIdx() {
        int currIdx = actionListFull.size() - actionQueue.size() - 1;
        assert currIdx >= 0;
        return currIdx;
    }
}
