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

    public NodeQWOP(NodeQWOP parent, Action action, State state) {
        super(parent, action, state);
    }

    @Override
    public NodeQWOP addChild(Action action, State state) {
        return new NodeQWOP(this, action, state);
    }

    @Override
    public NodeQWOP addUnconnectedChild(Action action, State state) {
        return null;
    }

    @Override
    protected NodeQWOP getThis() {
        return this;
    }
}
