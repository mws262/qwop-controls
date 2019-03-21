package tree;

import actions.Action;
import game.State;

public class NodePlaceholder implements INode {

    private Action action;
    private State state;
    private INode parent;

    public NodePlaceholder(INode parent, Action action, State state) {
        this.action = action;
        this.state = state;
        this.parent = parent;
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public INode getParent() {
        return parent;
    }
}
