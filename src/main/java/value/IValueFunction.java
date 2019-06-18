package value;

import game.actions.Action;
import game.IGameSerializable;
import tree.node.NodeQWOPBase;

import java.util.List;

public interface IValueFunction {

    /**
     * Find the child Action which is predicted to maximize value.
     * @param currentNode
     * @return
     */
    Action getMaximizingAction(NodeQWOPBase<?> currentNode);

    /**
     * Find the child Action which is predicted to maximize value.
     * @param currentNode
     * @return
     */
    Action getMaximizingAction(NodeQWOPBase<?> currentNode, IGameSerializable game);

    /**
     * Calculate the value of having gotten to the provided Node.
     * @param currentNode
     * @return
     */
    float evaluate(NodeQWOPBase<?> currentNode);

    /**
     * Provide a list of nodes from which information will be taken to update the value function.
     * @param nodes
     */
    void update(List<? extends NodeQWOPBase<?>> nodes);
}
