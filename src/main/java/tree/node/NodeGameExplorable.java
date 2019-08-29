package tree.node;

import game.action.Action;
import game.action.Command;
import game.action.IActionGenerator;
import game.state.IState;

/**
 * @see NodeGameExplorableBase
 */
public class NodeGameExplorable<C extends Command<?>, S extends IState> extends NodeGameExplorableBase<NodeGameExplorable<C, S>, C, S> {

    /**
     * @see NodeGameExplorableBase#NodeGameExplorableBase(IState)
     */
    public NodeGameExplorable(S rootState) {
        super(rootState);
    }

    /**
     * @see NodeGameExplorableBase#NodeGameExplorableBase(IState, IActionGenerator)
     */
    public NodeGameExplorable(S rootState, IActionGenerator<C> actionGenerator) {
        super(rootState, actionGenerator);
    }

    /**
     *
     * @see NodeGameExplorableBase#NodeGameExplorableBase(NodeGameExplorableBase, Action, IState, IActionGenerator, boolean)
     */
    private NodeGameExplorable(NodeGameExplorable<C, S> parent, Action<C> action,
                               S state, IActionGenerator<C> actionGenerator, boolean doublyLinked) {
        super(parent, action, state, actionGenerator, doublyLinked);
    }

    @Override
    protected NodeGameExplorable<C, S> getThis() {
        return this;
    }

    @Override
    public NodeGameExplorable<C, S> addDoublyLinkedChild(Action<C> action, S state) {
        return addDoublyLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeGameExplorable<C, S> addBackwardsLinkedChild(Action<C> action, S state) {
        return addBackwardsLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeGameExplorable<C, S> addDoublyLinkedChild(Action<C> action, S state,
                                                         IActionGenerator<C> actionGenerator) {
        return new NodeGameExplorable<>(this, action, state, actionGenerator, true);
    }

    @Override
    public NodeGameExplorable<C, S> addBackwardsLinkedChild(Action<C> action, S state,
                                                            IActionGenerator<C> actionGenerator) {
        return new NodeGameExplorable<>(this, action, state, actionGenerator, false);
    }
}
