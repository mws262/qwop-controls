package value;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameSerializable;
import game.action.Action;
import game.action.Command;
import org.jetbrains.annotations.NotNull;
import tree.node.NodeQWOPBase;

import java.util.List;
import java.util.Objects;

public class ValueFunction_Constant<C extends Command<?>> implements IValueFunction<C> {

    public final float constantValue;
    public final C baselineCommand;

    public ValueFunction_Constant(@JsonProperty("constantValue") float constantValue, @NotNull C baselineCommand) {
        this.constantValue = constantValue;
        this.baselineCommand = baselineCommand;
    }

    @Override
    public Action<C> getMaximizingAction(NodeQWOPBase<?, C> currentNode) {
        return new Action<>(1, baselineCommand);
    }

    @Override
    public Action<C> getMaximizingAction(NodeQWOPBase<?, C> currentNode, IGameSerializable<C> game) {
        return new Action<>(1, baselineCommand);
    }

    @Override
    public float evaluate(NodeQWOPBase<?, C> currentNode) {
        return 0;
    }

    @Override
    public void update(List<? extends NodeQWOPBase<?, C>> nodes) {}

    @Override
    public IValueFunction<C> getCopy() {
        return new ValueFunction_Constant<>(constantValue, baselineCommand);
    }

    @Override
    public void close() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueFunction_Constant<?> that = (ValueFunction_Constant<?>) o;
        return Float.compare(that.constantValue, constantValue) == 0 &&
                baselineCommand.equals(that.baselineCommand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constantValue, baselineCommand);
    }
}
