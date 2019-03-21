package evaluators;

import tree.INode;

import java.util.Objects;

/**
 * Evaluation of a node based strictly on torso horizontal position.
 *
 * @author Matt
 */
public class EvaluationFunction_Velocity implements IEvaluationFunction {

    @Override
    public float getValue(INode nodeToEvaluate) {
        return Objects.requireNonNull(nodeToEvaluate.getState()).body.getDx();
    }

    @Override
    public String getValueString(INode nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_Velocity getCopy() {
        return new EvaluationFunction_Velocity();
    }
}
