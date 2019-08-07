package goals.policy_gradient;

import org.jcodec.common.Preconditions;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;
import tflowtools.TrainableNetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates a Tensorflow network that is useful for doing policy gradient training. For evaluation, state values go
 * in, and a probability distribution for actions comes out (i.e. one number per discrete action; must sum to 1). For
 * training, states, one-hot actions, and discounted rewards go in.
 *
 * My intuition for this approach:
 * Basically a typical classification network. Based on state input, "classify" which action belongs with it. Except,
 * when training, some of the examples are really bad. But those contain information also: what not to do. So, when
 * training, the cross-entropy loss gets scaled according to the reward. Better-than-average rewards increase the
 * chance of this state-to-action "classification" and vice-versa.
 *
 * <a href="https://github.com/simoninithomas/Deep_reinforcement_learning_Course/blob/master/Policy%20Gradients/Cartpole/Cartpole%20REINFORCE%20Monte%20Carlo%20Policy%20Gradients.ipynb">This guide</a>
 * was used as a reference.
 */
public class PolicyGradientNetwork extends TrainableNetwork {

    final Random random = new Random(1);

    /**
     * Create a new network. Use the factory methods externally.
     * @param graphDefinition .pb file containing the tflow model.
     * @param useTensorboard Whether or not to log information for Tensorboard. Must call <code>tensorboard
     *                       --logdir ./logs/</code> to view. May use a lot of disk space.
     * @throws FileNotFoundException Given graph definition file could not be found.
     */
    private PolicyGradientNetwork(File graphDefinition, boolean useTensorboard) throws FileNotFoundException {
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

    public float trainingStep(float[][] flatStates, float[][] oneHotActions, float[] discountedRewards, int steps) {

        Tensor<Float> rewardTensor = Tensors.create(discountedRewards);
        Session.Runner sess = session.runner().feed("discounted_episode_rewards", rewardTensor);

        float loss = trainingStep(sess, flatStates, oneHotActions, steps);
        rewardTensor.close();
        return loss;
    }

    public static float[] discountRewards(float[] rewards, float discountRate) {
        float cumulative = 0f;
        float mean = 0;
        float[] discounted = new float[rewards.length];

        for (int i = discounted.length - 1; i >= 0; i--) {
            cumulative = cumulative * discountRate + rewards[i];
            discounted[i] = cumulative;
            mean += cumulative;
        }

        mean /= discounted.length;
        float stdev = 0f;
        for (float reward : discounted) {
            stdev += (reward - mean) * (reward - mean);
        }
        stdev /= (discounted.length); // TODO n or n - 1
        stdev = (float) Math.sqrt(stdev);

        for (int i = 0; i < discounted.length; i++) {
            discounted[i] -= mean;
            discounted[i] /= stdev;
        }
        return discounted;
    }

    public static PolicyGradientNetwork makeNewNetwork(String graphFileName, List<Integer> layerSizes,
                                                       List<String> additionalArgs, boolean useTensorboard) throws FileNotFoundException {
        if (additionalArgs.contains("-ao") || additionalArgs.contains("--activationsout")) {
            throw new IllegalArgumentException("Additional network creation arguments should not include the output " +
                    "activations. These are automatically set to softmax.");
        }
        additionalArgs.add("-ao");
        additionalArgs.add("softmax");
        return new PolicyGradientNetwork(makeGraphFile(graphFileName, layerSizes, additionalArgs), useTensorboard);
    }

    public static PolicyGradientNetwork makeNewNetwork(String graphName, List<Integer> layerSizes,
                                                       boolean useTensorboard) throws FileNotFoundException {
        return makeNewNetwork(graphName, layerSizes, new ArrayList<>(), useTensorboard);
    }
}
