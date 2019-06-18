package tree.sampler.rollout;

import game.actions.Action;
import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;

/**
 * This is a meta-rollout policy. It does multiple other rollouts and aggregates the results.
 *
 * For now, this is either a best or worst out of three. Intuition says worst of three should be more robust, but so
 * far, best of three has been better. Perhaps because overinflated scores tend to be explored by UCB and toned down,
 * whereas underestimated scores might never be touched again.
 *
 * @author matt
 */
public class RolloutPolicy_Window extends RolloutPolicy {

    private RolloutPolicy individualRollout;

    public enum Criteria {
        WORST, BEST, AVERAGE,
    }

    public Criteria selectionCriteria = Criteria.BEST;

    public RolloutPolicy_Window(RolloutPolicy individualRollout) {
        super(individualRollout.evaluationFunction);
        this.individualRollout = individualRollout;
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {

        // Need to do a rollout for the actual node we landed on.
        Action middleAction = startNode.getAction();
        Action aboveAction = new Action(middleAction.getTimestepsTotal() + 1, middleAction.peek());
        Action belowAction = new Action(middleAction.getTimestepsTotal() - 1, middleAction.peek());

        /* Value of the center action (the one we are targeting). */
        float startValue = individualRollout.evaluationFunction.getValue(startNode);
        float valMid = individualRollout.rollout(startNode, game);

        /* Value of the action one above the target one. */
        if (startNode.getTreeDepth() > 1)
            simGameToNode(startNode.getParent(), game);
        actionQueue.addAction(aboveAction);
        while (!actionQueue.isEmpty())
            game.step(actionQueue.pollCommand());

        NodeQWOPExplorableBase<?> nodeAbove = startNode.getParent().addBackwardsLinkedChild(aboveAction, game.getCurrentState());
        float valAbove =
                individualRollout.rollout(nodeAbove, game) + individualRollout.evaluationFunction.getValue(nodeAbove) - startValue;


        if (startNode.getTreeDepth() > 1)
            simGameToNode(startNode.getParent(), game);
        actionQueue.addAction(belowAction);
        while (!actionQueue.isEmpty())
            game.step(actionQueue.pollCommand());

        NodeQWOPExplorableBase<?> nodeBelow = startNode.getParent().addBackwardsLinkedChild(belowAction, game.getCurrentState());
        float valBelow =
                individualRollout.rollout(nodeBelow, game) + individualRollout.evaluationFunction.getValue(nodeBelow) - startValue;

        switch(selectionCriteria) {

            case WORST:
                return Float.min(valMid, Float.min(valAbove, valBelow));
            case BEST:
                return Float.max(valMid, Float.max(valAbove, valBelow));
            case AVERAGE:
                return (valMid + valAbove + valBelow) / 3f;
            default:
                return (valMid + valAbove + valBelow) / 3f;
        }
    }

    @Override
    public RolloutPolicy getCopy() {
        return new RolloutPolicy_Window(individualRollout.getCopy());
    }
}
