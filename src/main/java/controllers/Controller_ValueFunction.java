package controllers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Action;
import tree.node.NodeQWOPExplorableBase;
import value.IValueFunction;

public class Controller_ValueFunction<V extends IValueFunction> implements IController {

    private final V valueFunction;

    public Controller_ValueFunction(@JsonProperty("valueFunction") V valueFunction) {
        this.valueFunction = valueFunction;
    }

    @Override
    public Action policy(NodeQWOPExplorableBase<?> state) {
        return valueFunction.getMaximizingAction(state);
    }

    @Override
    @JsonIgnore
    public IController getCopy() {
        return new Controller_ValueFunction<>(valueFunction);
    } // TODO doesn't duplicate underlying value function. Is this bad? I don't know yet.

    public V getValueFunction() {
        return valueFunction;
    }
}
