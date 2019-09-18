package tflowtools;

import com.google.common.base.Preconditions;
import game.state.IState;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;

public abstract class SoftmaxPolicyNetwork extends TrainableNetwork {
    /**
     * RNG for selecting actions on a distribution. Has a fixed seed for repeatability.
     */
    protected final Random random = new Random(1);

    /**
     * Create a new network. Use the factory methods externally.
     * @param graphDefinition .pb file containing the tflow model.
     * @param useTensorboard Whether or not to log information for Tensorboard. Must call <code>tensorboard
     *                       --logdir ./logs/</code> to view. May use a lot of disk space.
     * @throws FileNotFoundException Given graph definition file could not be found.
     */
    public SoftmaxPolicyNetwork(File graphDefinition, boolean useTensorboard) throws FileNotFoundException {
        super(graphDefinition, useTensorboard);

        // Run a quick test to make sure that the provided graph actually puts out a real probability distribution
        // (adds to 1).
        float[][] testInput = new float[1][inputSize];
        for (int i = 0; i < inputSize; i++) {
            testInput[0][i] = i;
        }
        float[][] testOutput = evaluateInput(testInput);
        float cumulativeOutput = 0;
        for (float f : testOutput[0]) {
            cumulativeOutput += f;
        }

        // Constructor runs a quick test to make sure that the provided network puts out a distribution summing to 1.
        // This is the amount of floating point error allowed in this test.
        // TODO doesn't apply to Q nets but is still a good test for actual softmax. Change stuff around.
//        float softmaxTol = 1e-5f;
//        if (Math.abs(1 - Math.abs(cumulativeOutput)) > softmaxTol) {
//            throw new IllegalArgumentException("Given graph file, " + graphDefinition.getName() + ", did not seem to " +
//                    "put out a valid probability distribution. For a test input, the output distribution summed to " + cumulativeOutput);
//        }
    }

    /**
     * Given a state vector input, get the policy's distribution of which actions to take. For a greedy controller,
     * just take the command with the index of the max element of the returned distribution.
     * @param state StateQWOP vector.
     * @return
     */
    public float[] evaluateActionDistribution(IState state) {
        Preconditions.checkArgument(state.getStateSize() == getLayerSizes()[0], "Input state should match the dimension of " +
                "the input layer of the network.", state.getStateSize(), getLayerSizes()[0]);

        float[][] input = new float[][] {state.flattenState()};
        float[][] output = evaluateInput(input);
        return output[0];
    }

    /**
     * Get the command index with the highest value on the distribution produced by evaluating the net.
     * @param state Current state vector.
     * @return Index of the discrete command that is most likely correct (according to the policy).
     */
    public int policyGreedy(IState state) {
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
     * Get an command index randomly selected on the distribution returned by evaluating the policy network.
     * @param state Current state vector.
     * @return Index of a discrete command.
     */
    public int policyOnDistribution(IState state) {
        float[] distribution = evaluateActionDistribution(state);
        float selection = random.nextFloat();

        int selectedIndex = 0;
        float cumulativeDistribution = 0;
        for (int i = 0; i < distribution.length; i++) {
            cumulativeDistribution += distribution[i];
            if (selection <= cumulativeDistribution || i == distribution.length - 1) { // Due to floating point
                // error, sometimes the cumulative is fractionally less than 1. Don't want to miss that case.
                selectedIndex = i;
                break;
            }
        }
        return selectedIndex;
    }
}
