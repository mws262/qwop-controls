package value;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameSerializable;
import game.action.Action;
import game.action.CommandQWOP;
import tree.node.NodeQWOPBase;

import java.util.List;
import java.util.Objects;

public class ValueFunction_Constant implements IValueFunction {

    public final float constantValue;

    public ValueFunction_Constant(@JsonProperty("constantValue") float constantValue) {
        this.constantValue = constantValue;
    }

    @Override
    public Action getMaximizingAction(NodeQWOPBase<?> currentNode) {
        return new Action(1, CommandQWOP.NONE);
    }

    @Override
    public Action getMaximizingAction(NodeQWOPBase<?> currentNode, IGameSerializable game) {
        return new Action(1, CommandQWOP.NONE);
    }

    @Override
    public float evaluate(NodeQWOPBase<?> currentNode) {
        return 0;
    }

    @Override
    public void update(List<? extends NodeQWOPBase<?>> nodes) {

    }

    @Override
    public IValueFunction getCopy() {
        return new ValueFunction_Constant(constantValue);
    }

    @Override
    public void close() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueFunction_Constant that = (ValueFunction_Constant) o;
        return Float.compare(that.constantValue, constantValue) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(constantValue);
    }
}
