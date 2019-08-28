package game.cartpole;

import com.google.common.base.Preconditions;
import game.state.IState;
import org.jetbrains.annotations.NotNull;

public class StateCartPole implements IState {

    public final int STATE_SIZE = 4;

    private final float[] stateValues = new float[STATE_SIZE];

    private final boolean isFailed;

    public StateCartPole(@NotNull float[] stateVals, boolean isFailed) {
        this(stateVals[0], stateVals[1], stateVals[2], stateVals[3], isFailed);
        Preconditions.checkArgument(stateVals.length == STATE_SIZE,
                "Array to construct a state was the wrong size. It was: "
                        + stateVals.length + ", but should have been: " + STATE_SIZE);

    }

    public StateCartPole(float cartPosition, float cartVelocity, float poleAngle, float poleTipVelocity, boolean isFailed) {
        stateValues[0] = cartPosition;
        stateValues[1] = cartVelocity;
        stateValues[2] = poleAngle;
        stateValues[3] = poleTipVelocity;
        this.isFailed = isFailed;
    }

    @Override
    public float[] flattenState() {
        return stateValues;
    }

    @Override
    public float getCenterX() {
        return stateValues[0];
    }

    @Override
    public boolean isFailed() {
        return isFailed;
    }

    @Override
    public int getStateSize() {
        return STATE_SIZE;
    }
}
