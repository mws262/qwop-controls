package goals.policy_gradient;

import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;
import tflowtools.TrainableNetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class PolicyGradientNetwork extends TrainableNetwork {

    PolicyGradientNetwork(File graphDefinition, boolean useTensorboard) throws FileNotFoundException {
        super(graphDefinition, useTensorboard);
    }

    public float[] evaluate(float[] state) {
        float[][] input = new float[][] {state};
        float[][] output;
        try (Tensor<Float> stateTensor = Tensors.create(input)) {

            List<Tensor<?>> out = session.runner().feed("input", stateTensor)
                    .fetch("output").run();
            Tensor<Float> outputTensor = out.get(0).expect(Float.class);

            long[] outputShape = outputTensor.shape();
            output = new float[(int) outputShape[0]][(int) outputShape[1]];
            outputTensor.copyTo(output);

            out.forEach(Tensor::close);
        }
        return output[0];
    }

    public float trainingStep(float[][] flatStates, float[][] oneHotActions, float[] discountedRewards) {

        // Even though each reward is a scalar, we need to pass in as 2D.
        float[][] rewards = new float[discountedRewards.length][1];
        for (int i = 0; i < discountedRewards.length; i++) {
            rewards[i][0] = discountedRewards[i];
        }

        float loss;
        try (Tensor<Float> stateTensor = Tensors.create(flatStates);
             Tensor<Float> actionTensor = Tensors.create(oneHotActions);
             Tensor<Float> rewardTensor = Tensors.create(rewards)) {

            Session.Runner sess = session.runner().feed("input", stateTensor)
                    .feed("output_target", actionTensor)
                    .feed("discounted_episode_rewards", rewardTensor)
                    .addTarget("train")
                    .fetch("loss");

            if (useTensorboard) {
                sess = sess.fetch("summary/summary");
            }
            List<Tensor<?>> out = sess.run();

            loss = out.get(0).expect(Float.class).floatValue();

            if (useTensorboard) {
                toTensorBoardOutput(out.get(1));
            }
            out.forEach(Tensor::close);
        }
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
        additionalArgs.add("-ao");
        additionalArgs.add("softmax");
        return new PolicyGradientNetwork(makeGraphFile(graphFileName, layerSizes, additionalArgs), useTensorboard);
    }

    public static PolicyGradientNetwork makeNewNetwork(String graphName, List<Integer> layerSizes,
                                                       boolean useTensorboard) throws FileNotFoundException {
        return makeNewNetwork(graphName, layerSizes, new ArrayList<>(), useTensorboard);
    }
}
