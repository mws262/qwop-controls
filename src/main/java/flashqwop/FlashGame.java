package flashqwop;

import game.IGameCommandTarget;
import game.action.Action;
import game.action.ActionQueue;
import game.qwop.CommandQWOP;
import game.qwop.IStateQWOP;
import game.qwop.StateQWOP;
import game.state.StateVariable6D;
import hardware.KeypusherSerialConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * Makes it easier to feed Actions to the Flash game. With my game implementations, the caller requests the game to
 * step forward in time. With the Flash game, it is stepping forward in time anyway, and a controller had better keep
 * up. Hence this interface is a little different.
 *
 * This effectively extends the functionality of {@link FlashQWOPServer}. It provides the ability to send commands
 * and receive {@link StateQWOP states}, but does not help with getting specific game.command executed or timed correctly.
 *
 * @author matt
 *
 */
public abstract class FlashGame implements IFlashStateListener {

    private FlashQWOPServer server;
    private ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();
    private LinkedList<StateQWOP> stateCache = new LinkedList<>();

    /**
     * Using velocity estimation or use the "cheating" exact result.
     */
    public boolean velocityEstimation = false;

    /**
     * Keeps track of the most-recently sent command, so that commands only need to be sent on transitions.
     */
    private CommandQWOP prevCommand = null;

    /**
     * {@link FlashGame} will send its commands to this destination.
     */
    private IGameCommandTarget<CommandQWOP> commandTarget;

    /**
     * Keeps track of the number of timesteps received since the beginning of a game to make sure that we haven't
     * lost timestep data in limbo.
     */
    private int timestepsTracked = 0;

    /**
     * Have we commanded a game restart and are waiting for it to occur?
     */
    private boolean awaitingRestart = true;

    private static final Logger logger = LogManager.getLogger(FlashGame.class);
    /**
     * Create a new Flash game interface.
     *
     * @param hardwareCommandsOut Are commands sent via hardware key presser or software socket?
     */
    public FlashGame(boolean hardwareCommandsOut) {
        server = new FlashQWOPServer();
        server.addStateListener(this);

        logger.info("Flash game interface created with " + (hardwareCommandsOut ? "hardware" : "software") + " output" +
                " to game.");

        if (hardwareCommandsOut) {
            commandTarget = new KeypusherSerialConnection();
        } else {
            commandTarget = server;
        }
    }

    /**
     * Default to software commands out.
     */
    @SuppressWarnings("WeakerAccess")
    public FlashGame() {
        this(false);
    }

    /**
     * These Actions will be executed first, from the very beginning of the run after a resetGame. If you just want to
     * run a single, known sequence, this is the way to go. After these added game.command are consumed, then
     * {@link FlashGame} will turn to a feedback controller.
     * @return
     */
    public abstract List<Action<CommandQWOP>> getActionSequenceFromBeginning();

    /**
     * This gets called whenever the current ActionQueue runs out of things to do. If you just want to run a fixed
     * sequence, use {@link FlashGame#getActionSequenceFromBeginning()}. Otherwise, you can mix and match the two.
     * @param state Most recent state received from the Flash game.
     * @return An Action from a feedback controller.
     */
    public abstract Action<CommandQWOP> getControlAction(IStateQWOP state);

    /**
     * This class will handle the execution of game.command, but inheriting classes may want to listen in.
     * @param state Most-recent state received from the Flash game.
     * @param command Command WHICH LEAD TO THIS STATE. This will be null for for the first state, since it has no
     *                preceding command.
     * @param timestep Timestep count at this state.
     */
    public abstract void reportGameStatus(IStateQWOP state, CommandQWOP command, int timestep);

    /**
     * Tell the game to resetGame (equivalent to 'r' on the keyboard in the real game).
     */
    public synchronized void restart() {
        server.sendResetSignal();
        awaitingRestart = true;
    }

    public void printGameInfo() {
        server.sendInfoRequest();
    }

    private StateQWOP previousState;
    @Override
    public synchronized void stateReceived(int timestep, StateQWOP state) {
        // New run has started. Add the sequence of game.command from the beginning.
        if (timestep == 0) {
            logger.debug("Zero timestep from Flash game.");
            actionQueue.clearAll();
            actionQueue.addSequence(getActionSequenceFromBeginning());
            prevCommand = null;
            timestepsTracked = 0;
            previousState = state;
            awaitingRestart = false;
        } else if (awaitingRestart) { // If a restart has been commanded, but has not finished occurring on the Flash
            // side, then just wait.
            return;
        } else if (state.isFailed()) { // If the runner falls, auto-restart. TODO may want to not do this automatically.
            reportGameStatus(state, prevCommand, timestep);

            // Send all-keys-up command so it doesn't restart with some buttons active.
            commandTarget.command(CommandQWOP.NONE);
            if (state.getCenterX() < 1000) {
                logger.warn("Runner fallen at " + ((int) state.getCenterX()) / 10f + " meters.");
            } else {
                logger.warn("Runner reached finish.");
            }
            stateCache.clear();

            long freeMemoryBefore = Runtime.getRuntime().freeMemory();
            System.gc();
            logger.warn("Requested garbage collection in between runs. Freed " + (int)((Runtime.getRuntime().freeMemory() - freeMemoryBefore) / 1e6) + "mb.");

            try {
                Thread.sleep(4000); // Give it time for the "thud" animation to dissipate for taking screen captures.
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Restart the Flash game.
            restart();
            return;
        }

        stateCache.add(state);

        // TODO don't hardcode this.
//        int numDelayedStates = 2;
//        int timestepDelay = 5;
//        StateQWOP[] states = new StateQWOP[numDelayedStates + 1];
//        Arrays.fill(states, GameQWOP.getInitialState());
//
//        for (int i = 0; i < Integer.min(states.length, (stateCache.size() + timestepDelay - 1) / timestepDelay); i++) {
//            states[i] = stateCache.get(timestepDelay * i);
//        }
//
//        reportGameStatus(new StateQWOPDelayEmbedded_HigherDifferences(states), prevCommand, timestep);
        reportGameStatus(state, prevCommand, timestep);
        assert timestep == timestepsTracked; // Have we lost any timesteps?
        long timeBeforeController = System.currentTimeMillis();

        // Get a new Action if one is required.
        if (actionQueue.isEmpty()) {
            // TESTING FINITE DIFFERENCE STUFF TEMP
            StateQWOP st;
            if (velocityEstimation) {
                st = doFiniteDifferenceVelocityTransformation(previousState, state);
            } else {
                st = state;
            }
            Action<CommandQWOP> a = getControlAction(st);
            if (a == null) {
                return;
            }
            actionQueue.addAction(a);
        }

        // Only send command when it's different from the previous.
        CommandQWOP nextCommand = actionQueue.pollCommand();
        if (!prevCommand.equals(nextCommand)) {
            commandTarget.command(nextCommand);
        }
        prevCommand = nextCommand;
        previousState = state;

        // Check to make sure the controller didn't take way too long. This is not the most robust way to do this,
        // but it's better to at least know that this is occurring.
        long controlEvalTime = System.currentTimeMillis() - timeBeforeController;
        if (controlEvalTime > 30) {
            logger.warn("The control loop time was " + controlEvalTime + "ms. This might be too high.");
        }
        timestepsTracked++;
    }

    /**
     * Pretend that states don't include velocity and estimate the velocity using finite differences.
     */
    private static StateQWOP doFiniteDifferenceVelocityTransformation(StateQWOP statePrev, StateQWOP stateCurrent) {
        StateVariable6D[] svCurrent = stateCurrent.getAllStateVariables();
        StateVariable6D[] svPrev = statePrev.getAllStateVariables();

        StateVariable6D[] svOut = new StateVariable6D[svCurrent.length];
        for (int i = 0; i < svCurrent.length; i++) {
            svOut[i] = new StateVariable6D(svCurrent[i].getX(), svCurrent[i].getY(), svCurrent[i].getTh(),
                    (svCurrent[i].getX() - svPrev[i].getX())/0.04f,
                    (svCurrent[i].getY() - svPrev[i].getY())/0.04f,
                    (svCurrent[i].getTh() - svPrev[i].getTh())/0.04f);
        }
        return new StateQWOP(svOut[0], svOut[1], svOut[2], svOut[3], svOut[4], svOut[5], svOut[6], svOut[7], svOut[8],
                svOut[9], svOut[10], svOut[11], false);
    }

    /**
     * Get the underlying interface which communicates with the Flash version of QWOP.
     * @return The communication interface with the Flash game.
     */
    public FlashQWOPServer getServer() {
        return server;
    }
}
