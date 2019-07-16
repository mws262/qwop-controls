package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;
import value.IValueFunction;

/**
 * Meta-rollout policy which weights the rollout result with a straight-up evaluation of the value function at the
 * starting node.
 *
 * @author matt
 *
 */
public class RolloutPolicy_WeightWithValueFunction implements IRolloutPolicy {

    public float valueFunctionWeight = 0.8f;

    private final IRolloutPolicy individualRollout;

    private final IValueFunction valueFunction;

    public RolloutPolicy_WeightWithValueFunction(
            @JsonProperty("individualRollout") IRolloutPolicy individualRollout,
            @JsonProperty("valueFunction") IValueFunction valueFunction) {
        this.individualRollout = individualRollout;
        this.valueFunction = valueFunction;
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        float rolloutValue = individualRollout.rollout(startNode, game);

        return (1 - valueFunctionWeight) * rolloutValue + valueFunctionWeight * valueFunction.evaluate(startNode);
    }

    @Override
    public IRolloutPolicy getCopy() {
        RolloutPolicy_WeightWithValueFunction rolloutCopy =
                new RolloutPolicy_WeightWithValueFunction(individualRollout.getCopy(), valueFunction); // TODO decide
        // if it would be better to copy value function also.
        rolloutCopy.valueFunctionWeight = valueFunctionWeight;
        return rolloutCopy;
    }

    public IRolloutPolicy getIndividualRollout() {
        return individualRollout;
    }

    public IValueFunction getValueFunction() {
        return valueFunction;
    }

    @Override
    public void close() {
        valueFunction.close();
        individualRollout.close();
    }
}
