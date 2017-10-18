package main;

/**
 * Dumb sampler, either for filling a space or testing purposes.
 * Goes through the tree randomly and eventually adds nodes (randomly) until failure.
 * @author Matt
 *
 */
public class Sampler_Random implements ISampler {

	public Sampler_Random() {}

	@Override
	public Node treePolicy(Node startNode) {
		if (startNode.fullyExplored) throw new RuntimeException("Trying to do tree policy on a given node which is already fully-explored. Whoever called this is at fault.");
		Node currentNode = startNode;

		while (true) {
			if (currentNode.fullyExplored) throw new RuntimeException("Tree policy got itself to a node which is fully-explored. This is its fault.");

			// Count the number of available children to go to next.
			int notFullyExploredChildren = 0;
			for (Node child : currentNode.children) {
				if (!child.fullyExplored) notFullyExploredChildren++;
			}

			if (notFullyExploredChildren == 0 && currentNode.uncheckedActions.size() == 0) throw new RuntimeException("Sampler has nowhere to go from here and should have been marked fully explored before.");

			
			if (notFullyExploredChildren == 0) {
				// We got to a place we'd like to expand. Stop tree policy, hand over to expansion policy.
				return currentNode;
			}
			
			if (currentNode.uncheckedActions.size() == 0) { // No unchecked actions means that we pick a random not-fully-explored child.
				// Pick random not fully explored child. Keep going.
				int selection = Node.randInt(0, notFullyExploredChildren - 1);
				int count = 0;
				for (Node child : currentNode.children) {
					if (!child.fullyExplored) {
						if (count == selection) {
							currentNode = child;
							break;
						}else {
							count++;
						}
					}
				}
			}else {
				// Probability decides.
				int selection = Node.randInt(1, notFullyExploredChildren + currentNode.uncheckedActions.size());
				// Make a new node or pick a not fully explored child.
				if (selection > notFullyExploredChildren){
					return currentNode;
				}else{
					int count = 1;
					for (Node child : currentNode.children) {
						if (!child.fullyExplored) {
							if (count == selection) {
								currentNode = child;
								break;
							}else {
								count++;
							}
						}
					}	
				}	
			}
		}
	}

	@Override
	public Node expansionPolicy(Node startNode) {
		if (startNode.uncheckedActions.size() == 0) throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");
		
		Action childAction = startNode.uncheckedActions.get(Node.randInt(0,startNode.uncheckedActions.size() - 1));
		
		return startNode.addChild(childAction);
	}

	@Override
	public Node rolloutPolicy(Node startNode) {
		// No rollout policy.
		return null;
	}

	@Override
	public boolean treePolicyGuard(Node currentNode) {
		return true; // Never any case to return to tree policy for us.
	}

	@Override
	public boolean expansionPolicyGuard(Node currentNode) {
		// Keep going back to expansion policy until failure.
		return false;
	}

	@Override
	public boolean rolloutPolicyGuard(Node currentNode) {
		return true; // No rollout policy
	}

	@Override
	public void treePolicyActionDone(Node currentNode) {

	}

	@Override
	public void expansionPolicyActionDone(Node currentNode) {

	}

	@Override
	public void rolloutPolicyActionDone(Node currentNode) {

	}
	
}
