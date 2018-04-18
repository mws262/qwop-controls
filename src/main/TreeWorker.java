package main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import game.GameLoader;
import game.State;

/**
 * Addresses limitations of the old concurrent state machine approach.
 * Instead, both tree FSM and game FSM are combined but may both 
 * operate on the same tree as other of the new FSM_Trees The idea is
 * to run many instances of this class in parallel.
 * 
 * @author matt
 *
 */

public class TreeWorker extends PanelRunner implements Runnable {

	private static final long serialVersionUID = 1L;

	public enum Status{
		IDLE, INITIALIZE, TREE_POLICY_CHOOSING, TREE_POLICY_EXECUTING, EXPANSION_POLICY_CHOOSING, EXPANSION_POLICY_EXECUTING, ROLLOUT_POLICY_CHOOSING,  ROLLOUT_POLICY_EXECUTING, EVALUATE_GAME, EXHAUSTED
	}

	/** Is this worker running the FSM repeatedly? **/
	private boolean workerRunning = true;

	/** Sets the FSM to stop running the next time it is idle. **/
	private AtomicBoolean flagForTermination = new AtomicBoolean(false);

	/** Print debugging info? **/
	public boolean verbose = false;

	/** Print debugging info? **/
	public boolean debugDraw = false;

	/** The current game instance that this FSM is using. This will frequently change since a new one is created for each run. **/
	private final GameLoader game = new GameLoader();

	/** Strategy for sampling new nodes. **/
	private ISampler sampler;

	/** How data is saved. **/
	private IDataSaver saver;

	/** Root of tree that this FSM is operating on. **/
	private final Node rootNode;

	/** Node that the game is currently operating at. **/
	private Node currentGameNode;

	/** Node the game is attempting to run to. **/
	private Node targetNodeToTest;

	/** Node the game is initially expanding from. **/
	private Node expansionNode;
	private Node lastNodeAdded;

	/** Queued commands, IE QWOP key presses **/
	public ActionQueue actionQueue = new ActionQueue();

	/** Initial runner state. **/
	private State initState = GameLoader.getInitialState();

	/** Current status of this FSM **/
	private Status currentStatus = Status.IDLE;
	private Status prevStatus = Status.IDLE;

	/** Number of physics timesteps simulated by this TreeWorker. **/
	private long workerStepsSimulated = 0;

	/** Number of games simulated by this TreeWorker. **/
	private LongAdder workerGamesPlayed = new LongAdder();

	/** Total timesteps simulated by all TreeWorkers. **/
	private static LongAdder totalStepsSimulated = new LongAdder();

	/** Total games played by all TreeWorkers. **/
	private static LongAdder totalGamesPlayed = new LongAdder();

	/** Milli start time of last game. **/
	private long startMs;

	/** Slightly filtered timesteps simulated per second. **/
	private int tsPerSecond = 0;

	public String workerName = "";
	private final int workerID;
	private static int workerCount = 0;

	public TreeWorker(Node rootNode, ISampler sampler, IDataSaver saver) {
		this.sampler = sampler;
		this.saver = saver;
		this.rootNode = rootNode;
		workerID = workerCount;
		workerName = "worker" + workerID;
		workerCount++;
	}

	/* Finite state machine loop. Runnable. */
	@Override
	public void run() {
		while(workerRunning) {
			switch(currentStatus) {
			case IDLE:

				if (flagForTermination.get()) {
					workerRunning = false;
					break;
				}else {
					changeStatus(Status.INITIALIZE);
				}
				break;
			case INITIALIZE:
				startMs = System.currentTimeMillis();
				actionQueue.clearAll();
				newGame(); // Create a new game world.
				saver.reportGameInitialization(GameLoader.getInitialState());

				currentGameNode = rootNode;
				changeStatus(Status.TREE_POLICY_CHOOSING);

				break;		
			case TREE_POLICY_CHOOSING: // Picks a target leaf node within the tree to get to.
				if (isGameFailed()) throw new RuntimeException("Tree policy operates only within an existing tree. We should not find failures in here.");

				if (sampler.treePolicyGuard(currentGameNode)) { // Sampler tells us when we're done with the tree policy.
					changeStatus(Status.EXPANSION_POLICY_CHOOSING);
				}else {
					expansionNode = sampler.treePolicy(currentGameNode); // This gets us through the existing tree to a place that we plan to add a new node.
					if (debugDraw) {
						expansionNode.setBackwardsBranchColor(getColorFromWorkerID(workerID));
						expansionNode.setBackwardsBranchZOffset(0.1f);
					}
					targetNodeToTest = expansionNode;
					actionQueue.clearAll();//System.out.println(targetNodeToTest.treeDepth);
					actionQueue.addSequence(targetNodeToTest.getSequence());
					changeStatus(Status.TREE_POLICY_EXECUTING);
				}

				break;
			case TREE_POLICY_EXECUTING:

				executeNextOnQueue(); // Execute a single timestep with the actions that have been queued.
				//if (isGameFailed()) throw new RuntimeException("Game encountered a failure while executing the tree policy. The tree policy should be safe, since it's ground that's been covered before.");

				// When all actions in queue are done, figure out what to do next.
				if (actionQueue.isEmpty()) {
					currentGameNode = targetNodeToTest;
					if (currentGameNode.uncheckedActions.size() == 0) { // This case should only happen if another worker just happens to beat it here.
						//System.out.println("Wow! Another worker must have finished this node off before this worker got here. We're going to keep running tree policy down the tree. If there aren't other workers, you should be worried.");
						currentGameNode = rootNode;
						changeStatus(Status.TREE_POLICY_CHOOSING);
					}else {
						sampler.treePolicyActionDone(currentGameNode);
						changeStatus(Status.EXPANSION_POLICY_CHOOSING);
					}
				}

				break;
			case EXPANSION_POLICY_CHOOSING:
				if (sampler.expansionPolicyGuard(currentGameNode)) { // Some samplers keep adding nodes until failure, others add a fewer number and move to rollout before failure.	
					changeStatus(Status.ROLLOUT_POLICY_CHOOSING);
					if (debugDraw && lastNodeAdded != null) lastNodeAdded.clearNodeOverrideColor();
					lastNodeAdded = currentGameNode;
					if (debugDraw) lastNodeAdded.setBranchColor(getColorFromWorkerID(workerID));
					sampler.expansionPolicyActionDone(currentGameNode);
				}else {
					targetNodeToTest = sampler.expansionPolicy(currentGameNode);
					if (currentGameNode.treeDepth + 1 != targetNodeToTest.treeDepth) {
						throw new RuntimeException("Expansion policy tried to sample a node more than 1 depth below it in the tree. This is bad since tree policy should be used"
								+ "to traverse the existing tree and expansion policy should only be used for adding new nodes and extending the tree.");
					}
					actionQueue.clearAll();
					actionQueue.addAction(targetNodeToTest.getAction());
					changeStatus(Status.EXPANSION_POLICY_EXECUTING);
				}

				break;
			case EXPANSION_POLICY_EXECUTING:

				executeNextOnQueue(); // Execute a single timestep with the actions that have been queued.

				// When done, record state and go back to choosing. If failed, the sampler guards will tell us.
				if (actionQueue.isEmpty() || game.getFailureStatus()) {
					// TODO possibly update the action to what was actually possible until the runner fell. Subtract out the extra timesteps that weren't possible due to failure.
					currentGameNode = targetNodeToTest;
					if(currentGameNode.state != null) throw new RuntimeException("The expansion policy should only encounter new nodes. None of them should have their state assigned before now.");
					currentGameNode.setState(getGameState());
					sampler.expansionPolicyActionDone(currentGameNode);
					changeStatus(Status.EXPANSION_POLICY_CHOOSING);

					try {
						if (currentGameNode.isFailed()){ // If we've added a terminal node, we need to see how this affects the exploration status of the rest of the tree.
							targetNodeToTest.checkFullyExplored_lite();
						}
					}catch (NullPointerException e){
						e.printStackTrace();
						throw new NullPointerException("Tree was given a game state that did not have a failure status assigned.");
					}
				}	

				break;		
			case ROLLOUT_POLICY_CHOOSING:
				if (sampler.rolloutPolicyGuard(currentGameNode)) {
					changeStatus(Status.EVALUATE_GAME);
				}else {
					targetNodeToTest = sampler.rolloutPolicy(currentGameNode);
					actionQueue.clearAll();
					actionQueue.addAction(targetNodeToTest.getAction());
					changeStatus(Status.ROLLOUT_POLICY_EXECUTING);
				}

				break;
			case ROLLOUT_POLICY_EXECUTING:
				executeNextOnQueue(); // Execute a single timestep with the actions that have been queued.

				// When done, record state and go back to choosing. If failed, the sampler guards will tell us.
				if (actionQueue.isEmpty() || game.getFailureStatus()) {
					// TODO possibly update the action to what was actually possible until the runner fell. Subtract out the extra timesteps that weren't possible due to failure.
					currentGameNode = targetNodeToTest;
					if(currentGameNode.state != null) throw new RuntimeException("The expansion policy should only encounter new nodes. None of them should have their state assigned before now.");
					currentGameNode.setState(getGameState());
					sampler.rolloutPolicyActionDone(currentGameNode);
					changeStatus(Status.ROLLOUT_POLICY_CHOOSING);
				}

				break;		
			case EVALUATE_GAME:
				if (currentGameNode.isFailed()) { // 2/20/18 I don't remember why I put a conditional here. I've added an error to see if this ever actually is not true.
					currentGameNode.markTerminal();
				}else {
					// Not necessarily true with the FixedDepth sampler.
					//throw new RuntimeException("FSM_tree shouldn't be entering evaluation state unless the game is in a failed state.");
				}
				saver.reportGameEnding(currentGameNode);
				long gameTs = game.getTimestepsSimulated();
				addToTotalTimesteps(gameTs);
				workerGamesPlayed.increment();

				tsPerSecond = (int)(0.9f * tsPerSecond + 0.1f * 1000f * gameTs/(System.currentTimeMillis() - startMs));
				incrementTotalGameCount();

				expansionNode.releaseExpansionRights();

				if (debugDraw) {
					expansionNode.clearBackwardsBranchColor();
					expansionNode.clearBackwardsBranchZOffset();
				}

				if (rootNode.fullyExplored.get()) {
					changeStatus(Status.EXHAUSTED);
				}else {
					changeStatus(Status.IDLE);
				}
				break;
			case EXHAUSTED:
				System.out.println("Tree is fully explored.");
				terminateWorker();
				changeStatus(Status.IDLE);
				break;		
			default:
				break;
			}
		}
	}

	/** Do not directly change the game status. Use this. **/
	private void changeStatus(Status newStatus) {
		if (verbose) {
			System.out.println("Worker " + workerID + ": " + currentStatus + " --->  " + newStatus + "     game: " + workerGamesPlayed);
		}
		prevStatus = currentStatus;
		currentStatus = newStatus;
	}

	/** Begin a new game. **/
	private void newGame(){
		game.makeNewWorld();
	}

	public void addAction(Action action){
		actionQueue.addAction(action);
	}

	/** Pop the next action off the queue and execute one timestep. **/
	private void executeNextOnQueue() {
		if (!actionQueue.isEmpty()) {
			Action action = actionQueue.peekThisAction();
			boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
			boolean Q = nextCommand[0];
			boolean W = nextCommand[1]; 
			boolean O = nextCommand[2];
			boolean P = nextCommand[3];
			game.stepGame(Q,W,O,P);
			saver.reportTimestep(action, game);
			workerStepsSimulated++;
		}
	}

	/** Has the game gotten into a failed state (Either too much torso lean or body parts hitting the ground). **/
	public boolean isGameFailed() {
		return game.getFailureStatus();
	}

	/** QWOP initial condition. Good way to give the root node a state. **/
	public  State getInitialState(){
		return initState;
	}

	/** Get the state of the runner. **/
	public State getGameState(){
		return game.getCurrentState();
	}

	/** How many physics timesteps has this particular worker simulated? **/
	public long getWorkerStepsSimulated() {
		return workerStepsSimulated;
	}
	/** Terminate this worker after it's done with it's current task. **/
	public void terminateWorker() {
		flagForTermination.set(true);
	}

	/** Get the running average of timesteps simulated per second of realtime. **/
	public int getTsPerSecond() {
		return tsPerSecond;
	}

	/** Increase the the count of total games in a hopefully thread-safe way. **/
	private static long incrementTotalGameCount() {
		totalGamesPlayed.increment();
		return totalGamesPlayed.longValue();
	}

	/** Increase the number of timesteps simulated in a hopefully thread-safe way. **/
	private static long addToTotalTimesteps(long timesteps) {
		totalStepsSimulated.add(timesteps);
		return totalStepsSimulated.longValue();
	}

	/** Get the number of games played by all workers, total. **/
	public static long getTotalGamesPlayed() {
		return totalGamesPlayed.longValue();
	}

	/** Get the number of games played by all workers, total. **/
	public static long getTotalTimestepsSimulated() {
		return totalStepsSimulated.longValue();
	}

	/** Color the node scaled by depth in the tree. Skip the brightness argument for default value. **/
	public static Color getColorFromWorkerID(int ID){
		float brightness = 0.85f;
		float coloffset = 0f;
		float scaledDepth = (float)ID/4f;;
		float H = scaledDepth* 0.38f+coloffset;
		float S = 0.8f;
		return Color.getHSBColor(H, S, brightness);
	}

	/** Debug drawer. If you just want to display a run, use one of the other PanelRunner implementations. **/
	@Override
	public void paintComponent(Graphics g) {
		if (!active || game == null) return;
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		// Draw all non-highlighted runners.
		game.draw(g2, runnerScaling, xOffsetPixels, yOffsetPixels);
	}

	@Override
	public void deactivateTab() { active = false; } // Not really applicable.

	@Override
	public void update(Node node) {}
}


