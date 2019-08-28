package tree.sampler;

import game.action.Action;
import game.IGameInternal;
import game.action.Command;
import tree.node.NodeQWOPExplorableBase;
import tree.Utility;

/**
 * Dumb sampler, either for filling a space or testing purposes.
 * Goes through the tree randomly and eventually adds nodes (randomly) until failure.
 *
 * @author Matt
 */
public class Sampler_Random<C extends Command<?>> implements ISampler<C> {

    private boolean treePolicyDone = false;
    private boolean expansionPolicyDone = false;

    @Override
    public NodeQWOPExplorableBase<?, C> treePolicy(NodeQWOPExplorableBase<?, C> startNode) {
        if (startNode.isFullyExplored()) {
            throw new RuntimeException("Trying to do tree policy on a given node which is already fully-explored. " +
					"Whoever called this is at fault.");
        }
        NodeQWOPExplorableBase<?, C> currentNode = startNode;

        while (true) {
            if (currentNode.isFullyExplored())
                currentNode = startNode; // Just start over for now. I don't think it's a big enough problem to
			// stress over.

            // Count the number of available children to go to next.
            int notFullyExploredChildren = 0;
            for (NodeQWOPExplorableBase<?, C> child : currentNode.getChildren()) {
                if (!child.isFullyExplored() && !child.isLocked()) notFullyExploredChildren++;
            }

            if (notFullyExploredChildren == 0 && currentNode.getUntriedActionCount() == 0) {
                currentNode = startNode;
                continue; // TODO: investigate this error further.
            }

            if (notFullyExploredChildren == 0) {
                if (currentNode.reserveExpansionRights()) {
                    // We got to a place we'd like to expand. Stop tree policy, hand over to expansion policy.
                    return currentNode;
                }
            }

            if (currentNode.getUntriedActionCount() == 0) { // No unchecked game.command means that we pick a random
            	// not-fully-explored child.
                // Pick random not fully explored child. Keep going.
                int selection = Utility.randInt(0, notFullyExploredChildren - 1);
                int count = 0;
                for (NodeQWOPExplorableBase<?, C> child : currentNode.getChildren()) {
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
                if (selection > notFullyExploredChildren) {
                    if (currentNode.reserveExpansionRights()) {
                        if (currentNode.getState() != null && currentNode.getState().isFailed())
                            throw new RuntimeException("Sampler tried to return a failed state for its tree policy.");
                        return currentNode;
                    }
                }
            }
        }
    }

    @Override
    public Action<C> expansionPolicy(NodeQWOPExplorableBase<?, C> startNode) {
        if (startNode.getUntriedActionCount() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");
        return startNode.getUntriedActionRandom();
    }

    @Override
    public void rolloutPolicy(NodeQWOPExplorableBase<?, C> startNode, IGameInternal<C> game) {
    }

    @Override
    public boolean treePolicyGuard(NodeQWOPExplorableBase<?, C> currentNode) {
        return treePolicyDone; // True means ready to move on to the next.
    }

    @Override
    public boolean expansionPolicyGuard(NodeQWOPExplorableBase<?, C> currentNode) {
        return expansionPolicyDone;
    }

    @Override
    public boolean rolloutPolicyGuard(NodeQWOPExplorableBase<?, C> currentNode) {
        return true; // No rollout policy
    }

    @Override
    public void treePolicyActionDone(NodeQWOPExplorableBase<?, C> currentNode) {
        treePolicyDone = true; // Enable transition to next through the guard.
        expansionPolicyDone = false; // Prevent transition before it's done via the guard.
    }

    @Override
    public void expansionPolicyActionDone(NodeQWOPExplorableBase<?, C> currentNode) {
        treePolicyDone = false;
        expansionPolicyDone = currentNode.getState().isFailed();
    }

    @Override
    public Sampler_Random<C> getCopy() {
        return new Sampler_Random<>();
    }

    @Override
    public void close() {}
}
