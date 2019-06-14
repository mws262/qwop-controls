package game;

import java.util.Arrays;
import java.util.LinkedList;

public class GameUnifiedCaching extends GameUnified {

    private LinkedList<IState> cachedStates;

    // For delay embedding.
    public int timestepDelay = 1;
    public int numDelayedStates = 2;

    @Override
    public void makeNewWorld() {
        super.makeNewWorld();

        if (cachedStates == null) {
            cachedStates = new LinkedList<>();
        }
        cachedStates.clear();
        cachedStates.add(getInitialState());
    }

    @Override
    public void step(boolean q, boolean w, boolean o, boolean p) {
        super.step(q, w, o, p);
        cachedStates.addFirst(super.getCurrentState());
    }

    @Override
    public IState getCurrentState() {

        assert !cachedStates.isEmpty();

        IState[] states = new IState[numDelayedStates + 1];
        Arrays.fill(states, getInitialState());

        for (int i = 0; i < Integer.min(states.length, cachedStates.size()); i++) {
            states[i] = cachedStates.get(timestepDelay * i);
        }

        return new StateDelayEmbedded(states);
    }
}
