package tree.node.evaluator;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.state.IState;
import game.state.StateVariable;
import tree.node.NodeQWOPBase;

import java.util.Objects;

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
    private final IState comparisonState;

    /**
     * State list associated with the target node. Stored to avoid fetching multiple times.
     */
    private final StateVariable[] baseStateVars;

    /**
     * Create an evaluator function with a target node/state. Once created, the target may not be changed.
     *
     * @param comparisonState State all others will be compared to.
     */
    public EvaluationFunction_SqDistFromOther(@JsonProperty("comparisonState") IState comparisonState) {
        this.comparisonState = comparisonState;
        baseStateVars = comparisonState.getAllStateVariables();
    }

    @Override
    public float getValue(NodeQWOPBase<?> nodeToEvaluate) {
        StateVariable[] otherStateVarList = Objects.requireNonNull(nodeToEvaluate.getState()).getAllStateVariables();

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
    public String getValueString(NodeQWOPBase<?> nodeToEvaluate) {
        StateVariable[] otherStateVarList = Objects.requireNonNull(nodeToEvaluate.getState()).getAllStateVariables();
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
        return new EvaluationFunction_SqDistFromOther(comparisonState);
    }

    public IState getComparisonState() {
        return comparisonState;
    }
}
