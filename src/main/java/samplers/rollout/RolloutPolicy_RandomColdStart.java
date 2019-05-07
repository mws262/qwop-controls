package samplers.rollout;

import evaluators.IEvaluationFunction;
import game.IGameInternal;
import tree.Node;

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
    public float rollout(Node startNode, IGameInternal game) {
        actionQueue.clearAll();

        Node normalRolloutEndNode = randomRollout(startNode, game);
        actionQueue.resetQueue();
        coldStartGameToNode(startNode, game);

//        Node coldStartRolloutEndNode = randomRollout(startNode, game);

        while (!actionQueue.isEmpty() && !game.getFailureStatus()) {
            game.step(actionQueue.pollCommand());
        }


        return (evaluationFunction.getValue(normalRolloutEndNode) +
                game.getCurrentState().body.getX() - startNode.getState().body.getX()) / 2f; // TODO Stop hardcoding
        // this
        // shit.
    }

    @Override
    public RolloutPolicy getCopy() {
        return new RolloutPolicy_RandomColdStart(evaluationFunction.getCopy());
    }
}
