package tree.sampler;

import game.action.Action;
import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;

/**
 * Super-simple depth-first search with NO random selection. Tries to pick the first node it finds with an untried
 * option.
 * Used for debugging since deterministic behavior is nice for that kind of thing :)
 *
 * @author Matt
 */
@SuppressWarnings("unused")
public class Sampler_Deterministic implements ISampler {

    private boolean treePolicyDone = false;
    private boolean expansionPolicyDone = false;

    @Override
    public NodeQWOPExplorableBase<?> treePolicy(NodeQWOPExplorableBase<?> startNode) {
        if (startNode.isFullyExplored())
            throw new IllegalStateException("Trying to do tree policy on a given node at depth " + startNode.getTreeDepth() +
					" which is already fully-explored. Whoever called this is at fault.");
        NodeQWOPExplorableBase<?> currentNode = startNode;

            if (currentNode.isLocked()) {
                if (currentNode.getTreeDepth() > 0) {
                    treePolicy(startNode.getParent());
                } else {
                    return null;
                }
            }

            // If the given node has unchecked options (e.g. root hasn't tried all possible immediate children),
			// expand directly.
            if (currentNode.getUntriedActionCount() != 0 && currentNode.reserveExpansionRights())
                return currentNode;

            // Get the first child with some untried game.action after it, or at least a not-fully-explored one.
            for (NodeQWOPExplorableBase<?> child : currentNode.getChildren()) {
                if (child.getUntriedActionCount() > 0 && child.reserveExpansionRights()) {
                    return child;
                } else if (!child.isFullyExplored()) {
                    return treePolicy(child);
                }
            }
            return null;
    }

    @Override
    public void treePolicyActionDone(NodeQWOPExplorableBase<?> currentNode) {
        treePolicyDone = true; // Enable transition to next through the guard.
        expansionPolicyDone = false; // Prevent transition before it's done via the guard.
    }

    @Override
    public boolean treePolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        return treePolicyDone; // True means ready to move on to the next.
    }

    @Override
    public Action expansionPolicy(NodeQWOPExplorableBase<?> startNode) {
        if (startNode.getUntriedActionCount() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");

        return startNode.getUntriedActionByIndex(0); // Get the first available untried game.action.
    }

    @Override
    public void expansionPolicyActionDone(NodeQWOPExplorableBase<?> currentNode) {
        treePolicyDone = false;
        expansionPolicyDone = currentNode.getState().isFailed();
    }

    @Override
    public boolean expansionPolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        return expansionPolicyDone;
    }

    @Override
    public void rolloutPolicy(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {}

    @Override
    public boolean rolloutPolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        // Rollout policy not in use in the random sampler.
        return true; // No rollout policy
    }

    @Override
    public Sampler_Deterministic getCopy() {
        return new Sampler_Deterministic();
    }

}
