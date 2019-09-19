package goals.policy_gradient;

import game.qwop.GameQWOP;
import game.action.Action;
import game.action.ActionQueue;
import game.qwop.CommandQWOP;
import game.qwop.StateQWOP;
import org.jetbrains.annotations.NotNull;
import org.nd4j.base.Preconditions;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

// reference https://github.com/simoninithomas/Deep_reinforcement_learning_Course/blob/master/Policy%20Gradients/Cartpole/Cartpole%20REINFORCE%20Monte%20Carlo%20Policy%20Gradients.ipynb
public class PolicyGradientQWOP {

    private final PolicyGradientNetwork net;

    private final GameQWOP game = new GameQWOP();
    private final ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();
    private final List<StateQWOP> states = new ArrayList<>();
    private final List<Action<CommandQWOP>> actions = new ArrayList<>();
    private final List<Float> rewards = new ArrayList<>();

    private List<Action<CommandQWOP>> allowedActions;

    private StateQWOP.Normalizer normalizer;

    private final float dropoutKeep = 1f;

    private PolicyGradientQWOP(@NotNull PolicyGradientNetwork net,
                               @NotNull List<Action<CommandQWOP>> allowedActions) {
        Preconditions.checkArgument(allowedActions.size() == net.outputSize, "Net output size should match the number" +
                " of allowed actions for a softmax net.", allowedActions, net.outputSize);
        this.net = net;
        this.allowedActions = new ArrayList<>(allowedActions);

        try {
            normalizer = new StateQWOP.Normalizer(StateQWOP.Normalizer.NormalizationMethod.STDEV);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //new ArrayList<>(ActionGenerator_UniformNoRepeats.makeDefaultGenerator().getAllPossibleActions());
    }

    public float trainingStep(StateQWOP[] states, List<Action<CommandQWOP>> actions, float[] discountedRewards) {
        float[][] flatStates = new float[states.length][StateQWOP.STATE_SIZE];
        float[][] oneHotActions = new float[actions.size()][allowedActions.size()];

        // Flatten states to episode length x 72
        for (int i = 0; i < states.length; i++) {
            flatStates[i] = normalizer.transform(states[i]);
        }

        // Flatten actions to episode length x num actions
        for (int i = 0; i < actions.size(); i++) {
            oneHotActions[i][allowedActions.indexOf(actions.get(i))] = 1;
        }
        return net.trainingStep(flatStates, oneHotActions, discountedRewards, dropoutKeep, 1);
    }

    private void runEpisode() {
        game.resetGame();
        actionQueue.clearAll();
        actionQueue.addAction(new Action<>(7, CommandQWOP.WO));
        states.clear();
        actions.clear();
        rewards.clear();
        StateQWOP currentState;
        StateQWOP prevState = GameQWOP.getInitialState();

        while (!game.isFailed() && game.getTimestepsThisGame() < 3000) {
            if (actionQueue.isEmpty()) {
                currentState = game.getCurrentState();

                int bestIdx =
                        net.policyOnDistribution(new StateQWOP(normalizer.transform(currentState), false));
                Action<CommandQWOP> nextAction = allowedActions.get(bestIdx);
                states.add(currentState);
                actions.add(nextAction);
                rewards.add(currentState.getCenterX() - prevState.getCenterX()); // Reward is always 1
                // behind.
                actionQueue.addAction(nextAction);
                prevState = currentState;
            }
            game.step(actionQueue.pollCommand());
        }

        // Figure out the discounted rewards.
        states.remove(states.size() - 1);
        actions.remove(actions.size() - 1);
        rewards.remove(0);
        assert states.size() == actions.size() && states.size() == rewards.size();

        if (states.size() < 2)
            return; // TODO the case where the first command fails it. or with 1 stdev becomes 0.


        StateQWOP[] stateArray = states.toArray(new StateQWOP[0]);
        float[] rewardsFlat = new float[rewards.size()];
        int idx = 0;
        for (Float f : rewards) {
            rewardsFlat[idx++] = f;
        }
        float[] discounted = PolicyGradientNetwork.discountRewards(rewardsFlat, 0.99f);
        float loss = trainingStep(stateArray, actions, discounted);
        System.out.println("Distance: " + stateArray[stateArray.length - 1].getCenterX() / 10 + " Loss: " + loss);
    }

    public static void main(String[] args) throws FileNotFoundException {

        List<Action<CommandQWOP>> allowedActions = new ArrayList<>();
        allowedActions.add(new Action<>(1, CommandQWOP.QP));
        allowedActions.add(new Action<>(1, CommandQWOP.WO));
        allowedActions.add(new Action<>(1, CommandQWOP.NONE));

        List<Integer> hiddenLayerSizes = new ArrayList<>();
        hiddenLayerSizes.add(32);
        hiddenLayerSizes.add(16);
        int numberOfOutputs = allowedActions.size();

        List<String> addedArgs = new ArrayList<>();
        addedArgs.add("-lr");
        addedArgs.add("1e-5 ");
        PolicyGradientNetwork net = PolicyGradientNetwork.makeNewNetwork("src/main/resources/tflow_models/tmp.pb",
                new GameQWOP(), hiddenLayerSizes, numberOfOutputs, addedArgs, true);

        PolicyGradientQWOP policy = new PolicyGradientQWOP(net, allowedActions);
        for (int i = 0; i < 10000000; i++) {
            policy.runEpisode();
        }
    }
}
