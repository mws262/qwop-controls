package value;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameSerializable;
import game.action.Action;
import tree.node.NodeQWOPBase;

import java.util.List;

public class ValueFunction_Constant implements IValueFunction {

    public final float constantValue;

    public ValueFunction_Constant(@JsonProperty("constantValue") float constantValue) {
        this.constantValue = constantValue;
    }

    @Override
    public Action getMaximizingAction(NodeQWOPBase<?> currentNode) {
        return new Action(1, false, false, false, false);
    }

    @Override
    public Action getMaximizingAction(NodeQWOPBase<?> currentNode, IGameSerializable game) {
        return new Action(1, false, false, false, false);
    }

    @Override
    public float evaluate(NodeQWOPBase<?> currentNode) {
        return 0;
    }

    @Override
    public void update(List<? extends NodeQWOPBase<?>> nodes) {

    }
}
