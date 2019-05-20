package value;

import actions.Action;
import game.IGameInternal;
import tree.NodeQWOPBase;

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
    Action getMaximizingAction(NodeQWOPBase<?> currentNode, IGameInternal game);

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
