package tree;

import actions.Action;
import game.State;

public class NodeQWOP<N extends NodeQWOP<N>> extends NodeGeneric<N> {

    private final Action action;

    private final State state;

    public NodeQWOP(State rootState) {
        super();
        action = null;
        state = rootState;
    }

    public NodeQWOP(N parent, Action action, State state) {
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
        //noinspection unchecked
        parent.addChild((N) this); // I think this, by definition MUST be of type N.
    }

    public State getState() { return state; }

    public Action getAction() { return action; }
}
