package tree;

import actions.Action;
import actions.ActionQueue;
import game.GameUnified;
import game.IGameInternal;
import game.State;
import samplers.ISampler;
import samplers.Sampler_Random;
import savers.DataSaver_Null;
import savers.IDataSaver;
import ui.PanelRunner;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Addresses limitations of the old concurrent state machine approach.
 * Instead, both tree FSM and game FSM are combined but may both
 * operate on the same tree as other of the new FSM_Trees The idea is
 * to run many instances of this class in parallel.
 *
 * @author matt
 */

public class TreeWorker extends PanelRunner implements Runnable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        IDLE, INITIALIZE, TREE_POLICY_CHOOSING, TREE_POLICY_EXECUTING, EXPANSION_POLICY_CHOOSING,
        EXPANSION_POLICY_EXECUTING, ROLLOUT_POLICY, EVALUATE_GAME
    }

    /**
     * Is this worker running the FSM repeatedly?
     */
    private boolean workerRunning = true;

    /**
     * Sets the FSM to stop running the next time it is idle.
     */
    private AtomicBoolean flagForTermination = new AtomicBoolean(false);

    /**
     * Is this worker idle and waiting for a new task?
     */
    private boolean paused = true;

    /**
     * Print debugging info?
     */
    public boolean verbose = false;

    /**
     * The current game instance that this FSM is using. This will now not change.
     */
    private final IGameInternal game = new GameUnified();

    /**
     * Strategy for sampling new nodes. Defaults to random sampling.
     */
    private ISampler sampler = new Sampler_Random();

    /**
     * How data is saved. Defaults to no saving.
     */
    private IDataSaver saver = new DataSaver_Null();

    /**
     * Root of tree that this FSM is operating on.
     */
    private NodeQWOPExplorableBase<?> rootNode;

    /**
     * Node that the game is currently operating at.
     */
    private NodeQWOPExplorableBase<?> currentGameNode;

    /**
     * Node the game is attempting to run to.
     */
    private NodeQWOPExplorableBase<?> targetNodeToTest;

    private Action targetActionToTest;

    /**
     * Queued commands, IE QWOP key presses
     */
    public ActionQueue actionQueue = new ActionQueue();

    /**
     * Initial runner state.
     */
    private State initState = GameUnified.getInitialState();

    /**
     * Current status of this FSM
     */
    private Status currentStatus = Status.IDLE;

    /**
     * Number of physics timesteps simulated by this TreeWorker.
     */
    private long workerStepsSimulated = 0;

    /**
     * Number of games simulated by this TreeWorker.
     */
    private LongAdder workerGamesPlayed = new LongAdder();

    /**
     * Total timesteps simulated by all TreeWorkers.
     */
    private static LongAdder totalStepsSimulated = new LongAdder();

    /**
     * Total games played by all TreeWorkers.
     */
    private static LongAdder totalGamesPlayed = new LongAdder();

    /**
     * Last system time that we estimated timesteps per second at. This field is in milliseconds.
     */
    private long lastTsTimeMs;

    /**
     * Counter since last estimate of timesteps per second. This is done once per 1000 timesteps, so it's not so noisy.
     */
    private int tsPerSecondUpdateCounter = 0;

    /**
     * Slightly filtered timesteps simulated per second.
     */
    private double tsPerSecond = 0;

    public final String workerName;
    private final int workerID;

    /**
     * Number of workers created. Also used for the worker ID number.
     */
    private static AtomicInteger workerCount = new AtomicInteger();

    /**
     * For blocking until a pause command has kicked in.
     */
    private final Object pauseLock = new Object();

    private final List<Action> actionSequence = new ArrayList<>();

    public TreeWorker() {
        workerID = TreeWorker.getWorkerCountAndIncrement();
        workerName = "worker" + workerID;

        lastTsTimeMs = System.currentTimeMillis();

        // Thread that this worker is running on. Will stay constant with this worker.
        Thread workerThread = new Thread(this);
        workerThread.setName(workerName);
        workerThread.start();
    }

    /**
     * Set the root node to work from. Does not have to be the overall tree root.
     * Must be assigned, however. Must still startWorker() to get things rolling.
     *
     * @param rootNode Root node that this worker expands from.
     */
    public void setRoot(NodeQWOPExplorableBase<?> rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Set which sampler is used. Defaults to Sampler_Random. Clones when reassigned.
     */
    public void setSampler(ISampler sampler) {
        this.sampler = sampler.getCopy();
    }

    /**
     * Set which saver to  use. Defaults to no saving, Sampler_Null. Clones when reassigned.
     */
    public void setSaver(IDataSaver saver) {
        this.saver = saver.getCopy();
    }

    /**
     * Finite state machine loop. Runnable.
     */
    @Override
    public void run() {
        while (workerRunning) {
            switch (currentStatus) {
                case IDLE:
                    // Overall behavior does not switch until the worker reaches IDLE in order to not leave some task
                    // half-complete. At IDLE, the worker can permanently stop, temporarily pause, or return to the
                    // INITIALIZATION state.

                    if (flagForTermination.get()) { // Permanent stop. Call terminateWorker() to trigger at next time
                        // the worker reaches IDLE.
                        workerRunning = false;
                        tsPerSecond = 0; // Set to 0 so plots of worker speed don't get stuck at whatever value they
                        // were at before terminating the worker.
                        break;
                    } else if (paused) { // Temporary stop. Call pauseWorker().
                        synchronized (pauseLock) {
                            pauseLock.notify();
                        }
                        continue;
                    } else {
                        changeStatus(Status.INITIALIZE); // While running. Go immediately to making a new game.
                    }

                    break;
                case INITIALIZE:
                    actionQueue.clearAll();
                    game.makeNewWorld(); // Create a new game world.
                    saver.reportGameInitialization(GameUnified.getInitialState());
                    currentGameNode = rootNode;
                    changeStatus(Status.TREE_POLICY_CHOOSING);

                    break;
                case TREE_POLICY_CHOOSING: // Picks a target leaf node within the tree to get to.
                    if (game.getFailureStatus())
                        throw new RuntimeException("Tree policy operates only within an existing tree. We should not " +
                                "find failures in here.");

                    if (sampler.treePolicyGuard(currentGameNode)) { // Sampler tells us when we're done with the tree
                        // policy.
                        changeStatus(Status.EXPANSION_POLICY_CHOOSING);

                    } else {
                        // This gets us through the existing tree to a place that we plan to add a new node.
                        NodeQWOPExplorableBase<?> expansionNode = sampler.treePolicy(currentGameNode);
//                        Objects.requireNonNull(expansionNode, "This could be a sign of improperly shutting down " +
//                                "workers at the end of search stages.");




                        if (expansionNode == null) { // May happen with some samplers when the stage finishes.
                            changeStatus(Status.IDLE);
                        } else {
                            assert !expansionNode.getState().isFailed() : "Tree policy picked a node with a failed state." +
                                    " This is bad behavior.";
                            assert expansionNode.isLocked() : "It is the sampler's responsibility to lock a node when " +
                                    "executing the tree policy.";
                            actionQueue.clearAll();

                            actionSequence.clear();

                            targetNodeToTest = expansionNode;
                            if (targetNodeToTest.getTreeDepth() != 0) { // No action sequence to add if target node
                                // is root (we're already there!).
                                actionQueue.addSequence(targetNodeToTest.getSequence(actionSequence));
                            }
                            changeStatus(Status.TREE_POLICY_EXECUTING);
                        }
                    }

                    break;
                case TREE_POLICY_EXECUTING:

                    executeNextOnQueue(); // Execute a single timestep with the actions that have been queued.
                    assert !game.getFailureStatus() : "Game encountered a failure while executing the tree policy. The tree " +
                            "policy should be safe, since it's ground that's been covered before.";

                    // When all actions in queue are done, figure out what to do next.
                    if (actionQueue.isEmpty()) {
                        currentGameNode = targetNodeToTest;
                        if (currentGameNode.getUntriedActionCount() == 0) { // This case should only happen if
                            // another worker just happens to beat it here.
                            System.out.println("Wow! Another worker must have finished this node off before this worker got here. We're going to keep running tree policy down the tree. If there aren't other workers, you should be worried.");
                            currentGameNode = rootNode;
                            changeStatus(Status.TREE_POLICY_CHOOSING);
                        } else {
                            sampler.treePolicyActionDone(currentGameNode);
                            changeStatus(Status.EXPANSION_POLICY_CHOOSING);
                        }
                    }

                    break;
                case EXPANSION_POLICY_CHOOSING:
                    if (sampler.expansionPolicyGuard(currentGameNode)) { // Some samplers keep adding nodes until
                        // failure, others add a fewer number and move to rollout before failure.
                        changeStatus(Status.ROLLOUT_POLICY);
                        sampler.expansionPolicyActionDone(currentGameNode);
                    } else {
                        targetActionToTest = sampler.expansionPolicy(currentGameNode);
                        actionQueue.clearAll();
                        actionQueue.addAction(targetActionToTest);
                        changeStatus(Status.EXPANSION_POLICY_EXECUTING);
                    }

                    break;
                case EXPANSION_POLICY_EXECUTING:

                    executeNextOnQueue(); // Execute a single timestep with the actions that have been queued.

                    // When done, record state and go back to choosing. If failed, the sampler guards will tell us.
                    if (actionQueue.isEmpty() || game.getFailureStatus()) {
                        // TODO possibly update the action to what was actually possible until the runner fell.
                        // Subtract out the extra timesteps that weren't possible due to failure.
                        assert currentGameNode.isLocked();
                        currentGameNode = currentGameNode.addDoublyLinkedChild(targetActionToTest,
                                game.getCurrentState());

                        sampler.expansionPolicyActionDone(currentGameNode);
                        changeStatus(Status.EXPANSION_POLICY_CHOOSING);
                    }

                    break;
                case ROLLOUT_POLICY:
                    if (sampler.rolloutPolicyGuard(currentGameNode)) {
                        changeStatus(Status.EVALUATE_GAME);
                    } else {
                        sampler.rolloutPolicy(currentGameNode, game);
                    }

                    break;
                case EVALUATE_GAME:
                    saver.reportGameEnding(currentGameNode);
                    long gameTs = game.getTimestepsThisGame();
                    addToTotalTimesteps(gameTs);

                    workerGamesPlayed.increment();
                    incrementTotalGameCount();

                    currentGameNode.releaseExpansionRights();

                    if (rootNode.isFullyExplored()) {
                        pauseWorker();
                        changeStatus(Status.IDLE);
                        System.out.println("Tree is fully explored, but just pausing for next stage.");
                    } else {
                        changeStatus(Status.IDLE);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Do not directly change the game status. Use this.
     */
    private void changeStatus(Status newStatus) {
        if (verbose) {
            System.out.println("Worker " + workerID + ": " + currentStatus + " --->  " + newStatus + "     game: " + workerGamesPlayed);
        }
        currentStatus = newStatus;
    }

    /**
     * Pop the next action off the queue and execute one timestep.
     */
    private void executeNextOnQueue() {
        if (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
            Action action = actionQueue.peekThisAction();
            saver.reportTimestep(action, game);
            workerStepsSimulated++;
            tsPerSecondUpdateCounter++;

            // Estimate timesteps simulated per second by this worker.
            if (tsPerSecondUpdateCounter % 1000 == 0) { // Update every 1000 timesteps
                long currTime = System.currentTimeMillis();
                // 9/10 to old estimate, 1/10 to new estimate.
                tsPerSecond = tsPerSecond * 0.9 + 0.1 * 1000. * 1000. / (currTime - lastTsTimeMs);
                lastTsTimeMs = currTime;
                tsPerSecondUpdateCounter = 0;
            }
        }
    }

    /**
     * QWOP initial condition. Good way to give the root node a state.
     */
    public State getInitialState() {
        return initState;
    }

    /**
     * Get the state of the runner.
     */
    public State getGameState() {
        return game.getCurrentState();
    }

    /**
     * How many physics timesteps has this particular worker simulated?
     */
    public long getWorkerStepsSimulated() {
        return workerStepsSimulated;
    }

    /**
     * Terminate this worker after it's done with it's current task.
     */
    public void terminateWorker() {
        flagForTermination.set(true);
    }

    /**
     * Pause what the worker is doing. Good for changing objectives and samplers, etc. This blocks until the worker
     * is done with the current evaluation and is back to IDLE.
     */
    public void pauseWorker() {
        paused = true;
        synchronized (pauseLock) {
            try {
                pauseLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Pause what the worker is doing. Tells its saver to do whatever it should when the stage is complete.
     */
    public void triggerStageCompleted() {
        paused = true;
        saver.finalizeSaverData();
    }

    /**
     * Unpause what the worker is doing. Use once objectives have been set.
     */
    public void startWorker() {
        if (rootNode == null) throw new RuntimeException("Cannot start a worker while no root node is assigned.");
        paused = false;
    }

    /**
     * Check if this runner is done or not.
     */
    public synchronized boolean isRunning() {
        return workerRunning;
    }

    /**
     * Get the running average of timesteps simulated per second of realtime.
     */
    public double getTsPerSecond() {
        return tsPerSecond;
    }

    /**
     * Increase the the count of total games in a hopefully thread-safe way.
     */
    private static void incrementTotalGameCount() {
        totalGamesPlayed.increment();
    }

    /**
     * Increase the number of timesteps simulated in a hopefully thread-safe way.
     */
    private static void addToTotalTimesteps(long timesteps) {
        totalStepsSimulated.add(timesteps);
    }

    /**
     * Get the number of games played by all workers, total.
     */
    public static long getTotalGamesPlayed() {
        return totalGamesPlayed.longValue();
    }

    /**
     * Get the number of games played by all workers, total.
     */
    public static long getTotalTimestepsSimulated() {
        return totalStepsSimulated.longValue();
    }

    /**
     * Get the total number of workers created so far.
     * @return Total number of workers created so far this run.
     */
    public static int getTotalWorkerCount() {
        return workerCount.get();
    }

    /**
     * Get the total number of workers created so far and increment the counter.
     * @return Total number of workers created (before incrementation).
     */
    private static int getWorkerCountAndIncrement() {
        return workerCount.getAndIncrement();
    }
    /**
     * Color the node scaled by depth in the tree. Skip the brightness argument for default value.
     */
    public static Color getColorFromWorkerID(int ID) {
        float brightness = 0.85f;
        float colorOffset = 0f;
        float scaledDepth = (float) ID / 4f;
        float H = scaledDepth * 0.38f + colorOffset;
        float S = 0.8f;
        return Color.getHSBColor(H, S, brightness);
    }

    /**
     * Debug drawer. If you just want to display a run, use one of the other PanelRunner implementations.
     */
    @Override
    public void paintComponent(Graphics g) {
        if (!active) return;
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Draw all non-highlighted runners.
        game.draw(g2, runnerScaling, xOffsetPixels, yOffsetPixels);
    }

    @Override
    public void deactivateTab() {
        active = false;
    } // Not really applicable.
}


