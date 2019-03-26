package game.body_snapshots;

import game.coordinates.Coordinate;
import game.coordinates.PoseCoordinate;

import java.io.Serializable;

/**
 * Container for configuration variables of a single body link at a single timestep. See {@link BodyState} which
 * extends this for the version which includes velocities.
 *
 * @author matt
 */
public class BodyPose implements BodySnapshot, Serializable {

    /**
     * Horizontal position of the body.
     */
    private final float x;

    /**
     * Vertical position of the body.
     */
    private final float y;

    /**
     * Counterclockwise angle of the body.
     */
    private final float th;


    /**
     * Make a new BodyPose holding the configuration for a single runner link.
     *
     * @param x   Horizontal position of the body.
     * @param y   Vertical position of the body.
     * @param th  Counterclockwise angle of the body.
     */
    public BodyPose(float x, float y, float th) {
        this.x = x;
        this.y = y;
        this.th = th;
    }

    /**
     * Get the horizontal position of the body.
     *
     * @return Horizontal position of the body.
     */
    public float getX() {
        return x;
    }

    /**
     * Get the vertical position of the body.
     *
     * @return Vertical position of the body.
     */
    public float getY() {
        return y;
    }

    /**
     * Get the counterclockwise angle of the body.
     *
     * @return Counterclockwise angle of the body.
     */
    public float getTh() {
        return th;
    }

    @Override
    public float getCoordinate(Coordinate c) {
        if (c == PoseCoordinate.x) {
            return getX();
        } else if (c == PoseCoordinate.y) {
            return getY();
        } else if (c == PoseCoordinate.th) {
            return getTh();
        } else {
            throw new IllegalStateException("A coordinate which should not exist was requested.");
        }
    }
}
