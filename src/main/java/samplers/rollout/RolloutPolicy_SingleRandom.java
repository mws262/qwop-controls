package samplers.rollout;

import actions.Action;
import evaluators.IEvaluationFunction;
import game.GameLoader;
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
    public float rollout(Node startNode, GameLoader game) {
        actionQueue.clearAll();
        Node normalRolloutEndNode = randomRollout(startNode, game);

        return evaluationFunction.getValue(normalRolloutEndNode);
    }
}
