package evaluators;

import tree.node.NodeQWOPBase;

import java.util.Objects;

/**
 * Evaluation of a node based distance traveled since the last node.
 *
 * @author Matt
 */
public class EvaluationFunction_DeltaDistance implements IEvaluationFunction {

    @Override
    public float getValue(NodeQWOPBase<?> nodeToEvaluate) {

        if (nodeToEvaluate.getParent() != null)
            return Objects.requireNonNull(nodeToEvaluate.getState()).getCenterX() - nodeToEvaluate.getParent().getState().getCenterX();
        else
            return 0;
    }

    @Override
    public String getValueString(NodeQWOPBase<?> nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_DeltaDistance getCopy() {
        return new EvaluationFunction_DeltaDistance();
    }
}
