package tree;

import actions.Action;
import actions.ActionQueue;
import game.IGame;
import game.State;

import java.util.List;

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
}
