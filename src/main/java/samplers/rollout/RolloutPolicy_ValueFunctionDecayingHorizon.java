package samplers.rollout;

import actions.Action;
import game.IGameInternal;
import tree.Node;
import value.ValueFunction_TensorFlow_StateOnly;

public class RolloutPolicy_ValueFunctionDecayingHorizon extends RolloutPolicy {

    // Decaying horizon kernel parameters.
    // Kernel is an s-curve meant to be evaluated from 0 to 1 and producing values from 0 to 1.
    private static final float kernelCenter = 0.5f; // Where the center of the s lies. Greater values shift the drop
    // til later, smaller values make the drop occur earlier.
    private static final float kernelSteepness = 5; // Steepness of the drop. Higher values == more steep.

    public int maxTimestepsToSim = 200;

    ValueFunction_TensorFlow_StateOnly valFun;
    public RolloutPolicy_ValueFunctionDecayingHorizon(ValueFunction_TensorFlow_StateOnly valFun) {
        super(null);
        this.valFun = valFun;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public float rollout(Node startNode, IGameInternal game) {
        int timestepCounter = 0;
        Node rolloutNode = startNode;
        float accumulatedValue = 0f;

        float previousValue = game.getCurrentState().body.getX(); // TODO Stop hardcoding this as body x and instead
        // use the evaluation function.
        while (!rolloutNode.isFailed() && timestepCounter < maxTimestepsToSim) {
            Action childAction = valFun.getMaximizingAction(rolloutNode); //rolloutNode.uncheckedActions.getRandom();
            rolloutNode = new Node(rolloutNode, childAction, false);
            actionQueue.addAction(childAction);

            while (!actionQueue.isEmpty() && !game.getFailureStatus() && timestepCounter < maxTimestepsToSim) {
                game.step(actionQueue.pollCommand());
                float currentValue = game.getCurrentState().body.getX();
                float multiplier = getKernelMultiplier(timestepCounter / (float) maxTimestepsToSim);

                accumulatedValue += multiplier * (currentValue - previousValue);
                previousValue = currentValue;

                timestepCounter++;
            }

            rolloutNode.setState(game.getCurrentState());
        }

        return accumulatedValue;
    }

    @Override
    public RolloutPolicy getCopy() {
        return new RolloutPolicy_ValueFunctionDecayingHorizon(valFun);
    }

    private float getKernelMultiplier(float timestepsIn) {
        return (float) (-0.5 * Math.tanh(kernelSteepness * (timestepsIn - kernelCenter)) + 0.5);
    }
}
