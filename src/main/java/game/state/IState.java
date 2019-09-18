package game.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.cartpole.StateCartPole;
import game.qwop.*;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StateQWOP.class, name = "state_standard"),
        @JsonSubTypes.Type(value = StateQWOPDelayEmbedded.class, name = "state_delay_embedded"),
        @JsonSubTypes.Type(value = StateQWOPDelayEmbedded_Poses.class, name = "delay_embedded_poses"),
        @JsonSubTypes.Type(value = StateQWOPDelayEmbedded_Differences.class, name = "delay_differences"),
        @JsonSubTypes.Type(value = StateQWOPDelayEmbedded_HigherDifferences.class, name = "delay_higher_differences"),
        @JsonSubTypes.Type(value = StateCartPole.class, name = "cartpole")
})
public interface IState {

    /**
     * Turn the state into an array of floats with body x subtracted from all x coordinates.
     **/
    float[] flattenState();

    @JsonIgnore
    float getCenterX();

    boolean isFailed();

    @JsonIgnore
    int getStateSize();
}
