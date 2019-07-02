package controllers;

import game.action.Action;
import tree.node.NodeQWOPExplorableBase;

public class Controller_Random implements IController {
    @Override
    public Action policy(NodeQWOPExplorableBase<?> state) {
        return state.getUntriedActionOnDistribution();
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
}
