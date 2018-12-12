package samplers.rollout;

import evaluators.IEvaluationFunction;
import game.GameThreadSafe;
import tree.Node;

/**
 * Most basic rollout policy. Just randomly picks actions until failure. This is how {@link samplers.Sampler_UCB} was
 * hardcoded for most of its life.
 *
 * @author matt
 */
public class RolloutPolicy_SingleRandom extends RolloutPolicy {


   public RolloutPolicy_SingleRandom(IEvaluationFunction evaluationFunction) {
        super(evaluationFunction);
    }

    @Override
    public float rollout(Node startNode, GameThreadSafe game) {
        actionQueue.clearAll();
        Node normalRolloutEndNode = randomRollout(startNode, game);

        return evaluationFunction.getValue(normalRolloutEndNode);
    }
}
