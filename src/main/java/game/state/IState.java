package game.state;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import data.LoadStateStatistics;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = State.class, name = "state_standard"),
        @JsonSubTypes.Type(value = StateDelayEmbedded.class, name = "state_delay_embedded"),
})
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

    float[] flattenStateWithRescaling(LoadStateStatistics.StateStatistics stateStatistics);

    StateVariable getStateVariableFromName(ObjectName obj);

    StateVariable[] getAllStateVariables();

    float getCenterX();

    boolean isFailed();

    int getStateVariableCount();

}
