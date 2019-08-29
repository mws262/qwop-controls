package tree.sampler;

import game.IGameInternal;
import game.action.Action;
import game.action.Command;
import game.state.IState;
import tree.node.NodeGameExplorableBase;

import java.util.ArrayList;

/**
 * Purely sample according to the assigned distributions, no other heuristics.
 *
 * @author Matt
 */
public class Sampler_Distribution<C extends Command<?>, S extends IState> implements ISampler<C, S> {

    private boolean treePolicyDone = false;
    private boolean expansionPolicyDone = false;

    public Sampler_Distribution() {
    }

    @Override
    public NodeGameExplorableBase<?, C, S> treePolicy(NodeGameExplorableBase<?, C, S> startNode) {
        /* Normal random sampling now, but using distribution to decide when to switch to expand policy. */
        if (startNode.isFullyExplored())
            throw new RuntimeException("Trying to do tree policy on a given node which is already fully-explored. " +
					"Whoever called this is at fault.");
        NodeGameExplorableBase<?, C, S> currentNode = startNode;

        while (true) {
            if (currentNode.isFullyExplored())
                throw new RuntimeException("Tree policy got itself to a node which is fully-explored. This is its " +
						"fault.");
            if (currentNode.isLocked()) currentNode = currentNode.getRoot();

            // Count the number of available children to go to next.
            ArrayList<NodeGameExplorableBase<?, C, S>> notFullyExploredChildren = new ArrayList<>();
            ArrayList<Action<C>> notFullyExploredActions = new ArrayList<>();
            for (NodeGameExplorableBase<?, C, S> child : currentNode.getChildren()) {
                if (!child.isFullyExplored()) {
                    notFullyExploredChildren.add(child);
                    notFullyExploredActions.add(child.getAction());
                }
            }

            if (notFullyExploredChildren.isEmpty()) {
                // Nothing possible
                if (currentNode.getUntriedActionCount() == 0)
                    throw new RuntimeException("Sampler has nowhere to go from here and should have been marked fully" +
							" explored before.");

                // Only new things to try.
                if (currentNode.reserveExpansionRights()) {
                    // We got to a place we'd like to expand. Stop tree policy, hand over to expansion policy.
                    return currentNode;
                } else { // Multithreading got us in a funny place.
                    currentNode = startNode;
                    continue;
                }
            }

            // Only old things to select between.
            if (currentNode.getUntriedActionCount() == 0) {
                Action<C> chosen = currentNode.getActionDistribution().randOnDistribution(notFullyExploredActions);

                for (int i = 0; i < notFullyExploredChildren.size(); i++) {
                    if (notFullyExploredActions.get(i).equals(chosen)) {
                        currentNode = notFullyExploredChildren.get(i);
                        break;
                    }
                }
                continue;
            }

            // Both old and new things to try.
            boolean doNew = currentNode.getActionDistribution().chooseASet(currentNode.getUntriedActionListCopy(),
					notFullyExploredActions);
            if (doNew && currentNode.reserveExpansionRights()) { // Turn it over to the expansion policy.
                return currentNode;
            } else {
                Action<C> chosen = currentNode.getActionDistribution().randOnDistribution(notFullyExploredActions);
                for (int i = 0; i < notFullyExploredChildren.size(); i++) {
                    if (notFullyExploredActions.get(i).equals(chosen)) {
                        currentNode = notFullyExploredChildren.get(i);
                        break;
                    }
                }
            }
        }
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
