package goals.policy_gradient;

import com.google.common.primitives.Floats;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// See https://github.com/simoninithomas/Deep_reinforcement_learning_Course/blob/master/Deep%20Q%20Learning/Space
// %20Invaders/DQN%20Atari%20Space%20Invaders.ipynb
public class PolicyQCartPole {

    private final CartPole cartPole = new CartPole();
    private final Random random = new Random();

    // Parameters for picking whether to follow the policy or do something random.
    private final float exploreStart = 1.0f;
    private final float exploreStop = 0.005f;
    private final float decayRate = 1e-5f;

    private float decayStep = 0f;

    private final int batchSize = 20;
    private final float gamma = 0.95f;
    private final List<Timestep> timesteps = new ArrayList<>();

    PolicyQNetwork net;
    private float exploreProbability;

    PolicyQCartPole(PolicyQNetwork net) {
        this.net = net;
        cartPole.connect(true);
        random.setSeed(1);
    }

    // Returns the first ts in a game. Is a forward-linked list.
    public void playGame() {
        cartPole.reset();
        boolean done = false;

        Timestep firstTs = new Timestep();
        Timestep prevTs = null;
        Timestep currentTs = firstTs;
        int total = 0;
        while (!done) {
            timesteps.add(currentTs);

            currentTs.state = CartPole.toFloatArray(cartPole.getCurrentState());
            currentTs.action = predictAction(currentTs.state);
            cartPole.step(currentTs.action);
            currentTs.reward = (float) cartPole.getLastReward();
            done = cartPole.isDone();

            if (prevTs != null) {
                prevTs.nextTs = currentTs;
            }
            prevTs = currentTs;
            currentTs = new Timestep();

            decayStep++; // TODO should this be here.
            total++;
        }
        prevTs.reward = -prevTs.reward; // TMP penalty for last ts.
        System.out.print("ts: " + total + ", ");
    }

    public void justEvaluate() {
        cartPole.reset();
        int ts = 0;
        while (!cartPole.isDone()) {
            float[] state = CartPole.toFloatArray(cartPole.getCurrentState());
            int action = net.policyGreedy(state);
            cartPole.step(action);
            ts++;
            System.out.print(action);
        }
        System.out.println("Greedy evaluation went for: " + ts);
    }

    public void train() {

        // Play a new game and add it to the list.
        playGame();

        // Get a random subset of all games played.
        Collections.shuffle(timesteps);
        List<Timestep> batch = timesteps.subList(timesteps.size() - Math.min(timesteps.size(),
                batchSize), timesteps.size());

        float[] qTarget = new float[batch.size()];
        float[][] states = new float[batch.size()][CartPole.STATE_SIZE];
        float[][] oneHotActions = new float[batch.size()][CartPole.ACTIONSPACE_SIZE];
        int idx = 0;
        for (Timestep ts : batch) {
            if (ts.nextTs != null) {
                qTarget[idx] = ts.reward + gamma * Floats.max(net.evaluateActionDistribution(ts.nextTs.state));
            } else {
                qTarget[idx] = ts.reward; // Only reward is from the current state since this is terminal.
            }
            states[idx] = ts.state;
            oneHotActions[idx][ts.action] = 1;
            idx++;
        }

        float loss = net.trainingStep(states, oneHotActions, qTarget, batchSize);
        System.out.println(loss + ", " + exploreProbability);
    }

    /**
     * Select an action either randomly or from the policy.
     * @param state
     * @return
     */
    public int predictAction(float[] state) {
        float r = random.nextFloat();
        exploreProbability = (float) (exploreStop + (exploreStart - exploreStop) * Math.exp(-decayRate * decayStep));

        int actionSelection;
        if (exploreProbability > r) {
            actionSelection = random.nextInt(net.outputSize);
        } else {
            actionSelection = net.policyGreedy(state);
        }
        return actionSelection;
    }

    // Includes an intial state, an action taken from that state, a reward for the transition, and the data for the
    // next state arrived at.
    private class Timestep {

        public Timestep nextTs;
        public float[] state;
        public float reward;
        public int action;
    }

    public static void main(String[] args) throws FileNotFoundException {
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(CartPole.STATE_SIZE);
        layerSizes.add(4);
        layerSizes.add(2);
        layerSizes.add(CartPole.ACTIONSPACE_SIZE);

        List<String> addedArgs = new ArrayList<>();
        addedArgs.add("-lr");
        addedArgs.add("1e-3");
        addedArgs.add("-a");
        addedArgs.add("relu");
        PolicyQNetwork net = PolicyQNetwork.makeNewNetwork("src/main/resources/tflow_models/cpole.pb",
                layerSizes, addedArgs, true);

        PolicyQCartPole policy = new PolicyQCartPole(net);
        for (int i = 0; i < 10000000; i++) {
            System.out.print("epi: " + i);
            policy.train();
            if (i % 10 == 0) {
                System.out.println("Greedy evaluation coming up!");
                policy.justEvaluate();
            }
        }
    }
}
