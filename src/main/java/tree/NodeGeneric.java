package tree;

@SuppressWarnings("WeakerAccess")
public class NodeGeneric extends NodeGenericBase<NodeGeneric> {

    public NodeGeneric() {
        super();
    }

    NodeGeneric(NodeGeneric parent) {
        super(parent);
    }

    public NodeGeneric addBackwardsLinkedChild() {
        return new NodeGeneric(this);
    }

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
