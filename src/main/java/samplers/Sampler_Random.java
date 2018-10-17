package samplers;

import actions.Action;
import tree.Node;
import tree.Utility;

/**
 * Dumb sampler, either for filling a space or testing purposes.
 * Goes through the tree randomly and eventually adds nodes (randomly) until failure.
 *
 * @author Matt
 */
public class Sampler_Random implements ISampler {

    private boolean treePolicyDone = false;
    private boolean expansionPolicyDone = false;

    public Sampler_Random() {
    }

    @Override
    public Node treePolicy(Node startNode) {
        if (startNode.fullyExplored.get()) {
            throw new RuntimeException("Trying to do tree policy on a given node which is already fully-explored. " +
					"Whoever called this is at fault.");
        }
        Node currentNode = startNode;

        while (true) {
            if (currentNode.fullyExplored.get())
                currentNode = startNode; // Just start over for now. I don't think it's a big enough problem to
			// stress over.
            //throw new RuntimeException("Tree policy got itself to a node which is fully-explored. This is its fault
			// .");

            // Count the number of available children to go to next.
            int notFullyExploredChildren = 0;
            for (Node child : currentNode.getChildren()) {
                if (!child.fullyExplored.get() && !child.isLocked()) notFullyExploredChildren++;
            }

            if (notFullyExploredChildren == 0 && currentNode.uncheckedActions.isEmpty()) {
                currentNode = startNode;
                continue; // TODO: investigate this error further.
//				currentNode.nodeColor = Color.PINK;
//				System.out.println(currentNode.isLocked());
//				currentNode.displayPoint = true;
//				throw new RuntimeException("Sampler has nowhere to go from here and should have been marked fully
// explored before.");
            }

            if (notFullyExploredChildren == 0 && currentNode.reserveExpansionRights()) {
                // We got to a place we'd like to expand. Stop tree policy, hand over to expansion policy.
                return currentNode;
            }

            if (currentNode.uncheckedActions.size() == 0) { // No unchecked actions means that we pick a random
            	// not-fully-explored child.
                // Pick random not fully explored child. Keep going.
                int selection = Utility.randInt(0, notFullyExploredChildren - 1);
                int count = 0;
                for (Node child : currentNode.getChildren()) {
                    if (!child.fullyExplored.get() && !child.isLocked()) {
                        if (count == selection) {
                            currentNode = child;
                            break;
                        } else {
                            count++;
                        }
                    }
                }
            } else {
                // Probability decides.
                int selection = Utility.randInt(1, notFullyExploredChildren + currentNode.uncheckedActions.size());
                // Make a new node or pick a not fully explored child.
                if (selection > notFullyExploredChildren && currentNode.reserveExpansionRights()) {
                    if (currentNode.getState() != null && currentNode.getState().isFailed())
                        throw new RuntimeException("Sampler tried to return a failed state for its tree policy.");
                    return currentNode;
                } else {
                    int count = 1;

                }
            }
        }
    }

    @Override
    public Node expansionPolicy(Node startNode) {
        if (startNode.uncheckedActions.size() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");

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
        // Rollout policy not in use in the random sampler.
        return true; // No rollout policy
    }

    @Override
    public void treePolicyActionDone(Node currentNode) {
        treePolicyDone = true; // Enable transition to next through the guard.
        expansionPolicyDone = false; // Prevent transition before it's done via the guard.
    }

    @Override
    public void expansionPolicyActionDone(Node currentNode) {
        treePolicyDone = false;
        expansionPolicyDone = currentNode.getState().isFailed();
    }

    @Override
    public void rolloutPolicyActionDone(Node currentNode) {
    } // No rollout in random sampler.

    @Override
    public Sampler_Random getCopy() {
        return new Sampler_Random();
    }

}
