package game;

import java.io.Serializable;
import java.util.List;

public class StateVariable implements Serializable {

    private static final long serialVersionUID = 1L;

    public float x;
    public float y;
    public float th;
    public float dx;
    public float dy;
    public float dth;

    public StateVariable(float x, float y, float th, float dx, float dy, float dth) {
        this.x = x;
        this.y = y;
        this.th = th;
        this.dx = dx;
        this.dy = dy;
        this.dth = dth;
    }

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
}