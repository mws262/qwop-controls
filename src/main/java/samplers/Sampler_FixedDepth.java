package samplers;

import java.util.*;

import actions.Action;
import game.GameThreadSafe;
import tree.Node;
import tree.Utility;

public class Sampler_FixedDepth implements ISampler {

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
    private Set<Node> finishedNodes = new HashSet<>(); // Note: each worker populates its own list. This is kind of
    // dumb, but I don't expect to use this sampler enough for it to be a big deal.

    public Sampler_FixedDepth(int horizonDepth) {
        this.horizonDepth = horizonDepth;
    }

    @Override
    public Node treePolicy(Node startNode) {
        if (startNode.fullyExplored.get()) {
            throw new RuntimeException("Trying to do tree policy on a given node which is already fully-explored. " +
                    "Whoever called this is at fault.");
        }

        startDepth = startNode.getTreeDepth();
        effectiveHorizonDepth = startDepth + horizonDepth;
        Node currentNode = startNode;

        while (!finishedNodes.contains(startNode)) {

            if (currentNode.fullyExplored.get() || finishedNodes.contains(currentNode)) {
                if (currentNode.fullyExplored.get()) finishedNodes.add(currentNode);
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
            if (!currentNode.uncheckedActions.isEmpty() && currentNode.reserveExpansionRights()) {
                return currentNode;
            }

            // Otherwise, move down.
            boolean foundChild = false;
            // If the order of iteration is not randomized, once there are enough workers, they can manage to deadlock.
            Node[] children = currentNode.getChildren();
            Utility.shuffleArray(children);

            for (Node child : children) {
                if (!child.fullyExplored.get() && !finishedNodes.contains(child) && !child.isLocked()) {
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
        if (startNode.uncheckedActions.size() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");

        Action childAction = startNode.uncheckedActions.getRandom();

        return startNode.addChild(childAction);
    }

    @Override
    public void expansionPolicyActionDone(Node currentNode) {
        treePolicyDone = false;
        if (currentNode.getTreeDepth() == effectiveHorizonDepth || currentNode.isFailed()) {
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
    private void propagateFinishedNodes(Node currentNode) {
        if (currentNode.uncheckedActions.isEmpty()) {
            for (Node child : currentNode.getChildren()) {
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
    public boolean expansionPolicyGuard(Node currentNode) {
        return expansionPolicyDone;
    }

    @Override
    public void rolloutPolicy(Node startNode, GameThreadSafe gameThreadSafe) {}

    @Override
    public boolean rolloutPolicyGuard(Node currentNode) {
        return true; // No rollout policy
    }

    @Override
    public Sampler_FixedDepth getCopy() {
        return new Sampler_FixedDepth(horizonDepth);
    }
}
