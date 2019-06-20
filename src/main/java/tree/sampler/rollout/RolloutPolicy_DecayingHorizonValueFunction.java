package tree.sampler.rollout;

import game.action.Action;
import game.state.IState;
import tree.node.NodeQWOPExplorableBase;
import value.ValueFunction_TensorFlow_StateOnly;

public class RolloutPolicy_DecayingHorizonValueFunction extends RolloutPolicy_DecayingHorizon {

    /**
     * Value function controller used to execute the rollouts.
     */
    private final ValueFunction_TensorFlow_StateOnly valFun;

    public RolloutPolicy_DecayingHorizonValueFunction(ValueFunction_TensorFlow_StateOnly valFun) {
        super(null);
        this.valFun = valFun;
    }

    @Override
    float calculateFinalValue(float accumulatedValue, NodeQWOPExplorableBase<?> startNode) {
        return accumulatedValue;
    }

    @Override
    NodeQWOPExplorableBase<?> addNextRolloutNode(NodeQWOPExplorableBase<?> currentNode, Action action, IState state) {
        return currentNode.addBackwardsLinkedChild(action, state);
    }

    @Override
    Action getNextAction(NodeQWOPExplorableBase<?> currentNode) {
        return valFun.getMaximizingAction(currentNode);
    }

    @Override
    public RolloutPolicy getCopy() {
        RolloutPolicy_DecayingHorizonValueFunction rolloutCopy =
                new RolloutPolicy_DecayingHorizonValueFunction(valFun.getCopy());
        rolloutCopy.maxTimestepsToSim = this.maxTimestepsToSim;
        return rolloutCopy;
    }
}
