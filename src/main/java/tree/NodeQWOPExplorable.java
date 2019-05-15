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

    private NodeQWOPExplorable(NodeQWOPExplorable parent, Action action, State state, IActionGenerator actionGenerator) {
        super(parent, action, state, actionGenerator);
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
        NodeQWOPExplorable child = new NodeQWOPExplorable(this, action, state, actionGenerator);
        addToChildList(child);
        if (child.getState().isFailed()) {
            child.propagateFullyExploredStatusLite();
        }
        return child;
    }

    @Override
    public NodeQWOPExplorable addBackwardsLinkedChild(Action action, State state, IActionGenerator actionGenerator) {
        return new NodeQWOPExplorable(this, action, state, actionGenerator);
    }
}
