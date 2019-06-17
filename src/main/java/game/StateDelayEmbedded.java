package game;

import data.LoadStateStatistics;

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

//        // Subtract out the current body x from all other x coordinates, even the time delayed ones.
//        float currentBodyX = individualStates[0].getCenterX();

//        int idx = 0;
//        for (IState state : individualStates) {
//            StateVariable[] stateVariables = state.getAllStateVariables();
//
//            for (StateVariable stateVariable : stateVariables) {
//                // So far, just position coordinates. May want to implement first, second difference stuff.
//                flatState[idx++] = stateVariable.getX() - currentBodyX;
//                flatState[idx++] = stateVariable.getY();
//                flatState[idx++] = stateVariable.getTh();
//            }
//        }

        int idx = 0;
        StateVariable[] currStateVariables = individualStates[0].getAllStateVariables();
        for (int i = 0; i < individualStates.length; i++) {
            if (i == 0) {
                for (StateVariable st : currStateVariables) {
                    flatState[idx++] = st.getX() - currStateVariables[0].getX();
                    flatState[idx++] = st.getY();
                    flatState[idx++] = st.getTh();
                }
            } else {
                StateVariable[] stateVariables = individualStates[i].getAllStateVariables();

                for (int j = 0; j < stateVariables.length; j++) {
                    flatState[idx++] = stateVariables[j].getX() - currStateVariables[i].getX();
                    flatState[idx++] = stateVariables[j].getY() - currStateVariables[i].getY();
                    flatState[idx++] = stateVariables[j].getTh() - currStateVariables[i].getTh();
                }
            }
        }

        return flatState;
    }

    @Override
    public float[] flattenStateWithRescaling(LoadStateStatistics.StateStatistics stateStatistics) {

        // TODO this does not generalize well. Fix it later.
        float[] flatState = flattenState();
        for (int i = 0; i < individualStates.length; i++) {
            for (int j = 0; j < 12; j++) {
                for (int k = 0; k < 3; k++) {
                    float span = stateStatistics.range[6 * j + k];
                    if (span <= 0) {
                        span = 1f;
                    }
                    if (i == 0) { // Temp to only normalize the first set of positions.
                        flatState[i * 36 + 3 * j + k] = (flatState[i * 36 + j] - stateStatistics.max[6 * j + k]) / span;
                    }
                }
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
