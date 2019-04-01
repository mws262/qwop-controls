package flashqwop;

import actions.ActionQueue;
import game.State;

public class FlashGame implements QWOPStateListener {

    private FlashQWOPServer server;

    ActionQueue actionQueue = new ActionQueue();

    private boolean awaitingRestart = true;


    public FlashGame() {
        server = new FlashQWOPServer();
        server.addStateListener(this);
        restart();
    }

    @Override
    public void stateReceived(int timestep, State state) {

    }

    public void restart() {
        server.sendResetSignal();
        awaitingRestart = true;
    }
}
