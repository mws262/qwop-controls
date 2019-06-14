package samplers.rollout;

import actions.Action;
import game.IState;
import tree.NodeQWOPExplorableBase;
import value.ValueFunction_TensorFlow_StateOnly;

public class RolloutPolicy_ValueFunctionDecayingHorizon extends RolloutPolicy_DecayingHorizon {

    /**
     * Value function controller used to execute the rollouts.
     */
    private final ValueFunction_TensorFlow_StateOnly valFun;

    public RolloutPolicy_ValueFunctionDecayingHorizon(ValueFunction_TensorFlow_StateOnly valFun) {
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
        RolloutPolicy_ValueFunctionDecayingHorizon rolloutCopy =
                new RolloutPolicy_ValueFunctionDecayingHorizon(valFun.getCopy());
        rolloutCopy.maxTimestepsToSim = this.maxTimestepsToSim;
        return rolloutCopy;
    }
}
