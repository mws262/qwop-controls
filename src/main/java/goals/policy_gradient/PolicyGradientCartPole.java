package goals.policy_gradient;

import game.action.Action;
import game.state.State;
import org.jblas.util.Random;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class PolicyGradientCartPole {

    private final PolicyGradientNetwork net;

    private final CartPole cartPole = new CartPole();

    private final List<float[]> states = new ArrayList<>();
    private final List<Integer> actions = new ArrayList<>();
    private final List<Float> rewards = new ArrayList<>();

    PolicyGradientCartPole(PolicyGradientNetwork net) {
        this.net = net;
        cartPole.connect(false);
    }

    public float[] evaluate(float[] state) {
        return net.evaluate(state);
    }

    public void playGame() {
        cartPole.reset();
        states.clear();
        actions.clear();
        rewards.clear();
        int duration = 0;
        while (!cartPole.isDone()) {
            float[] currentState = toFloatArray(cartPole.getCurrentState());
            float[] predictionDist = evaluate(currentState); // These should add to 1.
            float[] cumDist = new float[predictionDist.length];
            cumDist[0] = predictionDist[0];
            for (int i = 1; i < predictionDist.length; i++) {
                cumDist[i] = cumDist[i - 1] + predictionDist[i];
            }
            float selection = Random.nextFloat();

            int bestIdx = 0;
            for (int i = 0; i < cumDist.length; i++) {
                if (selection <= cumDist[i]) {
                    bestIdx = i;
                    break;
                }
            }
            actions.add(bestIdx);
            states.add(currentState);
            cartPole.step(bestIdx);
            duration++;
            rewards.add((float) cartPole.getLastReward());
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

        float loss = net.trainingStep(flatStates, oneHotActions, discounted);
        System.out.println("Duration: " + duration + " Loss: " + loss);
    }

    private float[] toFloatArray(double[] in) {
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (float) in[i];
        }
        return out;
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
            policy.playGame();
        }
    }
}
