package game.qwop;

import game.state.StateVariable6D;

import java.util.Arrays;

public abstract class StateQWOPDelayEmbedded implements IStateQWOP {

    final StateQWOP[] individualStates;

    final int stateVariableCount;

    public static final int POSE_DIM = 3;

    /**
     * Construct a delay-embedded state. This is essentially several individual state snapshots stacked in some way
     * to make a bigger state.
     * @param states State snapshots in array from newest at the beginning of the array to oldest at the end.
     */
    public StateQWOPDelayEmbedded(StateQWOP[] states) { // To enforce that all IStates must be of the same type.
        individualStates = Arrays.copyOf(states, states.length);
        // Configurations variables (3), number of individual States making up this composite, number of body parts.
        stateVariableCount = individualStates.length * individualStates[0].getStateVariableCount();
    }

    public StateQWOP[] getIndividualStates() {
        return individualStates;
    }

    @Override
    public StateVariable6D[] getAllStateVariables() {
        return individualStates[0].getAllStateVariables();
    }

    /**
     * Returns the specified object's state variable from the newest state snapshot contained here.
     * @param obj Which body's {@link StateVariable6D} we want to fetch.
     * @return Corresponding StateVariable.
     */
    @Override
    public StateVariable6D getStateVariableFromName(ObjectName obj) {
        return individualStates[0].getStateVariableFromName(obj);
    }

    /**
     * Center X for the newest state snapshot contained here.
     * @return Body center X for the newest state contained.
     */
    @Override
    public float getCenterX() {
        return individualStates[0].getCenterX();
    }

    @Override
    public float getCenterDx() {
        return individualStates[0].getCenterDx();
    }

    /**
     * Failure status of the newest state snapshot contained.
     * @return Does the most recent state snapshot indicate that the runner has fallen?
     */
    @Override
    public boolean isFailed() {
        return individualStates[0].isFailed();
    }

    @Override
    public int getStateVariableCount() {
        return stateVariableCount;
    }

    @Override
    public StateQWOP getPositionCoordinates() {
        return getIndividualStates()[0];
    }
}
