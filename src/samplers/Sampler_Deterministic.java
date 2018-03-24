package samplers;

import main.Action;
import main.ISampler;
import main.Node;
import main.Utility;

/**
 * Super-simple depth-first search with NO random selection. Tries to pick the first node it finds with an untried option.
 * Used for debugging since deterministic behavior is nice for that kind of thing :)
 * 
 * @author Matt
 *
 */
public class Sampler_Deterministic implements ISampler {

	private boolean treePolicyDone = false;
	private boolean expansionPolicyDone = false;
	private boolean rolloutPolicyDone = true; // Rollout policy not in use in the random sampler.

	public Sampler_Deterministic() {}

	@Override
	public Node treePolicy(Node startNode) {
		if (startNode.fullyExplored) throw new RuntimeException("Trying to do tree policy on a given node at depth " + startNode.treeDepth + " which is already fully-explored. Whoever called this is at fault.");
		Node currentNode = startNode;

		while (true) {
			// TEMPORARY DELAY
			if (currentNode.fullyExplored || currentNode.isTerminal) throw new RuntimeException("Tree policy got itself to a node which is fully-explored. This is its fault.");
			if (currentNode.getLockStatus()) {
				currentNode = startNode;
				if (currentNode.treeDepth > startNode.treeDepth) {
					currentNode = currentNode.parent;
				}
				continue;
				//throw new RuntimeException("Deterministic sampler stuck on locked node at depth: " + currentNode.treeDepth);
			}
			// If the given node has unchecked options (e.g. root hasn't tried all possible immediate children), expand directly.
			if (!currentNode.uncheckedActions.isEmpty() && currentNode.reserveExpansionRights()) return currentNode;

			// Get the first child with some untried actions after it, or at least a not-fully-explored one.
			boolean foundViableChild = false;
			for (Node child : currentNode.children) {
				if (!child.uncheckedActions.isEmpty() && child.reserveExpansionRights()) {
					return child;
				}else if (!child.fullyExplored) {
					currentNode = child;
					foundViableChild = true;
					break;
				}
			}
			if (!foundViableChild) {
				System.out.println("One worker has to start over because its options were taken. Back to depth: " + startNode.treeDepth);
				currentNode = startNode;
			}
		}
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

		Action childAction = startNode.uncheckedActions.get(0); // Get the first available untried actions.

		return startNode.addChild(childAction);
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
	public boolean expansionPolicyGuard(Node currentNode) {
		return expansionPolicyDone;
	}

	@Override
	public Node rolloutPolicy(Node startNode) { return null; }

	@Override
	public void rolloutPolicyActionDone(Node currentNode) {}

	@Override
	public boolean rolloutPolicyGuard(Node currentNode) {
		return rolloutPolicyDone; // No rollout policy
	}

	@Override
	public Sampler_Deterministic clone() {
		return new Sampler_Deterministic();
	}

}
