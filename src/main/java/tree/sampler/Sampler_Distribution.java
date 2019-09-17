package tree.sampler;

import game.IGameInternal;
import game.action.Action;
import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;
import tree.node.NodeGameExplorableBase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Purely sample according to the assigned distributions, no other heuristics.
 *
 * @author Matt
 */
public class Sampler_Distribution<C extends Command<?>, S extends IState> extends Sampler_DeadlockDelay<C, S>{

    private boolean treePolicyDone = false;
    private boolean expansionPolicyDone = false;

    public Sampler_Distribution() {}

    @Override
    public NodeGameExplorableBase<?, C, S> treePolicy(NodeGameExplorableBase<?, C, S> startNode) {

//        if (startNode.isFullyExplored())
//            throw new IllegalStateException("Trying to do tree policy on a given node which is already fully-explored. " +
//                    "Whoever called this is at fault.");

        if (startNode.getTreeDepth() == 0 && (startNode.getChildCount() == 0 || startNode.isLocked())) {
            if (startNode.reserveExpansionRights()) {
                return startNode;
            } else {
                deadlockDelay();
                return treePolicy(startNode);
            }
        }
        if (startNode.getTreeDepth() > 0 && (startNode.isLocked() ||startNode.isFullyExplored())) {
            deadlockDelay();
            return treePolicy(startNode.getParent());
        }

        if (startNode.isLocked()) {
            deadlockDelay();
            return treePolicy(startNode); // TODO may want to change this to going back to root instead of just
            // trying again.
        }

        List<Action<C>> notFullyExploredChildActions = new ArrayList<>(startNode.getChildren())
                        .stream()
                        .filter(n -> !n.isFullyExplored())
                        .map(NodeGameBase::getAction)
                        .collect(Collectors.toList());

        // All existing nodes are fully explored.
        if (notFullyExploredChildActions.isEmpty()) {
            // Nothing possible
            if (startNode.getUntriedActionCount() == 0)
                throw new IllegalStateException("Sampler has nowhere to go from here and should have been marked fully" +
                        " explored before.");

            // Only new things to try.
            if (startNode.reserveExpansionRights()) {
                // We got to a place we'd like to expand. Stop tree policy, hand over to expansion policy.
                resetDeadlockDelay();
                return startNode;
            } else { // Multithreading got us in a funny place.
                deadlockDelay();
                return treePolicy(startNode);
            }
        } else if (startNode.getUntriedActionCount() == 0) {
            // Only old things to select between. Pick a child node and recurse down.
            return treePolicy(getChildFromActionDistribution(startNode));
        }

        // Both old and new things to try.
        boolean doNew = startNode.getActionDistribution().chooseASet(startNode.getUntriedActionListCopy(), notFullyExploredChildActions);
        if (doNew && startNode.reserveExpansionRights()) { // Turn it over to the expansion policy.
            resetDeadlockDelay();
            return startNode;
        } else {
            return treePolicy(getChildFromActionDistribution(startNode));
        }
    }

    private NodeGameExplorableBase<?, C, S> getChildFromActionDistribution(NodeGameExplorableBase<?, C, S> parentNode) {
        ArrayList<Action<C>> notFullyExploredActions = new ArrayList<>();
        ArrayList<NodeGameExplorableBase<?, C, S>> notFullyExploredChildren = new ArrayList<>();

        for (NodeGameExplorableBase<?, C, S> child : new ArrayList<>(parentNode.getChildren())) {
            if (!child.isFullyExplored()) {
                notFullyExploredChildren.add(child);
                notFullyExploredActions.add(child.getAction());
            }
        }

        Action<C> chosen = parentNode.getActionDistribution().randOnDistribution(notFullyExploredActions);
        for (int i = 0; i < notFullyExploredChildren.size(); i++) {
            if (notFullyExploredActions.get(i).equals(chosen)) {
                return notFullyExploredChildren.get(i);
            }
        }
        throw new IllegalStateException("Could not locate the expected child node.");
    }

    @Override
    public Action<C> expansionPolicy(NodeGameExplorableBase<?, C, S> startNode) {
        if (startNode.getUntriedActionCount() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");

        return startNode.getUntriedActionOnDistribution();
    }

    @Override
    public void rolloutPolicy(NodeGameExplorableBase<?, C, S> startNode, IGameInternal<C, S> game) {}

    @Override
    public boolean treePolicyGuard(NodeGameExplorableBase<?, C, S> currentNode) {
        return treePolicyDone; // True means ready to move on to the next.
    }

    @Override
    public boolean expansionPolicyGuard(NodeGameExplorableBase<?, C, S> currentNode) {
        return expansionPolicyDone;
    }

    @Override
    public boolean rolloutPolicyGuard(NodeGameExplorableBase<?, C, S> currentNode) {
        return true; // No rollout policy
    }

    @Override
    public void treePolicyActionDone(NodeGameExplorableBase<?, C, S> currentNode) {
        treePolicyDone = true; // Enable transition to next through the guard.
        expansionPolicyDone = false; // Prevent transition before it's done via the guard.
    }

    @Override
    public void expansionPolicyActionDone(NodeGameExplorableBase<?, C, S> currentNode) {
        treePolicyDone = false;
        expansionPolicyDone = currentNode.getState().isFailed();
    }

    @Override
    public Sampler_Distribution<C, S> getCopy() {
        return new Sampler_Distribution<>();
    }

    @Override
    public void close() {}
}
