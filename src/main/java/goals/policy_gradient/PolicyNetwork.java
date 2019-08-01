package goals.policy_gradient;

import game.GameUnified;
import game.action.Action;
import game.action.ActionQueue;
import game.state.State;
import org.jblas.util.Random;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;
import tflowtools.TrainableNetwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

// reference https://github.com/simoninithomas/Deep_reinforcement_learning_Course/blob/master/Policy%20Gradients/Cartpole/Cartpole%20REINFORCE%20Monte%20Carlo%20Policy%20Gradients.ipynb
public class PolicyNetwork extends TrainableNetwork {

    private GameUnified game = new GameUnified();
    private ActionQueue actionQueue = new ActionQueue();

    private List<State> states = new ArrayList<>();
    private List<Action> actions = new ArrayList<>();
    private List<Float> rewards = new ArrayList<>();


    List<Action> allowedActions;
    /**
     * Create a new wrapper for an existing Tensorflow graph.
     *
     * @param graphDefinition Graph definition file.
     */
    PolicyNetwork(File graphDefinition) throws FileNotFoundException {
        super(graphDefinition);
        allowedActions = new ArrayList<>();
        allowedActions.add(new Action(2, Action.Keys.qo));
//        allowedActions.add(new Action(2, Action.Keys.qp));
        allowedActions.add(new Action(2, Action.Keys.none));

                //new ArrayList<>(ActionGenerator_UniformNoRepeats.makeDefaultGenerator().getAllPossibleActions());
    }

    public float trainingStep(State[] states, Action[] actions, float[] discountedRewards) {
        float[][] flatStates = new float[states.length][State.STATE_SIZE];
        float[][] oneHotActions = new float[actions.length][allowedActions.size()];
        float loss;

        // Flatten states to episode length x 72
        for (int i = 0; i < states.length; i++) {
            flatStates[i] = states[i].flattenState(); // TODO normalize maybe.
        }

        // Flatten actions to episode length x 9
        for (int i = 0; i < actions.length; i++) {
            oneHotActions[i][allowedActions.indexOf(actions[i])] = 1;
        }

//        // Even though each reward is a scalar, we need to pass in as 2D.
//        float[][] rewards = new float[states.length][1];
//        for (int i = 0; i < states.length; i++) {
//            rewards[i][0] = discountedRewards[i];
//        }

        try (Tensor<Float> stateTensor = Tensors.create(flatStates);
             Tensor<Float> actionTensor = Tensors.create(oneHotActions);
             Tensor<Float> rewardTensor = Tensors.create(discountedRewards)) {

            List<Tensor<?>> out = session.runner().feed("input", stateTensor)
                    .feed("output_target", actionTensor)
                    .feed("discounted_episode_rewards", rewardTensor)
                    .addTarget("train")
                    .fetch("loss").run();
            loss = out.get(0).expect(Float.class).floatValue();
            out.forEach(Tensor::close);
        }

        return loss;
    }

    @SuppressWarnings("Duplicates")
    public float[] evaluate(State state) {
        float[][] input = new float[][] {state.flattenState()}; // TODO NORMALIZE?
        float[][] output;
        try (Tensor<Float> stateTensor = Tensors.create(input)) {

            List<Tensor<?>> out = session.runner().feed("input", stateTensor)
                    .fetch("output").run();
            Tensor<Float> outputTensor = out.get(0).expect(Float.class);

            long[] outputShape = outputTensor.shape();
            output = new float[(int) outputShape[0]][(int) outputShape[1]];
            outputTensor.copyTo(output);

            out.forEach(Tensor::close);
        }
        return output[0];
    }

    public float trainingStep(float[][] inputs, float[][] desiredOutputs, int steps) {
        throw new IllegalStateException("Don't use this."); // TODO something better lol.
    }

    public static PolicyNetwork makeNewNetwork(String graphFileName, List<Integer> layerSizes,
                                                  List<String> additionalArgs) throws FileNotFoundException {

        additionalArgs.add("-ao");
        additionalArgs.add("softmax");
        return new PolicyNetwork(makeGraphFile(graphFileName, layerSizes, additionalArgs));
    }

    public static PolicyNetwork makeNewNetwork(String graphName, List<Integer> layerSizes) throws FileNotFoundException {
        return makeNewNetwork(graphName, layerSizes, new ArrayList<>());
    }

    public void playGame() {
        game.makeNewWorld();
        actionQueue.clearAll();
        actionQueue.addAction(new Action(7, Action.Keys.wo));
        states.clear();
        actions.clear();
        rewards.clear();
        State currentState = (State) GameUnified.getInitialState();

        while (!game.getFailureStatus() && game.getTimestepsThisGame() < 3000) {
            if (actionQueue.isEmpty()) {

                State prevState = currentState;
                currentState = (State) game.getCurrentState();
                float[] predictionDist = evaluate(currentState); // These should add to 1.
                float[] cumDist = new float[predictionDist.length];
                cumDist[0] = predictionDist[0];
                for (int i = 1; i < predictionDist.length; i++) {
                    cumDist[i] = cumDist[i - 1] + predictionDist[i];
                }
                float selection = Random.nextFloat();

                Integer bestIdx = 0;
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
                rewards.add(1f);//currentState.getCenterX() - prevState.getCenterX()); // Reward is always 1 behind.
                actionQueue.addAction(nextAction);
            }
            game.step(actionQueue.pollCommand());
        }




        // Figure out the discounted rewards.
        states.remove(states.size() - 1);
        actions.remove(actions.size() - 1);
        rewards.remove(0);
        assert states.size() == actions.size() && states.size() == rewards.size();

        if (states.size() < 2)
            return; // TODO the case where the first action fails it. or with 1 stdev becomes 0.

        float cumulative = 0f;
        float gamma = 0.99f;
        float mean = 0;
        float[] discounted = new float[rewards.size()];
        for (int i = states.size() - 1; i >= 0; i--) {
            cumulative = cumulative * gamma + rewards.get(i);
            discounted[i] = cumulative;
            mean += cumulative;
        }

        mean /= discounted.length;
        float stdev = 0f;
        for (float reward : discounted) {
            stdev += (reward - mean) * (reward - mean);
        }
        stdev /= (discounted.length); // TODO n or n - 1
        stdev = (float) Math.sqrt(stdev);

        for (int i = 0; i < discounted.length; i++) {
            discounted[i] -= mean;
            discounted[i] /= stdev;
        }

        State[] stateArray = states.toArray(new State[0]);
        Action[] actionArray = actions.toArray(new Action[0]);

        float loss = trainingStep(stateArray, actionArray, discounted);

        if (Float.isNaN(loss)) {

            System.out.println("uhoh");
        }
        System.out.println("Distance: " + stateArray[stateArray.length - 1].getCenterX() / 10 + " Loss: " + loss);
    }

    public static void main(String[] args) throws FileNotFoundException {

        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(72);
        //layerSizes.add(128);
        //layerSizes.add(32);
        layerSizes.add(16);
        layerSizes.add(2); // TODO no hard code

        List<String> addedArgs = new ArrayList<>();
        addedArgs.add("-lr");
        addedArgs.add("1e-3");
        PolicyNetwork net = PolicyNetwork.makeNewNetwork("src/main/resources/tflow_models/tmp.pb", layerSizes, addedArgs);

        for (int i = 0; i < 10000000; i++) {
            net.playGame();
        }
//        float[] probs = net.evaluate((State)GameUnified.getInitialState());
//        for (float prob : probs) {
//            System.out.println(prob);
//        }

    }
}
