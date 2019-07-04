package controllers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Action;
import tree.node.NodeQWOPExplorableBase;
import value.IValueFunction;

public class Controller_ValueFunction implements IController {

    private final IValueFunction valueFunction;

    public Controller_ValueFunction(@JsonProperty("valueFunction") IValueFunction valueFunction) {
        this.valueFunction = valueFunction;
    }

    @Override
    public Action policy(NodeQWOPExplorableBase<?> state) {
        return valueFunction.getMaximizingAction(state);
    }

    @Override
    @JsonIgnore
    public IController getCopy() {
        return new Controller_ValueFunction(valueFunction);
    } // TODO doesn't duplicate underlying value function. Is this bad? I don't know yet.

    public IValueFunction getValueFunction() {
        return valueFunction;
    }
}
