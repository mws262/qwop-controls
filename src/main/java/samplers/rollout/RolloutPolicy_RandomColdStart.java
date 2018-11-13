package samplers.rollout;

import evaluators.IEvaluationFunction;
import game.GameLoader;
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
    public float rollout(Node startNode, GameLoader game) {
        actionQueue.clearAll();

        Node normalRolloutEndNode = randomRollout(startNode, game);
        actionQueue.resetQueue();
        coldStartGameToNode(startNode, game);

//        Node coldStartRolloutEndNode = randomRollout(startNode, game);

        while (!actionQueue.isEmpty() && !game.getFailureStatus()) {
            game.stepGame(actionQueue.pollCommand());
        }



        return (evaluationFunction.getValue(normalRolloutEndNode) +
                game.getCurrentState().body.getX())/2f;
    }
}
