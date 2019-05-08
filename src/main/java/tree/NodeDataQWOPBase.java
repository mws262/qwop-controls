package tree;

import actions.Action;
import game.State;

public class NodeDataQWOPBase extends NodeData<State, Action> {
    @Override
    State getState() {
        return null;
    }

    @Override
    Action getAction() {
        return null;
    }

    @Override
    void display(Gl2 gl) {

    }

    @Override
    void initialize(NodeGeneric node) {
        node.getParent().addChild(node);
    }

    @Override
    void dispose() {

    }
}
