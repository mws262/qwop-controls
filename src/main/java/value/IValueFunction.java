package value;

import actions.Action;
import tree.Node;

import java.util.List;

public interface IValueFunction {

    /**
     * Find the child Action which is predicted to maximize value.
     * @param currentNode
     * @return
     */
    Action getMaximizingAction(Node currentNode);

    /**
     * Calculate the value of having gotten to the provided Node.
     * @param currentNode
     * @return
     */
    float evaluate(Node currentNode);

    /**
     * Provide a list of nodes from which information will be taken to update the value function.
     * @param nodes
     */
    void update(List<Node> nodes);
}