package goals.policy_gradient;


import game.cartpole.CartPole;
import game.cartpole.CommandCartPole;
import game.state.IState;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class PolicyGradientCartPole {

    private final PolicyGradientNetwork net;

    private final CartPole cartPole = new CartPole();

    private final List<IState> states = new ArrayList<>();
    private final List<CommandCartPole> commands = new ArrayList<>();
    private final List<Float> rewards = new ArrayList<>();

    private PolicyGradientCartPole(PolicyGradientNetwork net) {
        this.net = net;
        cartPole.connect(true);

    }

    private void runEpisodeAndTrain() {
        cartPole.resetGame();
        states.clear();
        commands.clear();
        rewards.clear();

        int duration = 0;
        while (!cartPole.isFailed()) {
            IState currentState = cartPole.getCurrentState();

            int commandIndex = net.policyOnDistribution(currentState);
            CommandCartPole command = CommandCartPole.getByIndex(commandIndex);
            cartPole.step(command);

            commands.add(command);
            states.add(currentState);
            duration++;
            rewards.add(cartPole.getLastReward());
        }

        float[][] flatStates = new float[states.size()][CartPole.STATE_SIZE];
        float[][] oneHotActions = new float[states.size()][CartPole.ACTIONSPACE_SIZE];
        float[] rewardsFlat = new float[rewards.size()];

        for (int i = 0; i < flatStates.length; i++) {
            flatStates[i] = states.get(i).flattenState();
            oneHotActions[i] = commands.get(i).toOneHot();
        }

        int idx = 0;
        for (Float f : rewards) {
            rewardsFlat[idx++] = f;
        }
        float[] discounted = PolicyGradientNetwork.discountRewards(rewardsFlat, 0.95f);

        float loss = net.trainingStep(flatStates, oneHotActions, discounted, 1);
        System.out.println("Duration: " + duration + " Loss: " + loss);
    }

    public static void main(String[] args) throws FileNotFoundException {
        List<Integer> hiddenLayerSizes = new ArrayList<>();
        hiddenLayerSizes.add(10);
        hiddenLayerSizes.add(2);

        List<String> addedArgs = new ArrayList<>();
        addedArgs.add("-lr");
        addedArgs.add("1e-2 ");
        addedArgs.add("-a");
        addedArgs.add("relu");
        PolicyGradientNetwork net = PolicyGradientNetwork.makeNewNetwork(
                "src/main/resources/tflow_models/cpole.pb",
                new CartPole(), hiddenLayerSizes, addedArgs, true);

        PolicyGradientCartPole policy = new PolicyGradientCartPole(net);
        for (int i = 0; i < 10000000; i++) {
            policy.runEpisodeAndTrain();
        }
    }
}
