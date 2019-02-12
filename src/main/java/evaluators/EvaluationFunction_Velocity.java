package evaluators;

import tree.Node;

/**
 * Evaluation of a node based strictly on torso horizontal position.
 *
 * @author Matt
 */
public class EvaluationFunction_Velocity implements IEvaluationFunction {

    @Override
    public float getValue(Node nodeToEvaluate) {
        if (nodeToEvaluate.isStateUnassigned())
            throw new NullPointerException("Trying to evaluate a node based on state information which has not yet " +
                    "been assigned in that node.");
        return nodeToEvaluate.getState().body.getDx();
    }

    @Override
    public String getValueString(Node nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @Override
    public EvaluationFunction_Velocity getCopy() {
        return new EvaluationFunction_Velocity();
    }
}
