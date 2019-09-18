package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameInternal;
import game.action.Command;
import game.state.IState;
import org.jetbrains.annotations.NotNull;
import tree.node.NodeGameExplorableBase;
import value.IValueFunction;

/**
 * Meta-rollout policy which weights the rollout result with a straight-up evaluation of the value function at the
 * starting node.
 *
 * @author matt
 *
 */
public class RolloutPolicy_WeightWithValueFunction<C extends Command<?>, S extends IState> implements IRolloutPolicy<C, S> {

    public float valueFunctionWeight = 0.8f;

    private final IRolloutPolicy<C, S> individualRollout;

    private final IValueFunction<C, S> valueFunction;

    public RolloutPolicy_WeightWithValueFunction(
            @JsonProperty("individualRollout") IRolloutPolicy<C, S> individualRollout,
            @JsonProperty("valueFunction") IValueFunction<C, S> valueFunction) {
        this.individualRollout = individualRollout;
        this.valueFunction = valueFunction;
    }

    @Override
    public float rollout(@NotNull NodeGameExplorableBase<?, C, S> startNode, IGameInternal<C, S> game) {
        float rolloutValue = individualRollout.rollout(startNode, game);

        return (1 - valueFunctionWeight) * rolloutValue + valueFunctionWeight * valueFunction.evaluate(startNode);
    }

    @Override
    public IRolloutPolicy<C, S> getCopy() {
        RolloutPolicy_WeightWithValueFunction<C, S> rolloutCopy =
                new RolloutPolicy_WeightWithValueFunction<>(individualRollout.getCopy(), valueFunction); // TODO decide
        // if it would be better to copy value function also.
        rolloutCopy.valueFunctionWeight = valueFunctionWeight;
        return rolloutCopy;
    }

    public IRolloutPolicy<C, S> getIndividualRollout() {
        return individualRollout;
    }

    public IValueFunction<C, S> getValueFunction() {
        return valueFunction;
    }

    @Override
    public void close() {
        valueFunction.close();
        individualRollout.close();
    }
}
