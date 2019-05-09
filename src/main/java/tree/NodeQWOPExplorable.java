package tree;

import actions.Action;
import actions.IActionGenerator;
import game.State;

public class NodeQWOPExplorable extends NodeQWOPExplorableBase<NodeQWOPExplorable> {

    public NodeQWOPExplorable(State rootState, IActionGenerator actionGenerator) {
        super(rootState, actionGenerator);
    }

    public NodeQWOPExplorable(NodeQWOPExplorable parent, Action action, State state, IActionGenerator actionGenerator) {
        super(parent, action, state, actionGenerator);
    }

    @Override
    protected NodeQWOPExplorable getThis() {
        return this;
    }

    @Override
    public NodeQWOPExplorable addChild(Action action, State state) {
        return new NodeQWOPExplorable(this, action, state, actionGenerator);
    }
}
