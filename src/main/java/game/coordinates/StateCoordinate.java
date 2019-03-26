package game.coordinates;

public class StateCoordinate extends PoseCoordinate {
    public static final StateCoordinate dx = new StateCoordinate("dx");
    public static final StateCoordinate dy = new StateCoordinate("dy");
    public static final StateCoordinate dth = new StateCoordinate("dth");
    private static Coordinate[] stateValues;
    static {
        stateValues = new Coordinate[values.length + 3];
        for (int i = 0; i < values.length; i++) {
            stateValues[i] = values[i];
        }
        stateValues[values.length] = dx;
        stateValues[values.length + 1] = dy;
        stateValues[values.length + 2] = dth;
    }

    private StateCoordinate(String name) {
        super(name);
    }

    public static Coordinate[] values() {
        return stateValues;
    }
}