package goals.policy_gradient;

import data.LoadStateStatistics;
import game.GameUnified;
import game.action.Action;
import game.action.ActionQueue;
import game.state.IState;
import game.state.State;
import org.jblas.util.Random;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

// reference https://github.com/simoninithomas/Deep_reinforcement_learning_Course/blob/master/Policy%20Gradients/Cartpole/Cartpole%20REINFORCE%20Monte%20Carlo%20Policy%20Gradients.ipynb
public class PolicyGradientQWOP {

    private final PolicyGradientNetwork net;

    private final GameUnified game = new GameUnified();
    private final ActionQueue actionQueue = new ActionQueue();
    private final List<State> states = new ArrayList<>();
    private final List<Action> actions = new ArrayList<>();
    private final List<Float> rewards = new ArrayList<>();

    LoadStateStatistics.StateStatistics stateStats;
    {
        try {
            stateStats = LoadStateStatistics.loadStatsFromFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    List<Action> allowedActions;

    PolicyGradientQWOP(PolicyGradientNetwork net) {
        this.net = net;
        allowedActions = new ArrayList<>();
        allowedActions.add(new Action(1, Action.Keys.qp));
        allowedActions.add(new Action(1, Action.Keys.wo));
        //new ArrayList<>(ActionGenerator_UniformNoRepeats.makeDefaultGenerator().getAllPossibleActions());
    }

    public float trainingStep(State[] states, Action[] actions, float[] discountedRewards) {
        float[][] flatStates = new float[states.length][State.STATE_SIZE];
        float[][] oneHotActions = new float[actions.length][allowedActions.size()];

        // Flatten states to episode length x 72
        for (int i = 0; i < states.length; i++) {
            flatStates[i] = states[i].flattenStateWithRescaling(stateStats);
        }

        // Flatten actions to episode length x num actions
        for (int i = 0; i < actions.length; i++) {
            oneHotActions[i][allowedActions.indexOf(actions[i])] = 1;
        }

        return net.trainingStep(flatStates, oneHotActions, discountedRewards);
    }

    @SuppressWarnings("Duplicates")
    public float[] evaluate(IState state) {
        return net.evaluate(state.flattenStateWithRescaling(stateStats));
    }

    public void playGame() {
        game.makeNewWorld();
        actionQueue.clearAll();
        actionQueue.addAction(new Action(7, Action.Keys.wo));
        states.clear();
        actions.clear();
        rewards.clear();
        State currentState;
        State prevState = (State) GameUnified.getInitialState();

        while (!game.getFailureStatus() && game.getTimestepsThisGame() < 3000) {
            if (actionQueue.isEmpty()) {
                currentState = (State) game.getCurrentState();
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
//                Action nextAction = new Action(1, Action.Keys.values()[bestIdx]);
                Action nextAction = allowedActions.get(bestIdx);
                states.add(currentState);
                actions.add(nextAction);
                rewards.add(currentState.getCenterX() - prevState.getCenterX()); // Reward is always 1
                // behind.
                actionQueue.addAction(nextAction);
                prevState = currentState;
            }
            game.step(actionQueue.pollCommand());
        }
        //rewards.add(-5f);

        // Figure out the discounted rewards.
//        states.remove(states.size() - 1);
//        actions.remove(actions.size() - 1);
        rewards.remove(0);
        assert states.size() == actions.size() && states.size() == rewards.size();

        if (states.size() < 2)
            return; // TODO the case where the first action fails it. or with 1 stdev becomes 0.


        State[] stateArray = states.toArray(new State[0]);
        Action[] actionArray = actions.toArray(new Action[0]);
        float[] rewardsFlat = new float[rewards.size()];
        int idx = 0;
        for (Float f : rewards) {
            rewardsFlat[idx++] = f;
        }
        float[] discounted = PolicyGradientNetwork.discountRewards(rewardsFlat, 0.99f);
        float loss = trainingStep(stateArray, actionArray, discounted);
        System.out.println("Distance: " + stateArray[stateArray.length - 1].getCenterX() / 10 + " Loss: " + loss);
    }

    public static void main(String[] args) throws FileNotFoundException {

        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(72);
        //layerSizes.add(128);
//        layerSizes.add(32);
        layerSizes.add(16);
        layerSizes.add(2); // TODO no hard code

        List<String> addedArgs = new ArrayList<>();
        addedArgs.add("-lr");
        addedArgs.add("1e-2 ");
        PolicyGradientNetwork net = PolicyGradientNetwork.makeNewNetwork("src/main/resources/tflow_models/tmp.pb",
                layerSizes, addedArgs, true);

        PolicyGradientQWOP policy = new PolicyGradientQWOP(net);
        for (int i = 0; i < 10000000; i++) {
            policy.playGame();
        }
    }
}
