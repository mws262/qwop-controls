package tree.sampler.rollout;

import tree.node.evaluator.IEvaluationFunction;
import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;

/**
 * Most basic rollout policy. Just randomly picks game.action until failure. This is how {@link tree.sampler.Sampler_UCB} was
 * hardcoded for most of its life.
 *
 * @author matt
 */
public class RolloutPolicy_SingleRandom extends RolloutPolicy {

    /**
     * Maximum forward timesteps performed in a single rollout regardless of failure.
     */
    public int maxTimesteps = 100;

    /**
     * Reward can be reduced by a factor if failure results.
     */
    public float failureMultiplier = 1.0f;

   public RolloutPolicy_SingleRandom(IEvaluationFunction evaluationFunction) {
        super(evaluationFunction);
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        actionQueue.clearAll();
        NodeQWOPExplorableBase<?> normalRolloutEndNode = randomRollout(startNode, game, maxTimesteps);

        return (normalRolloutEndNode.getState().isFailed() ? failureMultiplier : 1.0f)
                * (evaluationFunction.getValue(normalRolloutEndNode) - evaluationFunction.getValue(startNode));
    }

    @Override
    public RolloutPolicy_SingleRandom getCopy() {
       RolloutPolicy_SingleRandom copy = new RolloutPolicy_SingleRandom(evaluationFunction.getCopy());
       copy.maxTimesteps = maxTimesteps;
       copy.failureMultiplier = failureMultiplier;
       return copy;
    }
}
