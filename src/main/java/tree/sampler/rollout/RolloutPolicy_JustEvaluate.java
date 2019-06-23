package tree.sampler.rollout;

import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.IEvaluationFunction;

/**
 * Rollout policy which just evaluates the node given, rather than actually doing any additional investigation.
 * Mostly useful as a sanity check.
 */
public class RolloutPolicy_JustEvaluate implements IRolloutPolicy {

    private final IEvaluationFunction evaluationFunction;

    public RolloutPolicy_JustEvaluate(IEvaluationFunction evaluationFunction) {
        this.evaluationFunction = evaluationFunction;
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        return evaluationFunction.getValue(startNode);
    }

    @Override
    public IRolloutPolicy getCopy() {
        return new RolloutPolicy_JustEvaluate(evaluationFunction);
    }
}
