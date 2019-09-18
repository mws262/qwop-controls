package tree.node.evaluator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

/**
 * Scores nodes by returning a constant value no matter what node is input. Mainly useful for testing.
 *
 * @author matt
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class EvaluationFunction_Constant<C extends Command<?>, S extends IState> implements IEvaluationFunction<C, S> {

    /**
     * Constant node value which will be returned for any input.
     */
    private final float constantValue;

    /**
     * Create a constant evaluation function by assigning the constant value associated with it.
     *
     * @param constantValue Value which will always be returned by this object.
     */
    @JsonCreator
    public EvaluationFunction_Constant(@JsonProperty("constantValue") float constantValue) {
        this.constantValue = constantValue;
    }

    @Override
    public float getValue(NodeGameBase<?, C, S> nodeToEvaluate) {
        return constantValue;
    }

    @Override
    public String getValueString(NodeGameBase<?, C, S> nodeToEvaluate) {
        return String.valueOf(constantValue);
    }

    @Override
    public IEvaluationFunction<C, S> getCopy() {
        return new EvaluationFunction_Constant<>(constantValue);
    }

    public float getConstantValue() {
        return constantValue;
    }

    @Override
    public void close() {}
}
