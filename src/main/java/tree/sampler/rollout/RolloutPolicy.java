package tree.sampler.rollout;

import game.actions.*;
import distributions.Distribution;
import distributions.Distribution_Normal;
import tree.node.evaluator.IEvaluationFunction;
import game.IGameInternal;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Sometimes, rollouts need to be a composite of many game.actions, some which may involve multiple simulations. This
 * interface allows for more complicated evaluations to be run.
 *
 * @author matt
 */
public abstract class RolloutPolicy {

    IEvaluationFunction evaluationFunction;

    ActionQueue actionQueue = new ActionQueue();

    private final List<Action> actionSequence = new ArrayList<>(); // Reused local list.

    RolloutPolicy(IEvaluationFunction evaluationFunction) {
        this.evaluationFunction = evaluationFunction;
    }

    /**
     * Do a rollout from a given node. Assumes that the given game is in the state of startNode! Be careful!
     * @param startNode Starting Node to rollout from.
     * @param game Instance of the game to use. Must already be at the state of startNode.
     * @return The reward associated with how good this rollout was.
     */
    public abstract float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game);

    /**
     * Run the simulation to get back to a specified node.
     * @param targetNode Node we want to simulate to.
     * @param game Game used for simulation. Will be reset before simulating.
     */
    void simGameToNode(NodeQWOPBase<?> targetNode, IGameInternal game) {
        // Reset the game and action queue.
        game.makeNewWorld();
        actionQueue.clearAll();
        targetNode.getSequence(actionSequence);
        actionQueue.addSequence(actionSequence);

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
    void coldStartGameToNode(NodeQWOPBase<?> target, IGameInternal game) {
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
    NodeQWOPExplorableBase<?> randomRollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game, int maxTimesteps) {
        int timestepCounter = 0;
        NodeQWOPExplorableBase<?> rolloutNode = startNode;
        while (!rolloutNode.getState().isFailed() && timestepCounter < maxTimesteps) {
            Action childAction = rolloutNode.getUntriedActionRandom();
            actionQueue.addAction(childAction);

            while (!actionQueue.isEmpty() && !game.getFailureStatus() && timestepCounter < maxTimesteps) {
                game.step(actionQueue.pollCommand());
                timestepCounter++;
            }

            rolloutNode = rolloutNode.addBackwardsLinkedChild(childAction, game.getCurrentState(), getRolloutActionGenerator());
        }
        return rolloutNode;
    }

    NodeQWOPExplorableBase<?> randomRollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        return randomRollout(startNode, game, Integer.MAX_VALUE);
    }

    public abstract RolloutPolicy getCopy();

    public static IActionGenerator getRolloutActionGenerator() {
        /* Space of allowed game.actions to sample */
        //Distribution<Action> uniform_dist = new Distribution_Equal();

        /* Repeated action 1 -- no keys pressed. */
        Distribution<Action> dist1 = new Distribution_Normal(12, 1f);
        ActionList actionList1 = ActionList.makeActionList(IntStream.range(8, 15).toArray(), new boolean[]{false,
                false, false, false}, dist1);

        /*  Repeated action 2 -- W-O pressed */
        Distribution<Action> dist2 = new Distribution_Normal(20, 1f);
        ActionList actionList2 = ActionList.makeActionList(IntStream.range(18, 22).toArray(), new boolean[]{false, true,
                true, false}, dist2);

        /* Repeated action 3 -- W-O pressed */
        Distribution<Action> dist3 = new Distribution_Normal(12f, 1f);
        ActionList actionList3 = ActionList.makeActionList(IntStream.range(8, 15).toArray(), new boolean[]{false,
                false, false, false}, dist3);

        /*  Repeated action 4 -- Q-P pressed */
        Distribution<Action> dist4 = new Distribution_Normal(20, 1f);
        ActionList actionList4 = ActionList.makeActionList(IntStream.range(18, 22).toArray(), new boolean[]{true, false,
                false, true}, dist4);

        ActionList[] repeatedActions = new ActionList[]{actionList1, actionList2, actionList3, actionList4};

        return new ActionGenerator_FixedSequence(repeatedActions);
    }
}
