package tree;

public abstract class NodeData<S, A> {

    abstract S getState();

    abstract A getAction();

    abstract void display(Gl2 gl);

    abstract void dispose();

}