package evaluators;

import java.util.Objects;

import game.body_snapshots.BodyState;
import tree.INode;

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
     */
    private final INode nodeToCompareAllOthersTo;

    /**
     * State list associated with the target node. Stored to avoid fetching multiple times.
     */
    private final BodyState[] baseStateVars;

    /**
     * Create an evaluator function with a target node/state. Once created, the target may not be changed.
     *
     * @param nodeToCompareAllOthersTo Node whose state all others will be compared to.
     */
    public EvaluationFunction_SqDistFromOther(INode nodeToCompareAllOthersTo) {
        this.nodeToCompareAllOthersTo = nodeToCompareAllOthersTo;
        baseStateVars = nodeToCompareAllOthersTo.getState().getStates();
    }

    @Override
    public float getValue(INode nodeToEvaluate) {
        BodyState[] otherStateVarList = Objects.requireNonNull(nodeToEvaluate.getState()).getStates();

        float sqError = 0;

        for (int i = 0; i < baseStateVars.length; i++) {
            float diff =
                    (baseStateVars[i].getX() - baseStateVars[0].getX()) - (otherStateVarList[i].getX() - otherStateVarList[0].getX()); // Subtract out the absolute x of body.
            sqError += diff * diff;
            diff = baseStateVars[i].getY() - otherStateVarList[i].getY();
            sqError += diff * diff;
            diff = baseStateVars[i].getTh() - otherStateVarList[i].getTh();
            sqError += diff * diff;
            diff = baseStateVars[i].getDx() - otherStateVarList[i].getDx();
            sqError += diff * diff;
            diff = baseStateVars[i].getDy() - otherStateVarList[i].getDy();
            sqError += diff * diff;
            diff = baseStateVars[i].getDth() - otherStateVarList[i].getDth();
            sqError += diff * diff;
        }
        return -sqError; // Negative error, so higher is better.
    }

    @Override
    public String getValueString(INode nodeToEvaluate) {
        BodyState[] otherStateVarList = Objects.requireNonNull(nodeToEvaluate.getState()).getStates();
        StringBuilder valueString = new StringBuilder();

        for (int i = 0; i < baseStateVars.length; i++) {
            float diff =
                    (baseStateVars[i].getX() - baseStateVars[0].getX()) - (otherStateVarList[i].getX() - otherStateVarList[0].getX());
            valueString.append("x: ").append(diff * diff).append(", ");
            diff = baseStateVars[i].getY() - otherStateVarList[i].getY();
            valueString.append("y: ").append(diff * diff).append(", ");
            diff = baseStateVars[i].getTh() - otherStateVarList[i].getTh();
            valueString.append("th: ").append(diff * diff).append(", ");
            diff = baseStateVars[i].getDx() - otherStateVarList[i].getDx();
            valueString.append("dx: ").append(diff * diff).append(", ");
            diff = baseStateVars[i].getDy() - otherStateVarList[i].getDy();
            valueString.append("dy: ").append(diff * diff).append(", ");
            diff = baseStateVars[i].getDth() - otherStateVarList[i].getDth();
            valueString.append("dth: ").append(diff * diff);
        }
        return valueString.toString();
    }

    @Override
    public EvaluationFunction_SqDistFromOther getCopy() {
        return new EvaluationFunction_SqDistFromOther(nodeToCompareAllOthersTo);
    }
}
