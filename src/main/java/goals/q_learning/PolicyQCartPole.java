package goals.q_learning;

import game.cartpole.CartPole;
import game.cartpole.CommandCartPole;
import game.state.IState;
import goals.q_learning.PolicyQNetwork.Timestep;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

// See https://github.com/simoninithomas/Deep_reinforcement_learning_Course/blob/master/Deep%20Q%20Learning/Space
// %20Invaders/DQN%20Atari%20Space%20Invaders.ipynb
public class PolicyQCartPole {

    private final CartPole cartPole = new CartPole();
    private PolicyQNetwork net;

    private PolicyQCartPole(PolicyQNetwork net) {
        this.net = net;
        cartPole.connect(true);
    }

    // Returns the first ts in a game. Is a forward-linked list.
    private void playGame() {
        cartPole.resetGame();
        boolean done = false;

        Timestep firstTs = new Timestep();
        Timestep prevTs = null;
        Timestep currentTs = firstTs;
        int total = 0;
        while (!done) {
            net.addTimestep(currentTs);

            currentTs.state = cartPole.getCurrentState();
            CommandCartPole command = CommandCartPole.getByIndex(net.policyExplore(currentTs.state));
            currentTs.commandOneHot = command.toOneHot();
            cartPole.step(command);
            currentTs.reward = cartPole.getLastReward();
            done = cartPole.isFailed();

            if (prevTs != null) {
                prevTs.nextTs = currentTs;
            }
            prevTs = currentTs;
            currentTs = new Timestep();

            net.incrementDecay();
            total++;
        }
        prevTs.reward = -prevTs.reward; // TMP penalty for last ts.
        System.out.print("ts: " + total + ", ");
    }

    private void justEvaluate() {
        cartPole.resetGame();
        int ts = 0;
        while (!cartPole.isFailed()) {
            IState state = cartPole.getCurrentState();
            int action = net.policyGreedy(state);
            cartPole.step(CommandCartPole.getByIndex(action));
            ts++;
            System.out.print(action);
        }
        System.out.println("Greedy evaluation went for: " + ts);
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

//        PolicyQNetwork net = new PolicyQNetwork(new File("./src/main/resources/tflow_models/cpole.pb"), false);
        PolicyQCartPole policy = new PolicyQCartPole(net);
        float dropoutKeep = 0.9f;
        net.outputSize = 2; // TMP override since there are more sets of layers than we're used to.
        for (int i = 0; i < 10000000; i++) {
            policy.playGame();
            float loss = net.train(1, dropoutKeep);
            System.out.println("epi: " + i + ", loss: " + loss);
            if (i % 10 == 0) {
                System.out.println("Greedy evaluation coming up!");
                policy.justEvaluate();
            }
        }
    }
}
