package tree.sampler.rollout;

import game.action.Action;
import game.state.IState;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.IEvaluationFunction;

public class RolloutPolicy_DecayingHorizonRandom extends RolloutPolicy_DecayingHorizon {

    public RolloutPolicy_DecayingHorizonRandom(IEvaluationFunction evaluationFunction) {
        super(evaluationFunction);
    }

    @Override
    float calculateFinalValue(float accumulatedValue, NodeQWOPExplorableBase<?> startNode) {
        return accumulatedValue;
    }

    @Override
    NodeQWOPExplorableBase<?> addNextRolloutNode(NodeQWOPExplorableBase<?> currentNode, Action action, IState state) {
        return currentNode.addBackwardsLinkedChild(action, state, rolloutActionGenerator);
    }

    @Override
    Action getNextAction(NodeQWOPExplorableBase<?> currentNode) {
        return currentNode.getUntriedActionRandom();
    }

    @Override
    public RolloutPolicy getCopy() {
        return new RolloutPolicy_DecayingHorizonRandom(evaluationFunction.getCopy());
    }
}
