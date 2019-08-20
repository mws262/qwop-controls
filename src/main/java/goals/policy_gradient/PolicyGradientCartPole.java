package goals.policy_gradient;


import game.cartpole.CartPole;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PolicyGradientCartPole {

    private final PolicyGradientNetwork net;

    private final CartPole cartPole = new CartPole();

    private final List<float[]> states = new ArrayList<>();
    private final List<Integer> actions = new ArrayList<>();
    private final List<Float> rewards = new ArrayList<>();

    private final Random random = new Random();

    PolicyGradientCartPole(PolicyGradientNetwork net) {
        this.net = net;
        cartPole.connect(true);
        random.setSeed(1);
    }

    public float[] evaluate(float[] state) {
        return net.evaluateActionDistribution(state);
    }

    public boolean playGame() {
        cartPole.reset();
        states.clear();
        actions.clear();
        rewards.clear();

        int duration = 0;
        while (!cartPole.isDone()) {
            float[] currentState = cartPole.getCurrentState();

            int actionIdx = net.policyOnDistribution(currentState);
            boolean success = cartPole.step(actionIdx);
            if (!success)
                return success;
            actions.add(actionIdx);
            states.add(currentState);
            duration++;
            rewards.add((float) cartPole.getLastReward() -  Math.abs(currentState[0]) * 0.2f); //TODO temp put a
            // penalty on x offset.
        }

        float[][] flatStates = new float[states.size()][CartPole.STATE_SIZE];
        float[][] oneHotActions = new float[states.size()][CartPole.ACTIONSPACE_SIZE];
        float[] rewardsFlat = new float[rewards.size()];

        for (int i = 0; i < flatStates.length; i++) {
            flatStates[i] = states.get(i);
            oneHotActions[i][actions.get(i)] = 1;
        }

        int idx = 0;
        for (Float f : rewards) {
            rewardsFlat[idx++] = f;
        }
        float[] discounted = PolicyGradientNetwork.discountRewards(rewardsFlat, 0.95f);

        float loss = net.trainingStep(flatStates, oneHotActions, discounted, 1);
        System.out.println("Duration: " + duration + " Loss: " + loss);
        return true;
    }

    public static void main(String[] args) throws FileNotFoundException {
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(CartPole.STATE_SIZE);
        layerSizes.add(10);
        layerSizes.add(2);
        layerSizes.add(CartPole.ACTIONSPACE_SIZE);

        List<String> addedArgs = new ArrayList<>();
        addedArgs.add("-lr");
        addedArgs.add("1e-2 ");
        addedArgs.add("-a");
        addedArgs.add("relu");
        PolicyGradientNetwork net = PolicyGradientNetwork.makeNewNetwork("src/main/resources/tflow_models/cpole.pb",
                layerSizes, addedArgs, true);

        PolicyGradientCartPole policy = new PolicyGradientCartPole(net);
        for (int i = 0; i < 10000000; i++) {
            boolean success = policy.playGame();
            if (!success) {
                break;
            }
        }
    }
}
