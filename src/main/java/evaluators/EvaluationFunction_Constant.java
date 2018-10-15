package evaluators;

import tree.Node;


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
    public EvaluationFunction_Constant(float constantValue) {
        this.constantValue = constantValue;
    }

    @Override
    public float getValue(Node nodeToEvaluate) {
        return constantValue;
    }

    @Override
    public String getValueString(Node nodeToEvaluate) {
        return String.valueOf(constantValue);
    }

    @Override
    public IEvaluationFunction getCopy() {
        return new EvaluationFunction_Constant(constantValue);
    }
}
