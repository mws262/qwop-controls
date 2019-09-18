package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameInternal;
import game.action.Command;
import game.state.IState;
import org.jetbrains.annotations.NotNull;
import tree.node.NodeGameExplorableBase;
import tree.node.evaluator.IEvaluationFunction;

/**
 * Rollout policy which just evaluates the node given, rather than actually doing any additional investigation.
 * Mostly useful as a sanity check.
 */
public class RolloutPolicy_JustEvaluate<C extends Command<?>, S extends IState> implements IRolloutPolicy<C, S> {

    public final IEvaluationFunction<C, S> evaluationFunction;

    @JsonCreator
    public RolloutPolicy_JustEvaluate(@JsonProperty("evaluationFunction") IEvaluationFunction<C, S> evaluationFunction) {
        this.evaluationFunction = evaluationFunction;
    }

    @Override
    public float rollout(@NotNull NodeGameExplorableBase<?, C, S> startNode, IGameInternal<C, S> game) {
        return evaluationFunction.getValue(startNode);
    }

    @Override
    public IRolloutPolicy<C, S> getCopy() {
        return new RolloutPolicy_JustEvaluate<>(evaluationFunction);
    }


    @Override
    public void close() {
        evaluationFunction.close();
    }
}
