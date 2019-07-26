package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.IController;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.EvaluationFunction_Constant;


/**
 * This rollout policy is first scoring according to how close to the finish line it gets followed by the average
 * speed to get there. It has the potential flaw that it is not invariant to X position.
 *
 * @author matt
 */
public class RolloutPolicy_EntireRun extends RolloutPolicyBase {

    private static final float distanceMultiplier = 0.5f;
    private static final float speedMultiplier = distanceMultiplier / 10f;
    private static final int maxTimesteps = 3000; // 2 minutes. This limit shouldn't be met, but I don't want the
    // game to get stuck infinitely either.

    private final IController rolloutController;

    public RolloutPolicy_EntireRun(@JsonProperty("rolloutController") IController rolloutController) {
        super(new EvaluationFunction_Constant(0f), maxTimesteps);
        this.rolloutController = rolloutController;
    }

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
        return (endNode.getState().getCenterX()) * distanceMultiplier;
    }

    @Override
    float calculateFinalScore(float accumulatedValue, NodeQWOPExplorableBase<?> startNode,
                              NodeQWOPExplorableBase<?> endNode, int rolloutDurationTimesteps) {
        return accumulatedValue +
                (endNode.getState().getCenterX() - startNode.getState().getCenterX()) * speedMultiplier / Math.max(rolloutDurationTimesteps, 1f);
    }

    @Override
    public IController getRolloutController() {
        return rolloutController;
    }

    @JsonIgnore
    @Override
    public RolloutPolicyBase getCopy() {
        return new RolloutPolicy_EntireRun(rolloutController.getCopy());
    }

    @Override
    public void close() {
        rolloutController.close();
    }
}
