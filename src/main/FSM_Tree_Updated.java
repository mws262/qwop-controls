package main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.dynamics.ContactListener;
import org.jbox2d.dynamics.contacts.ContactPoint;
import org.jbox2d.dynamics.contacts.ContactResult;

import main.FSM_Game.ActionQueue;
import main.FSM_Game.Status;

/**
 * Addresses limitations of the old concurrent state machine approach.
 * Instead, both tree FSM and game FSM are combined but may both 
 * operate on the same tree as other of the new FSM_Trees The idea is
 * to run many instances of this class in parallel.
 * 
 * @author matt
 *
 */

public class FSM_Tree_Updated implements Runnable {

	public enum Status{
		IDLE, INITIALIZE, TREE_POLICY_CHOOSING, TREE_POLICY_EXECUTING, EXPANSION_POLICY_CHOOSING, EXPANSION_POLICY_EXECUTING, ROLLOUT_POLICY_CHOOSING,  ROLLOUT_POLICY_EXECUTING, EVALUATE_GAME, EXHAUSTED
	}
	
	/** The current game instance that this FSM is using. This will frequently change since a new one is created for each run. **/
	private QWOPGame game;
	
	/** Collision listener to determine when the game has failed. **/
	private CollisionListener colListen = new CollisionListener();
	
	
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
	
	/** Is the game in a currently failed state? **/
	private boolean failFlag = false;
	
	/** Current status of this FSM **/
	private Status currentStatus = Status.IDLE;
	
	/** Flags for each of the QWOP keys being down **/
	public boolean Q = false;
	public boolean W = false;
	public boolean O = false;
	public boolean P = false;
	
	/** Physics engine stepping parameters. **/
	public final float timestep = 0.04f;
	private final int iterations = 5;
	private float stepsSimulated = 0;
	
	/** Angle failure limits. Fail if torso angle is too big or small to rule out stupid hopping that eventually falls. **/
	public float torsoAngUpper = 1.2f;
	public float torsoAngLower = -1.2f; // Negative is falling backwards. 0.4 is start angle.
	
	
	public FSM_Tree_Updated(ISampler sampler) {
		this.sampler = sampler;
		
		// Find initial runner state for later use.
		QWOPGame g = new QWOPGame();
		g.Setup();
		initState = new State(g);
		initState.failedState = false;
	}





	/* Finite state machine loop. Runnable. */
	public void run() {
		switch(currentStatus) {
		case IDLE:
			break;
		case INITIALIZE:
			actionQueue.clearAll();
			newGame(); // Create a new game world.
			changeStatus(Status.TREE_POLICY_CHOOSING);
			
			break;		
		case TREE_POLICY_CHOOSING: // Picks a target leaf node within the tree to get to.
			if (isGameFailed()) throw new RuntimeException("Tree policy operates only within an existing tree. We should not find failures in here.");
			
			if (sampler.treePolicyGuard(currentGameNode)) { // Sampler tells us when we're done with the tree policy.
				changeStatus(Status.EXPANSION_POLICY_CHOOSING);
				sampler.treePolicyActionDone(currentGameNode);
			}else {
				targetNodeToTest = sampler.treePolicy(currentGameNode); // This gets us through the existing tree to a place that we plan to add a new node.
				
				// Check special cases.
				if (targetNodeToTest == currentGameNode) { // We must be at the fringe of the tree and we should switch to expansion policy.
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
			if (isGameFailed()) throw new RuntimeException("Game encountered a failure while executing the tree policy. The tree policy should be safe, since it's ground that's been covered before.");
			
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
			if (isGameFailed()) throw new RuntimeException("Should not be returning to expansion policy choosing status if the game has failed.");
			
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
			if (actionQueue.isEmpty || failFlag) {
				// TODO possibly update the action to what was actually possible until the runner fell. Subtract out the extra timesteps that weren't possible due to failure.
				currentGameNode = targetNodeToTest;
				if(currentGameNode.state != null) throw new RuntimeException("The expansion policy should only encounter new nodes. None of them should have their state assigned before now.");
				currentGameNode.setState(getGameState());
				sampler.expansionPolicyActionDone(currentGameNode);
				changeStatus(Status.EXPANSION_POLICY_CHOOSING);
				
				try {
					if (currentGameNode.state.failedState && failFlag){ // If we've added a terminal node, we need to see how this affects the exploration status of the rest of the tree.
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
			if (actionQueue.isEmpty || failFlag) {
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
	}
	
	private void changeStatus(Status newStatus) {
		currentStatus = newStatus;
	}
	
	
	
	
	/** Begin a new game. **/
	private void newGame(){
		failFlag = false; // Unflag failure.
		game = new QWOPGame();
		game.Setup();
		colListen.resetContacts();
		game.getWorld().setContactListener(colListen);
	}
	
	public void addAction(Action action){
		actionQueue.addAction(action);
	}
	
	private void executeNextOnQueue() {
		boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
		Q = nextCommand[0];
		W = nextCommand[1]; 
		O = nextCommand[2];
		P = nextCommand[3];

		game.everyStep(Q,W,O,P);
		game.getWorld().step(timestep, iterations);
		stepsSimulated++;
		
		// Extra fail conditions besides contacts.
		float angle = game.TorsoBody.getAngle();
		if (angle > torsoAngUpper || angle < torsoAngLower) {
			failGame();
		}
	}
	
	private void failGame() {
		failFlag = true;
	}
	
	public boolean isGameFailed() {
		return failFlag;
	}
	
	/** QWOP initial condition. Good way to give the root node a state. **/
	public  State getInitialState(){
		return initState;
	}
	
	/** Get the state of the runner. **/
	public State getGameState(){
		State currentState = new State(game);
		currentState.failedState = failFlag;
		return currentState;
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
	
	
	/** Listens for collisions involving lower arms and head (implicitly with the ground) **/
	private class CollisionListener implements ContactListener{

		/** Keep track of whether the right foot is on the ground. **/
		private boolean rFootDown = false;
		
		/** Keep track of whether the left foot is on the ground. **/
		private boolean lFootDown = false;

		public CollisionListener(){}
		
		@Override
		public void add(ContactPoint point) {
			Shape fixtureA = point.shape1;
			Shape fixtureB = point.shape2;
			//Failure when head, arms, or thighs hit the ground.
			if(fixtureA.m_body.equals(game.HeadBody) ||
					fixtureB.m_body.equals(game.HeadBody) ||
					fixtureA.m_body.equals(game.LLArmBody) ||
					fixtureB.m_body.equals(game.LLArmBody) ||
					fixtureA.m_body.equals(game.RLArmBody) ||
					fixtureB.m_body.equals(game.RLArmBody)) {
				failGame();
			}else if(fixtureA.m_body.equals(game.LThighBody)||
					fixtureB.m_body.equals(game.LThighBody)||
					fixtureA.m_body.equals(game.RThighBody)||
					fixtureB.m_body.equals(game.RThighBody)){
				failGame();
			}else if(fixtureA.m_body.equals(game.RFootBody) || fixtureB.m_body.equals(game.RFootBody)){//Track when each foot hits the ground.
				rFootDown = true;		
			}else if(fixtureA.m_body.equals(game.LFootBody) || fixtureB.m_body.equals(game.LFootBody)){
				lFootDown = true;
			}	
		}
		@Override
		public void persist(ContactPoint point){}
		@Override
		public void remove(ContactPoint point) {
			//Track when each foot leaves the ground.
			Shape fixtureA = point.shape1;
			Shape fixtureB = point.shape2;
			if(fixtureA.m_body.equals(game.RFootBody) || fixtureB.m_body.equals(game.RFootBody)){
				rFootDown = false;
			}else if(fixtureA.m_body.equals(game.LFootBody) || fixtureB.m_body.equals(game.LFootBody)){
				lFootDown = false;
			}	
		}
		@Override
		public void result(ContactResult point) {}

		/** Check if the right foot is touching the ground. **/
		@SuppressWarnings("unused")
		public boolean isRightFootGrounded(){
			return rFootDown;
		}

		/** Check if the left foot is touching the ground. **/
		@SuppressWarnings("unused")
		public boolean isLeftFootGrounded(){
			return lFootDown;
		}
		
		/** Reset existing contacts so this collision listener can be reused in new games. **/
		public void resetContacts() {
			rFootDown = false;
			lFootDown = false;
		}
	}
	
}


