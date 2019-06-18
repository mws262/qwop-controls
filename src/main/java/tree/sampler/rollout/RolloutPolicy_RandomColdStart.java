package tree.sampler.rollout;

import evaluators.IEvaluationFunction;
import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;

/**
 * Rollout policy which does a normal random rollout followed by another one, but with the physics engine cold
 * started at the node's state. We're trying to garner some robustness against the internal contact solver parameters.
 *
 * @author matt
 */
public class RolloutPolicy_RandomColdStart extends RolloutPolicy{

    public RolloutPolicy_RandomColdStart(IEvaluationFunction evaluationFunction) {
        super(evaluationFunction);
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        actionQueue.clearAll();

        NodeQWOPExplorableBase<?> normalRolloutEndNode = randomRollout(startNode, game);
        actionQueue.resetQueue();
        coldStartGameToNode(startNode, game);

//        Node coldStartRolloutEndNode = randomRollout(startNode, game);

        while (!actionQueue.isEmpty() && !game.getFailureStatus()) {
            game.step(actionQueue.pollCommand());
        }


        return (evaluationFunction.getValue(normalRolloutEndNode) +
                game.getCurrentState().getCenterX() - startNode.getState().getCenterX()) / 2f; // TODO Stop hardcoding
        // this.
    }
    @Override
    public RolloutPolicy getCopy() {
        return new RolloutPolicy_RandomColdStart(evaluationFunction.getCopy());
    }
}
