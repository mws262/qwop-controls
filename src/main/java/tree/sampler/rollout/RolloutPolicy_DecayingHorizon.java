package tree.sampler.rollout;

import game.actions.Action;
import evaluators.IEvaluationFunction;
import game.IGameInternal;
import game.state.IState;
import tree.node.NodeQWOPExplorableBase;

public abstract class RolloutPolicy_DecayingHorizon extends RolloutPolicy {

    // Decaying horizon kernel parameters.
    // Kernel is an s-curve meant to be evaluated from 0 to 1 and producing values from 0 to 1.
    private static final float kernelCenter = 0.5f; // Where the center of the s lies. Greater values shift the drop
    // til later, smaller values make the drop occur earlier.
    private static final float kernelSteepness = 5; // Steepness of the drop. Higher values == more steep.

    public int maxTimestepsToSim = 200;

    RolloutPolicy_DecayingHorizon(IEvaluationFunction evaluationFunction) {
        super(evaluationFunction);
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        int timestepCounter = 0;
        NodeQWOPExplorableBase<?> rolloutNode = startNode;
        float accumulatedValue = 0f;

        float previousValue = game.getCurrentState().getCenterX();
        // TODO Stop hardcoding
        // this as
        // body x
        // and instead
        // use the evaluation function.
        while (!rolloutNode.getState().isFailed() && timestepCounter < maxTimestepsToSim) {
            Action childAction = getNextAction(rolloutNode);
            actionQueue.addAction(childAction);

            while (!actionQueue.isEmpty() && !game.getFailureStatus() && timestepCounter < maxTimestepsToSim) {
                game.step(actionQueue.pollCommand());
                float currentValue = game.getCurrentState().getCenterX();
                float multiplier = getKernelMultiplier(timestepCounter / (float) maxTimestepsToSim);

                accumulatedValue += multiplier * (currentValue - previousValue);
                previousValue = currentValue;

                timestepCounter++;
            }
            rolloutNode = addNextRolloutNode(rolloutNode, childAction, game.getCurrentState());
        }
        return calculateFinalValue(accumulatedValue, startNode);
    }

    abstract float calculateFinalValue(float accumulatedValue, NodeQWOPExplorableBase<?> startNode);

    abstract NodeQWOPExplorableBase<?> addNextRolloutNode(NodeQWOPExplorableBase<?> currentNode, Action action,
                                                          IState state);

    abstract Action getNextAction(NodeQWOPExplorableBase<?> currentNode);

    float getKernelMultiplier(float timestepsIn) {
        return (float) (-0.5 * Math.tanh(kernelSteepness * (timestepsIn - kernelCenter)) + 0.5);
    }
}
