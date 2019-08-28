package game.state.transform;

import game.state.IState;
import game.qwop.StateQWOP;
import game.state.StateVariable6D;

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
public class Transform_Identity<S extends IState> implements ITransform<S> {

    @Override
    public void updateTransform(List<S> nodesToUpdateFrom) {}

    @Override
    public List<float[]> transform(List<S> originalStates) {
        return originalStates.stream().map(IState::flattenState).collect(Collectors.toList());
    }

    @Override
    public float[] transform(S originalState) {
        return originalState.flattenState();
    }

    @Override
    public List<float[]> untransform(List<float[]> transformedStates) {
        return transformedStates;
    }

    @Override
    public List<float[]> compressAndDecompress(List<S> originalStates) {
        return originalStates.stream().map(IState::flattenState).collect(Collectors.toList());
    }

    @Override
    public int getOutputSize() {
        return StateQWOP.ObjectName.values().length * StateVariable6D.StateName.values().length;
    }

    @Override
    public String getName() {
        return "identity";
    }
}
