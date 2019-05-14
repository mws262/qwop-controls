package samplers.rollout;

import actions.Action;
import game.IGame;
import tree.NodeQWOPExplorableBase;

/**
 * This is a meta-rollout policy. It does multiple other rollouts and aggregates the results.
 *
 * @author matt
 */
public class RolloutPolicy_WorstCaseWindow extends RolloutPolicy {

    private RolloutPolicy individualRollout;

    public RolloutPolicy_WorstCaseWindow(RolloutPolicy individualRollout) {
        super(individualRollout.evaluationFunction);
        this.individualRollout = individualRollout;
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGame game) {

        // Need to do a rollout for the actual node we landed on.
        Action middleAction = startNode.getAction();
        Action aboveAction = new Action(middleAction.getTimestepsTotal() + 1, middleAction.peek());
        Action belowAction = new Action(middleAction.getTimestepsTotal() - 1, middleAction.peek());


        float startValue = evaluationFunction.getValue(startNode);
        float valMid = individualRollout.rollout(startNode, game);// - startValue;
        simGameToNode(startNode.getParent(), game);
        actionQueue.addAction(aboveAction);
        while (!actionQueue.isEmpty())
            game.step(actionQueue.pollCommand());
        NodeQWOPExplorableBase<?> nodeAbove = startNode.getParent().addBackwardsLinkedChild(aboveAction, game.getCurrentState());

        float valAbove = individualRollout.rollout(nodeAbove, game);// - startValue;
        simGameToNode(startNode.getParent(), game);
        actionQueue.addAction(belowAction);
        while (!actionQueue.isEmpty())
            game.step(actionQueue.pollCommand());
        NodeQWOPExplorableBase<?> nodeBelow = startNode.getParent().addBackwardsLinkedChild(belowAction, game.getCurrentState());


        float valBelow = individualRollout.rollout(nodeBelow, game);// - startValue;

        return Math.min(valMid, Math.min(valAbove, valBelow)); // Gets the worst out of three.
    }

    @Override
    public RolloutPolicy getCopy() {
        return new RolloutPolicy_WorstCaseWindow(individualRollout.getCopy());
    }
}
