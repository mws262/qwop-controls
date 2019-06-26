package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class RolloutPolicy_DeltaScore extends RolloutPolicyBase {

    /**
     * Reward can be reduced by a factor if failure results.
     */
    public float failureMultiplier = 1.0f;

    private final IController rolloutController;

    public static final int defaultMaxTimesteps = Integer.MAX_VALUE;

   public RolloutPolicy_DeltaScore(IEvaluationFunction evaluationFunction,
                                   IController rolloutController) {
       super(evaluationFunction, defaultMaxTimesteps);
       this.rolloutController = rolloutController;
    }

    public RolloutPolicy_DeltaScore(@JsonProperty("evaluationFunction") IEvaluationFunction evaluationFunction,
                                    @JsonProperty("controller") IController rolloutController,
                                    @JsonProperty("maxTimesteps") int maxTimesteps) {
        super(evaluationFunction, maxTimesteps);
        this.rolloutController = rolloutController;
    }

    @Override
    float startScore(NodeQWOPExplorableBase<?> startNode) {
        return -getEvaluationFunction().getValue(startNode);
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

    @JsonIgnore
    @Override
    public RolloutPolicy_DeltaScore getCopy() {
       RolloutPolicy_DeltaScore copy = new RolloutPolicy_DeltaScore(getEvaluationFunction().getCopy(),
               getController().getCopy(), maxTimesteps);
       copy.failureMultiplier = failureMultiplier;
       return copy;
    }
}
