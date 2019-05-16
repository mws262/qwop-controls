package tree;

import actions.Action;
import actions.IActionGenerator;
import game.State;

public class NodeQWOPExplorable extends NodeQWOPExplorableBase<NodeQWOPExplorable> {

    public NodeQWOPExplorable(State rootState) {
        super(rootState);
    }

    public NodeQWOPExplorable(State rootState, IActionGenerator actionGenerator) {
        super(rootState, actionGenerator);
    }

    private NodeQWOPExplorable(NodeQWOPExplorable parent, Action action, State state,
                               IActionGenerator actionGenerator, boolean doublyLinked) {
        super(parent, action, state, actionGenerator, doublyLinked);
    }

    @Override
    protected NodeQWOPExplorable getThis() {
        return this;
    }

    @Override
    public NodeQWOPExplorable addDoublyLinkedChild(Action action, State state) {
        return addDoublyLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeQWOPExplorable addBackwardsLinkedChild(Action action, State state) {
        return addBackwardsLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeQWOPExplorable addDoublyLinkedChild(Action action, State state, IActionGenerator actionGenerator) {
        return new NodeQWOPExplorable(this, action, state, actionGenerator, true);
    }

    @Override
    public NodeQWOPExplorable addBackwardsLinkedChild(Action action, State state, IActionGenerator actionGenerator) {
        return new NodeQWOPExplorable(this, action, state, actionGenerator, false);
    }
}
