package value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.IGameSerializable;
import game.action.Action;
import game.action.Command;
import tree.node.NodeQWOPBase;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ValueFunction_Constant.class, name = "constant"),
        @JsonSubTypes.Type(value = ValueFunction_TensorFlow.class, name = "tensorflow")
})
public interface IValueFunction<C extends Command<?>> extends AutoCloseable {

    /**
     * Find the child Action which is predicted to maximize value.
     */
    @JsonIgnore
    Action<C> getMaximizingAction(NodeQWOPBase<?, C> currentNode);

    /**
     * Find the child Action which is predicted to maximize value.
     */
    @JsonIgnore
    Action<C> getMaximizingAction(NodeQWOPBase<?, C> currentNode, IGameSerializable<C> game);

    /**
     * Calculate the value of having gotten to the provided Node.
     */
    float evaluate(NodeQWOPBase<?, C> currentNode);

    /**
     * Provide a list of nodes from which information will be taken to update the value function.
     */
    void update(List<? extends NodeQWOPBase<?, C>> nodes);

    @JsonIgnore
    IValueFunction<C> getCopy();

    @Override
    void close();
}
