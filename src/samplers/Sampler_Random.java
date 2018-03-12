package samplers;

import main.Action;
import main.ISampler;
import main.Node;
import main.Utility;

/**
 * Dumb sampler, either for filling a space or testing purposes.
 * Goes through the tree randomly and eventually adds nodes (randomly) until failure.
 * @author Matt
 *
 */
public class Sampler_Random implements ISampler {

	private boolean treePolicyDone = false;
	private boolean expansionPolicyDone = false;
	private boolean rolloutPolicyDone = true; // Rollout policy not in use in the random sampler.
	
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

			// 3/3/18 Removed because I'm confused. But will it still work?
//			if (notFullyExploredChildren == 0) {
//				// We got to a place we'd like to expand. Stop tree policy, hand over to expansion policy.
//				return currentNode;
//			}
			
			if (currentNode.uncheckedActions.size() == 0) { // No unchecked actions means that we pick a random not-fully-explored child.
				// Pick random not fully explored child. Keep going.
				int selection = Utility.randInt(0, notFullyExploredChildren - 1);
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
				int selection = Utility.randInt(1, notFullyExploredChildren + currentNode.uncheckedActions.size());
				// Make a new node or pick a not fully explored child.
				if (selection > notFullyExploredChildren){
					if (currentNode.state.failedState) throw new RuntimeException("Sampler tried to return a failed state for its tree policy.");
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
		
		Action childAction = startNode.uncheckedActions.getRandom();
		
		return startNode.addChild(childAction);
	}

	@Override
	public Node rolloutPolicy(Node startNode) {
		// No rollout policy.
		return null;
	}

	@Override
	public boolean treePolicyGuard(Node currentNode) {
		return treePolicyDone; // True means ready to move on to the next.
	}

	@Override
	public boolean expansionPolicyGuard(Node currentNode) {
		return expansionPolicyDone;
	}

	@Override
	public boolean rolloutPolicyGuard(Node currentNode) {
		return rolloutPolicyDone; // No rollout policy
	}

	@Override
	public void treePolicyActionDone(Node currentNode) {
		treePolicyDone = true; // Enable transition to next through the guard.
		expansionPolicyDone = false; // Prevent transition before it's done via the guard.
	}

	@Override
	public void expansionPolicyActionDone(Node currentNode) {
		treePolicyDone = false;
		if (currentNode.state.failedState) {
			expansionPolicyDone = true;
		}else {
			expansionPolicyDone = false;
		}
	}

	@Override
	public void rolloutPolicyActionDone(Node currentNode) {} // No rollout in random sampler.
	
}
