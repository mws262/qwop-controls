package tree;

import actions.Action;
import actions.ActionGenerator_Null;
import actions.IActionGenerator;
import game.State;

public class NodeQWOPGraphics extends NodeQWOPGraphicsBase<NodeQWOPGraphics> {

    public NodeQWOPGraphics(State rootState, IActionGenerator actionGenerator) {
        super(rootState, actionGenerator);
    }
    public NodeQWOPGraphics(State rootState) {
        super(rootState);
    }

    private NodeQWOPGraphics(NodeQWOPGraphics parent, Action action, State state, IActionGenerator actionGenerator,
                             boolean doublyLinked) {
        super(parent, action, state, actionGenerator, doublyLinked);
    }

    @Override
    protected NodeQWOPGraphics getThis() {
        return this;
    }

    @Override
    public NodeQWOPGraphics addDoublyLinkedChild(Action action, State state) {
        return addDoublyLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeQWOPGraphics addBackwardsLinkedChild(Action action, State state) {
        return addBackwardsLinkedChild(action, state, actionGenerator);
    }

    @Override
    public NodeQWOPGraphics addDoublyLinkedChild(Action action, State state, IActionGenerator actionGenerator) {
        return new NodeQWOPGraphics(this, action, state, actionGenerator, true);
    }

    @Override
    public NodeQWOPGraphics addBackwardsLinkedChild(Action action, State state, IActionGenerator actionGenerator) {
        return new NodeQWOPGraphics(this, action, state, actionGenerator, false);
    }
}
