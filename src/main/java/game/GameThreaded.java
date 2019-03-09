package game;

import actions.Action;
import actions.ActionQueue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A wrapper for GameSingleThread. It can be run on a separate thread. It contains a queue of actions which can be
 * added to at any time. These actions will be run as soon as possible. GameThreaded will continue to wait for more
 * actions until terminate is called on the application is ended.
 *
 * @author matt
 */
public class GameThreaded implements Runnable{

    /**
     * Whether the game is waiting for actions to be added (becomes true when run() is called).
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Thread safe instance of the game which should belong to this only.
     */
    private final  GameThreadSafe game = new GameThreadSafe();

    /**
     * Queue of actions ready to be sent to the game. They will be executed as soon as they arrive.
     */
    private final ActionQueue actionQueue = new ActionQueue();

    @Override
    public void run() {
        running.set(true);
        while (running.get()) {
            if (!actionQueue.isEmpty()) {
                game.step(actionQueue.pollCommand());
            }
        }
    }

    /**
     * Return whether the action queue is empty and the game is waiting.
     * @return Whether the game just waiting for more actions to be queued.
     */
    public synchronized boolean isWaiting() {
        return actionQueue.isEmpty();
    }

    /**
     * Cause this game to cease waiting for new inputs.
     */
    public synchronized void terminate() {
        running.set(false);
    }

    /**
     * Make a new game and clear any queued actions.
     */
    public synchronized void newGame() {
        actionQueue.clearAll();
        game.makeNewWorld();
    }

    /**
     * Get the state of the runner.
     * @return The current state of the runner in the game (72 numbers).
     */
    public synchronized State getState() {
        return game.getCurrentState();
    }

    /**
     * Add an action to the queue. It will be executed as soon as it arrives.
     * @param action An action to add to the queue.
     */
    public synchronized void addAction(Action action) {
        actionQueue.addAction(action);
    }

    /**
     * Add multiple actions to the queue. They will be executed as soon as possible, in the order of the list.
     * @param actions A series of actions to execute.
     */
    public synchronized void addAllActions(List<Action> actions) {
        actionQueue.addSequence(actions);
    }

    /**
     * Add multiple actions to the queue. They will be executed as soon as possible, in the order of the array indices.
     * @param actions A series of actions to execute.
     */
    public synchronized void addAllActions(Action[] actions) {
        actionQueue.addSequence(actions);
    }
}
