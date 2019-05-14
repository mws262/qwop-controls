package tree;

import actions.Action;
import actions.ActionQueue;
import data.SavableSingleGame;
import game.IGame;
import game.State;
import value.updaters.IValueUpdater;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Most basic QWOP node. Just does states and actions.
 * See {@link NodeGenericBase} for more information about the bizarre generics.
 *
 * @param <N>
 */
public abstract class NodeQWOPBase<N extends NodeQWOPBase<N>> extends NodeGenericBase<N> {

    private final Action action;

    private final State state;

    public NodeQWOPBase(State rootState) {
        super();
        action = null;
        state = rootState;
    }

    NodeQWOPBase(N parent, Action action, State state) {
        super(parent);
        this.action = action;
        this.state = state;
        // Check to make sure this node isn't already there in the parent's nodes.
        for (N parentChildren : parent.getChildren()) {
            if (parentChildren.getAction() == action) {
                throw new IllegalArgumentException("Tried to add a duplicate action node at depth " + getTreeDepth() + ". Action " +
                        "was: " + action.toString() + ".");
            }
        }
    }

    /**
     * Make a new child. Both parent and child have references to each other.
     * @param action
     * @param state
     * @return
     */
    public abstract N addDoublyLinkedChild(Action action, State state);

    /**
     * Make a new child. The child has a reference to the parent, but the parent does not know about the child. This
     * is useful for doing action rollouts that we don't want to be a permanent part of the tree.
     * @param action
     * @param state
     * @return
     */
    public abstract N addBackwardsLinkedChild(Action action, State state);

    public State getState() { return state; }

    public Action getAction() { return action; }

    public synchronized List<Action> getSequence(List<Action> actionList) {
        if (getTreeDepth() == 0)
            throw new IndexOutOfBoundsException("Cannot get a sequence at the root node, since it has no actions " +
                    "leading up to it.");
        actionList.clear();
        N current = getThis();
        actionList.add(getTreeDepth() - 1, current.getAction());
        for (int i = getTreeDepth() - 2; i >= 0; i--) {
            current = current.getParent();
            actionList.add(i, current.getAction());
        }
        return actionList;
    }

    /**
     * Add nodes based on saved action sequences. Has to re-simulate each to get the states.
     */
    public static <N extends NodeQWOPBase<N>> void makeNodesFromActionSequences(List<Action[]> actions, N root, IGame game) {

        ActionQueue actQueue = new ActionQueue();
        for (Action[] acts : actions) {
            game.makeNewWorld();
            N currNode = root;
            actQueue.clearAll();

            for (Action act : acts) {
                act = act.getCopy();
                act.reset();

                // Simulate
                actQueue.addAction(act);
                while (!actQueue.isEmpty()) {
                    game.step(actQueue.pollCommand());
                }

                // If there is already a node for this action, use it.
                boolean foundExisting = false;
                for (N child : currNode.getChildren()) {
                    if (child.getAction().equals(act)) {
                        currNode = child;
                        foundExisting = true;
                        break;
                    }
                }

                // Otherwise, make a new one.
                if (!foundExisting) currNode = currNode.addDoublyLinkedChild(act, game.getCurrentState());
            }
        }
    }

    /* Takes a list of runs and figures out the tree hierarchy without duplicate objects. Adds to an existing givenroot.
     * If trimActionAddingToDepth is >= than 0, then actions will be stripped from the imported nodes up to, and
     * including the depth specified.
     * Set to -1 or something if you don't want this.**/
    public static synchronized <N extends NodeQWOPBase<N>> void makeNodesFromRunInfo(List<SavableSingleGame> runs,
                                                                                     N existingRootToAddTo) {
        for (SavableSingleGame run : runs) { // Go through all runs, placing them in the tree.
            N currentNode = existingRootToAddTo;

            for (int i = 0; i < run.actions.length; i++) { // Iterate through individual actions of this run,
                // travelling down the tree in the process.

                boolean foundExistingMatch = false;
                for (N child : currentNode.getChildren()) { // Look at each child of the currently investigated node.
                    if (child.getAction() == run.actions[i]) { // If there is already a node for the action we are trying
                        // to place, just use it.
                        currentNode = child;
                        foundExistingMatch = true;
                        break; // We found a match, move on.
                    }
                }
                // If this action is unique at this point in the tree, we need to add a new node there.
                if (!foundExistingMatch) {
                    currentNode = currentNode.addDoublyLinkedChild(run.actions[i], run.states[i]);
                }
            }
        }
    }


    // Value and value updating

    private float value;
    private AtomicInteger updateCount;

    private Object lock = new Object();
    private AtomicBoolean beingUpdated = new AtomicBoolean();


    public void updateValue(float valueUpdate, IValueUpdater updater) {
        beingUpdated.set(true);
        value = updater.update(value, valueUpdate, updateCount.get(), this);
        updateCount.incrementAndGet();
        beingUpdated.set(false);
        lock.notify();
    }

    public float getValue() {
        if (beingUpdated.get()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return value;
    }

    public int getUpdateCount() {
        if (beingUpdated.get()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return updateCount.get();
    }
}
