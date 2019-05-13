package tree;

import actions.Action;
import game.State;

/**
 * The usable form of {@link NodeQWOPBase}.
 */
public final class NodeQWOP extends NodeQWOPBase<NodeQWOP> {

    public NodeQWOP(State rootState) {
        super(rootState);
    }

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
