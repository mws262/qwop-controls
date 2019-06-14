package game;

import java.util.ArrayList;
import java.util.List;

public class GameUnifiedCaching extends GameUnified {

    private List<IState> cachedStates = new ArrayList<>(2500);

    // For delay embedding.
    public int timestepDelay = 1;
    public int numDelayedStates = 2;

    @Override
    public void makeNewWorld() {
        super.makeNewWorld();
        cachedStates.clear();
        cachedStates.add(getInitialState());
    }

    @Override
    public void step(boolean q, boolean w, boolean o, boolean p) {
        super.step(q, w, o, p);
        cachedStates.add(super.getCurrentState());
    }

    @Override
    public IState getCurrentState() {
        // TODO figure out the state format.
        //new StateDelayEmbedded();
        if (cachedStates.isEmpty()) {
            return getInitialState();
        } else {
            return cachedStates.get(cachedStates.size() - 1);
        }
    }
}
