package samplers.rollout;

import actions.Action;
import evaluators.IEvaluationFunction;
import game.IGameInternal;
import tree.NodeQWOPExplorableBase;

/**
 * To evaluate the score of a node. Run a rollout for each of its potential child nodes. Average them out. If run for
 * all potential children, this can be quite slow.
 *
 * @author matt
 */
public class RolloutPolicy_MultiChildren extends RolloutPolicy {

    /**
     * Maximum number of allowed rollouts.
     */
    public int maxRollouts = 3;

    /**
     * Do repeated rollouts as cold-starts? This will be faster for deep trees.
     */
    public boolean doColdStarts = true;

    public RolloutPolicy_MultiChildren(IEvaluationFunction evaluationFunction) {
        super(evaluationFunction);
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        // See how we should advance through untried actions. If the number of unchecked actions is less than the
        // number of rollouts allowed, we run all of them. Otherwise, we try to evenly-space them.
        float advancement = 1f;
        int numUnchecked = startNode.getUntriedActionCount();
        if (numUnchecked > maxRollouts) {
            advancement = numUnchecked / (float)maxRollouts;
        }

        actionQueue.clearAll();
        float score = 0;
        int count = 0; // First loop, we don't need to re-simulate back to the start node since we're already there.
        for (float i = 0; i < numUnchecked; i += advancement) {
            if (count++ > 0) {
                // Re-sim back to the fringe of the tree.
                game.makeNewWorld();
                if (doColdStarts) {
                    coldStartGameToNode(startNode, game);
                } else {
                    simGameToNode(startNode, game);
                }
            }

            // Try one of the child actions.
            Action nextAction = startNode.getUntriedActionByIndex((int) i);
            actionQueue.addAction(nextAction);
            while (!actionQueue.isEmpty() && !game.getFailureStatus()) {
                game.step(actionQueue.pollCommand());
            }
            NodeQWOPExplorableBase<?> childNode = startNode.addBackwardsLinkedChild(nextAction, game.getCurrentState());

            // Continue rollout randomly.
            NodeQWOPExplorableBase<?> terminalNode = randomRollout(childNode, game);

            // Accumulate score.
            score += evaluationFunction.getValue(terminalNode);
        }

        return score/(float)(maxRollouts > startNode.getUntriedActionCount() ? startNode.getUntriedActionCount() :
                maxRollouts);
    }

    @Override
    public RolloutPolicy getCopy() {
        RolloutPolicy_MultiChildren copy = new RolloutPolicy_MultiChildren(evaluationFunction.getCopy());
        copy.doColdStarts = doColdStarts;
        copy.maxRollouts = maxRollouts;
        return copy;
    }
}
