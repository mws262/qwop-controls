package ui;

import tree.Node;
import tree.NodeQWOPGraphicsBase;

import java.util.EventListener;

/**
 * Listener for selection of nodes by any UI component.
 *
 * @author matt
 */
public interface NodeSelectionListener extends EventListener {

    /**
     * Callback for a node which has been selected (usually by a click on the tree).
     * @param node Selected node.
     */
    void nodeSelected(NodeQWOPGraphicsBase<?> node);
}
