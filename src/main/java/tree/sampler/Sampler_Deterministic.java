package tree.sampler;

import game.action.Action;
import game.IGameInternal;
import game.action.Command;
import tree.node.NodeGameExplorableBase;

/**
 * Super-simple depth-first search with NO random selection. Tries to pick the first node it finds with an untried
 * option.
 * Used for debugging since deterministic behavior is nice for that kind of thing :)
 *
 * @author Matt
 */
@SuppressWarnings("unused")
public class Sampler_Deterministic<C extends Command<?>> implements ISampler<C> {

    private boolean treePolicyDone = false;
    private boolean expansionPolicyDone = false;

    @Override
    public NodeGameExplorableBase<?, C> treePolicy(NodeGameExplorableBase<?, C> startNode) {
        if (startNode.isFullyExplored())
            throw new IllegalStateException("Trying to do tree policy on a given node at depth " + startNode.getTreeDepth() +
					" which is already fully-explored. Whoever called this is at fault.");
        NodeGameExplorableBase<?, C> currentNode = startNode;

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

            // Get the first child with some untried game.command after it, or at least a not-fully-explored one.
            for (NodeGameExplorableBase<?, C> child : currentNode.getChildren()) {
                if (child.getUntriedActionCount() > 0 && child.reserveExpansionRights()) {
                    return child;
                } else if (!child.isFullyExplored()) {
                    return treePolicy(child);
                }
            }
            return null;
    }

    @Override
    public void treePolicyActionDone(NodeGameExplorableBase<?, C> currentNode) {
        treePolicyDone = true; // Enable transition to next through the guard.
        expansionPolicyDone = false; // Prevent transition before it's done via the guard.
    }

    @Override
    public boolean treePolicyGuard(NodeGameExplorableBase<?, C> currentNode) {
        return treePolicyDone; // True means ready to move on to the next.
    }

    @Override
    public Action<C> expansionPolicy(NodeGameExplorableBase<?, C> startNode) {
        if (startNode.getUntriedActionCount() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");

        return startNode.getUntriedActionByIndex(0); // Get the first available untried game.command.
    }

    @Override
    public void expansionPolicyActionDone(NodeGameExplorableBase<?, C> currentNode) {
        treePolicyDone = false;
        expansionPolicyDone = currentNode.getState().isFailed();
    }

    @Override
    public boolean expansionPolicyGuard(NodeGameExplorableBase<?, C> currentNode) {
        return expansionPolicyDone;
    }

    @Override
    public void rolloutPolicy(NodeGameExplorableBase<?, C> startNode, IGameInternal<C> game) {}

    @Override
    public boolean rolloutPolicyGuard(NodeGameExplorableBase<?, C> currentNode) {
        // Rollout policy not in use in the random sampler.
        return true; // No rollout policy
    }

    @Override
    public Sampler_Deterministic getCopy() {
        return new Sampler_Deterministic();
    }

    @Override
    public void close() {}
}
