package tree.node;

import game.action.Action;
import game.action.Command;
import game.action.IActionGenerator;
import game.state.IState;

/**
 * @see NodeGameExplorableBase
 */
public class NodeGameExplorable<C extends Command<?>> extends NodeGameExplorableBase<NodeGameExplorable<C>, C> {

    /**
     * @see NodeGameExplorableBase#NodeGameExplorableBase(IState)
     */
    public NodeGameExplorable(IState rootState) {
        super(rootState);
    }

    /**
     * @see NodeGameExplorableBase#NodeGameExplorableBase(IState, IActionGenerator)
     */
    public NodeGameExplorable(IState rootState,
                              IActionGenerator<C> actionGenerator) {
        super(rootState, actionGenerator);
    }

    /**
     *
     * @see NodeGameExplorableBase#NodeGameExplorableBase(NodeGameExplorableBase, Action, IState, IActionGenerator, boolean)
     */
    private NodeGameExplorable(NodeGameExplorable<C> parent,
                               Action<C> action,
                               IState state,
                               IActionGenerator<C> actionGenerator,
                               boolean doublyLinked) {
        super(parent, action, state, actionGenerator, doublyLinked);
    }

    @Override
    protected NodeGameExplorable<C> getThis() {
        return this;
    }

    @Override
    public NodeGameExplorable<C> addDoublyLinkedChild(Action<C> action,
                                                      IState state) {
        return addDoublyLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeGameExplorable<C> addBackwardsLinkedChild(Action<C> action,
                                                         IState state) {
        return addBackwardsLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeGameExplorable<C> addDoublyLinkedChild(Action<C> action,
                                                      IState state,
                                                      IActionGenerator<C> actionGenerator) {
        return new NodeGameExplorable<>(this, action, state, actionGenerator, true);
    }

    @Override
    public NodeGameExplorable<C> addBackwardsLinkedChild(Action<C> action,
                                                         IState state,
                                                         IActionGenerator<C> actionGenerator) {
        return new NodeGameExplorable<>(this, action, state, actionGenerator, false);
    }
}
