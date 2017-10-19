package main;

public class Sampler_UCB implements ISampler {


	float c = 0.8f;


	/** Are we done with the tree policy? **/
	private boolean treePolicyDone = false;

	/** Are we done with the expansion policy? **/
	private boolean expansionPolicyDone = false;

	/** Are we done with the rollout policy? **/
	private boolean rolloutPolicyDone = false;

	@Override
	public Node treePolicy(Node startNode) {
		if (!startNode.uncheckedActions.isEmpty()) { // We immediately expand if there's an untried action.
			return startNode;
		}

		double bestScoreSoFar = -Double.MAX_VALUE;
		Node bestNodeSoFar = null;

		// Otherwise, we go through the existing tree picking the "best."
		Node parent = startNode;
		for (Node child: parent.children){

			if (!child.fullyExplored){
				float val = child.ucbValue/(float)child.visitCount + c*(float)Math.sqrt(2.*Math.log((float)parent.visitCount)/(float)child.visitCount);
				System.out.println(val);
				if (val > bestScoreSoFar){
					bestNodeSoFar = child;
					bestScoreSoFar = val;
				}
			}
		}

		return bestNodeSoFar;
	}

	@Override
	public void treePolicyActionDone(Node currentNode) {
		treePolicyDone = true; // Enable transition to next through the guard.
		expansionPolicyDone = false; // Prevent transition before it's done via the guard.
	}

	@Override
	public boolean treePolicyGuard(Node currentNode) {
		return treePolicyDone; // True means ready to move on to the next.
	}

	@Override
	public Node expansionPolicy(Node startNode) {
		if (startNode.uncheckedActions.size() == 0) throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");
		Action childAction = startNode.uncheckedActions.get(Node.randInt(0,startNode.uncheckedActions.size() - 1));
		
		return startNode.addChild(childAction);
	}

	@Override
	public void expansionPolicyActionDone(Node currentNode) {
		expansionPolicyDone = true; // We move on after adding only one node.
		rolloutPolicyDone = false;
	}

	@Override
	public boolean expansionPolicyGuard(Node currentNode) {
		return expansionPolicyDone;
	}

	@Override
	public Node rolloutPolicy(Node startNode) {
		// Do shit without adding nodes to the rest of the tree hierarchy.
		Action childAction = startNode.uncheckedActions.get(Node.randInt(0,startNode.uncheckedActions.size() - 1));
		
		Node rolloutNode = new Node(startNode,childAction,false);
		
		return rolloutNode;
	}

	@Override
	public void rolloutPolicyActionDone(Node currentNode) {
		expansionPolicyDone = false;
		
		if (currentNode.state.failedState) {
			rolloutPolicyDone = true;
			
			float score = currentNode.state.body.x;
			// Do evaluation and propagation of scores.
			currentNode.visitCount++;
			currentNode.ucbValue += score;
			while (currentNode.treeDepth > 0){//TODO test 0
				currentNode = currentNode.parent;
				currentNode.visitCount++;
				currentNode.ucbValue += score;
			}	
		}else {
			rolloutPolicyDone = false;
		}
	}

	@Override
	public boolean rolloutPolicyGuard(Node currentNode) {
		return rolloutPolicyDone;
	}
}
