package tree;

import actions.Action;
import actions.ActionGenerator_Null;
import actions.ActionList;
import actions.IActionGenerator;
import distributions.Distribution;
import game.State;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Expands on basic QWOP data storage ({@link NodeQWOPBase}) and tree functionality ({@link NodeGenericBase}) to provide
 * basic tree exploration functions, like the concepts of untested actions, fully-explored nodes, and branches locked
 * for exploration in a multi-threaded scenario.
 *
 * @param <N>
 */
public abstract class NodeQWOPExplorableBase<N extends NodeQWOPExplorableBase<N>> extends NodeQWOPBase<N> {

    private ActionList untriedActions;

    /**
     * Are there any untried things below this node? This is not necessarily a TERMINAL node, it is simply a node
     * past which there are no potential child actions to try.
     */
    private final AtomicBoolean fullyExplored = new AtomicBoolean(false);

    /**
     * If one TreeWorker is expanding from this leaf node (if it is one), then no other worker should try to
     * simultaneously expand from here too.
     */
    private final AtomicBoolean locked = new AtomicBoolean(false);

    final IActionGenerator actionGenerator;

    public NodeQWOPExplorableBase(State rootState, IActionGenerator actionGenerator) {
        super(rootState);
        this.actionGenerator = actionGenerator;
        untriedActions = actionGenerator.getPotentialChildActionSet(this);
    }

    public NodeQWOPExplorableBase(State rootState) {
        super(rootState);
        this.actionGenerator = new ActionGenerator_Null();
    }

    NodeQWOPExplorableBase(N parent, Action action, State state, IActionGenerator actionGenerator) {
        super(parent, action, state);
        this.actionGenerator = actionGenerator;
        untriedActions = actionGenerator.getPotentialChildActionSet(parent);
    }

    public abstract N addDoublyLinkedChild(Action action, State state, IActionGenerator actionGenerator);
    public abstract N addBackwardsLinkedChild(Action action, State state, IActionGenerator actionGenerator);

    public boolean isFullyExplored() {
        return fullyExplored.get();
    }

    void setFullyExploredStatus(boolean fullyExploredStatus) {
        fullyExplored.set(fullyExploredStatus);
    }

    public int getUntriedActionCount() {
        if (untriedActions == null) {
            return 0;
        } else {
            return untriedActions.size();
        }
    }

    public Action getUntriedActionByIndex(int idx) {
        return untriedActions.get(idx);
    }

    public Action getUntriedActionRandom() {
        return untriedActions.getRandom();
    }

    public Action getUntriedActionOnDistribution() {
        return untriedActions.sampleDistribution();
    }

    public List<Action> getUntriedActionListCopy() {
        return new ArrayList<>(untriedActions);
    }
    public Distribution<Action> getActionDistribution() {
        return untriedActions.samplingDist;
    }

    /**
     * Change whether this node or any above it have become fully explored. Generally called from a leaf node which
     * has just been assigned fully-explored status, and we need to propagate the effects back up the tree.
     * <p>
     * This is the "lite" version because it assumes that all child nodes it encounters have correct fully-explored
     * statuses already assigned. This should be true during normal operation, but when a bunch of saved nodes are
     * imported, it is useful to do a complete check}.
     */
    public void propagateFullyExploredStatus_lite() {
        boolean flag = true; // Assume this node is fully-explored and negate if we find evidence that it is not.

        if (!getState().isFailed()) {
            if (untriedActions != null && !untriedActions.isEmpty()) {
                flag = false;
            }
            for (N child : getChildren()) {
                if (!child.isFullyExplored()) { // If any child is not fully explored, then this node isn't too.
                    flag = false;
                }
            }
        }
        setFullyExploredStatus(flag);

        if (getTreeDepth() > 0) { // We already know this node is fully explored, check the parent.
            getParent().propagateFullyExploredStatus_lite();
        }
    }

    /**
     * Change whether this node or any above it have become fully explored. This is the complete version, which
     * resets any existing fully-explored tags from the descendants of this node before redoing all checks. Call from
     * root to re-label the whole tree. During normal tree-building, a {@link Node#propagateFullyExploredStatus_lite()
     * lite check} should suffice and is more computationally efficient.
     * <p>
     * This should only be used when a bunch of nodes are imported at once and need to all be checked, or if we need
     * to validate correct behavior of some feature.
     **/
    private void propagateFullyExplored_complete() {
        ArrayList<N> leaves = new ArrayList<>(5000);
        getLeaves(leaves);

        // Reset all existing exploration flags out there.
        for (N leaf : leaves) {
            N currNode = leaf;
            while (currNode.getTreeDepth() > getTreeDepth()) {
                currNode.setFullyExploredStatus(false);
                currNode = currNode.getParent();
            }
            currNode.setFullyExploredStatus(false);
        }

        for (N leaf : leaves) {
            leaf.propagateFullyExploredStatus_lite();
        }
    }

    /*
     * LOCKING AND UNLOCKING NODES:
     *
     * Due to multithreading, it is a major headache if several threads are sampling from the same node at the same
     * time. Hence, we lock nodes to prevent sampling by other threads while another one is working in that part of
     * the tree. In general, we try to be overzealous in locking. For broad trees, there is minimal blocking slowdown
     * . For narrow trees, however, we may get only minimal gains from multithreading.
     *
     * Cases:
     * 1. Select node to expand. It has only 1 untried option. We lock the node, and expand.
     * 2. Select node to expand. It has multiple options. We still lock the node for now. This could be changed.
     * 3. Select node to expand. It is now locked according to 1 and 2. This node's parent only has fully explored
     * children and locked children. For all practical purposes, this node is also out of play. Lock it too and
     * recurse up the tree until we reach a node with at least one unlocked and not fully explored child.
     * When unlocking, we should propagate fully-explored statuses back up the tree first, and then remove locks as
     * far up the tree as possible.
     */

    /**
     * Set a flag to indicate that the invoking TreeWorker has temporary exclusive rights to expand from this node.
     *
     * @return Whether the lock was successfully obtained. True means that the caller obtained the lock. False means
     * that someone else got to it first.
     */
    public synchronized boolean reserveExpansionRights() {

        if (locked.get()) { // Already owned by another worker.
            return false;
        } else {
            locked.set(true);

            // May need to add locks to nodes further up the tree towards root. For example, if calling
            // reserveExpansionRights locks the final available node of this node's parent, then the parent should be
            // locked off too. This effect can chain all the way up the tree towards the root.
            if (getTreeDepth() > 0) getParent().propagateLock();

            return true;
        }
    }

    /**
     * Set a flag to indicate that the invoking TreeWorker has released exclusive rights to expand from this node.
     */
    synchronized void releaseExpansionRights() {
        locked.set(false); // Release the lock.
        // Unlocking this node may cause nodes further up the tree to become available.
        if ((getTreeDepth() > 0)) getParent().propagateUnlock();
    }

    /**
     * Locking one node may cause some parent nodes to become unavailable also. propagateLock will check as far up
     * the tree towards the root node as necessary.
     */
    synchronized void propagateLock() {
        // Lock this node unless we find evidence that we don't need to.
        for (N child : getChildren()) {
            if (!child.isLocked() && !child.isFullyExplored()) {
                return; // In this case, we don't need to continue locking things further up the tree.
            }
        }
        reserveExpansionRights();
    }

    /**
     * Releasing one node's lock may make others further up the tree towards the root node become available.
     */
    synchronized void propagateUnlock() {
        if (!isLocked()) return; // We've worked our way up to a node which is already not locked. No need to
        // propagate further.

        // A single free child means we can unlock this node.
        for (N child : getChildren()) {
            if (!child.isLocked()) {  // Does not need to stay locked.
                releaseExpansionRights();
                return;
            }
        }
    }

    /**
     * Determine whether any sampler has exclusive rights to sample from this node.
     *
     * @return Whether any worker has exclusive rights to expand from this node (true/false).
     */
    public boolean isLocked() {
        return locked.get();
    }

}
