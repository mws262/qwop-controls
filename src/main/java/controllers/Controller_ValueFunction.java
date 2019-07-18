package controllers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameSerializable;
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
        //        return new Action(Math.max(1, Math.min(a.getTimestepsTotal(), 8)), a.peek());
        return valueFunction.getMaximizingAction(state);
    }

    public Action policy(NodeQWOPExplorableBase<?> state, IGameSerializable game) {
        return valueFunction.getMaximizingAction(state, game);
    }

    @Override
    @JsonIgnore
    public IController getCopy() {
        return new Controller_ValueFunction<>(valueFunction.getCopy());
    }

    public V getValueFunction() {
        return valueFunction;
    }

    @Override
    public void close() {
        valueFunction.close();
    }
}
