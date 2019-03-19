package samplers.rollout;

import actions.Action;
import evaluators.IEvaluationFunction;
import game.IGame;
import tree.Node;
import value.IValueFunction;

public class RolloutPolicy_ValueFunction extends RolloutPolicy {

    /**
     * Rollout is terminated on game failure (falling) or a maximum number of timesteps simulated.
     */
    public int maxRolloutTimesteps = 100;

    private IValueFunction valueFunction;

    public RolloutPolicy_ValueFunction(IEvaluationFunction evaluationFunction, IValueFunction valueFunction) {
        super(evaluationFunction);
        this.valueFunction = valueFunction;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public float rollout(Node startNode, IGame game) {
        Node currentNode = startNode;
        int rolloutTimesteps = 0;
        while (!game.getFailureStatus() && rolloutTimesteps < maxRolloutTimesteps) {

            Action chosenAction = valueFunction.getMaximizingAction(currentNode);

            // For convenience and debugging, make an unattached node for the chosen action.
            currentNode = new Node(currentNode, chosenAction, false);

            // Execute the action. Break out on completion or failure.
            actionQueue.addAction(chosenAction);
            while (!actionQueue.isEmpty() && !game.getFailureStatus() && rolloutTimesteps < maxRolloutTimesteps) {
                game.step(actionQueue.pollCommand());
                rolloutTimesteps++;
            }
            currentNode.setState(game.getCurrentState());
        }

        // System.out.println("Rollout: " + currentNode.getState().body.getX() + ", " + rolloutTimesteps);
        return evaluationFunction.getValue(currentNode) - evaluationFunction.getValue(startNode);
    }

    public RolloutPolicy getCopy() {
        RolloutPolicy_ValueFunction copy = new RolloutPolicy_ValueFunction(evaluationFunction.getCopy(),
                valueFunction); //TODO is it ok to not copy the value function network too?
        copy.maxRolloutTimesteps = maxRolloutTimesteps;
        return copy;
    }
}
