package controllers;

import game.IGameSerializable;
import game.action.Action;
import tree.node.NodeQWOPExplorableBase;

public class Controller_Random implements IController {
    @Override
    public Action policy(NodeQWOPExplorableBase<?> state) {
        if (state.getUntriedActionCount() > 0) {
            return state.getUntriedActionOnDistribution();
        }else {
            return new Action(1, false, false, false, false);
        }
    }

    @Override
    public Action policy(NodeQWOPExplorableBase<?> state, IGameSerializable game) {
        return policy(state);
    }

    @Override
    public IController getCopy() {
        return new Controller_Random();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Controller_Random;
    }

    @Override
    public int hashCode() {
        return Controller_Random.class.hashCode();
    }

    @Override
    public void close() {}
}
