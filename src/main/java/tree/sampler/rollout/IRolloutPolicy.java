package tree.sampler.rollout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.evaluator.EvaluationFunction_Distance;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RolloutPolicy_JustEvaluate.class, name = "just_evaluate"),
        @JsonSubTypes.Type(value = RolloutPolicy_DeltaScore.class, name = "delta_score")
})
public interface IRolloutPolicy {

    float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game);

    @JsonIgnore
    IRolloutPolicy getCopy();
}
