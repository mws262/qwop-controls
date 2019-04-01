package flashqwop;

import game.GameUnified;
import game.State;

public class CompareFlashToJava implements QWOPStateListener {
    FlashQWOPServer server;
    GameUnified game = new GameUnified();

    public CompareFlashToJava() {
        server = new FlashQWOPServer(2900);

    }

    @Override
    public void stateReceived(int timestep, State state) {

    }
}
