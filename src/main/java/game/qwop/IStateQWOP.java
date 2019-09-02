package game.qwop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import game.state.IState;
import game.state.StateVariable6D;

public interface IStateQWOP extends IState {
    /**
     * Name of each body part.
     */
    enum ObjectName {
        BODY, HEAD, RTHIGH, LTHIGH, RCALF, LCALF, RFOOT, LFOOT, RUARM, LUARM, RLARM, LLARM
    }

    StateVariable6D getStateVariableFromName(ObjectName obj);

    @JsonIgnore
    StateVariable6D[] getAllStateVariables();

    @JsonIgnore
    int getStateVariableCount();

    @JsonIgnore
    StateQWOP getPositionCoordinates();
}
