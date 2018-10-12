package evaluators;

import main.Node;

/**
 * Evaluation of a node based strictly on torso horizontal position.
 *
 * @author Matt
 */
public class EvaluationFunction_Distance implements IEvaluationFunction {

    @Override
    public float getValue(Node nodeToEvaluate) {
        if (!nodeToEvaluate.isStateAvailable())
            throw new NullPointerException("Trying to evaluate a node based on state information which has not yet " +
                    "been assigned in that node.");
        if (nodeToEvaluate.treeDepth > 0) {
            return nodeToEvaluate.getState().body.x;
        } else {
            return 0.f; // Root gets score of 0.
        }
    }

    @Override
    public String getValueString(Node nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_Distance clone() {
        return new EvaluationFunction_Distance();
    }
}
