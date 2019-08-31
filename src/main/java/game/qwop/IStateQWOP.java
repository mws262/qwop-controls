package game.qwop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import game.state.IState;
import game.state.StateVariable6D;

//@JsonTypeInfo(
//        use = JsonTypeInfo.Id.NAME,
//        property = "type")
//@JsonSubTypes({
//        @JsonSubTypes.Type(value = StateQWOP.class, name = "posvel"),
//        @JsonSubTypes.Type(value = StateQWOPDelayEmbedded.class, name = "delayembed"),
//        @JsonSubTypes.Type(value = StateQWOPDelayEmbedded_Poses.class, name = "delayposes"),
//        @JsonSubTypes.Type(value = StateQWOPDelayEmbedded_Differences.class, name = "delaydiffs"),
//        @JsonSubTypes.Type(value = StateQWOPDelayEmbedded_HigherDifferences.class, name = "delayhighdiffs"),
//})
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
