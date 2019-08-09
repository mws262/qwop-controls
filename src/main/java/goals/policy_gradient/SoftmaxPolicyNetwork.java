package goals.policy_gradient;

import com.google.common.base.Preconditions;
import tflowtools.TrainableNetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;

public abstract class SoftmaxPolicyNetwork extends TrainableNetwork {

    final Random random = new Random(1);

    /**
     * Create a new network. Use the factory methods externally.
     * @param graphDefinition .pb file containing the tflow model.
     * @param useTensorboard Whether or not to log information for Tensorboard. Must call <code>tensorboard
     *                       --logdir ./logs/</code> to view. May use a lot of disk space.
     * @throws FileNotFoundException Given graph definition file could not be found.
     */
    SoftmaxPolicyNetwork(File graphDefinition, boolean useTensorboard) throws FileNotFoundException {
        super(graphDefinition, useTensorboard);
    }


    /**
     * Given a state vector input, get the policy's distribution of which actions to take. For a greedy controller,
     * just take the action with the index of the max element of the returned distribution.
     * @param state State vector.
     * @return
     */
    public float[] evaluateActionDistribution(float[] state) {

        Preconditions.checkArgument(state.length == getLayerSizes()[0], "Input state should match the dimension of " +
                "the input layer of the network.", state.length, getLayerSizes()[0]);

        float[][] input = new float[][] {state};
        float[][] output = evaluateInput(input);
        return output[0];
    }

    /**
     * Get the action index with the highest value on the distribution produced by evaluating the net.
     * @param state Current state vector.
     * @return Index of the discrete action that is most likely correct (according to the policy).
     */
    public int policyGreedy(float[] state) {
        float[] distribution = evaluateActionDistribution(state);

        float bestVal = -Float.MAX_VALUE;
        int bestIdx = 0;
        for (int i = 0; i < distribution.length; i++) {
            if (distribution[i] > bestVal) {
                bestVal = distribution[i];
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    /**
     * Get an action index randomly selected on the distribution returned by evaluating the policy network.
     * @param state Current state vector.
     * @return Index of a discrete action.
     */
    public int policyOnDistribution(float[] state) {
        float[] distribution = evaluateActionDistribution(state);
        float[] cumulativeDistribution = new float[distribution.length];
        cumulativeDistribution[0] = distribution[0];
        for (int i = 1; i < distribution.length - 1; i++) {
            cumulativeDistribution[i] = cumulativeDistribution[i - 1] + distribution[i];
        }
        cumulativeDistribution[distribution.length - 1] = 1; // Sometimes due to roundoff, the last value ends up
        // slightly less than 1, and I don't want this to screw up selection once every million game executions lol.

        float selection = random.nextFloat();

        int bestIdx = 0;
        for (int i = 0; i < cumulativeDistribution.length; i++) {
            if (selection <= cumulativeDistribution[i]) {
                bestIdx = i;
                break;
            }
        }
        return bestIdx;
    }
}
