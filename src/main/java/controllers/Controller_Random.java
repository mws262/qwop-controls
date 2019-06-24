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
}
