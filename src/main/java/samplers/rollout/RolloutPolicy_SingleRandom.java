package samplers.rollout;

import evaluators.IEvaluationFunction;
import game.IGame;
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
    public float rollout(Node startNode, IGame game) {
        actionQueue.clearAll();
        Node normalRolloutEndNode = randomRollout(startNode, game);

        return evaluationFunction.getValue(normalRolloutEndNode) - evaluationFunction.getValue(startNode);
    }

    @Override
    public RolloutPolicy getCopy() {
       return new RolloutPolicy_SingleRandom(evaluationFunction.getCopy());
    }
}
