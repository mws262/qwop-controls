package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.IController;
import game.action.Command;
import game.action.IActionGenerator;
import game.state.IState;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorableBase;
import tree.node.evaluator.IEvaluationFunction;

<<<<<<< HEAD
/**
 * Most basic rollout policy. Just randomly picks game.command until failure. This is how {@link tree.sampler.Sampler_UCB} was
 * hardcoded for most of its life.
 *
 * @author matt
 */
=======
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
public class RolloutPolicy_EndScore<C extends Command<?>, S extends IState> extends RolloutPolicyBase<C, S> {

    /**
     * Reward can be reduced by a factor if failure results.
     */
    public float failureMultiplier = 1.0f;

    private IController<C, S> rolloutController;

    private static final int defaultMaxTimesteps = Integer.MAX_VALUE;

    public RolloutPolicy_EndScore(IEvaluationFunction<C, S> evaluationFunction,
                                  IActionGenerator<C> rolloutActionGenerator,
                                  IController<C, S> rolloutController) {
        super(evaluationFunction, rolloutActionGenerator, defaultMaxTimesteps);
        this.rolloutController = rolloutController;
    }

    public RolloutPolicy_EndScore(@JsonProperty("evaluationFunction") IEvaluationFunction<C, S> evaluationFunction,
                                  @JsonProperty("rolloutActionGenerator") IActionGenerator<C> rolloutActionGenerator,
                                  @JsonProperty("rolloutController") IController<C, S> rolloutController,
                                  @JsonProperty("maxTimesteps") int maxTimesteps) {
        super(evaluationFunction, rolloutActionGenerator, maxTimesteps);
        this.rolloutController = rolloutController;
    }

    // Only difference between this and delta score. The initial evaluation is not subtracted from the end evaluation.
    @Override
    float startScore(NodeGameExplorableBase<?, C, S> startNode) {
        return 0;
    }

    @Override
    float accumulateScore(int timestepSinceRolloutStart, NodeGameBase<?, C, S> before, NodeGameBase<?, C, S> after) {
        return 0;
    }

    @Override
    float endScore(NodeGameExplorableBase<?, C, S> endNode) {
        return getEvaluationFunction().getValue(endNode);
    }

    @Override
    float calculateFinalScore(float accumulatedValue, NodeGameExplorableBase<?, C, S> startNode,
                              NodeGameExplorableBase<?, C, S> endNode, int rolloutDurationTimesteps) {
        return (endNode.getState().isFailed() ? failureMultiplier : 1.0f) * accumulatedValue;
    }

    @Override
    public IController<C, S> getRolloutController() {
        return rolloutController;
    }

    @Override
    public RolloutPolicy_EndScore<C, S> getCopy() {
        RolloutPolicy_EndScore<C, S> copy = new RolloutPolicy_EndScore<>(getEvaluationFunction().getCopy(),
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
