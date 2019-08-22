package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.IController;
import game.action.Command;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.IEvaluationFunction;

/**
 * Most basic rollout policy. Just randomly picks game.action until failure. This is how {@link tree.sampler.Sampler_UCB} was
 * hardcoded for most of its life.
 *
 * @author matt
 */
public class RolloutPolicy_EndScore<C extends Command<?>> extends RolloutPolicyBase<C> {

    /**
     * Reward can be reduced by a factor if failure results.
     */
    public float failureMultiplier = 1.0f;

    private IController<C> rolloutController;

    private static final int defaultMaxTimesteps = Integer.MAX_VALUE;

    public RolloutPolicy_EndScore(IEvaluationFunction evaluationFunction, IController<C> rolloutController) {
        super(evaluationFunction, defaultMaxTimesteps);
        this.rolloutController = rolloutController;
    }

    public RolloutPolicy_EndScore(@JsonProperty("evaluationFunction") IEvaluationFunction evaluationFunction,
                                  @JsonProperty("rolloutController") IController<C> rolloutController,
                                  @JsonProperty("maxTimesteps") int maxTimesteps) {
        super(evaluationFunction, maxTimesteps);
        this.rolloutController = rolloutController;
    }

    // Only difference between this and delta score. The initial evaluation is not subtracted from the end evaluation.
    @Override
    float startScore(NodeQWOPExplorableBase<?, C> startNode) {
        return 0;
    }

    @Override
    float accumulateScore(int timestepSinceRolloutStart, NodeQWOPBase<?, C> before, NodeQWOPBase<?, C> after) {
        return 0;
    }

    @Override
    float endScore(NodeQWOPExplorableBase<?, C> endNode) {
        return getEvaluationFunction().getValue(endNode);
    }

    @Override
    float calculateFinalScore(float accumulatedValue, NodeQWOPExplorableBase<?, C> startNode,
                              NodeQWOPExplorableBase<?, C> endNode, int rolloutDurationTimesteps) {
        return (endNode.getState().isFailed() ? failureMultiplier : 1.0f) * accumulatedValue;
    }

    @Override
    public IController<C> getRolloutController() {
        return rolloutController;
    }

    @Override
    public RolloutPolicy_EndScore<C> getCopy() {
        RolloutPolicy_EndScore<C> copy = new RolloutPolicy_EndScore<>(getEvaluationFunction().getCopy(),
                getRolloutController().getCopy(), maxTimesteps);
        copy.failureMultiplier = failureMultiplier;
        return copy;
    }

    @Override
    public void close() {
        evaluationFunction.close();
        rolloutController.close();
    }
}
