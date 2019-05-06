package samplers.rollout;

import actions.Action;
import game.IGame;
import tree.Node;
import tree.NodeRollout;

public class RolloutPolicy_RandomDecayingHorizon extends RolloutPolicy {

    // Decaying horizon kernel parameters.
    // Kernel is an s-curve meant to be evaluated from 0 to 1 and producing values from 0 to 1.
    private static final float kernelCenter = 0.5f; // Where the center of the s lies. Greater values shift the drop
    // til later, smaller values make the drop occur earlier.
    private static final float kernelSteepness = 5; // Steepness of the drop. Higher values == more steep.

    public int maxTimestepsToSim = 150;

    public RolloutPolicy_RandomDecayingHorizon() {
        super(null);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public float rollout(Node startNode, IGame game) {
        int timestepCounter = 0;
        Node rolloutNode = startNode;
        float accumulatedValue = 0f;

        float previousValue = game.getCurrentState().torso.getX(); // TODO Stop hardcoding this as torso x and instead
        // use the evaluation function.
        while (!rolloutNode.isFailed() && timestepCounter < maxTimestepsToSim) {
            Action childAction = rolloutNode.uncheckedActions.getRandom();
            rolloutNode = new NodeRollout(rolloutNode, childAction);
            actionQueue.addAction(childAction);

            while (!actionQueue.isEmpty() && !game.getFailureStatus() && timestepCounter < maxTimestepsToSim) {
                game.step(actionQueue.pollCommand());
                float currentValue = game.getCurrentState().torso.getX();
                float multiplier = getKernelMultiplier(timestepCounter / (float) maxTimestepsToSim);

                accumulatedValue += multiplier * (currentValue - previousValue);
                previousValue = currentValue;

                timestepCounter++;
            }

            rolloutNode.setState(game.getCurrentState());
        }

        return accumulatedValue / (float) maxTimestepsToSim * 200f;
    }

    @Override
    public RolloutPolicy getCopy() {
        return new RolloutPolicy_RandomDecayingHorizon();
    }

    private float getKernelMultiplier(float timestepsIn) {
        return (float) (-0.5 * Math.tanh(kernelSteepness * (timestepsIn - kernelCenter)) + 0.5);
    }
}
