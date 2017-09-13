import java.util.ArrayList;

public class FSM_Tree implements Runnable{

	/* Continues looping the thread until killed with this. */
	private boolean running = true;

	/* State machine current and previous loop status. Only reports to negotiator when changes. */
	public Status currentStatus = Status.IDLE;
	public Status previousStatus = Status.IDLE;
	private Status queuedStatusChange;
	private boolean forcedStatusChange = false;
	
	/* Reports changes to negotiator and uses it to get reports from the game. */
	private Negotiator negotiator;

	/* List of root nodes being tracked. TODO: currently only supports one. */
	public ArrayList<TrialNodeMinimal> rootNodes = new ArrayList<TrialNodeMinimal>();

	/* Targeted node to be tested. Negotiator looks at this. */
	public TrialNodeMinimal targetNodeToTest;
	/* Node from where we are sampling now. */
	public TrialNodeMinimal currentNode;
	/* Root of tree we care about. */
	public TrialNodeMinimal currentRoot;
	
	public enum Status{
		IDLE, INITIALIZE_TREE, DO_PREDETERMINED, ADD_NODE, WAITING_FOR_PREDETERMINED, WAITING_FOR_SINGLE, EVALUATE_GAME, EXHAUSTED
	}

	/* Handles creating the tree of many games. TRUE passed in turns on the visualization (off by default). */
	public FSM_Tree() {
	}

	/* Finite state machine loop. Runnable. */
	public void run() {
		while (running){ // Call kill() to break
			if (rootNodes.isEmpty()){
				continue;
			}
			
			// Evaluate the state
			switch(currentStatus){
			case IDLE:
				currentStatus = Status.INITIALIZE_TREE;
				break;
			case INITIALIZE_TREE:
				// Either use an existing, loaded tree, or make a new one.
				//TODO placeholder for now.
				currentRoot = rootNodes.get(0);
				currentNode = currentRoot;
				currentStatus = Status.DO_PREDETERMINED;
				break;
			case DO_PREDETERMINED:
				// Target an untried node.
				targetNodeToTest = currentNode.sampleNode();
				currentStatus = Status.WAITING_FOR_PREDETERMINED; // Need to wait for negotiator to get the game played.
				break;
			case ADD_NODE: // Triggered by receiving a non-failed state from the tree.
				targetNodeToTest = currentNode.sampleNode();
				//currentRoot.stepTreePhys(5);
				//currentRoot.calcNodePos_below();
				currentStatus = Status.WAITING_FOR_SINGLE;
				break;	
			case WAITING_FOR_PREDETERMINED:
				// Sit here until we receive a game state to add to our node.
				currentNode = targetNodeToTest;

				break;
			case WAITING_FOR_SINGLE:
				// Sit here until we receive a game state to add to our node.
				currentNode = targetNodeToTest;
				break;
			case EVALUATE_GAME: // Triggered once tree is given a failed state.
				// TODO placeholder for now.
				//currentNode = currentRoot;
				negotiator.saveRunToFile(currentNode);
				currentStatus = Status.IDLE;
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
	public void giveGameState(CondensedStateInfo state){
		
		if (currentStatus == Status.WAITING_FOR_PREDETERMINED || currentStatus == Status.WAITING_FOR_SINGLE){
			targetNodeToTest.setState(state);
			try{
				if (state.failedState){
					targetNodeToTest.checkFullyExplored_lite();
					setStatus(Status.EVALUATE_GAME);					
				}else{
					//TODO Expand potential child nodes.
					targetNodeToTest.expandNodeChoices(25, 2);
					setStatus(Status.ADD_NODE);
				}
			}catch (NullPointerException e){
				throw new NullPointerException("Tree was given a game state that did not have a failure status assigned. " + e.getMessage());
			}
		}else{
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


	public void setNegotiator(Negotiator negotiator) {
		this.negotiator = negotiator;
	}

	/* Completely break the out of the FSM loop. Usually it's better to switch to an IDLE state instead */
	public void kill(){
		running = false;
	}
}	