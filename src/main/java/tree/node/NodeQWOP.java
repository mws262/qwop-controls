package tree.node;

import game.action.Action;
import game.action.Command;
import game.state.IState;

/**
 * @see NodeQWOPBase
 */
public final class NodeQWOP<C extends Command<?>> extends NodeQWOPBase<NodeQWOP<C>, C> {

    /**
     * @see NodeQWOPBase#NodeQWOPBase(IState)
     */
    public NodeQWOP(IState rootState) {
        super(rootState);
    }

    /**
     * @see NodeQWOPBase#NodeQWOPBase(NodeQWOPBase, Action, IState)
     */
    private NodeQWOP(NodeQWOP parent, Action<C> action, IState state) {
        super(parent, action, state);
    }

    @Override
    public NodeQWOP<C> addDoublyLinkedChild(Action<C> action, IState state) {
        NodeQWOP<C> child = new NodeQWOP<>(this, action, state);
        addToChildList(child);
        return child;
    }

    @Override
    public NodeQWOP<C> addBackwardsLinkedChild(Action<C> action, IState state) {
        return new NodeQWOP<>(this, action, state);
    }

    @Override
    protected NodeQWOP<C> getThis() {
        return this;
    }
}
