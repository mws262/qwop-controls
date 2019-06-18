package tree.node;

import game.actions.Action;
import game.actions.IActionGenerator;
import game.state.IState;

/**
 * @see NodeQWOPExplorableBase
 */
public class NodeQWOPExplorable extends NodeQWOPExplorableBase<NodeQWOPExplorable> {

    /**
     * @see NodeQWOPExplorableBase#NodeQWOPExplorableBase(IState)
     */
    public NodeQWOPExplorable(IState rootState) {
        super(rootState);
    }

    /**
     * @see NodeQWOPExplorableBase#NodeQWOPExplorableBase(IState, IActionGenerator)
     */
    public NodeQWOPExplorable(IState rootState, IActionGenerator actionGenerator) {
        super(rootState, actionGenerator);
    }

    /**
     *
     * @see NodeQWOPExplorableBase#NodeQWOPExplorableBase(NodeQWOPExplorableBase, Action, IState, IActionGenerator, boolean)
     */
    private NodeQWOPExplorable(NodeQWOPExplorable parent, Action action, IState state,
                               IActionGenerator actionGenerator, boolean doublyLinked) {
        super(parent, action, state, actionGenerator, doublyLinked);
    }

    @Override
    protected NodeQWOPExplorable getThis() {
        return this;
    }

    @Override
    public NodeQWOPExplorable addDoublyLinkedChild(Action action, IState state) {
        return addDoublyLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeQWOPExplorable addBackwardsLinkedChild(Action action, IState state) {
        return addBackwardsLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeQWOPExplorable addDoublyLinkedChild(Action action, IState state, IActionGenerator actionGenerator) {
        return new NodeQWOPExplorable(this, action, state, actionGenerator, true);
    }

    @Override
    public NodeQWOPExplorable addBackwardsLinkedChild(Action action, IState state, IActionGenerator actionGenerator) {
        return new NodeQWOPExplorable(this, action, state, actionGenerator, false);
    }
}
