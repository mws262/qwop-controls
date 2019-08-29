package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.IController;
import game.action.Command;
import game.action.IActionGenerator;
import game.state.IState;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorableBase;
import tree.node.evaluator.IEvaluationFunction;

public class RolloutPolicy_DecayingHorizon<C extends Command<?>, S extends IState> extends RolloutPolicyBase<C, S> {

    // Decaying horizon kernel parameters.
    // Kernel is an s-curve meant to be evaluated from 0 to 1 and producing values from 0 to 1.
    public static float kernelCenter = 0.5f; // Where the center of the s lies. Greater values shift the drop
    // til later, smaller values make the drop occur earlier.
    public static float kernelSteepness = 5; // Steepness of the drop. Higher values == more steep.

    public static final int defaultMaxTimesteps = 200;

    public final IController<C, S> rolloutController;

    public RolloutPolicy_DecayingHorizon(@JsonProperty("evaluationFunction") IEvaluationFunction<C, S> evaluationFunction,
                                         @JsonProperty("rolloutActionGenerator") IActionGenerator<C> rolloutActionGenerator,
                                         @JsonProperty("rolloutController") IController<C, S> rolloutController,
                                         @JsonProperty("maxTimesteps") int maxTimesteps) {
        super(evaluationFunction, rolloutActionGenerator, maxTimesteps);
        this.rolloutController = rolloutController;
    }

    public RolloutPolicy_DecayingHorizon(IEvaluationFunction<C, S> evaluationFunction,
                                         IActionGenerator<C> rolloutActionGenerator,
                                         IController<C, S> rolloutController) {
        this(evaluationFunction, rolloutActionGenerator, rolloutController, defaultMaxTimesteps);
    }

    float startScore(NodeGameExplorableBase<?, C, S> startNode) {
        return 0; // -evaluationFunction.getValue(startNode);
    }

    float accumulateScore(int timestepSinceRolloutStart, NodeGameBase<?, C, S> before, NodeGameBase<?, C, S> after) {
        float multiplier = getKernelMultiplier(
                timestepSinceRolloutStart / (float) (maxTimesteps - 1));

        return multiplier * (getEvaluationFunction().getValue(after) - getEvaluationFunction().getValue(before));
    }

    float endScore(NodeGameExplorableBase<?, C, S> endNode) {
        return 0; // evaluationFunction.getValue(endNode);
    }

    float calculateFinalScore(float accumulatedValue, NodeGameExplorableBase<?, C, S> startNode,
                              NodeGameExplorableBase<?, C, S> endNode, int rolloutDurationTimesteps) {
        return accumulatedValue;
    }

    @Override
    @JsonIgnore
    public IController<C, S> getRolloutController() {
        return rolloutController;
    }

    @JsonIgnore
    @Override
    public RolloutPolicyBase<C, S> getCopy() {
        return new RolloutPolicy_DecayingHorizon<>(getEvaluationFunction().getCopy(),
                rolloutActionGenerator,
                rolloutController.getCopy(),
                maxTimesteps);
    }

    float getKernelMultiplier(float normalizedTimesteps) {
        assert normalizedTimesteps <= 1f;
        assert normalizedTimesteps >= 0f;
        return (float) (-0.5 * Math.tanh(kernelSteepness * (normalizedTimesteps - kernelCenter)) + 0.5);
    }

    @Override
    public void close() {
        evaluationFunction.close();
        rolloutController.close();
    }
}
