package samplers;

import java.util.ArrayList;

import actions.Action;
import game.IGame;
import tree.Node;
import tree.NodeQWOPExplorable;
import tree.NodeQWOPExplorableBase;

/**
 * Purely sample according to the assigned distributions, no other heuristics.
 *
 * @author Matt
 */
public class Sampler_Distribution implements ISampler {

    private boolean treePolicyDone = false;
    private boolean expansionPolicyDone = false;

    public Sampler_Distribution() {
    }

    @Override
    public NodeQWOPExplorableBase<?> treePolicy(NodeQWOPExplorableBase<?> startNode) {
        /* Normal random sampling now, but using distribution to decide when to switch to expand policy. */
        if (startNode.isFullyExplored())
            throw new RuntimeException("Trying to do tree policy on a given node which is already fully-explored. " +
					"Whoever called this is at fault.");
        NodeQWOPExplorableBase<?> currentNode = startNode;

        while (true) {
            if (currentNode.isFullyExplored())
                throw new RuntimeException("Tree policy got itself to a node which is fully-explored. This is its " +
						"fault.");
            if (currentNode.isLocked()) currentNode = currentNode.getRoot();

            // Count the number of available children to go to next.
            ArrayList<Node> notFullyExploredChildren = new ArrayList<>();
            ArrayList<Action> notFullyExploredActions = new ArrayList<>();
            for (Node child : currentNode.getChildren()) {
                if (!child.fullyExplored.get()) {
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
                Action chosen = currentNode.uncheckedActions.samplingDist.randOnDistribution(notFullyExploredActions);

                for (int i = 0; i < notFullyExploredChildren.size(); i++) {
                    if (notFullyExploredActions.get(i).equals(chosen)) {
                        currentNode = notFullyExploredChildren.get(i);
                        break;
                    }
                }
                continue;
            }

            // Both old and new things to try.
            boolean doNew = currentNode.uncheckedActions.samplingDist.chooseASet(currentNode.uncheckedActions,
					notFullyExploredActions);
            if (doNew && currentNode.reserveExpansionRights()) { // Turn it over to the expansion policy.
                return currentNode;
            } else {
                Action chosen = currentNode.uncheckedActions.samplingDist.randOnDistribution(notFullyExploredActions);
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
    public Action expansionPolicy(Node startNode) {
        if (startNode.uncheckedActions.size() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");

        return startNode.uncheckedActions.sampleDistribution();
    }

    @Override
    public void rolloutPolicy(NodeQWOPExplorableBase<?> startNode, IGame game) {}

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
    public Sampler_Distribution getCopy() {
        return new Sampler_Distribution();
    }
}
