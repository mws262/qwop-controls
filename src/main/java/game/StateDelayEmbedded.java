package game;

import java.util.Arrays;

public class StateDelayEmbedded implements IState {

    private State[] individualStates;

    // Order is NEWEST to OLDEST
    public StateDelayEmbedded(State[] states) {
        individualStates = Arrays.copyOf(states, states.length);
    }

    @Override
    public float[] flattenState() {

        float[] flatState = new float[3 * individualStates.length * individualStates[0].stateVariables.length];

        // Subtract out the current body x from all other x coordinates, even the time delayed ones.
        float currentBodyX = individualStates[0].body.getX();

        int idx = 0;
        for (State state : individualStates) {
            StateVariable[] stateVariables = state.stateVariables;

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
        return individualStates[0].body.getX();
    }

    @Override
    public boolean isFailed() {
        return individualStates[0].isFailed();
    }
}
