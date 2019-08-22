package tree.sampler;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameInternal;
import game.action.Action;
import game.action.Command;
import tree.node.NodeQWOPExplorableBase;

import java.util.HashSet;
import java.util.Set;

public class Sampler_FixedDepth<C extends Command<?>> implements ISampler<C> {

    /**
     * Sampler only goes to this horizon.
     */
    private final int horizonDepth;

    /**
     * Depth of the starting node.
     */
    private int startDepth;

    /**
     * If given a start node which is not at tree depth 0, this will be important for getting the absolute tree depth
     * to go to.
     */
    private int effectiveHorizonDepth;

    private boolean treePolicyDone = false;
    private boolean expansionPolicyDone = false;

    /**
     * Since we're only going to a certain depth, the whole "fullyExplored" thing doesn't really work
     * to tell us when to terminate. Here we track nodes we're done with.
     */
    private Set<NodeQWOPExplorableBase<?, C>> finishedNodes = new HashSet<>(); // Note: each worker populates its own
    // list. This is kind of
    // dumb, but I don't expect to use this sampler enough for it to be a big deal.

    public Sampler_FixedDepth(@JsonProperty("horizonDepth") int horizonDepth) {
        this.horizonDepth = horizonDepth;
    }

    @Override
    public NodeQWOPExplorableBase<?, C> treePolicy(NodeQWOPExplorableBase<?, C> startNode) {
        if (startNode.isFullyExplored()) {
            throw new RuntimeException("Trying to do tree policy on a given node which is already fully-explored. " +
                    "Whoever called this is at fault.");
        }

        startDepth = startNode.getTreeDepth();
        effectiveHorizonDepth = startDepth + horizonDepth;
        NodeQWOPExplorableBase<?, C> currentNode = startNode;

        while (!finishedNodes.contains(startNode)) {

            if (currentNode.isFullyExplored() || finishedNodes.contains(currentNode)) {
                if (currentNode.isFullyExplored()) finishedNodes.add(currentNode);
                currentNode = startNode; // Just start over for now. I don't think it's a big enough problem to
                // stress over.
                continue;
            }

            // If this worker doesn't yet know that this is a finished node, fix that and move back.
            if (currentNode.getTreeDepth() == effectiveHorizonDepth) {
                finishedNodes.add(currentNode);
                propagateFinishedNodes(currentNode.getParent());
                currentNode = startNode;
                continue;
            }

            // Grab the first thing we can with an untried action.
            if (currentNode.getUntriedActionCount() != 0 && currentNode.reserveExpansionRights()) {
                return currentNode;
            }

            // Otherwise, move down.
            boolean foundChild = false;
            // If the order of iteration is not randomized, once there are enough workers, they can manage to deadlock.
            // TODO put randomness back in.
//            List<NodeQWOPExplorableBase<?>> children = currentNode.getChildren().get(0);
//            Utility.shuffleArray(children);

            for (NodeQWOPExplorableBase<?, C> child : currentNode.getChildren()) {
                if (!child.isFullyExplored() && !finishedNodes.contains(child) && !child.isLocked()) {
                    currentNode = child;
                    foundChild = true;
                    break;
                }
            }
            if (!foundChild) {
                currentNode = startNode;
                propagateFinishedNodes(startNode);
            }
        }
        return null;
    }

    @Override
    public void treePolicyActionDone(NodeQWOPExplorableBase<?, C> currentNode) {
        treePolicyDone = true; // Enable transition to next through the guard.
        expansionPolicyDone = false; // Prevent transition before it's done via the guard.
    }

    @Override
    public boolean treePolicyGuard(NodeQWOPExplorableBase<?, C> currentNode) {
        return treePolicyDone; // True means ready to move on to the next.
    }

    @Override
    public Action<C> expansionPolicy(NodeQWOPExplorableBase<?, C> startNode) {
        if (startNode.getUntriedActionCount() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");

        return startNode.getUntriedActionRandom();
    }

    @Override
    public void expansionPolicyActionDone(NodeQWOPExplorableBase<?, C> currentNode) {
        treePolicyDone = false;
        if (currentNode.getTreeDepth() == effectiveHorizonDepth || currentNode.getState().isFailed()) {
            expansionPolicyDone = true;

            finishedNodes.add(currentNode);
            propagateFinishedNodes(currentNode.getParent());
        } else {
            expansionPolicyDone = false;
        }
    }

    /**
     * Propagate the finished status back up the tree towards the start node.
     */
    private void propagateFinishedNodes(NodeQWOPExplorableBase<?, C> currentNode) {
        if (currentNode.getUntriedActionCount() == 0) {
            for (NodeQWOPExplorableBase<?, C> child : currentNode.getChildren()) {
                if (child.getTreeDepth() == effectiveHorizonDepth) {
                    finishedNodes.add(child);
                }
                if (!finishedNodes.contains(child)) {
                    return;
                }
            }
            finishedNodes.add(currentNode);
            // Recurse if above the start depth.
            if (currentNode.getTreeDepth() > startDepth) propagateFinishedNodes(currentNode.getParent());
        }
    }

    @Override
    public boolean expansionPolicyGuard(NodeQWOPExplorableBase<?, C> currentNode) {
        return expansionPolicyDone;
    }

    @Override
    public void rolloutPolicy(NodeQWOPExplorableBase<?, C> startNode, IGameInternal<C> gameThreadSafe) {}

    @Override
    public boolean rolloutPolicyGuard(NodeQWOPExplorableBase<?, C> currentNode) {
        return true; // No rollout policy
    }

    @JsonProperty("horizonDepth")
    public int getHorizonDepth() {
        return horizonDepth;
    }

    @Override
    public Sampler_FixedDepth<C> getCopy() {
        return new Sampler_FixedDepth<>(horizonDepth);
    }

    @Override
    public void close() {}
}
