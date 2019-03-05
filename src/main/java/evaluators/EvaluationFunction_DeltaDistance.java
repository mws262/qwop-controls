package evaluators;

import tree.Node;

/**
 * Evaluation of a node based distance traveled since the last node.
 *
 * @author Matt
 */
public class EvaluationFunction_DeltaDistance implements IEvaluationFunction {

    @Override
    public float getValue(Node nodeToEvaluate) {
        if (nodeToEvaluate.isStateUnassigned())
            throw new NullPointerException("Trying to evaluate a node based on state information which has not yet " +
                    "been assigned in that node.");
        if (nodeToEvaluate.getTreeDepth() > 0)
            return nodeToEvaluate.getState().body.getX() - nodeToEvaluate.getParent().getState().body.getX();
        else
            return 0;
    }

    @Override
    public String getValueString(Node nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_DeltaDistance getCopy() {
        return new EvaluationFunction_DeltaDistance();
    }
}
