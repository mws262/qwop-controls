package samplers;

import actions.Action;
import game.IGame;
import tree.NodeQWOPExplorableBase;
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
    public NodeQWOPExplorableBase<?> treePolicy(NodeQWOPExplorableBase<?> startNode) {
        if (startNode.isFullyExplored()) {
            throw new RuntimeException("Trying to do tree policy on a given node which is already fully-explored. " +
					"Whoever called this is at fault.");
        }
        NodeQWOPExplorableBase<?> currentNode = startNode;

        while (true) {
            if (currentNode.isFullyExplored())
                currentNode = startNode; // Just start over for now. I don't think it's a big enough problem to
			// stress over.

            // Count the number of available children to go to next.
            int notFullyExploredChildren = 0;
            for (NodeQWOPExplorableBase<?> child : currentNode.getChildren()) {
                if (!child.isFullyExplored() && !child.isLocked()) notFullyExploredChildren++;
            }

            if (notFullyExploredChildren == 0 && currentNode.getUntriedActionCount() == 0) {
                currentNode = startNode;
                continue; // TODO: investigate this error further.
            }

            if (notFullyExploredChildren == 0 && currentNode.reserveExpansionRights()) {
                // We got to a place we'd like to expand. Stop tree policy, hand over to expansion policy.
                return currentNode;
            }

            if (currentNode.getUntriedActionCount() == 0) { // No unchecked actions means that we pick a random
            	// not-fully-explored child.
                // Pick random not fully explored child. Keep going.
                int selection = Utility.randInt(0, notFullyExploredChildren - 1);
                int count = 0;
                for (NodeQWOPExplorableBase<?> child : currentNode.getChildren()) {
                    if (!child.isFullyExplored() && !child.isLocked()) {
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
                int selection = Utility.randInt(1, notFullyExploredChildren + currentNode.getUntriedActionCount());
                // Make a new node or pick a not fully explored child.
                if (selection > notFullyExploredChildren && currentNode.reserveExpansionRights()) {
                    if (currentNode.getState() != null && currentNode.getState().isFailed())
                        throw new RuntimeException("Sampler tried to return a failed state for its tree policy.");
                    return currentNode;
                }
            }
        }
    }

    @Override
    public Action expansionPolicy(NodeQWOPExplorableBase<?> startNode) {
        if (startNode.getUntriedActionCount() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");
        return startNode.getUntriedActionRandom();
    }

    @Override
    public void rolloutPolicy(NodeQWOPExplorableBase<?> startNode, IGame game) {
    }

    @Override
    public boolean treePolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        return treePolicyDone; // True means ready to move on to the next.
    }

    @Override
    public boolean expansionPolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        return expansionPolicyDone;
    }

    @Override
    public boolean rolloutPolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        return true; // No rollout policy
    }

    @Override
    public void treePolicyActionDone(NodeQWOPExplorableBase<?> currentNode) {
        treePolicyDone = true; // Enable transition to next through the guard.
        expansionPolicyDone = false; // Prevent transition before it's done via the guard.
    }

    @Override
    public void expansionPolicyActionDone(NodeQWOPExplorableBase<?> currentNode) {
        treePolicyDone = false;
        expansionPolicyDone = currentNode.getState().isFailed();
    }

    @Override
    public Sampler_Random getCopy() {
        return new Sampler_Random();
    }
}
