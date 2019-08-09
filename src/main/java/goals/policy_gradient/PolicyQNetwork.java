package goals.policy_gradient;

import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class PolicyQNetwork extends SoftmaxPolicyNetwork {

    /**
     * Create a new network. Use the factory methods externally.
     *
     * @param graphDefinition .pb file containing the tflow model.
     * @param useTensorboard  Whether or not to log information for Tensorboard. Must call <code>tensorboard
     *                        --logdir ./logs/</code> to view. May use a lot of disk space.
     * @throws FileNotFoundException Given graph definition file could not be found.
     */
    PolicyQNetwork(File graphDefinition, boolean useTensorboard) throws FileNotFoundException {
        super(graphDefinition, useTensorboard);
    }


    public float trainingStep(float[][] flatStates, float[][] oneHotActions, float[] targetQ, int steps) {

        Tensor<Float> targetQTensor = Tensors.create(targetQ);
        Session.Runner sess = session.runner().feed("scalar_target", targetQTensor);

        float loss = trainingStep(sess, flatStates, oneHotActions, steps);
        targetQTensor.close();
        return loss;
    }



    public static PolicyQNetwork makeNewNetwork(String graphFileName, List<Integer> layerSizes,
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
        additionalArgs.add("identity");
        additionalArgs.add("--loss");
        additionalArgs.add("qlearn");
        return new PolicyQNetwork(makeGraphFile(graphFileName, layerSizes, additionalArgs), useTensorboard);
    }

    public static PolicyQNetwork makeNewNetwork(String graphName, List<Integer> layerSizes,
                                                       boolean useTensorboard) throws FileNotFoundException {
        return makeNewNetwork(graphName, layerSizes, new ArrayList<>(), useTensorboard);
    }
}
