package evaluators;

import tree.Node;

/**
 * Evaluation of a node based strictly on torso horizontal position.
 *
 * @author Matt
 */
public class EvaluationFunction_Distance implements IEvaluationFunction {

    @Override
    public float getValue(Node nodeToEvaluate) {
        if (nodeToEvaluate.isStateUnassigned())
            throw new NullPointerException("Trying to evaluate a node based on state information which has not yet " +
                    "been assigned in that node.");
        return nodeToEvaluate.getState().body.getX();
    }

    @Override
    public String getValueString(Node nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_Distance getCopy() {
        return new EvaluationFunction_Distance();
    }
}
