package samplers.rollout;

import actions.Action;
import game.IGameInternal;
import tree.Node;

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
    public float rollout(Node startNode, IGameInternal game) {

        // Need to do a rollout for the actual node we landed on.
        Action middleAction = startNode.getAction();

        // One action duration above.
        Node nodeAbove = new Node(startNode.getParent(), new Action(middleAction.getTimestepsTotal() + 1,
                middleAction.peek()), false);

        // One action duration below.
        Node nodeBelow = new Node(startNode.getParent(), new Action(middleAction.getTimestepsTotal() - 1,
                middleAction.peek()), false);

        float startValue = evaluationFunction.getValue(startNode);
        float valMid = individualRollout.rollout(startNode, game);// - startValue;
        simGameToNode(nodeAbove, game);
        nodeAbove.setState(game.getCurrentState());
        float valAbove = individualRollout.rollout(nodeAbove, game);// - startValue;
        simGameToNode(nodeBelow, game);
        nodeBelow.setState(game.getCurrentState());
        float valBelow = individualRollout.rollout(nodeBelow, game);// - startValue;

        return Math.min(valMid, Math.min(valAbove, valBelow)); // Gets the worst out of three.
    }

    @Override
    public RolloutPolicy getCopy() {
        return new RolloutPolicy_WorstCaseWindow(individualRollout.getCopy());
    }
}
