package tree.sampler;

import game.action.Action;
import game.IGameInternal;
import game.action.Command;
import game.state.IState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import tree.TreeWorker;
import tree.node.NodeGameExplorableBase;

/**
 * Super-simple depth-first search with NO random selection. Tries to pick the first node it finds with an untried
 * option.
 * Used for debugging since deterministic behavior is nice for that kind of thing :)
 *
 * @author Matt
 */
@SuppressWarnings("unused")
public class Sampler_Deterministic<C extends Command<?>, S extends IState> implements ISampler<C, S> {

    private boolean treePolicyDone = false;
    private boolean expansionPolicyDone = false;
    private int deadlockDelayCurrent = 0;

    private int maxDepth = 10000;

    private static final Logger logger = LogManager.getLogger(Sampler_Deterministic.class);

    @Override
    @NotNull
    public NodeGameExplorableBase<?, C, S> treePolicy(NodeGameExplorableBase<?, C, S> startNode) {
        if (startNode.isFullyExplored())
            throw new IllegalStateException("Trying to do tree policy on a given node at depth " + startNode.getTreeDepth() +
                    " which is already fully-explored. Whoever called this is at fault.");

        if (startNode.isLocked()) {
            if (startNode.getTreeDepth() > 0) {
                treePolicy(startNode.getParent());
            } else {
                deadlockDelay();
                return treePolicy(startNode);
            }
        }

        // If the given node has unchecked options (e.g. root hasn't tried all possible immediate children),
        // expand directly.
        if (startNode.getUntriedActionCount() != 0 && startNode.reserveExpansionRights()) {
            resetDeadlockDelay();
            return startNode;
        }

        // Get the first child with some untried game.command after it, or at least a not-fully-explored one.
        for (NodeGameExplorableBase<?, C, S> child : startNode.getChildren()) {
            if (child.getUntriedActionCount() > 0 && child.reserveExpansionRights()) {
                resetDeadlockDelay();
                return child;
            } else if (!child.isFullyExplored()) {
                return treePolicy(child);
            }
        }
        deadlockDelay();
        return treePolicy(startNode);
    }

    @Override
    public void treePolicyActionDone(NodeGameExplorableBase<?, C, S> currentNode) {
        treePolicyDone = true; // Enable transition to next through the guard.
        expansionPolicyDone = false; // Prevent transition before it's done via the guard.
    }

    @Override
    public boolean treePolicyGuard(NodeGameExplorableBase<?, C, S> currentNode) {
        return treePolicyDone; // True means ready to move on to the next.
    }

    @Override
    public Action<C> expansionPolicy(NodeGameExplorableBase<?, C, S> startNode) {
        if (startNode.getUntriedActionCount() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");

        return startNode.getUntriedActionByIndex(0); // Get the first available untried game.command.
    }

    @Override
    public void expansionPolicyActionDone(NodeGameExplorableBase<?, C, S> currentNode) {
        treePolicyDone = false;
        expansionPolicyDone = currentNode.getState().isFailed();
        if (currentNode.getTreeDepth() > maxDepth) {
            logger.warn("Max tree depth of " + maxDepth + " reached in a sampler. This is probably an infinite series" +
                    " of actions.");
            expansionPolicyDone = true;
        }
    }

    @Override
    public boolean expansionPolicyGuard(NodeGameExplorableBase<?, C, S> currentNode) {
        return expansionPolicyDone;
    }

    @Override
    public void rolloutPolicy(NodeGameExplorableBase<?, C, S> startNode, IGameInternal<C, S> game) {}

    @Override
    public boolean rolloutPolicyGuard(NodeGameExplorableBase<?, C, S> currentNode) {
        // Rollout policy not in use in the random sampler.
        return true; // No rollout policy
    }

    @Override
    public Sampler_Deterministic<C, S> getCopy() {
        return new Sampler_Deterministic<>();
    }

    @Override
    public void close() {}

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
}
