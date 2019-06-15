package game;

import java.util.Arrays;

public class StateDelayEmbedded implements IState {

    private IState[] individualStates;

    private final int stateVariableCount;

    // Order is NEWEST to OLDEST
    public StateDelayEmbedded(IState[] states) {
        individualStates = Arrays.copyOf(states, states.length);
        // Configurations variables (3), number of individual States making up this composite, number of body parts.
        stateVariableCount = individualStates.length * individualStates[0].getStateVariableCount();
    }

    @Override
    public float[] flattenState() {

        float[] flatState = new float[3 * stateVariableCount];

        // Subtract out the current body x from all other x coordinates, even the time delayed ones.
        float currentBodyX = individualStates[0].getCenterX();

        int idx = 0;
        for (IState state : individualStates) {
            StateVariable[] stateVariables = state.getAllStateVariables();

            for (StateVariable stateVariable : stateVariables) {
                // So far, just position coordinates. May want to implement first, second difference stuff.
                flatState[idx++] = stateVariable.getX() - currentBodyX;
                flatState[idx++] = stateVariable.getY();
                flatState[idx++] = stateVariable.getTh();
            }
        }
        return flatState;
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
