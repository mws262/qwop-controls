package samplers.rollout;

import actions.Action;
import actions.ActionSet;
import evaluators.IEvaluationFunction;
import game.GameThreadSafe;
import tflowtools.TrainableNetwork;
import tree.Node;

public class RolloutPolicy_ValueFunction extends RolloutPolicy {

    /**
     * Rollout is terminated on game failure (falling) or a maximum number of timesteps simulated.
     */
    public int maxRolloutTimesteps = 100;

    private TrainableNetwork valueFunction;

    public RolloutPolicy_ValueFunction(IEvaluationFunction evaluationFunction, TrainableNetwork valueFunction) { // TODO
        // make value functions more generic.
        super(evaluationFunction);
        this.valueFunction = valueFunction;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public float rollout(Node startNode, GameThreadSafe game) {
        Node currentNode = startNode;
        float[][] state = new float[1][73];
        float[][] values;
        int rolloutTimesteps = 0;
        while (!game.getFailureStatus() && rolloutTimesteps < maxRolloutTimesteps) {
            // Get values of potential actions from this state (Q policy essentially).
            float[] st= currentNode.getState().flattenState();
            for (int i = 0; i < st.length; i++) {
                state[0][i] = st[i];
            }

            // Choose an action based on values.
            ActionSet actionChoices = Node.potentialActionGenerator.getPotentialChildActionSet(currentNode);
            // TODO decide the best way to select based on values. Right now, just the max value action. Future --
            //  make it reflect the best region of the action space. Should be more robust that way.

            float maxVal = -Float.MAX_VALUE;
            Action chosenAction = null;
//            int chosenIdx = 0; // TODO make sure that it isn't always choosing the same index at the beginning. Could
            // end up taking a gazillion tiny steps.
            for (int i = 0; i < actionChoices.size(); i++) {
                state[0][72] = actionChoices.get(i).getTimestepsTotal();
                float value = valueFunction.evaluateInput(state)[0][0];
                if (value > maxVal) {
                    maxVal = value;
                    chosenAction = actionChoices.get(i);
                }
            }

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

        System.out.println("Rollout: " + currentNode.getState().body.getX() + ", " + rolloutTimesteps);
        return evaluationFunction.getValue(currentNode);
    }

    public RolloutPolicy getCopy() {
        RolloutPolicy_ValueFunction copy = new RolloutPolicy_ValueFunction(evaluationFunction.getCopy(),
                valueFunction); //TODO is it ok to not copy the value function network too?
        copy.maxRolloutTimesteps = maxRolloutTimesteps;
        return copy;
    }
}
