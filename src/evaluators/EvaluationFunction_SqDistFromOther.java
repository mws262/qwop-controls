package evaluators;

import java.util.List;

import game.StateVariable;
import main.Node;

/**
 * Evaluation of node value based on squared difference in the node's game state from another specified node's state.
 * Higher score is still better, or closer to the target state. The target node/state cannot be changed, and a new
 * evaluation function should be created for each target node/state.
 *
 * @author matt
 */
public class EvaluationFunction_SqDistFromOther implements IEvaluationFunction {

    /**
     * All nodes will be compared to this one by square distance in state space.
     **/
    private final Node nodeToCompareAllOthersTo;

    /**
     * State list associated with the target node. Stored to avoid fetching multiple times.
     */
    private final List<StateVariable> baseStateVarList;

    /**
     * Create an evaluator function with a target node/state. Once created, the target may not be changed.
     *
     * @param nodeToCompareAllOthersTo Node whose state all others will be compared to.
     */
    public EvaluationFunction_SqDistFromOther(Node nodeToCompareAllOthersTo) {
        this.nodeToCompareAllOthersTo = nodeToCompareAllOthersTo;
        baseStateVarList = nodeToCompareAllOthersTo.getState().getStateList();
    }

    @Override
    public float getValue(Node nodeToEvaluate) {
        if (!nodeToEvaluate.isStateAvailable())
            throw new NullPointerException("Trying to evaluate a node based on state information which has not yet " +
                    "been assigned in that node.");

        List<StateVariable> otherStateVarList = nodeToEvaluate.getState().getStateList();

        float sqError = 0;

        for (int i = 0; i < baseStateVarList.size(); i++) {
            float diff =
                    (baseStateVarList.get(i).x - baseStateVarList.get(0).x) - (otherStateVarList.get(i).x - otherStateVarList.get(0).x); // Subtract out the absolute x of body.
            sqError += diff * diff;
            diff = baseStateVarList.get(i).y - otherStateVarList.get(i).y;
            sqError += diff * diff;
            diff = baseStateVarList.get(i).th - otherStateVarList.get(i).th;
            sqError += diff * diff;
            diff = baseStateVarList.get(i).dx - otherStateVarList.get(i).dx;
            sqError += diff * diff;
            diff = baseStateVarList.get(i).dy - otherStateVarList.get(i).dy;
            sqError += diff * diff;
            diff = baseStateVarList.get(i).dth - otherStateVarList.get(i).dth;
        }
        return -sqError; // Negative error, so higher is better.
    }

    @Override
    public String getValueString(Node nodeToEvaluate) {
        if (!nodeToEvaluate.isStateAvailable())
            throw new NullPointerException("Trying to evaluate a node based on state information which has not yet " +
                    "been assigned in that node.");

        List<StateVariable> otherStateVarList = nodeToEvaluate.getState().getStateList();
        StringBuilder valueString = new StringBuilder();

        for (int i = 0; i < baseStateVarList.size(); i++) {
            float diff =
                    (baseStateVarList.get(i).x - baseStateVarList.get(0).x) - (otherStateVarList.get(i).x - otherStateVarList.get(0).x);
            valueString.append("x: ").append(diff * diff).append(", ");
            diff = baseStateVarList.get(i).y - otherStateVarList.get(i).y;
            valueString.append("y: ").append(diff * diff).append(", ");
            diff = baseStateVarList.get(i).th - otherStateVarList.get(i).th;
            valueString.append("th: ").append(diff * diff).append(", ");
            diff = baseStateVarList.get(i).dx - otherStateVarList.get(i).dx;
            valueString.append("dx: ").append(diff * diff).append(", ");
            diff = baseStateVarList.get(i).dy - otherStateVarList.get(i).dy;
            valueString.append("dy: ").append(diff * diff).append(", ");
            diff = baseStateVarList.get(i).dth - otherStateVarList.get(i).dth;
            valueString.append("dth: ").append(diff * diff);
        }
        return valueString.toString();
    }

    @Override
    public EvaluationFunction_SqDistFromOther clone() {
        return new EvaluationFunction_SqDistFromOther(nodeToCompareAllOthersTo);
    }

}
