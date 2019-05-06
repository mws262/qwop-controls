package evaluators;

import tree.INode;

import java.util.Objects;

/**
 * Evaluation of a node based distance traveled since the last node.
 *
 * @author Matt
 */
public class EvaluationFunction_DeltaDistance implements IEvaluationFunction {

    @Override
    public float getValue(INode nodeToEvaluate) {

        if (nodeToEvaluate.getParent() != null)
            return Objects.requireNonNull(nodeToEvaluate.getState()).torso.getX() - nodeToEvaluate.getParent().getState().torso.getX();
        else
            return 0;
    }

    @Override
    public String getValueString(INode nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_DeltaDistance getCopy() {
        return new EvaluationFunction_DeltaDistance();
    }
}
