package game.state.transform;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.state.IState;

import java.util.List;
/**
 * ITransforms convert {@link IState states} to an array of numbers, and potentially back again. This can be useful when
 * doing model reduction.
 *
 * @author matt
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Transform_Autoencoder.class, name = "autoencoder"),
        @JsonSubTypes.Type(value = Transform_PCA.class, name = "pca"),
        @JsonSubTypes.Type(value = Transform_Identity.class, name = "identity")
})
public interface ITransform<S extends IState> {

    /**
     * Update any necessary transform calculations. Not used by all types of transforms.
     *
     * @param statesToUpdateFrom The transform is updated using the data in this list.
     */
    void updateTransform(List<S> statesToUpdateFrom);

    /**
     * Transform states into arrays of numbers. Potentially, this is state reduction by means of an
     * autoencoder, PCA, etc.
     *
     * @param originalStates List of states to transform.
     * @return A list of float arrays containing the transformed versions of each of the states in the input list.
     */
    List<float[]> transform(List<S> originalStates);

    float[] transform(S originalState);

    /**
     * Reverse a transform, if possible. In other words, take an array of numbers and turn it into a state.
     *
     * @param transformedStates List of float arrays containing transformed representations of states.
     * @return List of states constructed by untransforming the input values.
     */
    List<float[]> untransform(List<float[]> transformedStates);

    /**
     * Transform and then untransform a list of states. This can be useful to see what kinds of properties are
     * preserved under the transformation, or what sort of things are lost by compressing the state.
     *
     * @param originalStates A list of states to transform/untransform.
     * @return A list of states resulting from transforming and then untransforming an original list of states.
     */
    List<float[]> compressAndDecompress(List<S> originalStates);

    /**
     * Find out the number of floats used to represent each state after transformation. For model reduction, this
     * value will be less than the number of values in a {@link IState}.
     *
     * @return Number of floats that each {@link IState} will be transformed into when calling
     * {@link ITransform#transform(List)}.
     */
    int getOutputSize();

    /**
     * Get the name of this transform.
     *
     * @return The name of this transform.
     */
    String getName();
}
