package ui;

import game.action.Command;
import game.state.IState;
import tree.node.NodeGameGraphicsBase;

import java.util.EventListener;

/**
 * Listener for selection of nodes by any UI component.
 *
 * @author matt
 */
public interface NodeSelectionListener<C extends Command<?>, S extends IState> extends EventListener {
    /**
     * Callback for a node which has been selected (usually by a click on the tree).
     * @param node Selected node.
     */
    void nodeSelected(NodeGameGraphicsBase<?, C, S> node);
}
