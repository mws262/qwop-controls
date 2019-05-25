package samplers.rollout;

import actions.Action;
import actions.IActionGenerator;
import evaluators.EvaluationFunction_Distance;
import game.IGameInternal;
import tree.NodeQWOPExplorableBase;

public class RolloutPolicy_RandomDecayingHorizon extends RolloutPolicy {

    // Decaying horizon kernel parameters.
    // Kernel is an s-curve meant to be evaluated from 0 to 1 and producing values from 0 to 1.
    private static final float kernelCenter = 0.5f; // Where the center of the s lies. Greater values shift the drop
    // til later, smaller values make the drop occur earlier.
    private static final float kernelSteepness = 5; // Steepness of the drop. Higher values == more steep.

    public int maxTimestepsToSim = 200;

    private final IActionGenerator actionGenerator = getRolloutActionGenerator();

    public RolloutPolicy_RandomDecayingHorizon() {
        super(new EvaluationFunction_Distance());
    }

    @SuppressWarnings("Duplicates")
    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        int timestepCounter = 0;
        NodeQWOPExplorableBase<?> rolloutNode = startNode;
        float accumulatedValue = 0f;

        float previousValue = game.getCurrentState().body.getX(); // TODO Stop hardcoding this as body x and instead
        // use the evaluation function.
        while (!rolloutNode.getState().isFailed() && timestepCounter < maxTimestepsToSim) {
            Action childAction = rolloutNode.getUntriedActionRandom();
            actionQueue.addAction(childAction);

            while (!actionQueue.isEmpty() && !game.getFailureStatus() && timestepCounter < maxTimestepsToSim) {
                game.step(actionQueue.pollCommand());
                float currentValue = game.getCurrentState().body.getX();
                float multiplier = getKernelMultiplier(timestepCounter / (float) maxTimestepsToSim);

                accumulatedValue += multiplier * (currentValue - previousValue);
                previousValue = currentValue;

                timestepCounter++;
            }
            rolloutNode = rolloutNode.addBackwardsLinkedChild(childAction, game.getCurrentState(), actionGenerator);
        }

        return accumulatedValue;
    }

    @Override
    public RolloutPolicy getCopy() {
        return new RolloutPolicy_RandomDecayingHorizon();
    }

    private float getKernelMultiplier(float timestepsIn) {
        return (float) (-0.5 * Math.tanh(kernelSteepness * (timestepsIn - kernelCenter)) + 0.5);
    }
}
