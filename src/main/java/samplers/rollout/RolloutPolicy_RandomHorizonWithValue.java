package samplers.rollout;

import game.IGameInternal;
import tree.NodeQWOPExplorableBase;
import value.IValueFunction;

public class RolloutPolicy_RandomHorizonWithValue extends RolloutPolicy_RandomDecayingHorizon {

    private IValueFunction valueFunction;
    public float valueFunctionWeight = 0.5f;

    public RolloutPolicy_RandomHorizonWithValue(IValueFunction valueFunction) {
        this.valueFunction = valueFunction;
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        float rolloutScore = super.rollout(startNode, game);
        return (1 - valueFunctionWeight) * rolloutScore + valueFunctionWeight * valueFunction.evaluate(startNode);
    }
}
