package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.Controller_Random;
import tree.node.evaluator.IEvaluationFunction;
import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;

/**
 * Rollout policy which does a normal random rollout followed by another one, but with the physics engine cold
 * started at the node's state. We're trying to garner some robustness against the internal contact solver parameters.
 *
 * @author matt
 */
public class RolloutPolicy_RandomColdStart extends RolloutPolicy_DeltaScore {

    public RolloutPolicy_RandomColdStart(@JsonProperty("evaluationFunction") IEvaluationFunction evaluationFunction) {
        super(evaluationFunction, new Controller_Random());
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        float normalScore = super.rollout(startNode, game);
        coldStartGameToNode(startNode, game);
        float coldStartScore = super.rollout(startNode, game);
        return (normalScore + coldStartScore) / 2f;
    }
    @Override
    public RolloutPolicy_DeltaScore getCopy() {
        return new RolloutPolicy_RandomColdStart(getEvaluationFunction().getCopy());
    }
}
