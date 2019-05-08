package tree;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class NodeGeneric<N extends NodeData<S,A>, S,A> {

    private final N data;

    /**
     * Node which leads up to this node. Parentage should not be changed externally.
     */
    private final NodeGeneric<N,S,A> parent;

    /**
     * Child nodes. Not fixed size any more.
     */
    private final List<NodeGeneric<N,S,A>> children = new CopyOnWriteArrayList<>();

    private final int treeDepth;

    /**
     * If one TreeWorker is expanding from this leaf node (if it is one), then no other worker should try to
     * simultaneously expand from here too.
     */
    private final AtomicBoolean locked = new AtomicBoolean(false);

    /**
     * For making a root node.
     * @param data
     */
    public NodeGeneric(N data) {
        this.data = data;
        treeDepth = 0;
        parent = null;
    }

    public NodeGeneric(N data, NodeGeneric<N,S,A> parent) {
        this.data = data;
        this.parent = parent;
        treeDepth = parent.treeDepth + 1;
    }

    public N getData() {
        return data;
    }

    /**
     * Get the parent node of this node. If called from root, will return null.
     *
     * @return Parent node of this node.
     */
    public NodeGeneric<N,S,A> getParent() {
        return parent;
    }

    /**
     * Get the children of this node.
     *
     * @return A copy of the list of children of this node. Removing the nodes from this array will not remove them
     * from this node's actual children, but the nodes in the array are the originals.
     */
    public List<NodeGeneric<N,S,A>> getChildren() {
        return children;
    }

    /**
     * Remove a node from this node's list of children if the node is present. Do NOT use this method lightly.
     * Usually want to do something else.
     * @param node Child node to remove.
     */
    public void removeFromChildren(NodeGeneric<N,S,A> node) {
        children.remove(node);
    }

    /**
     * Get the index of this node in it's parent list of nodes. Hence, parent.children.get(index) == this.
     *
     * @return This node's index in its parent's list of nodes.
     * @throws IndexOutOfBoundsException When called on a root node (depth 0).
     */
    public int getIndexAccordingToParent() {
        if (treeDepth == 0)
            throw new IndexOutOfBoundsException("The root node has no parent, and thus this method call doesn't make " +
                    "sense.");
        return parent.children.indexOf(this);
    }

    /**
     * Get the number of siblings, i.e. other nodes in this node's parent's list of children. Does not include this
     * node itself.
     *
     * @return Number of sibling nodes of this one.
     */
    public int getSiblingCount() {
        if (treeDepth == 0) return 0; // Root node has no siblings.
        int siblings = parent.children.size() - 1;
        assert siblings >= 0;
        return siblings;
    }

    /**
     * Get the total number of children of this node. Only includes actually created children, not potential children
     * . Only includes direct children, not all descendants.
     *
     * @return Total created children of this node.
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Get a created child node of this node by its index (order of creation).
     *
     * @param childIndex Index of the child node to retrieve.
     * @return A child of this node, with the index as specified.
     */
    public NodeGeneric<N,S,A> getChildByIndex(int childIndex) {
        return children.get(childIndex);
    }

    /**
     * Get a random already-created child of this node. Can be useful for sampling.
     *
     * @return A random child node of this node.
     * @throws IndexOutOfBoundsException Has no children.
     */
    public NodeGeneric<N,S,A> getRandomChild() {
        if (children.isEmpty())
            throw new IndexOutOfBoundsException("Tried to get a random child from a node with no children.");
        return children.get(Utility.randInt(0, children.size() - 1));
    }

    /**
     * Add all the nodes below and including this one to a list. Does not include nodes whose state have not yet been
     * assigned.
     *
     * @param nodeList                  A list to add all of this branches' nodes to. This list must be caller-provided, and will not
     *                                  be cleared.
     * @return Returns the list of nodes below. This is done in place, so the object is the same as the argument one.
     */
    public Collection<NodeGeneric<N,S,A>> getNodesBelow(Collection<NodeGeneric<N,S,A>> nodeList) {
        nodeList.add(this);

        for (NodeGeneric<N,S,A> child : children) {
            child.getNodesBelow(nodeList);
        }
        return nodeList;
    }


    /**
     * Get a list of all tree endpoints (leaves) below this node, i.e. on this branch. If called from a leaf, the
     * list will only contain that leaf itself.
     *
     * @param leaves A list of leaves below this node. The list must be provided by the caller, and will not be
     *               cleared by this method.
     * @return Returns the list of nodes below. This is done in place, so the object is the same as the argument one.
     */
    public Collection<NodeGeneric<N,S,A>> getLeaves(Collection<NodeGeneric<N,S,A>> leaves) {

        if (children.isEmpty()) { // If leaf, add itself.
            leaves.add(this);
        } else { // Otherwise keep traversing down.
            for (NodeGeneric<N,S,A> child : children) {
                child.getLeaves(leaves);
            }
        }
        return leaves;
    }

    /**
     * How deep is this node down the tree? 0 is root.
     */
    public int getTreeDepth() {
        return treeDepth;
    }

    /**
     * Returns the tree root no matter which node in the tree this is called from. This method defines the tree root
     * to be the node with a depth of 0.
     *
     * @return The tree root node.
     */
    public NodeGeneric<N,S,A> getRoot() {
        NodeGeneric<N,S,A> currentNode = this;
        while (currentNode.treeDepth > 0) {
            currentNode = currentNode.parent;
        }
        return currentNode;
    }

    /**
     * Count the number of descendants this node has. Does not include the node first called on.
     *
     * @return Number of descendants, i.e. number of nodes on the branch below this node.
     */
    public int countDescendants() {
        int count = 0;
        for (NodeGeneric<N,S,A> current : children) {
            count++;
            count += current.countDescendants(); // Recurse down through the tree.
        }
        return count;
    }

    /**
     * Check whether a node is an ancestor of this node. This means that there is a direct path from this node to the
     * given node that only requires decreasing tree depth.
     *
     * @param possibleAncestorNode Node to check whether is an ancestor of this node.
     * @return Whether the provided node is an ancestor of this node (true/false).
     */
    public boolean isOtherNodeAncestor(NodeGeneric<N,S,A> possibleAncestorNode) {
        if (possibleAncestorNode.treeDepth >= treeDepth) { // Don't need to check if this is as far down the
            // tree.
            return false;
        }
        NodeGeneric<N,S,A> currNode = parent;
        while (currNode.treeDepth != possibleAncestorNode.treeDepth) { // Find the node at the same depth as the one
            // we're checking.
            currNode = currNode.parent;
        }
        return currNode.equals(possibleAncestorNode);
    }


    public synchronized List<A> getSequence(List<A> list) {
        list.clear();
        recurseUpTreeInclusiveNoRoot(n->list.add(n.getAction()));
        Collections.reverse(list);
        return list;
    }

    /**
     * Get the action object (most likely keypress + duration) that leads to this node from its parent.
     *
     * @return This node's action.
     * @throws NullPointerException When called on a root node (tree depth of 0).
     */
    public A getAction() {
        if (treeDepth == 0) // Root has no action
            throw new NullPointerException("Root node does not have an action associated with it.");
        return data.getAction();
    }

    /**
     * Get the game state associated with this node. Represents the state achieved from the parent node's state after
     * performing the action in this node.
     *
     * @return Game state at this node.
     * @throws NullPointerException If state is unassigned.
     */
    public S getState() {
        if (data.getState() == null)
            throw new NullPointerException("Node state unassigned. Call isStateUnassigned first to check.");
        return data.getState();
    }

    /**
     * Try to de-reference everything on this branch so garbage collection throws out all the state values and other
     * info stored for this branch to keep memory in check.
     */
    public void destroyNodesBelow() {
        for (NodeGeneric<N,S,A> child : children) {
            child.data.dispose();
            child.destroyNodesBelow();
        }
        children.clear();
    }

    /**
     * Can pass a lambda to recurse down the tree. Will include the node called.
     * @param operation Lambda to run on all the nodes in the branch below and including the node called upon.
     */
    public void recurseDownTreeInclusive(Consumer<NodeGeneric<N,S,A>> operation) {
        operation.accept(this);
        for (NodeGeneric<N,S,A> child : children) {
            child.recurseDownTreeInclusive(operation);
        }
    }

    public void recurseDownTreeExclusive(Consumer<NodeGeneric<N,S,A>> operation) {
        for (NodeGeneric<N,S,A> child : children) {
            operation.accept(child);
            child.recurseDownTreeInclusive(operation);
        }
    }

    public void recurseUpTreeInclusive(Consumer<NodeGeneric<N,S,A>> operation) {
        operation.accept(this);
        if (treeDepth > 0) {
            parent.recurseUpTreeInclusive(operation);
        }
    }
    public void recurseUpTreeInclusiveNoRoot(Consumer<NodeGeneric<N,S,A>> operation) {
        operation.accept(this);
        if (treeDepth > 1) {
            parent.recurseUpTreeInclusive(operation);
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
            if (getTreeDepth() > 0) parent.propagateLock();
            return true;
        }
    }

    /**
     * Set a flag to indicate that the invoking TreeWorker has released exclusive rights to expand from this node.
     */
    public synchronized void releaseExpansionRights() {
        locked.set(false); // Release the lock.
        // Unlocking this node may cause nodes further up the tree to become available.
        if ((treeDepth > 0) && (parent != null)) parent.propagateUnlock();
    }

    /**
     * Locking one node may cause some parent nodes to become unavailable also. propagateLock will check as far up
     * the tree towards the root node as necessary.
     */
    private synchronized void propagateLock() {
        // Lock this node unless we find evidence that we don't need to.
        for (NodeGeneric<N,S,A> child : children) {
            if (!child.isLocked() && !child.fullyExplored.get()) {
                return; // In this case, we don't need to continue locking things further up the tree.
            }
        }
        reserveExpansionRights();
    }

    /**
     * Releasing one node's lock may make others further up the tree towards the root node become available.
     */
    private synchronized void propagateUnlock() {
        if (!isLocked()) return; // We've worked our way up to a node which is already not locked. No need to
        // propagate further.

        // A single free child means we can unlock this node.
        for (NodeGeneric<N,S,A> child : children) {
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
