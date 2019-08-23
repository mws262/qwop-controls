package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.Controller_Random;
import game.action.Command;
import game.action.IActionGenerator;
import org.jetbrains.annotations.NotNull;
import tree.node.evaluator.IEvaluationFunction;
import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;

/**
 * Rollout policy which does a normal random rollout followed by another one, but with the physics engine cold
 * started at the node's state. We're trying to garner some robustness against the internal contact solver parameters.
 *
 * @author matt
 */
public class RolloutPolicy_RandomColdStart<C extends Command<?>> extends RolloutPolicy_DeltaScore<C> {

    public RolloutPolicy_RandomColdStart(@JsonProperty("evaluationFunction") IEvaluationFunction<C> evaluationFunction,
                                         @JsonProperty("rolloutActionGenerator") IActionGenerator<C> rolloutActionGenerator) {
        super(evaluationFunction, rolloutActionGenerator, new Controller_Random<>());
    }

    @Override
    public float rollout(@NotNull NodeQWOPExplorableBase<?, C> startNode, @NotNull IGameInternal<C> game) {
        float normalScore = super.rollout(startNode, game);
        coldStartGameToNode(startNode, game);
        float coldStartScore = super.rollout(startNode, game);
        return (normalScore + coldStartScore) / 2f;
    }
    @Override
    public RolloutPolicy_DeltaScore<C> getCopy() {
        return new RolloutPolicy_RandomColdStart<>(getEvaluationFunction().getCopy(),
                rolloutActionGenerator);
    }
}
