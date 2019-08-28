package tree.node;

import game.action.Action;
import game.action.Command;
import game.state.IState;

/**
 * @see NodeGameBase
 */
public final class NodeGame<C extends Command<?>> extends NodeGameBase<NodeGame<C>, C> {

    /**
     * @see NodeGameBase#NodeGameBase(IState)
     */
    public NodeGame(IState rootState) {
        super(rootState);
    }

    /**
     * @see NodeGameBase#NodeGameBase(NodeGameBase, Action, IState)
     */
    private NodeGame(NodeGame parent, Action<C> action, IState state) {
        super(parent, action, state);
    }

    @Override
    public NodeGame<C> addDoublyLinkedChild(Action<C> action, IState state) {
        NodeGame<C> child = new NodeGame<>(this, action, state);
        addToChildList(child);
        return child;
    }

    @Override
    public NodeGame<C> addBackwardsLinkedChild(Action<C> action, IState state) {
        return new NodeGame<>(this, action, state);
    }

    @Override
    protected NodeGame<C> getThis() {
        return this;
    }
}
