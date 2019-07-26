package goals.value_function;

import controllers.Controller_ValueFunction;
import controllers.IController;
import game.GameConstants;
import game.GameUnified;
import game.action.Action;
import game.action.ActionQueue;
import game.state.IState;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;
import tree.node.NodeQWOPExplorable;
import value.ValueFunction_TensorFlow;
import value.ValueFunction_TensorFlow_StateOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MAIN_ImproveValFun {


    public MAIN_ImproveValFun() throws IOException {

        GameUnified game = new GameUnified();
        List<Integer> layerSizes = new ArrayList<>();
        layerSizes.add(128);
        layerSizes.add(32);

        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add("--loss");
        additionalArgs.add("target_only"); // Loss has nothing to do with the network output in this case. Just the
        // performance of the controller.
        ValueFunction_TensorFlow valFun = new ValueFunction_TensorFlow_StateOnly("src/main/resources/tflow_models" +
                "/test.pb", game, layerSizes, additionalArgs, "src/main/resources/tflow_models/checkpoints" +
                "/checkpoint_lots360");

        valFun.network.printGraphOperations();
        float[][] layer = new float[128][32];
        valFun.network.session.runner().fetch("fully_connected1/weights/Variable/read").run().get(0).copyTo(layer);

        float[][] layerZeros = new float[128][32];
        Tensor<Float> inputTensor = Tensors.create(layerZeros);
        valFun.network.session.runner().feed("fully_connected1/weights/weight", inputTensor).addTarget(
                "fully_connected1/weights/Variable/Assign").run();


        valFun.network.session.runner().fetch("fully_connected1/weights/Variable/read").run().get(0).copyTo(layer);

        IController controller = new Controller_ValueFunction<>(valFun);


        for (int i = 0; i < 1000; i++) {
            ActionQueue actionQueue = new ActionQueue();
            actionQueue.addAction(new Action(7, Action.Keys.none));
            game.makeNewWorld();

            int maxTs = 3000;
            while (!game.getFailureStatus() && game.getTimestepsThisGame() < maxTs && game.getCurrentState().getCenterX() < GameConstants.goalDistance) {

                if (actionQueue.isEmpty()) {
                    actionQueue.addAction(controller.policy(new NodeQWOPExplorable(game.getCurrentState()))); // Can
                    // change to game serialization version here.
                }
                game.step(actionQueue.pollCommand());
            }

            IState finalState = game.getCurrentState();
            float loss =
                    (GameConstants.goalDistance - (finalState.getCenterX() - GameUnified.getInitialState().getCenterX())) * 1f // Distance term should dominate.
                            - (finalState.getCenterX() - GameUnified.getInitialState().getCenterX()) / (game.getTimestepsThisGame() * GameConstants.timestep) * 0.1f; //
            System.out.println(loss);

            //input should be ignored?
            float[][] inDummy = new float[1][72];
            float[][] outLoss = new float[1][1];
            outLoss[0][0] = loss;
            valFun.network.trainingStep(inDummy, outLoss, 1);


        }
    }

    public static void main(String[] args) throws IOException {

        new MAIN_ImproveValFun();

        System.exit(0);

    }
}
