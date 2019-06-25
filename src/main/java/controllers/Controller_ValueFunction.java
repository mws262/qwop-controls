package controllers;

import game.action.Action;
import tree.node.NodeQWOPExplorableBase;
import value.IValueFunction;

public class Controller_ValueFunction implements IController {

    private IValueFunction valueFunction;

    public Controller_ValueFunction(IValueFunction valueFunction) {
        this.valueFunction = valueFunction;
    }

    @Override
    public Action policy(NodeQWOPExplorableBase<?> state) {
        return valueFunction.getMaximizingAction(state);
    }

    @Override
    public IController getCopy() {
        return null;
    }
}
