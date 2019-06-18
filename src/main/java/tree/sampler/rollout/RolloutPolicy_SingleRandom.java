package tree.sampler.rollout;

import evaluators.IEvaluationFunction;
import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;

/**
 * Most basic rollout policy. Just randomly picks game.actions until failure. This is how {@link tree.sampler.Sampler_UCB} was
 * hardcoded for most of its life.
 *
 * @author matt
 */
public class RolloutPolicy_SingleRandom extends RolloutPolicy {


    int maxTimesteps = 100;

   public RolloutPolicy_SingleRandom(IEvaluationFunction evaluationFunction) {
        super(evaluationFunction);
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        actionQueue.clearAll();
        NodeQWOPExplorableBase<?> normalRolloutEndNode = randomRollout(startNode, game, maxTimesteps);

        float multiplier = 1f;
        if (normalRolloutEndNode.getState().isFailed()) {
            multiplier = 0.75f;
        }
        return multiplier * (evaluationFunction.getValue(normalRolloutEndNode) - evaluationFunction.getValue(startNode));
    }

    @Override
    public RolloutPolicy getCopy() {
       return new RolloutPolicy_SingleRandom(evaluationFunction.getCopy());
    }
}
