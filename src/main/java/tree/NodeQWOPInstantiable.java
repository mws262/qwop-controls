package tree;

import actions.Action;
import game.State;

public class NodeQWOPInstantiable extends NodeQWOP<NodeQWOPInstantiable> {

    public NodeQWOPInstantiable(State rootState) {
        super(rootState);
    }

    public NodeQWOPInstantiable(NodeQWOPInstantiable parent, Action action, State state) {
        super(parent, action, state);
    }

    @Override
    protected NodeQWOPInstantiable getThis() {
        return this;
    }
}
