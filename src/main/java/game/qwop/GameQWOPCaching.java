package game.qwop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.state.*;

import java.util.Arrays;
import java.util.LinkedList;

public class GameQWOPCaching extends GameQWOP {

    @JsonIgnore
    public final int STATE_SIZE;

    private LinkedList<StateQWOP> cachedStates;

    // For delay embedding.
    public final int timestepDelay;
    public final int numDelayedStates;

    public enum StateType {
        POSES, DIFFERENCES, HIGHER_DIFFERENCES
    }

    public final StateType stateType;

    public GameQWOPCaching(@JsonProperty("timestepDelay") int timestepDelay,
                           @JsonProperty("numDelayedStates") int numDelayedStates,
                           @JsonProperty("stateType") StateType stateType) {
        super.resetGame();
        if (timestepDelay < 1) {
            throw new IllegalArgumentException("Timestep delay must be at least one. Was: " + timestepDelay);
        }
        this.timestepDelay = timestepDelay;
        this.numDelayedStates = numDelayedStates;

        STATE_SIZE = (numDelayedStates + 1) * 36;
        this.stateType = stateType;
    }

    @Override
    public void resetGame() {
        super.resetGame();

        if (cachedStates == null) {
            cachedStates = new LinkedList<>();
        }
        cachedStates.clear();
        cachedStates.add((StateQWOP)getInitialState());
    }

    @Override
    public void step(CommandQWOP c) {
        super.step(c);
        cachedStates.addFirst((StateQWOP)super.getCurrentState());
    }

    @Override
    public IState getCurrentState() {

        StateQWOP[] states = new StateQWOP[numDelayedStates + 1];
        Arrays.fill(states, getInitialState());

        for (int i = 0; i < Integer.min(states.length, (cachedStates.size() + timestepDelay - 1) / timestepDelay); i++) {
            states[i] = cachedStates.get(timestepDelay * i);
        }

        switch (stateType) {
            case POSES:
                return new StateQWOPDelayEmbedded_Poses(states);
            case DIFFERENCES:
                return new StateQWOPDelayEmbedded_Differences(states);
            case HIGHER_DIFFERENCES:
                return new StateQWOPDelayEmbedded_HigherDifferences(states);
            default:
                throw new IllegalStateException("Unhandled state return type.");
        }
    }

    @Override
    public void setState(IState state) {
        cachedStates.clear();
        if (state instanceof StateQWOPDelayEmbedded) {
            StateQWOPDelayEmbedded st = (StateQWOPDelayEmbedded) state;
            for (int i = st.individualStates.length - 1; i >= 0; i--) { // element zero is the newest.
                cachedStates.add(st.individualStates[i]);
            }
            super.setState(st.individualStates[0]);
        } else {
            super.setState(state);
        }
    }

    @Override
    public int getStateDimension() {
        return STATE_SIZE;
    }

    @Override
    public GameQWOP getCopy() {
        return new GameQWOPCaching(timestepDelay, numDelayedStates, stateType);
    }
}
