package tree.node;

import game.action.Action;
import game.action.Command;
import game.state.IState;

/**
 * @see NodeGameBase
 */
public final class NodeGame<C extends Command<?>, S extends IState> extends NodeGameBase<NodeGame<C, S>, C, S> {

    /**
     * @see NodeGameBase#NodeGameBase(IState)
     */
    public NodeGame(S rootState) {
        super(rootState);
    }

    /**
     * @see NodeGameBase#NodeGameBase(NodeGameBase, Action, IState)
     */
    private NodeGame(NodeGame<C, S> parent, Action<C> action, S state) {
        super(parent, action, state);
    }

    @Override
    public NodeGame<C, S> addDoublyLinkedChild(Action<C> action, S state) {
        NodeGame<C, S> child = new NodeGame<>(this, action, state);
        addToChildList(child);
        return child;
    }

    @Override
    public NodeGame<C, S> addBackwardsLinkedChild(Action<C> action, S state) {
        return new NodeGame<>(this, action, state);
    }

    @Override
    protected NodeGame<C, S> getThis() {
        return this;
    }
}
