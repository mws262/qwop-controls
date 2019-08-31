package tree.node.evaluator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

import java.util.Objects;

/**
 * Evaluation of node value based on squared difference in the node's game state from another specified node's state.
 * Higher score is still better, or closer to the target state. The target node/state cannot be changed, and a new
 * evaluation function should be created for each target node/state.
 *
 * @author matt
 */
public class EvaluationFunction_SqDistFromOther<C extends Command<?>, S extends IState> implements IEvaluationFunction<C, S> {

    /**
     * All nodes will be compared to this one by square distance in state space.
     */
    private final S comparisonState;

    /**
     * StateQWOP list associated with the target node. Stored to avoid fetching multiple times.
     */
    private final float[] baseStateVars;

    /**
     * Create an evaluator function with a target node/state. Once created, the target may not be changed.
     *
     * @param comparisonState StateQWOP all others will be compared to.
     */
    @JsonCreator
    public EvaluationFunction_SqDistFromOther(@JsonProperty("comparisonState") S comparisonState) {
        this.comparisonState = comparisonState;
        baseStateVars = comparisonState.flattenState();
    }

    @JsonIgnore
    @Override
    public float getValue(NodeGameBase<?, C, S> nodeToEvaluate) {
        float[] otherStateVals = Objects.requireNonNull(nodeToEvaluate.getState()).flattenState();

        float sqError = 0;
        for (int i = 0; i < baseStateVars.length; i++) {
            float diff = baseStateVars[i] - otherStateVals[i];
            sqError += diff * diff;
        }

        return -sqError; // Negative error, so higher is better.
    }

    @JsonIgnore
    @Override
    public String getValueString(NodeGameBase<?, C, S> nodeToEvaluate) {
        return String.valueOf(getValue(nodeToEvaluate));
    }

    @JsonIgnore
    @Override
    public EvaluationFunction_SqDistFromOther<C, S> getCopy() {
        return new EvaluationFunction_SqDistFromOther<>(comparisonState);
    }

    @JsonGetter("comparisonState")
    public S getComparisonState() {
        return comparisonState;
    }

    @Override
    public void close() {}
}
