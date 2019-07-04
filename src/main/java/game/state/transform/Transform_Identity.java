package game.state.transform;

import game.state.IState;
import game.state.State;
import game.state.StateVariable;

import java.util.List;
import java.util.stream.Collectors;

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
    public void updateTransform(List<IState> nodesToUpdateFrom) {}

    @Override
    public List<float[]> transform(List<IState> originalStates) {
        return originalStates.stream().map(IState::flattenState).collect(Collectors.toList());
    }

    @Override
    public List<IState> untransform(List<float[]> transformedStates) {
        return transformedStates.stream().map(f -> new State(f, false)).collect(Collectors.toList());
    }

    @Override
    public List<IState> compressAndDecompress(List<IState> originalStates) {
        return originalStates;
    }

    @Override
    public int getOutputSize() {
        return State.ObjectName.values().length * StateVariable.StateName.values().length;
    }

    @Override
    public String getName() {
        return "identity";
    }
}
