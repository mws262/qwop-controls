package value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.IGameSerializable;
import game.action.Action;
import tree.node.NodeQWOPBase;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ValueFunction_Constant.class, name = "constant"),
        @JsonSubTypes.Type(value = ValueFunction_TensorFlow.class, name = "tensorflow")
})
public interface IValueFunction extends AutoCloseable {

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

    @JsonIgnore
    IValueFunction getCopy();

    @Override
    void close();
}
