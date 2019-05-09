package tree;

import actions.Action;
import actions.ActionList;
import actions.IActionGenerator;
import game.State;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class NodeQWOPAssignedChildren extends NodeQWOP<NodeQWOPAssignedChildren> {

    private ActionList untriedActions;

    /**
     * Are there any untried things below this node? This is not necessarily a TERMINAL node, it is simply a node
     * past which there are no potential child actions to try.
     */
    private final AtomicBoolean fullyExplored = new AtomicBoolean(false);

    public NodeQWOPAssignedChildren(State rootState, IActionGenerator actionGenerator) {
        super(rootState);
        //untriedActions = actionGenerator.getPotentialChildActionSet(this);
    }

    public NodeQWOPAssignedChildren(NodeQWOPAssignedChildren parent, Action action, State state, IActionGenerator actionGenerator) {
        super(parent, action, state);
        //untriedActions = actionGenerator.getPotentialChildActionSet(this);
    }

    public boolean isFullyExplored() {
        return fullyExplored.get();
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
            for (NodeQWOPAssignedChildren child : getChildren()) {
                if (!child.fullyExplored.get()) { // If any child is not fully explored, then this node isn't too.
                    flag = false;
                }
            }
        }
        fullyExplored.set(flag);

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
        ArrayList<NodeQWOPAssignedChildren> leaves = new ArrayList<>(5000);
        getLeaves(leaves);

        // Reset all existing exploration flags out there.
        for (NodeQWOPAssignedChildren leaf : leaves) {
            NodeQWOPAssignedChildren currNode = leaf;
            while (currNode.getTreeDepth() > getTreeDepth()) {
                currNode.fullyExplored.set(false);
                currNode = currNode.getParent();
            }
            currNode.fullyExplored.set(false);
        }

        for (Node leaf : leaves) {
            leaf.propagateFullyExploredStatus_lite();
        }
    }
}
