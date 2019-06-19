package tree.sampler.rollout;

import game.action.Action;
import tree.node.evaluator.IEvaluationFunction;
import game.IGameInternal;
import game.IGameSerializable;
import tree.node.NodeQWOPExplorableBase;
import value.IValueFunction;

public class RolloutPolicy_ValueFunction extends RolloutPolicy {

    /**
     * Rollout is terminated on game failure (falling) or a maximum number of timesteps simulated.
     */
    public int maxRolloutTimesteps = 200;

    private IValueFunction valueFunction;

    public RolloutPolicy_ValueFunction(IEvaluationFunction evaluationFunction, IValueFunction valueFunction) {
        super(evaluationFunction);
        this.valueFunction = valueFunction;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        NodeQWOPExplorableBase<?> currentNode = startNode;
        int rolloutTimesteps = 0;
        while (!game.getFailureStatus() && rolloutTimesteps < maxRolloutTimesteps) {

            Action chosenAction;
            if (game instanceof IGameSerializable) {
                chosenAction = valueFunction.getMaximizingAction(currentNode, (IGameSerializable) game);
            } else {
                chosenAction = valueFunction.getMaximizingAction(currentNode);
            }

            // Execute the action. Break out on completion or failure.
            actionQueue.addAction(chosenAction);
            while (!actionQueue.isEmpty() && !game.getFailureStatus() && rolloutTimesteps < maxRolloutTimesteps) {
                game.step(actionQueue.pollCommand());
                rolloutTimesteps++;
            }
            currentNode = currentNode.addBackwardsLinkedChild(chosenAction, game.getCurrentState());
        }

        // System.out.println("Rollout: " + currentNode.getState().body.getX() + ", " + rolloutTimesteps);
        return (evaluationFunction.getValue(currentNode) - evaluationFunction.getValue(startNode)) / ((float) maxRolloutTimesteps / 40f);
    }

    public RolloutPolicy getCopy() {
        RolloutPolicy_ValueFunction copy = new RolloutPolicy_ValueFunction(evaluationFunction.getCopy(),
                valueFunction); //TODO is it ok to not copy the value function network too?
        copy.maxRolloutTimesteps = maxRolloutTimesteps;
        return copy;
    }
}
