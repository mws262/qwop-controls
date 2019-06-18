package game;

import game.state.IState;
import game.state.State;
import game.state.StateDelayEmbedded;

import java.util.Arrays;
import java.util.LinkedList;

public class GameUnifiedCaching extends GameUnified {

    public final int STATE_SIZE;

    private LinkedList<State> cachedStates;

    // For delay embedding.
    public final int timestepDelay;
    public final int numDelayedStates;

    public GameUnifiedCaching(int timestepDelay, int numDelayedStates) {
        if (timestepDelay < 1) {
            throw new IllegalArgumentException("Timestep delay must be at least one. Was: " + timestepDelay);
        }
        this.timestepDelay = timestepDelay;
        this.numDelayedStates = numDelayedStates;

        STATE_SIZE = (numDelayedStates + 1) * 36;
    }

    @Override
    public void makeNewWorld() {
        super.makeNewWorld();

        if (cachedStates == null) {
            cachedStates = new LinkedList<>();
        }
        cachedStates.clear();
        cachedStates.add((State)getInitialState());
    }

    @Override
    public void step(boolean q, boolean w, boolean o, boolean p) {
        super.step(q, w, o, p);
        cachedStates.addFirst((State)super.getCurrentState());
    }

    @Override
    public IState getCurrentState() {

        State[] states = new State[numDelayedStates + 1];
        Arrays.fill(states, getInitialState());

        for (int i = 0; i < Integer.min(states.length, (cachedStates.size() + timestepDelay - 1) / timestepDelay); i++) {
            states[i] = cachedStates.get(timestepDelay * i);
        }

        return new StateDelayEmbedded(states);
    }

    @Override
    public int getStateDimension() {
        return STATE_SIZE;
    }

    @Override
    public GameUnified getCopy() {
        return new GameUnifiedCaching(timestepDelay, numDelayedStates);
    }
}
