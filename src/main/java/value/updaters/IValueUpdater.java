package value.updaters;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tree.node.NodeQWOPBase;

/**
 * A rule for updating the estimated value of a {@link NodeQWOPBase} given a node and an update value.
 *
 * @author matt
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ValueUpdater_Average.class, name = "average"),
        @JsonSubTypes.Type(value = ValueUpdater_HardSet.class, name = "hard_set"),
        @JsonSubTypes.Type(value = ValueUpdater_StdDev.class, name = "stdev_above"),
        @JsonSubTypes.Type(value = ValueUpdater_TopNChildren.class, name = "top_children")
})
public interface IValueUpdater {

    /**
     * Provide an updated value for a node.
     * @param valueUpdate An update value.
     * @param node Node to calculate an updated value for.
     * @return The provided node's updated value.
     */
    float update(float valueUpdate, NodeQWOPBase<?> node);
}
