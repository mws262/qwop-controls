package goals.deep_rl;

import game.State;
import org.deeplearning4j.rl4j.space.Encodable;

public class StateRL implements Encodable {

    private double[] stateVals;

    public StateRL(State state) {
        float[] stateVals = state.flattenState();
        double[] stateValsDouble = new double[stateVals.length];
        for (int i = 0; i < stateVals.length; i++) {
            stateValsDouble[i] = (double)stateVals[i];
        }
        this.stateVals = stateValsDouble;
    }

    @Override
    public double[] toArray() {
        return stateVals;
    }
}
