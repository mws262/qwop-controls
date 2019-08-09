package goals.policy_gradient;

import game.GameUnified;
import game.action.Action;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class PolicyQQWOP {

    private final GameUnified game = new GameUnified();
    PolicyQNetwork net;

    PolicyQQWOP(PolicyQNetwork net) {
        this.net = net;
    }

    // Returns the first ts in a game. Is a forward-linked list.
    public void playGame() {
        game.makeNewWorld();
        boolean done = false;

        PolicyQNetwork.Timestep firstTs = new PolicyQNetwork.Timestep();
        PolicyQNetwork.Timestep prevTs = null;
        PolicyQNetwork.Timestep currentTs = firstTs;
        float prevX = GameUnified.getInitialState().getCenterX();
        int total = 0;
        while (!done) {
            net.addTimestep(currentTs);

            currentTs.state = game.getCurrentState().flattenState();
            currentTs.action = net.policyExplore(currentTs.state);
            game.step(Action.keysToBooleans(Action.Keys.values()[currentTs.action]));
            float currX = game.getCurrentState().getCenterX();
            currentTs.reward = currX - prevX;
            prevX = currX;
            done = game.getFailureStatus();

            if (prevTs != null) {
                prevTs.nextTs = currentTs;
            }
            prevTs = currentTs;
            currentTs = new PolicyQNetwork.Timestep();

            net.incrementDecay();
            total++;
        }
        System.out.print("ts: " + total + ", dist: " + prevX/10f);
    }

//    public void justEvaluate() {
//        game.reset();
//        int ts = 0;
//        while (!cartPole.isDone()) {
//            float[] state = CartPole.toFloatArray(cartPole.getCurrentState());
//            int action = net.policyGreedy(state);
//            cartPole.step(action);
//            ts++;
//            System.out.print(action);
//        }
//        System.out.println("Greedy evaluation went for: " + ts);
//    }

    public static void main(String[] args) throws FileNotFoundException {
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(GameUnified.STATE_SIZE);
        layerSizes.add(80);
        layerSizes.add(20);
        layerSizes.add(Action.Keys.values().length);

        List<String> addedArgs = new ArrayList<>();
        addedArgs.add("-lr");
        addedArgs.add("1e-3");
        addedArgs.add("-a");
        addedArgs.add("relu");
        PolicyQNetwork net = PolicyQNetwork.makeNewNetwork("src/main/resources/tflow_models/cpole.pb",
                layerSizes, addedArgs, true);

        PolicyQQWOP policy = new PolicyQQWOP(net);
        for (int i = 0; i < 10000000; i++) {
            policy.playGame();
            float loss = net.train(1);
            System.out.println("epi: " + i + ", loss: " + loss);
//            if (i % 10 == 0) {
//                System.out.println("Greedy evaluation coming up!");
////                policy.justEvaluate();
//            }
        }
    }
}
