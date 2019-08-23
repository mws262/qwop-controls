package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameInternal;
import game.action.Command;
import org.jetbrains.annotations.NotNull;
import tree.node.NodeQWOPExplorableBase;
import value.IValueFunction;

/**
 * Meta-rollout policy which weights the rollout result with a straight-up evaluation of the value function at the
 * starting node.
 *
 * @author matt
 *
 */
public class RolloutPolicy_WeightWithValueFunction<C extends Command<?>> implements IRolloutPolicy<C> {

    public float valueFunctionWeight = 0.8f;

    private final IRolloutPolicy<C> individualRollout;

    private final IValueFunction<C> valueFunction;

    public RolloutPolicy_WeightWithValueFunction(
            @JsonProperty("individualRollout") IRolloutPolicy<C> individualRollout,
            @JsonProperty("valueFunction") IValueFunction<C> valueFunction) {
        this.individualRollout = individualRollout;
        this.valueFunction = valueFunction;
    }

    @Override
    public float rollout(@NotNull NodeQWOPExplorableBase<?, C> startNode, IGameInternal<C> game) {
        float rolloutValue = individualRollout.rollout(startNode, game);

        return (1 - valueFunctionWeight) * rolloutValue + valueFunctionWeight * valueFunction.evaluate(startNode);
    }

    @Override
    public IRolloutPolicy<C> getCopy() {
        RolloutPolicy_WeightWithValueFunction<C> rolloutCopy =
                new RolloutPolicy_WeightWithValueFunction<>(individualRollout.getCopy(), valueFunction); // TODO decide
        // if it would be better to copy value function also.
        rolloutCopy.valueFunctionWeight = valueFunctionWeight;
        return rolloutCopy;
    }

    public IRolloutPolicy<C> getIndividualRollout() {
        return individualRollout;
    }

    public IValueFunction<C> getValueFunction() {
        return valueFunction;
    }

    @Override
    public void close() {
        valueFunction.close();
        individualRollout.close();
    }
}
