package goals.policy_gradient;

import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

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
public class PolicyGradientNetwork extends SoftmaxPolicyNetwork {


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

    public float trainingStep(float[][] flatStates, float[][] oneHotActions, float[] discountedRewards, int steps) {

        Tensor<Float> rewardTensor = Tensors.create(discountedRewards);
        Session.Runner sess = session.runner().feed("scalar_target", rewardTensor);

        float loss = trainingStep(sess, flatStates, oneHotActions, steps);
        rewardTensor.close();
        return loss;
    }

    static float[] discountRewards(float[] rewards, float discountRate) {
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
        if (additionalArgs.contains("--loss") || additionalArgs.contains("-ls")) {
            throw new IllegalArgumentException("Additional network creation arguments should not include the loss " +
                    "type. This is defined by being policy gradient.");
        }
        additionalArgs.add("-ao");
        additionalArgs.add("softmax");
        additionalArgs.add("--loss");
        additionalArgs.add("policy_gradient");
        return new PolicyGradientNetwork(makeGraphFile(graphFileName, layerSizes, additionalArgs), useTensorboard);
    }

    public static PolicyGradientNetwork makeNewNetwork(String graphName, List<Integer> layerSizes,
                                                       boolean useTensorboard) throws FileNotFoundException {
        return makeNewNetwork(graphName, layerSizes, new ArrayList<>(), useTensorboard);
    }
}
