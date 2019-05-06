package evaluators;

import tree.INode;

import java.util.Objects;

/**
 * Evaluation of a node based strictly on torso horizontal position.
 *
 * @author Matt
 */
public class EvaluationFunction_Distance implements IEvaluationFunction {

    @Override
    public float getValue(INode nodeToEvaluate) {
        return Objects.requireNonNull(nodeToEvaluate.getState()).torso.getX();
    }

    @Override
    public String getValueString(INode nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_Distance getCopy() {
        return new EvaluationFunction_Distance();
    }
}
