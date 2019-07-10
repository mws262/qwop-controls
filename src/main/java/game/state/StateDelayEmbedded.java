package game.state;

import java.util.Arrays;

public abstract class StateDelayEmbedded implements IState {

    public State[] individualStates;

    final int stateVariableCount;

    // Order is NEWEST to OLDEST
    public StateDelayEmbedded(State[] states) {
        individualStates = Arrays.copyOf(states, states.length);
        // Configurations variables (3), number of individual States making up this composite, number of body parts.
        stateVariableCount = individualStates.length * individualStates[0].getStateVariableCount();
    }

    // Just applies to the most recent state for now.
    @Override
    public StateVariable getStateVariableFromName(ObjectName obj) {
        return individualStates[0].getStateVariableFromName(obj);
    }

    @Override
    public StateVariable[] getAllStateVariables() {
        return individualStates[0].getAllStateVariables();
    }

    @Override
    public float getCenterX() {
        return individualStates[0].getCenterX();
    }

    @Override
    public boolean isFailed() {
        return individualStates[0].isFailed();
    }

    @Override
    public int getStateVariableCount() {
        return stateVariableCount;
    }
}
