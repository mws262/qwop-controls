package goals.q_learning;

import game.qwop.GameQWOP;
import game.action.Action;
import game.action.ActionQueue;
import game.qwop.CommandQWOP;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PolicyQQWOP {

    private final GameQWOP game = new GameQWOP();
    private PolicyQNetwork net;
    private List<Action<CommandQWOP>> allowedActions;
    private ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();

    private PolicyQQWOP(PolicyQNetwork net) {
        this.net = net;
        allowedActions = new ArrayList<>();
        allowedActions.add(new Action<>(1, CommandQWOP.QP));
        allowedActions.add(new Action<>(1, CommandQWOP.WO));
        allowedActions.add(new Action<>(1, CommandQWOP.NONE));
    }

    // Returns the first ts in a game. Is a forward-linked list.
    private float[] playGame() {
        game.resetGame();
        boolean done = false;

        PolicyQNetwork.Timestep firstTs = new PolicyQNetwork.Timestep();
        PolicyQNetwork.Timestep prevTs = null;
        PolicyQNetwork.Timestep currentTs = firstTs;
        float prevX = GameQWOP.getInitialState().getCenterX();
        int total = 0;
        float[] oneHotCommand = null;
        while (!done && total < 2000) {

            net.addTimestep(currentTs);

            currentTs.state = game.getCurrentState();
            if (actionQueue.isEmpty()) {
                int actionIdx = net.policyExplore(currentTs.state);
                actionQueue.addAction(allowedActions.get(actionIdx));
                oneHotCommand = new float[allowedActions.size()];
                oneHotCommand[actionIdx] = 1;
            }

            CommandQWOP command = actionQueue.pollCommand();
            currentTs.commandOneHot = Objects.requireNonNull(oneHotCommand);

            game.step(command);
            float currX = game.getCurrentState().getCenterX();
            currentTs.reward = currX - prevX;
            prevX = currX;
            done = game.isFailed();

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
//        game.resetGame();
//        int ts = 0;
//        while (!cartPole.isDone()) {
//            float[] state = CartPole.toFloatArray(cartPole.getCurrentState());
//            int command = net.policyGreedy(state);
//            cartPole.step(command);
//            ts++;
//            System.out.print(command);
//        }-
//        System.out.println("Greedy evaluation went for: " + ts);
//    }

    public static void main(String[] args) throws FileNotFoundException {
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(GameQWOP.STATE_SIZE);
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
