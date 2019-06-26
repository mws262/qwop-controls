package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.IController;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.IEvaluationFunction;

/**
 * Most basic rollout policy. Just randomly picks game.action until failure. This is how {@link tree.sampler.Sampler_UCB} was
 * hardcoded for most of its life.
 *
 * @author matt
 */
public class RolloutPolicy_EndScore extends RolloutPolicyBase {

    /**
     * Reward can be reduced by a factor if failure results.
     */
    public float failureMultiplier = 1.0f;

    private IController rolloutController;

    public static final int defaultMaxTimesteps = Integer.MAX_VALUE;

   public RolloutPolicy_EndScore(IEvaluationFunction evaluationFunction, IController rolloutController) {
       super(evaluationFunction, defaultMaxTimesteps);
       this.rolloutController = rolloutController;
    }

    public RolloutPolicy_EndScore(@JsonProperty("evaluationFunction") IEvaluationFunction evaluationFunction,
                                  @JsonProperty("controller") IController rolloutController,
                                  @JsonProperty("maxTimesteps") int maxTimesteps) {
        super(evaluationFunction, maxTimesteps);
        this.rolloutController = rolloutController;
    }

    // Only difference between this and delta score. The initial evaluation is not subtracted from the end evaluation.
    @Override
    float startScore(NodeQWOPExplorableBase<?> startNode) {
        return 0;
    }

    @Override
    float accumulateScore(int timestepSinceRolloutStart, NodeQWOPBase<?> before, NodeQWOPBase<?> after) {
        return 0;
    }

    @Override
    float endScore(NodeQWOPExplorableBase<?> endNode) {
        return getEvaluationFunction().getValue(endNode);
    }

    @Override
    float calculateFinalScore(float accumulatedValue, NodeQWOPExplorableBase<?> startNode, NodeQWOPExplorableBase<?> endNode) {
        return (endNode.getState().isFailed() ? failureMultiplier : 1.0f) * accumulatedValue;
    }

    @Override
    public IController getController() {
        return rolloutController;
    }

    @Override
    public RolloutPolicy_EndScore getCopy() {
       RolloutPolicy_EndScore copy = new RolloutPolicy_EndScore(getEvaluationFunction().getCopy(),
               getController().getCopy(), maxTimesteps);
       copy.failureMultiplier = failureMultiplier;
       return copy;
    }
}
