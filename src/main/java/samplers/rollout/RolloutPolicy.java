package samplers.rollout;

import actions.Action;
import actions.ActionQueue;
import evaluators.IEvaluationFunction;
import game.IGame;
import tree.Node;

/**
 * Sometimes, rollouts need to be a composite of many actions, some which may involve multiple simulations. This
 * interface allows for more complicated evaluations to be run.
 *
 * @author matt
 */
public abstract class RolloutPolicy {

    IEvaluationFunction evaluationFunction;

    ActionQueue actionQueue = new ActionQueue();

    RolloutPolicy(IEvaluationFunction evaluationFunction) {
        this.evaluationFunction = evaluationFunction;
    }

    /**
     * Do a rollout from a given node. Assumes that the given game is in the state of startNode! Be careful!
     * @param startNode Starting Node to rollout from.
     * @param game Instance of the game to use. Must already be at the state of startNode.
     * @return The reward associated with how good this rollout was.
     */
    public abstract float rollout(Node startNode, IGame game);

    /**
     * Run the simulation to get back to a specified node.
     * @param targetNode Node we want to simulate to.
     * @param game Game used for simulation. Will be reset before simulating.
     */
    void simGameToNode(Node targetNode, IGame game) {
        // Reset the game and action queue.
        game.makeNewWorld();
        actionQueue.clearAll();
        actionQueue.addSequence(targetNode.getSequence());

        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
        }
    }

    /**
     * Force-set the state of the game to the state at a node. This is not the same thing since the warm-start states
     * will not be set.
     * @param target Node to set the game's state to.
     * @param game Game used for simulation. Will be reset before setting the state.
     */
    void coldStartGameToNode(Node target, IGame game) {
        // Reset the game.
        game.makeNewWorld();
//        actionQueue.clearAll();
        game.setState(target.getState());
    }

    /**
     * Do a random rollout from a node, returning the terminal node arrived at.
     * @param startNode Node to expand from.
     * @param game Game simulation to use. Note that it is not reset ahead of time.
     * @param maxTimesteps Maximum timesteps to simulate in the rollout before returning. If parameter is not given,
     *                     then there is no limit.
     * @return The Node we arrive at at failure.
     */
    Node randomRollout(Node startNode, IGame game, int maxTimesteps) {
        int timestepCounter = 0;
        Node rolloutNode = startNode;
        while (!rolloutNode.isFailed() && timestepCounter < maxTimesteps) {
            Action childAction = rolloutNode.uncheckedActions.getRandom();
            rolloutNode = new Node(rolloutNode, childAction, false);
            actionQueue.addAction(childAction);

            while (!actionQueue.isEmpty() && !game.getFailureStatus() && timestepCounter < maxTimesteps) {
                game.step(actionQueue.pollCommand());
                timestepCounter++;
            }

            rolloutNode.setState(game.getCurrentState());
        }
        return rolloutNode;
    }

    Node randomRollout(Node startNode, IGame game) {
        return randomRollout(startNode, game, Integer.MAX_VALUE);
    }

    public abstract RolloutPolicy getCopy();
}
