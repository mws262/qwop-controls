package transformations;

import java.util.List;
import java.util.stream.Collectors;

import game.State;

/**
 * Transform that simply turns a state into an array of numbers and back. No state reduction occurs. This is
 * primarily useful as a placeholder or a testing tool. Note that the x component of the body is subtracted out in
 * transform.
 *
 * @author Matt
 */
@SuppressWarnings("unused")
public class Transform_Identity implements ITransform {

    @Override
    public void updateTransform(List<State> nodesToUpdateFrom) {}

    @Override
    public List<float[]> transform(List<State> originalStates) {
        return originalStates.stream().map(State::flattenState).collect(Collectors.toList());
    }

    @Override
    public List<State> untransform(List<float[]> transformedStates) {
        return transformedStates.stream().map(f -> new State(f, false)).collect(Collectors.toList());
    }

    @Override
    public List<State> compressAndDecompress(List<State> originalStates) {
        return originalStates;
    }

    @Override
    public int getOutputStateSize() {
        return State.ObjectName.values().length * State.StateName.values().length;
    }

    @Override
    public String getName() {
        return "identity";
    }
}
