package game.coordinates;

/**
 * Long-winded way of making an extensible enum.
 */
public class PoseCoordinate implements Coordinate{
    public static final PoseCoordinate x = new PoseCoordinate("x");
    public static final PoseCoordinate y = new PoseCoordinate("y");
    public static final PoseCoordinate th = new PoseCoordinate("th");
    protected static PoseCoordinate[] values = new PoseCoordinate[]{x, y, th};

    private final String name;

    PoseCoordinate(String name) {
        this.name = name;
    }

    public static Coordinate[] values() {
        return values;
    }
}