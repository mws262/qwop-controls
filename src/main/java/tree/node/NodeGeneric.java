package tree.node;

/**
 * @see NodeGenericBase
 */
@SuppressWarnings("WeakerAccess")
public class NodeGeneric extends NodeGenericBase<NodeGeneric> {

    /**
     * @see NodeGenericBase#NodeGenericBase()
     */
    public NodeGeneric() {
        super();
    }

    /**
     * @see NodeGenericBase#NodeGenericBase(NodeGenericBase)
     */
    NodeGeneric(NodeGeneric parent) {
        super(parent);
    }

    /**
     * Add a new child node with a link to its parent (this), but unknown to the parent.
     * @return A new, anonymous child node.
     */
    public NodeGeneric addBackwardsLinkedChild() {
        return new NodeGeneric(this);
    }

    /**
     * Add a new child node with bi-directional linking to its parent (this).
     * @return A new, doubly-connected child node.
     */
    public NodeGeneric addDoublyLinkedChild() {
        NodeGeneric child = new NodeGeneric(this);
        this.addToChildList(child);
        return child;
    }

    @Override
    protected NodeGeneric getThis() {
        return this;
    }


}
