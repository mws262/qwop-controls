package tree.nodes;

import actions.Action;
import actions.ActionQueue;
import game.IGame;
import game.State;

import java.util.List;

/**
 */
public abstract class NodeQWOP extends NodeGenericBase<NodeQWOP> {

    private final Action action;

    private final State state;

    public NodeQWOP(State rootState) {
        super();
        action = null;
        state = rootState;
    }

    public NodeQWOP(NodeQWOP parent, Action action, State state) {
        super(parent);
        this.action = action;
        this.state = state;
        // Check to make sure this node isn't already there in the parent's nodes.
        for (NodeQWOP parentChildren : parent.getChildren()) {
            if (parentChildren.getAction() == action) {
                throw new IllegalArgumentException("Tried to add a duplicate action node at depth " + getTreeDepth() + ". Action " +
                        "was: " + action.toString() + ".");
            }
        }
        getParent().addChild(getThis());
    }

    public State getState() { return state; }

    public Action getAction() { return action; }

    public synchronized List<Action> getSequence(List<Action> actionList) {
        if (getTreeDepth() == 0)
            throw new IndexOutOfBoundsException("Cannot get a sequence at the root node, since it has no actions " +
                    "leading up to it.");
        actionList.clear();
        NodeQWOP current = getThis();
        actionList.add(getTreeDepth() - 1, current.getAction());
        for (int i = getTreeDepth() - 2; i >= 0; i--) {
            current = current.getParent();
            actionList.add(i, current.getAction());
        }
        return actionList;
    }

    public abstract NodeQWOP addChild(Action action, State state);

    /**
     * Add nodes based on saved action sequences. Has to re-simulate each to get the states.
     */
    public static void makeNodesFromActionSequences(List<Action[]> actions, NodeQWOP root, IGame game) {

        ActionQueue actQueue = new ActionQueue();
        for (Action[] acts : actions) {
            game.makeNewWorld();
            NodeQWOP currNode = root;
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
                for (NodeQWOP child : currNode.getChildren()) {
                    if (child.getAction().equals(act)) {
                        currNode = child;
                        foundExisting = true;
                        break;
                    }
                }

                // Otherwise, make a new one.
                if (!foundExisting) currNode = currNode.addChild(act, game.getCurrentState());
            }
        }
    }
}
