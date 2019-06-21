package tree.sampler.rollout;

import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;

public interface IRolloutPolicy {

    float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game);

    IRolloutPolicy getCopy();
}
