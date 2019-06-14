package game;

public interface IState {

    /**
     * Name of each body part.
     */
    enum ObjectName {
        BODY, HEAD, RTHIGH, LTHIGH, RCALF, LCALF, RFOOT, LFOOT, RUARM, LUARM, RLARM, LLARM
    }

    /**
     * Turn the state into an array of floats with body x subtracted from all x coordinates.
     **/
    float[] flattenState();

    StateVariable getStateVariableFromName(ObjectName obj);

    StateVariable[] getAllStateVariables();

    float getCenterX();

    boolean isFailed();

    int getStateVariableCount();

}
