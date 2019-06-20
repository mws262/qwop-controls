package tree.sampler.rollout;

import game.IGameInternal;
import game.action.Action;
import game.state.IState;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.IEvaluationFunction;

public abstract class RolloutPolicy_DecayingHorizon extends RolloutPolicy {

    // Decaying horizon kernel parameters.
    // Kernel is an s-curve meant to be evaluated from 0 to 1 and producing values from 0 to 1.
    public static float kernelCenter = 0.5f; // Where the center of the s lies. Greater values shift the drop
    // til later, smaller values make the drop occur earlier.
    public static float kernelSteepness = 5; // Steepness of the drop. Higher values == more steep.

    public static int maxTimestepsToSim = 200;

    RolloutPolicy_DecayingHorizon(IEvaluationFunction evaluationFunction) {
        super(evaluationFunction);
    }

    @Override
    public float rollout(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        assert startNode.getState().equals(game.getCurrentState());

        // Duplicate the start node, but use the rollout-specific action generator.
        NodeQWOPExplorableBase<?> rolloutNode = startNode.addBackwardsLinkedChild(startNode.getAction(),
                startNode.getState(), rolloutActionGenerator);
        float accumulatedValue = 0f;
        int timestepCounter = 0;

        float previousValue = evaluationFunction.getValue(startNode);
        while (!rolloutNode.getState().isFailed() && timestepCounter < maxTimestepsToSim) {
            Action childAction = getNextAction(rolloutNode);
            actionQueue.addAction(childAction);

            NodeQWOPBase<?> intermediateNode = rolloutNode;
            while (!actionQueue.isEmpty() && !game.getFailureStatus() && timestepCounter < maxTimestepsToSim) {
                game.step(actionQueue.pollCommand());

                intermediateNode = intermediateNode.addBackwardsLinkedChild(childAction, game.getCurrentState());

                float currentValue = evaluationFunction.getValue(intermediateNode);
                float multiplier = getKernelMultiplier(timestepCounter / (float) (maxTimestepsToSim - 1));

                accumulatedValue += multiplier * (currentValue - previousValue); // Incremental distance travelled.
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

    float getKernelMultiplier(float normalizedTimesteps) {
        assert normalizedTimesteps <= 1f;
        assert normalizedTimesteps >= 0f;
        return (float) (-0.5 * Math.tanh(kernelSteepness * (normalizedTimesteps - kernelCenter)) + 0.5);
    }
}
