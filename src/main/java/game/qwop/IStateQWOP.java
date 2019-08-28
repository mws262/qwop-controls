package game.qwop;

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

    StateVariable6D[] getAllStateVariables();

    int getStateVariableCount();
}
