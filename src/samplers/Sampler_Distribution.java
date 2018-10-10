package samplers;

import java.util.ArrayList;

import main.Action;
import main.ISampler;
import main.Node;

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
    public Node treePolicy(Node startNode) {
        /******** Normal random sampling now, but using distribution to decide when to switch to expand policy.
		 *  **********/
        if (startNode.fullyExplored.get())
            throw new RuntimeException("Trying to do tree policy on a given node which is already fully-explored. " +
					"Whoever called this is at fault.");
        Node currentNode = startNode;

        while (true) {
            if (currentNode.fullyExplored.get())
                throw new RuntimeException("Tree policy got itself to a node which is fully-explored. This is its " +
						"fault.");
            if (currentNode.getLockStatus()) currentNode = currentNode.getRoot();

            // Count the number of available children to go to next.
            ArrayList<Node> notFullyExploredChildren = new ArrayList<>();
            ArrayList<Action> notFullyExploredActions = new ArrayList<>();
            for (Node child : currentNode.children) {
                if (!child.fullyExplored.get()) {
                    notFullyExploredChildren.add(child);
                    notFullyExploredActions.add(child.getAction());
                }
            }


            if (notFullyExploredChildren.isEmpty()) {
                // Nothing possible
                if (currentNode.uncheckedActions.isEmpty())
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
            if (currentNode.uncheckedActions.isEmpty()) {
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
                continue;
            }
        }
    }

    @Override
    public Node expansionPolicy(Node startNode) {
        if (startNode.uncheckedActions.size() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");

        Action childAction = startNode.uncheckedActions.sampleDistribution();

        //startNode.uncheckedActions.get(Node.randInt(0,startNode.uncheckedActions.size() - 1));

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
        expansionPolicyDone = currentNode.state.isFailed();
    }

    @Override
    public void rolloutPolicyActionDone(Node currentNode) {
    } // No rollout in random sampler.

    @Override
    public Sampler_Distribution clone() {
        return new Sampler_Distribution();
    }
}
