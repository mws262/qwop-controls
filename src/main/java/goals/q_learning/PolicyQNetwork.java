package goals.q_learning;

import com.google.common.primitives.Floats;
import game.state.IState;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;
import tflowtools.SoftmaxPolicyNetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PolicyQNetwork extends SoftmaxPolicyNetwork {

    // Parameters for picking whether to follow the policy or do something random.
    public float exploreStart = 1.0f;
    public float exploreStop = 0.005f;
    public float decayRate = 1e-5f;
    private float decayStep = 0f;
    private float exploreProbability;

    public int batchSize = 50;
    public float gamma = 0.92f;

    private final List<Timestep> timesteps = new ArrayList<>();

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

    /**
     * Select an command either randomly or greedily from the policy.
     * @param state
     * @return
     */
    int policyExplore(IState state) {
        float r = random.nextFloat();
        exploreProbability = (float) (exploreStop + (exploreStart - exploreStop) * Math.exp(-decayRate * decayStep));

        int actionSelection;
        if (exploreProbability > r) {
            actionSelection = random.nextInt(outputSize);
        } else {
            actionSelection = policyGreedy(state);
        }
        return actionSelection;
    }

    void incrementDecay() {
        decayStep++;
    }

    void addTimestep(Timestep timestep) {
        timesteps.add(timestep);
    }

    float train(int iterations, float keepProbability) {
        // Get a random subset of all games played. People call this experience replay to be all fancy-like.
        Collections.shuffle(timesteps);
        List<Timestep> batch = timesteps.subList(timesteps.size() - Math.min(timesteps.size(),
                batchSize), timesteps.size());

        float[] qTarget = new float[batch.size()];
        float[][] states = new float[batch.size()][inputSize];
        float[][] oneHotActions = new float[batch.size()][outputSize];
        int idx = 0;
        for (Timestep ts : batch) {
            if (ts.nextTs != null) {
                qTarget[idx] = ts.reward + gamma * Floats.max(evaluateActionDistribution(ts.nextTs.state));
            } else {
                qTarget[idx] = ts.reward; // Only reward is from the current state since this is terminal.
            }
            states[idx] = ts.state.flattenState();
            oneHotActions[idx] = ts.commandOneHot;
            idx++;
        }

        Tensor<Float> targetQTensor = Tensors.create(qTarget);
        float loss = 0;
        for (int i = 0; i < iterations; i++) {
            Session.Runner sess = session.runner().feed("scalar_target", targetQTensor);
            loss += trainingStep(sess, states, oneHotActions, keepProbability, 1);
        }
        targetQTensor.close();
        return loss / (float) iterations;
    }

    public static PolicyQNetwork makeNewNetwork(String graphFileName, List<Integer> layerSizes,
                                                       List<String> additionalArgs, boolean useTensorboard) throws FileNotFoundException {
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

    public static PolicyQNetwork makeNewNetwork(String graphName, List<Integer> layerSizes, boolean useTensorboard) throws FileNotFoundException {
        return makeNewNetwork(graphName, layerSizes, new ArrayList<>(), useTensorboard);
    }

    // Includes an intial state, an command taken from that state, a reward for the transition, and the data for the
    // next state arrived at.
    public static class Timestep {
        public Timestep nextTs;
        public IState state;
        public float reward;
        public float[] commandOneHot;
    }
}
