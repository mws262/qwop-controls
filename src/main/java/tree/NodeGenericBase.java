package tree;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Generic tree implementation non-specific to QWOP.
 * This uses F-bounded polymorphism, i.e. the recursive-looking generic types. This oddity lets all inheriting
 * classes use these methods and get back their own type. For example, to use this:
 *
 * class Foo extends NodeGenericBase<Foo>...
 * Then, fooInstance.getParent() will return something of type Foo specifically, and not of the more general type
 * NodeGenericBase.
 *
 * @author matt
 * @param <N>
 */
abstract class NodeGenericBase<N extends NodeGenericBase<N>> {

    /**
     * Node which leads up to this node. Parentage should not be changed externally.
     */
    private final N parent;

    /**
     * Child nodes. Not fixed size any more.
     */
    private final List<N> children = new CopyOnWriteArrayList<>();

    /**
     * Depth of this node in the tree. Root node is 0; its children are 1, etc.
     */
    private final int treeDepth;

    /**
     * For making a root node.
     */
    public NodeGenericBase() {
        treeDepth = 0;
        parent = null;
    }

    public NodeGenericBase(N parent) {
        this.parent = parent;
        treeDepth = parent.getTreeDepth() + 1;
    }

    void addChild(N child) {
        assert !children.contains(child);
        children.add(child);
    }

    /**
     * Get the parent node of this node. If called from root, will return null.
     *
     * @return Parent node of this node.
     */
    public N getParent() {
        return parent;
    }

    /**
     * Get the children of this node.
     *
     * @return A copy of the list of children of this node. Removing the nodes from this array will not remove them
     * from this node's actual children, but the nodes in the array are the originals.
     */
    public List<N> getChildren() {
        return children;
    }

    /**
     * Remove a node from this node's list of children if the node is present. Do NOT use this method lightly.
     * Usually want to do something else.
     * @param node Child node to remove.
     */
    public void removeFromChildren(N node) {
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
        return parent.getChildren().indexOf(getThis());
    }

    /**
     * Get the number of siblings, i.e. other nodes in this node's parent's list of children. Does not include this
     * node itself.
     *
     * @return Number of sibling nodes of this one.
     */
    public int getSiblingCount() {
        if (treeDepth == 0) return 0; // Root node has no siblings.
        int siblings = parent.getChildren().size() - 1;
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
    public N getChildByIndex(int childIndex) {
        return children.get(childIndex);
    }

    /**
     * Get a random already-created child of this node. Can be useful for sampling.
     *
     * @return A random child node of this node.
     * @throws IndexOutOfBoundsException Has no children.
     */
    public N getRandomChild() {
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
    public Collection<N> getNodesBelow(Collection<N> nodeList) {
        recurseDownTreeInclusive(nodeList::add);
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
    public Collection<N> getLeaves(Collection<N> leaves) {

        if (children.isEmpty()) { // If leaf, add itself.
            leaves.add(getThis());
        } else { // Otherwise keep traversing down.
            for (NodeGenericBase<N> child : children) {
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
    public N getRoot() {
        N currentNode = getThis();
        while (currentNode.getTreeDepth() > 0) {
            currentNode = currentNode.getParent();
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
        for (N current : children) {
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
    public boolean isOtherNodeAncestor(N possibleAncestorNode) {
        if (possibleAncestorNode.getTreeDepth() >= treeDepth) { // Don't need to check if this is as far down the
            // tree.
            return false;
        }
        N currNode = parent;
        while (currNode.getTreeDepth() != possibleAncestorNode.getTreeDepth()) { // Find the node at the same depth as the one
            // we're checking.
            currNode = currNode.getParent();
        }
        return currNode.equals(possibleAncestorNode);
    }


    /**
     * Try to de-reference everything on this branch so garbage collection throws out all the state values and other
     * info stored for this branch to keep memory in check.
     */
    public void destroyNodesBelow() {
        for (N child : children) {
            child.destroyNodesBelow();
        }
        children.clear();
    }

    /**
     * Can pass a lambda to recurse down the tree. Will include the node called.
     * @param operation Lambda to run on all the nodes in the branch below and including the node called upon.
     */
    public void recurseDownTreeInclusive(Consumer<N> operation) {
        operation.accept(getThis());
        for (N child : children) {
            child.recurseDownTreeInclusive(operation);
        }
    }

    public void recurseDownTreeExclusive(Consumer<N> operation) {
        for (N child : children) {
            operation.accept(child);
            child.recurseDownTreeInclusive(operation);
        }
    }

    public void recurseUpTreeInclusive(Consumer<N> operation) {
        operation.accept(getThis());
        if (treeDepth > 0) {
            parent.recurseUpTreeInclusive(operation);
        }
    }
    public void recurseUpTreeInclusiveNoRoot(Consumer<N> operation) {
        operation.accept(getThis());
        if (treeDepth > 1) {
            parent.recurseUpTreeInclusive(operation);
        }
    }

    protected abstract N getThis();
}
