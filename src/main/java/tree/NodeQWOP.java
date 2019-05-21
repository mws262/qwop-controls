package tree;

import actions.Action;
import game.State;

/**
 * @see NodeQWOPBase
 */
public final class NodeQWOP extends NodeQWOPBase<NodeQWOP> {

    /**
     * @see NodeQWOPBase#NodeQWOPBase(State)
     */
    public NodeQWOP(State rootState) {
        super(rootState);
    }

    /**
     * @see NodeQWOPBase#NodeQWOPBase(NodeQWOPBase, Action, State)
     */
    private NodeQWOP(NodeQWOP parent, Action action, State state) {
        super(parent, action, state);
    }

    @Override
    public NodeQWOP addDoublyLinkedChild(Action action, State state) {
        NodeQWOP child = new NodeQWOP(this, action, state);
        addToChildList(child);
        return child;
    }

    @Override
    public NodeQWOP addBackwardsLinkedChild(Action action, State state) {
        return new NodeQWOP(this, action, state);
    }

    @Override
    protected NodeQWOP getThis() {
        return this;
    }
}
