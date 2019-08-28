package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.IController;
import game.action.Command;
import game.action.IActionGenerator;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorableBase;
import tree.node.evaluator.IEvaluationFunction;

/**
 * Most basic rollout policy. Just randomly picks game.command until failure. This is how {@link tree.sampler.Sampler_UCB} was
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

    public RolloutPolicy_EndScore(IEvaluationFunction<C> evaluationFunction,
                                  IActionGenerator<C> rolloutActionGenerator,
                                  IController<C> rolloutController) {
        super(evaluationFunction, rolloutActionGenerator, defaultMaxTimesteps);
        this.rolloutController = rolloutController;
    }

    public RolloutPolicy_EndScore(@JsonProperty("evaluationFunction") IEvaluationFunction<C> evaluationFunction,
                                  @JsonProperty("rolloutActionGenerator") IActionGenerator<C> rolloutActionGenerator,
                                  @JsonProperty("rolloutController") IController<C> rolloutController,
                                  @JsonProperty("maxTimesteps") int maxTimesteps) {
        super(evaluationFunction, rolloutActionGenerator, maxTimesteps);
        this.rolloutController = rolloutController;
    }

    // Only difference between this and delta score. The initial evaluation is not subtracted from the end evaluation.
    @Override
    float startScore(NodeGameExplorableBase<?, C> startNode) {
        return 0;
    }

    @Override
    float accumulateScore(int timestepSinceRolloutStart, NodeGameBase<?, C> before, NodeGameBase<?, C> after) {
        return 0;
    }

    @Override
    float endScore(NodeGameExplorableBase<?, C> endNode) {
        return getEvaluationFunction().getValue(endNode);
    }

    @Override
    float calculateFinalScore(float accumulatedValue, NodeGameExplorableBase<?, C> startNode,
                              NodeGameExplorableBase<?, C> endNode, int rolloutDurationTimesteps) {
        return (endNode.getState().isFailed() ? failureMultiplier : 1.0f) * accumulatedValue;
    }

    @Override
    public IController<C> getRolloutController() {
        return rolloutController;
    }

    @Override
    public RolloutPolicy_EndScore<C> getCopy() {
        RolloutPolicy_EndScore<C> copy = new RolloutPolicy_EndScore<>(getEvaluationFunction().getCopy(),
                rolloutActionGenerator,
                getRolloutController().getCopy(),
                maxTimesteps);
        copy.failureMultiplier = failureMultiplier;
        return copy;
    }

    @Override
    public void close() {
        evaluationFunction.close();
        rolloutController.close();
    }
}
