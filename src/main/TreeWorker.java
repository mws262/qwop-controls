package main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Addresses limitations of the old concurrent state machine approach.
 * Instead, both tree FSM and game FSM are combined but may both 
 * operate on the same tree as other of the new FSM_Trees The idea is
 * to run many instances of this class in parallel.
 * 
 * @author matt
 *
 */

public class TreeWorker implements Runnable {

	public enum Status{
		IDLE, INITIALIZE, TREE_POLICY_CHOOSING, TREE_POLICY_EXECUTING, EXPANSION_POLICY_CHOOSING, EXPANSION_POLICY_EXECUTING, ROLLOUT_POLICY_CHOOSING,  ROLLOUT_POLICY_EXECUTING, EVALUATE_GAME, EXHAUSTED
	}
	
	/** Is this worker running the FSM repeatedly? **/
	private boolean workerRunning = true;
	
	/** Sets the FSM to stop running the next time it is idle. **/
	private boolean flagForTermination = false;
	
	/** Print debugging info? **/
	public boolean verbose = false;

	/** The current game instance that this FSM is using. This will frequently change since a new one is created for each run. **/
	private QWOPGame game;

	/** Strategy for sampling new nodes. **/
	private ISampler sampler;

	/** Root of tree that this FSM is operating on. **/
	private Node rootNode;

	/** Node that the game is currently operating at. **/
	private Node currentGameNode;

	/** Node the game is attempting to run to. **/
	private Node targetNodeToTest;

	/** Queued commands, IE QWOP key presses **/
	public ActionQueue actionQueue = new ActionQueue();

	/** Initial runner state. **/
	private State initState;

	/** Current status of this FSM **/
	private Status currentStatus = Status.IDLE;

	/** Flags for each of the QWOP keys being down **/
	public boolean Q = false;
	public boolean W = false;
	public boolean O = false;
	public boolean P = false;

	private long workerStepsSimulated = 0;

	public TreeWorker(Node rootNode, ISampler sampler) {
		this.sampler = sampler;
		this.rootNode = rootNode;

		// Find initial runner state for later use.
		QWOPGame g = new QWOPGame();
		initState = g.getCurrentGameState();
		initState.failedState = false;
	}

	/* Finite state machine loop. Runnable. */
	public void run() {
		while(workerRunning) {
			switch(currentStatus) {
			case IDLE:
				if (flagForTermination) {
					workerRunning = false;
				}else {
					changeStatus(Status.INITIALIZE);
				}
				
				break;
			case INITIALIZE:
				actionQueue.clearAll();
				newGame(); // Create a new game world.
				currentGameNode = rootNode;
				changeStatus(Status.TREE_POLICY_CHOOSING);

				break;		
			case TREE_POLICY_CHOOSING: // Picks a target leaf node within the tree to get to.
				if (isGameFailed()) throw new RuntimeException("Tree policy operates only within an existing tree. We should not find failures in here.");

				if (sampler.treePolicyGuard(currentGameNode)) { // Sampler tells us when we're done with the tree policy.
					changeStatus(Status.EXPANSION_POLICY_CHOOSING);
				}else {
					targetNodeToTest = sampler.treePolicy(currentGameNode); // This gets us through the existing tree to a place that we plan to add a new node.

					// Check special cases.
					if (targetNodeToTest.treeDepth == 0) { //targetNodeToTest == currentGameNode) { // We must be at the fringe of the tree and we should switch to expansion policy.
						currentGameNode = targetNodeToTest;
						changeStatus(Status.EXPANSION_POLICY_CHOOSING);
						sampler.treePolicyActionDone(currentGameNode);

					}else if (targetNodeToTest.treeDepth <= currentGameNode.treeDepth) {
						throw new RuntimeException("Picked a node in the tree policy that is further up the tree towards the root than the current node.");
					}else if (!targetNodeToTest.isOtherNodeAncestor(currentGameNode)) {
						throw new RuntimeException("Target node in the tree policy should be a descendant of the current node.");
					}else {
						// Otherwise, this is a valid target point. We should add its actions and then execute.
						actionQueue.addSequence(targetNodeToTest.getSequence());
						changeStatus(Status.TREE_POLICY_EXECUTING);
					}
				}

				break;
			case TREE_POLICY_EXECUTING:

				executeNextOnQueue(); // Execute a single timestep with the actions that have been queued.
				//if (isGameFailed()) throw new RuntimeException("Game encountered a failure while executing the tree policy. The tree policy should be safe, since it's ground that's been covered before.");

				// When all actions in queue are done, figure out what to do next.
				if (actionQueue.isEmpty) {
					currentGameNode = targetNodeToTest;
					if (currentGameNode.uncheckedActions.size() == 0) { // This case should only happen if another worker just happens to beat it here.
						System.out.println("Wow! Another worker must have finished this node off before this worker got here. We're going to keep running tree policy down the tree. If there aren't other workers, you should be worried.");
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
					sampler.expansionPolicyActionDone(currentGameNode);
				}else {
					targetNodeToTest = sampler.expansionPolicy(currentGameNode);
					if (currentGameNode.treeDepth + 1 != targetNodeToTest.treeDepth) {
						throw new RuntimeException("Expansion policy tried to sample a node more than 1 depth below it in the tree. This is bad since tree policy should be used"
								+ "to traverse the existing tree and expansion policy should only be used for adding new nodes and extending the tree.");
					}

					actionQueue.addAction(targetNodeToTest.getAction());
					changeStatus(Status.EXPANSION_POLICY_EXECUTING);
				}

				break;
			case EXPANSION_POLICY_EXECUTING:

				executeNextOnQueue(); // Execute a single timestep with the actions that have been queued.

				// When done, record state and go back to choosing. If failed, the sampler guards will tell us.
				if (actionQueue.isEmpty || game.getFailureStatus()) {
					// TODO possibly update the action to what was actually possible until the runner fell. Subtract out the extra timesteps that weren't possible due to failure.
					currentGameNode = targetNodeToTest;
					if(currentGameNode.state != null) throw new RuntimeException("The expansion policy should only encounter new nodes. None of them should have their state assigned before now.");
					currentGameNode.setState(getGameState());
					sampler.expansionPolicyActionDone(currentGameNode);
					changeStatus(Status.EXPANSION_POLICY_CHOOSING);

					try {
						if (currentGameNode.state.failedState && game.getFailureStatus()){ // If we've added a terminal node, we need to see how this affects the exploration status of the rest of the tree.
							targetNodeToTest.checkFullyExplored_lite();
						}
					}catch (NullPointerException e){
						throw new NullPointerException("Tree was given a game state that did not have a failure status assigned. " + e.getMessage());
					}
				}	

				break;		
			case ROLLOUT_POLICY_CHOOSING:
				if (sampler.rolloutPolicyGuard(currentGameNode)) {
					changeStatus(Status.EVALUATE_GAME);
				}else {
					targetNodeToTest = sampler.rolloutPolicy(currentGameNode);
					changeStatus(Status.ROLLOUT_POLICY_EXECUTING);
				}

				break;
			case ROLLOUT_POLICY_EXECUTING:
				executeNextOnQueue(); // Execute a single timestep with the actions that have been queued.

				// When done, record state and go back to choosing. If failed, the sampler guards will tell us.
				if (actionQueue.isEmpty || game.getFailureStatus()) {
					// TODO possibly update the action to what was actually possible until the runner fell. Subtract out the extra timesteps that weren't possible due to failure.
					currentGameNode = targetNodeToTest;
					if(currentGameNode.state != null) throw new RuntimeException("The expansion policy should only encounter new nodes. None of them should have their state assigned before now.");
					currentGameNode.setState(getGameState());
					sampler.rolloutPolicyActionDone(currentGameNode);
					changeStatus(Status.ROLLOUT_POLICY_CHOOSING);
				}

				break;		
			case EVALUATE_GAME:

				if (currentGameNode.state.failedState) { // 2/20/18 I don't remember why I put a conditional here. I've added an error to see if this ever actually is not true.
					currentGameNode.markTerminal();
				}else {
					throw new RuntimeException("FSM_tree shouldn't be entering evaluation state unless the game is in a failed state.");
				}
				if (rootNode.fullyExplored) {
					changeStatus(Status.EXHAUSTED);
				}else {
					changeStatus(Status.IDLE);
				}

				break;
			case EXHAUSTED:
				System.out.println("Tree is fully explored.");
				break;		
			default:
				break;
			}
//			try {
//				Thread.sleep(10);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
	}

	/** Do not directly change the game status. Use this. **/
	private void changeStatus(Status newStatus) {
		//if (verbose && newStatus != Status.ROLLOUT_POLICY_CHOOSING && newStatus != Status.ROLLOUT_POLICY_EXECUTING) {
			System.out.println(currentStatus + " --->  " + newStatus);
		//}
		currentStatus = newStatus;
	}


	/** Begin a new game. **/
	private void newGame(){
		game = new QWOPGame();
	}

	
	public void addAction(Action action){
		actionQueue.addAction(action);
	}

	/** Pop the next action off the queue and execute one timestep. **/
	private void executeNextOnQueue() {
		boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
		Q = nextCommand[0];
		W = nextCommand[1]; 
		O = nextCommand[2];
		P = nextCommand[3];
		game.stepGame(Q,W,O,P);
		workerStepsSimulated++;
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
		return game.getCurrentGameState();
	}
	
	/** How many physics timesteps has this particular worker simulated? **/
	public long getWorkerStepsSimulated() {
		return workerStepsSimulated;
	}
	/** Terminate this worker after it's done with it's current task. **/
	public void terminateWorker() {
		flagForTermination = true;
	}

	/**
	 * All things related to queueing actions should happen in here. Actions themselves act like queues,
	 * so this action queue decides when to switch actions when one is depleted.
	 * @author Matt
	 *
	 */
	public class ActionQueue{

		/** Actions are the delays between keypresses. **/
		private Queue<Action> actionQueue = new LinkedList<Action>();

		/** All actions done or queued since the last reset. Unlike the queue, things aren't removed until reset. **/
		private ArrayList<Action> actionListFull = new ArrayList<Action>();

		/** Integer action currently in progress. If the action is 20, this will be 20 even when 15 commands have been issued. **/
		private Action currentAction;

		/** Is there anything at all queued up to execute? Includes both the currentAction and the actionQueue **/
		private boolean isEmpty = true;

		public ActionQueue(){}

		/** See the action we are currently executing. Does not change the queue. **/
		public Action peekThisAction(){
			return currentAction;
		}

		/** See the next action we will execute. Does not change the queue. **/
		public Action peekNextAction(){
			return actionQueue.peek();
		}

		/** See the next keypresses. **/
		public boolean[] peekCommand(){
			return currentAction.peek();
		}

		/** Adds a new action to the end of the queue. **/
		public synchronized void addAction(Action action){
			actionQueue.add(action);
			actionListFull.add(action);
			isEmpty = false;
		}

		/** Add a sequence of actions. NOTE: sequence is NOT reset unless clearAll is called. **/
		public synchronized void addSequence(Action[] actions){
			for (int i = 0; i < actions.length; i++){
				addAction(actions[i]);
			}
		}

		/** Request the next QWOP keypress commands from the added sequence. **/
		public synchronized boolean[] pollCommand(){
			if (actionQueue.isEmpty() && (currentAction == null || !currentAction.hasNext())){
				throw new RuntimeException("Tried to get a command off the queue when nothing is queued up.");
			}

			// If the current action has no more keypresses, load up the next one.
			if (currentAction == null || !currentAction.hasNext()){
				if (currentAction != null) currentAction.reset();
				currentAction = actionQueue.poll();
				//if (currentAction == null) System.out.println("WTF");
			}

			boolean[] nextCommand = currentAction.poll();

			if (!currentAction.hasNext() && actionQueue.isEmpty()){
				currentAction.reset();
				isEmpty = true;
			}
			return nextCommand;
		}

		/** Remove everything from the queues and reset the sequence. **/
		public synchronized void clearAll(){
			actionQueue.clear();
			actionListFull.clear();
			if (currentAction != null) currentAction.reset();
			currentAction = null;

			//while (actionQueue.size() > 0 || currentAction != null
			isEmpty = true;
		}

		/** Check if the queue has anything in it. **/
		public synchronized boolean isEmpty(){ return isEmpty; }

		public Action[] getActionsInCurrentRun(){
			Action[] actions = new Action[actionListFull.size()];
			for (int i = 0; i < actions.length; i++){
				actions[i] = actionListFull.get(i);
			}
			return actions;
		}

		public int getCurrentActionIdx(){
			return actionListFull.size() - actionQueue.size() - 1;
		}
	}	
}


