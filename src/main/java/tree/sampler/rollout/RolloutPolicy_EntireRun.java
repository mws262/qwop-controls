package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import controllers.IController;
import game.action.Command;
import game.action.IActionGenerator;
import game.state.IState;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorableBase;
import tree.node.evaluator.EvaluationFunction_Constant;


/**
 * This rollout policy is first scoring according to how close to the finish line it gets followed by the average
 * speed to get there. It has the potential flaw that it is not invariant to X position.
 *
 * @author matt
 */
public class RolloutPolicy_EntireRun<C extends Command<?>, S extends IState> extends RolloutPolicyBase<C, S> {

    private static final float distanceMultiplier = 0.5f;
    private static final float speedMultiplier = distanceMultiplier / 10f;
    private static final int maxTimesteps = 3000; // 2 minutes. This limit shouldn't be met, but I don't want the
    // game to get stuck infinitely either.

    private final IController<C, S> rolloutController;

    public RolloutPolicy_EntireRun(
            @JsonProperty("rolloutActionGenerator") IActionGenerator<C> rolloutActionGenerator,
            @JsonProperty("rolloutController") IController<C, S> rolloutController) {
        super(new EvaluationFunction_Constant<>(0f), rolloutActionGenerator, maxTimesteps);
        this.rolloutController = rolloutController;
    }

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
        return (endNode.getState().getCenterX()) * distanceMultiplier;
    }

    @Override
    float calculateFinalScore(float accumulatedValue, NodeGameExplorableBase<?, C, S> startNode,
                              NodeGameExplorableBase<?, C, S> endNode, int rolloutDurationTimesteps) {
        return accumulatedValue +
                (endNode.getState().getCenterX() - startNode.getState().getCenterX())
                        * speedMultiplier / Math.max(rolloutDurationTimesteps, 1f);
    }

    @Override
    public IController<C, S> getRolloutController() {
        return rolloutController;
    }

    @JsonIgnore
    @Override
    public RolloutPolicyBase<C, S> getCopy() {
        return new RolloutPolicy_EntireRun<>(rolloutActionGenerator, rolloutController.getCopy());
    }

    @Override
    public void close() {
        rolloutController.close();
    }
}
