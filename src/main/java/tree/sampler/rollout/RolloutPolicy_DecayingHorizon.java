package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.IController;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.IEvaluationFunction;

public class RolloutPolicy_DecayingHorizon extends RolloutPolicyBase {

    // Decaying horizon kernel parameters.
    // Kernel is an s-curve meant to be evaluated from 0 to 1 and producing values from 0 to 1.
    public static float kernelCenter = 0.5f; // Where the center of the s lies. Greater values shift the drop
    // til later, smaller values make the drop occur earlier.
    public static float kernelSteepness = 5; // Steepness of the drop. Higher values == more steep.

    public static final int defaultMaxTimesteps = 200;

    public final IController rolloutController;

    public RolloutPolicy_DecayingHorizon(@JsonProperty("evaluationFunction") IEvaluationFunction evaluationFunction,
                                         @JsonProperty("rolloutController") IController rolloutController,
                                         @JsonProperty("maxTimesteps") int maxTimesteps) {
        super(evaluationFunction, maxTimesteps);
        this.rolloutController = rolloutController;
    }

    public RolloutPolicy_DecayingHorizon(IEvaluationFunction evaluationFunction, IController rolloutController) {
        this(evaluationFunction, rolloutController, defaultMaxTimesteps);
    }

    float startScore(NodeQWOPExplorableBase<?> startNode) {
        return 0; // -evaluationFunction.getValue(startNode);
    }

    float accumulateScore(int timestepSinceRolloutStart, NodeQWOPBase<?> before, NodeQWOPBase<?> after) {
        float multiplier = getKernelMultiplier(
                timestepSinceRolloutStart / (float) (maxTimesteps - 1));

        return multiplier * (getEvaluationFunction().getValue(after) - getEvaluationFunction().getValue(before));
    }

    float endScore(NodeQWOPExplorableBase<?> endNode) {
        return 0; // evaluationFunction.getValue(endNode);
    }

    float calculateFinalScore(float accumulatedValue, NodeQWOPExplorableBase<?> startNode,
                        NodeQWOPExplorableBase<?> endNode) {
        return accumulatedValue;
    }

    @Override
    @JsonIgnore
    public IController getController() {
        return rolloutController;
    }

    @JsonIgnore
    @Override
    public RolloutPolicyBase getCopy() {
        return new RolloutPolicy_DecayingHorizon(getEvaluationFunction().getCopy(), rolloutController.getCopy(), maxTimesteps);
    }

    float getKernelMultiplier(float normalizedTimesteps) {
        assert normalizedTimesteps <= 1f;
        assert normalizedTimesteps >= 0f;
        return (float) (-0.5 * Math.tanh(kernelSteepness * (normalizedTimesteps - kernelCenter)) + 0.5);
    }
}
