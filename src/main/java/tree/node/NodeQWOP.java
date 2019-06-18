package tree.node;

import game.actions.Action;
import game.state.IState;

/**
 * @see NodeQWOPBase
 */
public final class NodeQWOP extends NodeQWOPBase<NodeQWOP> {

    /**
     * @see NodeQWOPBase#NodeQWOPBase(IState)
     */
    public NodeQWOP(IState rootState) {
        super(rootState);
    }

    /**
     * @see NodeQWOPBase#NodeQWOPBase(NodeQWOPBase, Action, IState)
     */
    private NodeQWOP(NodeQWOP parent, Action action, IState state) {
        super(parent, action, state);
    }

    @Override
    public NodeQWOP addDoublyLinkedChild(Action action, IState state) {
        NodeQWOP child = new NodeQWOP(this, action, state);
        addToChildList(child);
        return child;
    }

    @Override
    public NodeQWOP addBackwardsLinkedChild(Action action, IState state) {
        return new NodeQWOP(this, action, state);
    }

    @Override
    protected NodeQWOP getThis() {
        return this;
    }
}
