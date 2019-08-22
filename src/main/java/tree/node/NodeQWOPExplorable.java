package tree.node;

import game.action.Action;
import game.action.Command;
import game.action.IActionGenerator;
import game.state.IState;

/**
 * @see NodeQWOPExplorableBase
 */
public class NodeQWOPExplorable<C extends Command<?>> extends NodeQWOPExplorableBase<NodeQWOPExplorable<C>, C> {

    /**
     * @see NodeQWOPExplorableBase#NodeQWOPExplorableBase(IState)
     */
    public NodeQWOPExplorable(IState rootState) {
        super(rootState);
    }

    /**
     * @see NodeQWOPExplorableBase#NodeQWOPExplorableBase(IState, IActionGenerator)
     */
    public NodeQWOPExplorable(IState rootState,
                              IActionGenerator<C> actionGenerator) {
        super(rootState, actionGenerator);
    }

    /**
     *
     * @see NodeQWOPExplorableBase#NodeQWOPExplorableBase(NodeQWOPExplorableBase, Action, IState, IActionGenerator, boolean)
     */
    private NodeQWOPExplorable(NodeQWOPExplorable<C> parent,
                               Action<C> action,
                               IState state,
                               IActionGenerator<C> actionGenerator,
                               boolean doublyLinked) {
        super(parent, action, state, actionGenerator, doublyLinked);
    }

    @Override
    protected NodeQWOPExplorable<C> getThis() {
        return this;
    }

    @Override
    public NodeQWOPExplorable<C> addDoublyLinkedChild(Action<C> action,
                                                      IState state) {
        return addDoublyLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeQWOPExplorable<C> addBackwardsLinkedChild(Action<C> action,
                                                         IState state) {
        return addBackwardsLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeQWOPExplorable<C> addDoublyLinkedChild(Action<C> action,
                                                      IState state,
                                                      IActionGenerator<C> actionGenerator) {
        return new NodeQWOPExplorable<>(this, action, state, actionGenerator, true);
    }

    @Override
    public NodeQWOPExplorable<C> addBackwardsLinkedChild(Action<C> action,
                                                         IState state,
                                                         IActionGenerator<C> actionGenerator) {
        return new NodeQWOPExplorable<>(this, action, state, actionGenerator, false);
    }
}
