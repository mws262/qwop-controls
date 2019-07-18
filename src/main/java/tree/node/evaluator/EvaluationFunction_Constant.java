package tree.node.evaluator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import tree.node.NodeQWOPBase;

/**
 * Scores nodes by returning a constant value no matter what node is input. Mainly useful for testing.
 *
 * @author matt
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class EvaluationFunction_Constant implements IEvaluationFunction {

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
    public float getValue(NodeQWOPBase<?> nodeToEvaluate) {
        return constantValue;
    }

    @Override
    public String getValueString(NodeQWOPBase<?> nodeToEvaluate) {
        return String.valueOf(constantValue);
    }

    @Override
    public IEvaluationFunction getCopy() {
        return new EvaluationFunction_Constant(constantValue);
    }

    public float getConstantValue() {
        return constantValue;
    }

    @Override
    public void close() {}
}
