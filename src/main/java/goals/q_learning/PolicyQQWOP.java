package goals.q_learning;

import game.GameUnified;
import game.action.Action;
import game.action.ActionQueue;
import game.action.CommandQWOP;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class PolicyQQWOP {

    private final GameUnified game = new GameUnified();
    private PolicyQNetwork net;
    private List<Action<CommandQWOP>> allowedActions;
    private ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();

    PolicyQQWOP(PolicyQNetwork net) {
        this.net = net;
        allowedActions = new ArrayList<>();
        allowedActions.add(new Action<>(1, CommandQWOP.QP));
        allowedActions.add(new Action<>(1, CommandQWOP.WO));
        allowedActions.add(new Action<>(1, CommandQWOP.NONE));
    }

    // Returns the first ts in a game. Is a forward-linked list.
    public float[] playGame() {
        game.makeNewWorld();
        boolean done = false;

        PolicyQNetwork.Timestep firstTs = new PolicyQNetwork.Timestep();
        PolicyQNetwork.Timestep prevTs = null;
        PolicyQNetwork.Timestep currentTs = firstTs;
        float prevX = GameUnified.getInitialState().getCenterX();
        int total = 0;
        int actionIdx = 0;
        while (!done && total < 2000) {

            net.addTimestep(currentTs);

            currentTs.state = game.getCurrentState().flattenState();
            if (actionQueue.isEmpty()) {
                actionIdx = net.policyExplore(currentTs.state);
                actionQueue.addAction(allowedActions.get(actionIdx));
            }
            currentTs.action = actionIdx;

            game.step(actionQueue.pollCommand());
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
        return new float[] {total, prevX};
       // System.out.print("ts: " + total + ", dist: " + prevX/10f);
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
//        }-
//        System.out.println("Greedy evaluation went for: " + ts);
//    }

    public static void main(String[] args) throws FileNotFoundException {
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(GameUnified.STATE_SIZE);
        layerSizes.add(20);
        layerSizes.add(15);
        layerSizes.add(3);

        List<String> addedArgs = new ArrayList<>();
        addedArgs.add("-lr");
        addedArgs.add("1e-4");
        addedArgs.add("-a");
        addedArgs.add("relu");
        PolicyQNetwork net = PolicyQNetwork.makeNewNetwork("src/main/resources/tflow_models/cpole.pb",
                layerSizes, addedArgs, true);

        PolicyQQWOP policy = new PolicyQQWOP(net);
        float lossAvg = 0;
        float tsAvg = 0;
        float xAvg = 0;
        for (int i = 0; i < 10000000; i++) {
            float[] result = policy.playGame();
            lossAvg += net.train(1);
            tsAvg += result[0];
            xAvg += result[1];
            if (i % 20 == 0) {
                System.out.println("epi: " + i + ", loss: " + lossAvg / 20f + ", ts: " + tsAvg / 20f + ", x: " + xAvg / 20f);
                lossAvg = 0;
                tsAvg = 0;
                xAvg = 0;
            }
        }
    }
}
