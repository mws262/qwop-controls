package flashqwop;

import game.State;

/**
 * New state information is sent from the real QWOP game every timestep. If anyone needs to listen in for these
 * states, they should implement this.
 */
public interface QWOPStateListener {
    void stateReceived(int timestep, State state);
}
