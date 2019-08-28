package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class RolloutPolicy_DeltaScore<C extends Command<?>> extends RolloutPolicyBase<C> {

    /**
     * Reward can be reduced by a factor if failure results.
     */
    public float failureMultiplier = 1.0f;

    private final IController<C> rolloutController;

    public static final int defaultMaxTimesteps = Integer.MAX_VALUE;

   public RolloutPolicy_DeltaScore(IEvaluationFunction<C> evaluationFunction,
                                   IActionGenerator<C> rolloutActionGenerator,
                                   IController<C> rolloutController) {
       super(evaluationFunction, rolloutActionGenerator, defaultMaxTimesteps);
       this.rolloutController = rolloutController;
    }

    public RolloutPolicy_DeltaScore(@JsonProperty("evaluationFunction") IEvaluationFunction<C> evaluationFunction,
                                    @JsonProperty("rolloutActionGenerator") IActionGenerator<C> rolloutActionGenerator,
                                    @JsonProperty("getRolloutController") IController<C> rolloutController,
                                    @JsonProperty("maxTimesteps") int maxTimesteps) {
        super(evaluationFunction, rolloutActionGenerator, maxTimesteps);
        this.rolloutController = rolloutController;
    }

    @Override
    float startScore(NodeGameExplorableBase<?, C> startNode) {
        return -getEvaluationFunction().getValue(startNode);
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

    @JsonIgnore
    @Override
    public RolloutPolicy_DeltaScore<C> getCopy() {
       RolloutPolicy_DeltaScore<C> copy = new RolloutPolicy_DeltaScore<>(getEvaluationFunction().getCopy(),
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
