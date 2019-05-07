package flashqwop;

import actions.Action;
import actions.ActionQueue;
import game.State;
import game.StateVariable;
import hardware.ArduinoSerial;

import java.io.IOException;
import java.util.Arrays;

/**
 * Makes it easier to feed Actions to the Flash game. With my game implementations, the caller requests the game to
 * step forward in time. With the Flash game, it is stepping forward in time anyway, and a controller had better keep
 * up. Hence this interface is a little different.
 *
 * This effectively extends the functionality of {@link FlashQWOPServer}. It provides the ability to send commands
 * and receive {@link State states}, but does not help with getting specific actions executed or timed correctly.
 *
 * @author matt
 *
 */
public abstract class FlashGame implements QWOPStateListener {

    private FlashQWOPServer server;
    private ActionQueue actionQueue = new ActionQueue();

    /**
     * Using velocity estimation or use the "cheating" exact result.
     */
    public boolean velocityEstimation = false;

    /**
     * Keeps track of the most-recently sent command, so that commands only need to be sent on transitions.
     */
    private boolean[] prevCommand = null;

    private boolean awaitingRestart = true;

    ArduinoSerial serial;

    boolean hardwareCommandsOut = false;
    /**
     * Keeps track of the number of timesteps received since the beginning of a game to make sure that we haven't
     * lost timestep data in limbo.
     */
    private int timestepsTracked = 0;

    public FlashGame() {
        server = new FlashQWOPServer();
        server.addStateListener(this);

        if (hardwareCommandsOut) {
            try {
                serial = new ArduinoSerial();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //restart();
    }

    /**
     * These Actions will be executed first, from the very beginning of the run after a reset. If you just want to
     * run a single, known sequence, this is the way to go. After these added actions are consumed, then
     * {@link FlashGame} will turn to a feedback controller.
     * @return
     */
    public abstract Action[] getActionSequenceFromBeginning();

    /**
     * This gets called whenever the current ActionQueue runs out of things to do. If you just want to run a fixed
     * sequence, use {@link FlashGame#getActionSequenceFromBeginning()}. Otherwise, you can mix and match the two.
     * @param state Most recent state received from the Flash game.
     * @return An Action from a feedback controller.
     */
    public abstract Action getControlAction(State state);

    /**
     * This class will handle the execution of actions, but inheriting classes may want to listen in.
     * @param state Most-recent state received from the Flash game.
     * @param command Command WHICH LEAD TO THIS STATE. This will be null for for the first state, since it has no
     *                preceding command.
     * @param timestep Timestep count at this state.
     */
    public abstract void reportGameStatus(State state, boolean[] command, int timestep);

    /**
     * Tell the game to reset (equivalent to 'r' on the keyboard in the real game).
     */
    public void restart() {
        server.sendResetSignal();
        awaitingRestart = true;
    }

    public void printGameInfo() {
        server.sendInfoRequest();
    }

    private State previousState;
    @Override
    public synchronized void stateReceived(int timestep, State state) {
        // New run has started. Add the sequence of actions from the beginning.
        if (timestep == 0) {
            System.out.println("zero timestep");
            awaitingRestart = false;
            actionQueue.clearAll();
            actionQueue.addSequence(getActionSequenceFromBeginning()); // TODO Should not be here
            prevCommand = null;
            timestepsTracked = 0;
            previousState = state;
        } else if (awaitingRestart) { // If a restart has been commanded, but has not finished occurring on the Flash
            // side, then just wait.
            return;
        } else if (state.isFailed()) { // If the runner falls, auto-restart. TODO may want to not do this automatically.
            reportGameStatus(state, prevCommand, timestep);

            if (hardwareCommandsOut) {
                try {
                    serial.doCommand(false, false, false, false);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(4000); // Give it time for the "thud" animation to dissipate for taking screen captures.
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            restart();
            return;
        }
        reportGameStatus(state, prevCommand, timestep);

        assert timestep == timestepsTracked; // Have we lost any timesteps?
        long timeBeforeController = System.currentTimeMillis();

        // Get a new Action if one is required.
        if (actionQueue.isEmpty()) {
            // TESTING FINITE DIFFERENCE STUFF TEMP
            State st;
            if (velocityEstimation) {
                st = doFiniteDifferenceVelocityTransformation(previousState, state);
            } else {
                st = state;
            }
            Action a = getControlAction(st);
            if (a == null) {
                return;
            }
            System.out.println(a.toString() + ". Eval time: " + (System.currentTimeMillis() - timeBeforeController) + " ms.");
            actionQueue.addAction(a);
        }

        // Only send command when it's different from the previous.
        boolean[] nextCommand = actionQueue.pollCommand();
        if (!Arrays.equals(prevCommand, nextCommand)) {

            if (hardwareCommandsOut) {
                try {
                    serial.doCommand(nextCommand[0], nextCommand[1], nextCommand[2], nextCommand[3]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                server.sendCommand(nextCommand);
            }
        }
        prevCommand = nextCommand;
        previousState = state;
        // Check to make sure the controller didn't take way too long. This is not the most robust way to do this,
        // but it's better to at least know that this is occurring.
        long controlEvalTime = System.currentTimeMillis() - timeBeforeController;
        if (controlEvalTime > 30) {
            System.out.println("Warning: the control loop time was " + controlEvalTime + "ms. This might be too high.");
        }
        timestepsTracked++;
    }


    /**
     * Pretend that states don't include velocity and estimate the velocity using finite differences.
     */
    private static State doFiniteDifferenceVelocityTransformation(State statePrev, State stateCurrent) {
        StateVariable[] svCurrent = stateCurrent.getStates();
        StateVariable[] svPrev = statePrev.getStates();

        StateVariable[] svOut = new StateVariable[svCurrent.length];
        for (int i = 0; i < svCurrent.length; i++) {
            svOut[i] = new StateVariable(svCurrent[i].getX(), svCurrent[i].getY(), svCurrent[i].getTh(),
                    (svCurrent[i].getX() - svPrev[i].getX())/0.04f,
                    (svCurrent[i].getY() - svPrev[i].getY())/0.04f,
                    (svCurrent[i].getTh() - svPrev[i].getTh())/0.04f);

        }
        return new State(svOut[0], svOut[1], svOut[2], svOut[3], svOut[4], svOut[5], svOut[6], svOut[7], svOut[8],
                svOut[9], svOut[10], svOut[11], false);
    }

    /**
     * Get the underlying interface which communicates with the Flash version of QWOP.
     * @return
     */
    public FlashQWOPServer getServer() {
        return server;
    }
}
