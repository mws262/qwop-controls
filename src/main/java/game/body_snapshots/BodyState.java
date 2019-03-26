package game.body_snapshots;

import game.State;
import game.coordinates.Coordinate;
import game.coordinates.StateCoordinate;

import java.util.List;

/**
 * Container for state values for a single body link at a single timestep. See {@link BodyPose} for the version which
 * only includes configuration variables.
 * <p>
 * These StateVariables are generally stored by {@link State State} to represent the full runner state.
 *
 * @author matt
 */
public class BodyState extends BodyPose {

    /**
     * Horizontal velocity of the body.
     */
    private final float dx;

    /**
     * Vertical velocity of the body.
     */
    private final float dy;

    /**
     * Counterclockwise angular rate of the body.
     */
    private final float dth;

    /**
     * Make a new StateVariables holding the configuration and velocity information for a single runner link.
     *
     * @param x   Horizontal position of the body.
     * @param y   Vertical position of the body.
     * @param th  Counterclockwise angle of the body.
     * @param dx  Horizontal velocity of the body.
     * @param dy  Vertical velocity of the body.
     * @param dth Counterclockwise angular rate of the body.
     */
    public BodyState(float x, float y, float th, float dx, float dy, float dth) {
        super(x, y, th);
        this.dx = dx;
        this.dy = dy;
        this.dth = dth;
    }

    /**
     * Make a new StateVariables holding the configuration and velocity information for a single runner link.
     *
     * @param stateVals List containing the 6 state values for a single link. Order should be x, y, th, dx, dy, dth.
     */
    public BodyState(List<Float> stateVals) {
        super(stateVals.get(0), stateVals.get(1), stateVals.get(2));
        if (stateVals.size() != 6)
            throw new RuntimeException("Tried to make a BodyState with the wrong number of values.");
        dx = stateVals.get(3);
        dy = stateVals.get(4);
        dth = stateVals.get(5);
    }

    /**
     * Get the horizontal velocity of the body.
     *
     * @return Horizontal velocity of the body.
     */
    public float getDx() {
        return dx;
    }

    /**
     * Get the vertical velocity of the body.
     *
     * @return Vertical velocity of the body.
     */
    public float getDy() {
        return dy;
    }

    /**
     * Get the counterclockwise angular rate of the body.
     *
     * @return Counterclockwise angular rate of the body.
     */
    public float getDth() {
        return dth;
    }


    public float getCoordinate(Coordinate c) {
        if (c == StateCoordinate.dx) {
            return getDx();
        } else if (c == StateCoordinate.dy) {
            return getDy();
        } else if (c == StateCoordinate.th) {
            return getTh();
        } else {
            return super.getCoordinate(c);
        }
    }

    /**
     * Get just the pose variables.
     * @return The superclass version containing x, y, theta.
     */
    public BodyPose getBodyPose() {
        return this;
    }
}