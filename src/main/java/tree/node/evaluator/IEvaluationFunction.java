package tree.node.evaluator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tree.node.NodeQWOPBase;

/**
 * Generic evaluation of a node based on any factors. Going with "higher-is-better" interpretation of value.
 * Primarily used by ISampler
 *
 * @author Matt
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EvaluationFunction_Constant.class, name = "constant"),
        @JsonSubTypes.Type(value = EvaluationFunction_Distance.class, name = "distance")
})
public interface IEvaluationFunction {

    /**
     * Determine and return the value of a node. The methodology is determined by the implementation.
     *
     * @param nodeToEvaluate Node to determine the value of.
     * @return Scalar value of the node, with higher being "better".
     */
    float getValue(NodeQWOPBase<?> nodeToEvaluate);

    /**
     * Get a formatted string of the evaluated value of a node. Typically this will divide the value up into whatever
     * components go into it, making it easier to interpret and debug.
     *
     * @param nodeToEvaluate Node to determine the value of.
     * @return A formatted string of calculated value components.
     */
    String getValueString(NodeQWOPBase<?> nodeToEvaluate);

    /**
     * Create a copy of this IEvaluationFunction.
     *
     * @return A copy of this object.
     */
    @JsonIgnore
    IEvaluationFunction getCopy();
}
