package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.IGameInternal;
import game.action.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tree.node.NodeQWOPExplorableBase;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RolloutPolicy_JustEvaluate.class, name = "just_evaluate"),
        @JsonSubTypes.Type(value = RolloutPolicy_DeltaScore.class, name = "delta_score"),
        @JsonSubTypes.Type(value = RolloutPolicy_EndScore.class, name = "end_score"),
        @JsonSubTypes.Type(value = RolloutPolicy_DecayingHorizon.class, name = "decaying_horizon"),
        @JsonSubTypes.Type(value = RolloutPolicy_Window.class, name = "window"),
        @JsonSubTypes.Type(value = RolloutPolicy_EntireRun.class, name = "entire_run")

})
public interface IRolloutPolicy<C extends Command<?>> extends AutoCloseable {

    float rollout(@NotNull NodeQWOPExplorableBase<?, C> startNode, @Nullable IGameInternal<C> game);

    @JsonIgnore
    IRolloutPolicy<C> getCopy();

    @Override
    void close();
}
