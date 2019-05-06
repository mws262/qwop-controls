package game;

import java.io.Serializable;
import java.util.List;

/**
 * Container for state values for a single torso link at a single timestep.
 * <p>
 * These StateVariables are generally stored by {@link State State} to represent the full runner state.
 *
 * @author matt
 */
public class StateVariable implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Horizontal position of the torso.
     */
    private final float x;

    /**
     * Vertical position of the torso.
     */
    private final float y;

    /**
     * Counterclockwise angle of the torso.
     */
    private float th;

    /**
     * Horizontal velocity of the torso.
     */
    private final float dx;

    /**
     * Vertical velocity of the torso.
     */
    private final float dy;

    /**
     * Counterclockwise angular rate of the torso.
     */
    private final float dth;

    /**
     * Make a new StateVariables holding the configuration and velocity information for a single runner link.
     *
     * @param x   Horizontal position of the torso.
     * @param y   Vertical position of the torso.
     * @param th  Counterclockwise angle of the torso.
     * @param dx  Horizontal velocity of the torso.
     * @param dy  Vertical velocity of the torso.
     * @param dth Counterclockwise angular rate of the torso.
     */
    public StateVariable(float x, float y, float th, float dx, float dy, float dth) {
        this.x = x;
        this.y = y;
        this.th = th;
        this.dx = dx;
        this.dy = dy;
        this.dth = dth;
    }

    /**
     * Make a new StateVariables holding the configuration and velocity information for a single runner link.
     *
     * @param stateVals List containing the 6 state values for a single link. Order should be x, y, th, dx, dy, dth.
     */
    public StateVariable(List<Float> stateVals) {
        if (stateVals.size() != 6)
            throw new RuntimeException("Tried to make a StateVariable with the wrong number of values.");

        x = stateVals.get(0);
        y = stateVals.get(1);
        th = stateVals.get(2);
        dx = stateVals.get(3);
        dy = stateVals.get(4);
        dth = stateVals.get(5);
    }

    /**
     * Get the horizontal position of the torso.
     *
     * @return Horizontal position of the torso.
     */
    public float getX() {
        return x;
    }

    /**
     * Get the vertical position of the torso.
     *
     * @return Vertical position of the torso.
     */
    public float getY() {
        return y;
    }

    /**
     * Get the counterclockwise angle of the torso.
     *
     * @return Counterclockwise angle of the torso.
     */
    public float getTh() {
        return th;
    }

    /**
     * Get the horizontal velocity of the torso.
     *
     * @return Horizontal velocity of the torso.
     */
    public float getDx() {
        return dx;
    }

    /**
     * Get the vertical velocity of the torso.
     *
     * @return Vertical velocity of the torso.
     */
    public float getDy() {
        return dy;
    }

    /**
     * Get the counterclockwise angular rate of the torso.
     *
     * @return Counterclockwise angular rate of the torso.
     */
    public float getDth() {
        return dth;
    }

    /**
     * The QWOP flash game uses different reference angles than I do. It's now allowed to adjust the angle.
     * @param angleAdjustment Radians to be added to the current angle.
     */
    void doAngleAdjustment(float angleAdjustment) {
        th += angleAdjustment;
    }
}