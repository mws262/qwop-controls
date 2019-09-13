package tree.sampler;

import game.IGameInternal;
import game.action.Action;
import game.action.Command;
import game.state.IState;
import tree.Utility;
import tree.node.NodeGameExplorableBase;

import java.util.stream.IntStream;

/**
 * Dumb sampler, either for filling a space or testing purposes.
 * Goes through the tree randomly and eventually adds nodes (randomly) until failure.
 *
 * @author Matt
 */
public class Sampler_Random<C extends Command<?>, S extends IState> implements ISampler<C, S> {

    private boolean treePolicyDone = false;
    private boolean expansionPolicyDone = false;
    private int deadlockDelayCurrent = 0;

    @Override
    public NodeGameExplorableBase<?, C, S> treePolicy(NodeGameExplorableBase<?, C, S> startNode) {
        // First nodes at root can be handled a little differently to make sure duplicate actions aren't added.
        if (startNode.getTreeDepth() == 0 && (startNode.getChildCount() == 0 || startNode.isLocked())) {
            if (startNode.reserveExpansionRights()) {
                return startNode;
            } else {
                deadlockDelay();
                return treePolicy(startNode);
            }
        }

        NodeGameExplorableBase<?, C, S> n = startNode;
        while (n.getTreeDepth() > 0 && (n.isLocked() || n.isFullyExplored())) {
            n = n.getParent();
        }

        // Count the number of available children to go to next.
        int candidateChildren =
                (int) startNode
                        .getChildren()
                        .stream()
                        .filter(c -> !c.isLocked() && !c.isFullyExplored())
                        .count();

        // Use weighted probability to decide whether to go deeper down existing nodes or expand right here.
        int candidateExpansions = startNode.getUntriedActionCount();
        if (candidateChildren + candidateExpansions == 0) {
            deadlockDelay();
            return treePolicy(startNode); // No options here. Another worker may have beaten us to it.
        }
        int selection = Utility.randInt(0, candidateChildren + candidateExpansions - 1);

        if (selection < candidateChildren) { // Go deeper.
            int[] elementOrder = IntStream.range(0, startNode.getChildCount()).toArray();
            Utility.shuffleIntArray(elementOrder);

            for (int idx : elementOrder) { // Go through children in random order and get the first one that meets
                // criteria. If another worker gets to these first, then we have to go back up the tree and try again.
                NodeGameExplorableBase<?, C, S> child = startNode.getChildByIndex(idx);
                if (!child.isLocked() && !child.isFullyExplored()) {
                    return treePolicy(child);
                }
            }
        } else { // Expand here.
            if (startNode.reserveExpansionRights()) {
                resetDeadlockDelay();
                return startNode;
            }
        }

        // Some other worker snagged our options, so we call again, potentially backing up the tree until we find a
        // node that is open.
        deadlockDelay();
        return treePolicy(startNode);
    }

    private void deadlockDelay() {
        try {
            Thread.sleep(deadlockDelayCurrent);
            deadlockDelayCurrent = deadlockDelayCurrent * 2 + 1;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void resetDeadlockDelay() {
        deadlockDelayCurrent = 0;
    }

    @Override
    public Action<C> expansionPolicy(NodeGameExplorableBase<?, C, S> startNode) {
        if (startNode.getUntriedActionCount() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");
        return startNode.getUntriedActionRandom();
    }

    @Override
    public void rolloutPolicy(NodeGameExplorableBase<?, C, S> startNode, IGameInternal<C, S> game) {
    }

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
//        if (!currentNode.getState().isFailed()) {
//            boolean reserved = currentNode.reserveExpansionRights();
//            assert reserved;
//        }
        expansionPolicyDone = currentNode.getState().isFailed();
    }

    @Override
    public Sampler_Random<C, S> getCopy() {
        return new Sampler_Random<>();
    }

    @Override
    public void close() {}
}
