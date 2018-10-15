package transformations;

import java.util.List;

import game.State;
import tree.Node;

/**
 * ITransforms convert {@link State states} to an array of numbers, and potentially back again. This can be useful when
 * doing model reduction.
 *
 * @author matt
 */
public interface ITransform {

    /**
     * Update any necessary transform calculations. Not used by all types of transforms.
     *
     * @param nodesToUpdateFrom The transform is updated using the data in this list of {@link Node nodes}.
     */
    void updateTransform(List<State> nodesToUpdateFrom);

    /**
     * Transform states into arrays of numbers. Potentially, this is state reduction by means of an
     * autoencoder, PCA, etc.
     *
     * @param originalStates List of states to transform.
     * @return A list of float arrays containing the transformed versions of each of the states in the input list.
     */
    List<float[]> transform(List<State> originalStates);

    /**
     * Reverse a transform, if possible. In other words, take an array of numbers and turn it into a state.
     *
     * @param transformedStates List of float arrays containing transformed representations of states.
     * @return List of states constructed by untransforming the input values.
     */
    List<State> untransform(List<float[]> transformedStates);

    /**
     * Transform and then untransform a list of states. This can be useful to see what kinds of properties are
     * preserved under the transformation, or what sort of things are lost by compressing the state.
     *
     * @param originalStates A list of states to transform/untransform.
     * @return A list of states resulting from transforming and then untransforming an original list of states.
     */
    List<State> compressAndDecompress(List<State> originalStates);

    /**
     * Find out the number of floats used to represent each state after transformation. For model reduction, this
     * value will be less than the number of values in a {@link State}.
     *
     * @return Number of floats that each {@link State} will be transformed into when calling
     * {@link ITransform#transform(List)}.
     */
    int getOutputStateSize();

    /**
     * Get the name of this transform.
     *
     * @return The name of this transform.
     */
    String getName();
}
