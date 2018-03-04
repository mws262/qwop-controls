package main;
import java.util.ArrayList;

public class FSM_Tree implements Runnable{

	/** Continues looping the thread until killed with this. **/
	private boolean running = true;

	/* State machine current and previous loop status. Only reports to negotiator when changes. */
	public Status currentStatus = Status.IDLE;
	public Status previousStatus = Status.IDLE;
	private volatile Status queuedStatusChange;
	private volatile boolean forcedStatusChange = false;

	/** Queued sampler ready to swap in when the FSM is ready for it. **/
	private ISampler queuedSamplerChange = null;

	/** Reports changes to negotiator and uses it to get reports from the game. **/
	private Negotiator negotiator;

	/** Strategy for sampling new nodes. **/
	ISampler sampler;

	/* List of root nodes being tracked. TODO: currently only supports one. **/
	public ArrayList<Node> rootNodes = new ArrayList<Node>();

	/** Targeted node to be tested. Negotiator looks at this. **/
	public Node targetNodeToTest;
	/** Node from where we are sampling now. **/
	public Node currentNode;
	/** Root of tree we care about. **/
	public Node currentRoot;

	/** Whether the FSM is locked for some other action to continue. **/
	private volatile boolean locked = false;
	/** Confirms whether the FSM has finished its previous cycle and is effectively locked. **/
	private volatile boolean isLocked = false;

	/** How often do we estimate games/s? **/
	private int gpsFrequency = 25;
	private int gpsCounter = 1; // Counts up to gpsFrequency at which point gps is calculated.
	private long lastTime = System.currentTimeMillis();
	public float currentGPS = 120;
	private int gpsFilter = 5;

	public enum Status{
		IDLE, INITIALIZE, TREE_POLICY, EXPANSION_POLICY, ROLLOUT_POLICY, TREE_POLICY_WAITING, EXPANSION_POLICY_WAITING, ROLLOUT_POLICY_WAITING, EVALUATE_GAME, EXHAUSTED
	}

	/* Handles creating the tree of many games. TRUE passed in turns on the visualization (off by default). */
	public FSM_Tree(ISampler sampler) {
		this.sampler = sampler;
	}

	/* Finite state machine loop. Runnable. */
	public void run() {
		while (running){ // Call kill() to break
			if (rootNodes.isEmpty()){
				continue;
			}

			if (locked){
				isLocked = true;
				continue; // If we call one of the lock methods, the loop skips everything.
			}else{
				isLocked = false;
			}

			// Evaluate the state
			switch(currentStatus){
			case IDLE:
				currentStatus = Status.INITIALIZE;
				break;
			case INITIALIZE:
				currentRoot = rootNodes.get(0);
				currentNode = currentRoot;
				currentStatus = Status.TREE_POLICY;
				break;

				/******** Getting through the tree to the point we plan to add new nodes. *********/
			case TREE_POLICY:
				if (!sampler.treePolicyGuard(currentNode)) {
					targetNodeToTest = sampler.treePolicy(currentNode); // This gets us through the existing tree to a place that we plan to add a new node.

					if (targetNodeToTest.treeDepth > 0) {
						currentStatus = Status.TREE_POLICY_WAITING; // Need to wait for negotiator to get the game played.
					}else {
						currentNode = targetNodeToTest;
						sampler.treePolicyActionDone(currentNode);
						currentStatus = Status.EXPANSION_POLICY;
					}

				}else {
					currentStatus = Status.EXPANSION_POLICY;
				}
				break;
			case TREE_POLICY_WAITING:
				// Sit here until we receive a game state to add to our node.
				currentNode = targetNodeToTest;
				break;

				/******** Doing tree expansion. *********/
			case EXPANSION_POLICY:
				if (!sampler.expansionPolicyGuard(currentNode)) {
					targetNodeToTest = sampler.expansionPolicy(currentNode);
					currentStatus = Status.EXPANSION_POLICY_WAITING;			
				}else {
					currentStatus = Status.ROLLOUT_POLICY;
				}
				break;
			case EXPANSION_POLICY_WAITING:
				// Sit here until we receive a game state to add to our node.
				currentNode = targetNodeToTest;
				break;

				/******** Doing post-simulation for evaluation purposes. No tree expansion. *********/
			case ROLLOUT_POLICY:
				if (!sampler.rolloutPolicyGuard(currentNode)) {
					targetNodeToTest = sampler.rolloutPolicy(currentNode);
					currentStatus = Status.ROLLOUT_POLICY_WAITING;
				}else {
					currentStatus = Status.EVALUATE_GAME;
				}
				break;
			case ROLLOUT_POLICY_WAITING:
				currentNode = targetNodeToTest;
				break;
			case EVALUATE_GAME: // Triggered once tree is given a failed state.

				if (currentNode.state.failedState) { // 2/20/18 I don't remember why I put a conditional here. I've added an error to see if this ever actually is not true.
					currentNode.markTerminal();
				}else {
					throw new RuntimeException("FSM_tree shouldn't be entering evaluation state unless the game is in a failed state.");
				}
				
				currentStatus = Status.IDLE;

				// Estimate games per second if the frequency met.
				if (gpsCounter % gpsFrequency == 0){
					currentGPS = (currentGPS * (gpsFilter - 1) + 1000f * gpsFrequency/(float)(System.currentTimeMillis() - lastTime))/gpsFilter;
					gpsCounter = 1;
					lastTime = System.currentTimeMillis();
				}else{
					gpsCounter++;
				}

				// Sampler change safely if queued.
				if (queuedSamplerChange != null) {
					sampler = queuedSamplerChange;
					queuedSamplerChange = null;
				}
				break;
			case EXHAUSTED: // Similar to idle, but we haven't decided whether to introduce more options or start over.
				kill();
				break;
			default:
				throw new RuntimeException("Unknown tree builder state.");
			}

			if (currentRoot != null && currentRoot.fullyExplored) currentStatus = Status.EXHAUSTED;

			// Handle external status changes in a way that lets them be reported properly. 
			if (forcedStatusChange){
				currentStatus = queuedStatusChange;
				queuedStatusChange = null;
				forcedStatusChange = false;
			}

			// Report up if something has changed.
			if (previousStatus != currentStatus){
				negotiator.statusChange_tree(currentStatus);
			}
			previousStatus = currentStatus;
		}
	}

	/** Give the tree a state to assign to a node it's investigating. **/
	public void giveGameState(State state){
		switch (currentStatus){
		case TREE_POLICY_WAITING: // Should only ever be going through nodes which have a game state assigned.
			// Either go forward to the rollout policy or backwards to expansion depending on whether the guard says ready.
			if (state != null && state.failedState) throw new RuntimeException("Shouldn't be encountering failed states while executing tree policy.");
			sampler.treePolicyActionDone(currentNode);
			setStatus(Status.TREE_POLICY);
			break;
		case EXPANSION_POLICY_WAITING:
			if(currentNode.state != null) throw new RuntimeException("The expansion policy should only encounter new nodes. None of them should have their state assigned before now.");
			currentNode.setState(state);

			try {
				if (state.failedState){ // If we've added a terminal node, we need to see how this affects the exploration status of the rest of the tree.
					targetNodeToTest.checkFullyExplored_lite();
				}
			}catch (NullPointerException e){
				throw new NullPointerException("Tree was given a game state that did not have a failure status assigned. " + e.getMessage());
			}

			// Either go forward to the rollout policy or backwards to expansion depending on whether the guard says ready.
			sampler.expansionPolicyActionDone(currentNode);
			setStatus(Status.EXPANSION_POLICY);
			break;
		case ROLLOUT_POLICY_WAITING:
			if(currentNode.state != null) throw new RuntimeException("The rollout policy should only encounter new nodes. None of them should have their state assigned before now.");
			currentNode.setState(state);

			// Either go forward to the rollout policy or backwards to expansion depending on whether the guard says ready.
			sampler.rolloutPolicyActionDone(currentNode);
			setStatus(Status.ROLLOUT_POLICY);
			break;
		default: // Should only be called when the tree is expecting to receive a game state. Otherwise, this:
			throw new RuntimeException("Someone passed the tree a game state when the tree wasn't waiting for one.");
		}
	}

	/** Force a status change. DO NOT try to just change currentStatus. **/
	private void setStatus(Status status){
		queuedStatusChange = status;
		forcedStatusChange = true;
	}


	/** Get the FSM status. **/
	public Status getFSMStatus(){
		return currentStatus;
	}

	/** Stop the FSM after the current cycle, and return the status. Prevents changes from having odd effects in the middle of a cycle. **/
	public Status getFSMStatusAndLock(){
		if (isLocked) throw new RuntimeException("Someone tried to lock the game while it was already locked.");
		locked = true;

		while (!isLocked);

		return getFSMStatus();
	}

	/** Stop the FSM after the current cycle, and return the status. 
	 * Prevents changes from having odd effects in the middle of a cycle.
	 * Returns whether or not it did anything.
	 **/
	public boolean unlockFSM(){
		if (!locked) return false;
		if (!isLocked) throw new RuntimeException("Tried to unlock the game FSM, but it never got around to locked in the first place. This really shouldn't happen.");
		locked = false;
		while (isLocked);
		return true;
	}

	/** Change the sampler safely. **/
	public void changeSampler(ISampler newSampler) {
		queuedSamplerChange = newSampler;
	}

	/** Wait until a specific status is reached, and then pause there. Returns whether it succeeded or not. **/
	public boolean latchAtFSMStatus(Status status){
		if (isLocked) return false; //throw new RuntimeException("Someone tried to latch the game while it was already latched.");

		getFSMStatusAndLock();
		while (currentStatus != status){
			unlockFSM();
			getFSMStatusAndLock();
		}

		if (currentStatus != status){
			throw new RuntimeException("Tried to lock at status: " + status.toString() + ". Actually got: " + currentStatus.toString());
		}
		return true;
	}


	public void setNegotiator(Negotiator negotiator) {
		this.negotiator = negotiator;
	}

	/* Completely break the out of the FSM loop. Usually it's better to switch to an IDLE state instead */
	public void kill(){
		running = false;
	}
}	