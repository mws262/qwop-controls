package samplers.rollout;

import actions.Action;
import evaluators.IEvaluationFunction;
import game.IGame;
import tree.Node;

public class RolloutPolicy_WorstCaseWindow extends RolloutPolicy {

    public RolloutPolicy_WorstCaseWindow(IEvaluationFunction evaluationFunction) {
        super(evaluationFunction);
    }

    @Override
    public float rollout(Node startNode, IGame game) {

        // Need to do a rollout for the actual node we landed on.
        Node endNodeMiddle = randomRollout(startNode, game);
        Action middleAction = startNode.getAction();

        // Need to do a rollout for each one-off action from this node's.

        // One action duration above.
        Node nodeAbove = new Node(startNode.getParent(), new Action(middleAction.getTimestepsTotal() + 1,
                middleAction.peek()), false);
        simGameToNode(nodeAbove, game);
        Node endNodeAbove = randomRollout(nodeAbove, game);

        // One action duration below.
        Node nodeBelow = new Node(startNode.getParent(), new Action(middleAction.getTimestepsTotal() - 1,
                middleAction.peek()), false);
        simGameToNode(nodeAbove, game);
        Node endNodeBelow = randomRollout(nodeAbove, game);

        float startValue = evaluationFunction.getValue(startNode);
        float valMid = evaluationFunction.getValue(endNodeMiddle) - startValue;
        float valAbove = evaluationFunction.getValue(endNodeAbove) - startValue;
        float valBelow = evaluationFunction.getValue(endNodeBelow) - startValue;

        return Math.min(valMid, Math.min(valAbove, valBelow)); // Gets the worst out of three.
    }

    @Override
    public RolloutPolicy getCopy() {
        return new RolloutPolicy_WorstCaseWindow(evaluationFunction.getCopy());
    }
}
