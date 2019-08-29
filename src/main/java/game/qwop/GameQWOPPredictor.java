package game.qwop;

import game.action.Action;
import game.action.ActionQueue;
import org.jetbrains.annotations.NotNull;
import org.tensorflow.Tensor;
import tflowtools.TensorflowLoader;
import ui.runner.PanelRunner_SimpleState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GameQWOPPredictor extends TensorflowLoader {

    /**
     * Load the computational graph from a .pb file and also make a new session.
     *
     * @param pbFile    Name of the graph save file (usually *.pb), including the file extension.
     * @param directory Directory name containing the graph save file.
     */
    public GameQWOPPredictor(@NotNull String pbFile, @NotNull String directory) {
        super(pbFile, directory);
    }

    public List<StateQWOP> predictSimulation(StateQWOP initialState, ActionQueue<CommandQWOP> actions) {
        String stateInputName = "input/qwop_state_input";
        String actionInputName = "input/qwop_action_input";
        String hiddenStateOutputName = "output/internal_state_output";
        String stateOutputName = "output/state_output";

        List<StateQWOP> resultStates = new ArrayList<>();

        float[][][] stateIn = new float[1][][]; // Awkward singleton dimensions: [sample no (1), timesteps (1), state
        // vals (72)]

        stateIn[0] = flattenState(initialState);
        Tensor<Float> stateInputTensor = Tensor.create(stateIn, Float.class);

        // First evaluation, we don't have an internal state to feed into the RNN.
        List<Tensor<?>> result =
                getSession().runner().feed(stateInputName + ":0", stateInputTensor)
                        .feed(actionInputName + ":0", makeActionTensor(actions.pollCommand()))
                        .fetch(stateOutputName + ":0")
                        .fetch(hiddenStateOutputName + ":0")
                        .run();

        Tensor<Float> stateResult = result.get(0).expect(Float.class);
        Tensor<Float> internalStateResult = result.get(1).expect(Float.class);


        long[] outputShape = stateResult.shape();
        float[] reshapedResult =
                stateResult.copyTo(new float[(int) outputShape[0]]
                        [(int) outputShape[1]]
                        [(int) outputShape[2]])[0][0];

        resultStates.add(new StateQWOP(reshapedResult, false));

        // Second evaluation, we have an internal state to feed in.
        while (!actions.isEmpty()) {
            String internalStateInput = "rnn/full_internal_state_input";
            result =
                    getSession().runner().feed(stateInputName + ":0", stateResult)
                            .feed(actionInputName + ":0", makeActionTensor(actions.pollCommand()))
                            .feed(internalStateInput + ":0", internalStateResult)
                            .fetch(stateOutputName + ":0")
                            .fetch(hiddenStateOutputName + ":0")
                            .run();

            stateResult = result.get(0).expect(Float.class);
            internalStateResult = result.get(1).expect(Float.class);

            reshapedResult =
                    stateResult.copyTo(new float[(int) outputShape[0]]
                            [(int) outputShape[1]]
                            [(int) outputShape[2]])[0][0];

            resultStates.add(new StateQWOP(reshapedResult, false));

        }
        stateInputTensor.close();
        stateResult.close();
        internalStateResult.close(); // TODO make sure all are closed.

        return resultStates;
    }

    /**
     * Turn an Action into a 3 element, one-hot tensor. This is explicitly for the WO, QP [NONE] key combinations.
     * Any order is allowed, but only those three key configurations.
     *
     * @return 3 element float tensor with one-hot representation of the command.
     */
    private Tensor<Float> makeActionTensor(CommandQWOP command) {
        float[][][] oneHotAction = new float[1][1][3];

        if (command.equals(CommandQWOP.NONE)) {
            oneHotAction[0][0][2] = 1;
        } else if (command.equals(CommandQWOP.WO)) {
            oneHotAction[0][0][0] = 1;
        } else if (command.equals(CommandQWOP.QP)) {
            oneHotAction[0][0][1] = 1;
        } else {
            throw new IllegalArgumentException("Only none, QP, WO combinations allowed in this. Was given: " + command.keys.name());
        }
        return Tensor.create(oneHotAction, Float.class);
    }

    public static void main(String[] args) {
        GameQWOPPredictor gp = new GameQWOPPredictor("frozen_model.pb", "src/main/resources/tflow_models");
        Runtime.getRuntime().addShutdownHook(new Thread(gp::close));

        StateQWOP initState = GameQWOP.getInitialState();
        Action<CommandQWOP> singleAction = new Action<>(1000, CommandQWOP.WO);

        ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();
        actionQueue.addAction(singleAction);
//        ActionQueue actionQueue = CompareWarmStartToColdBase.getSampleActions();

        List<StateQWOP> states = gp.predictSimulation(initState, actionQueue);

        JFrame frame = new JFrame();
        PanelRunner_SimpleState panelRunner = new PanelRunner_SimpleState("Runner");
        panelRunner.activateTab();
        frame.getContentPane().add(panelRunner);
        frame.setPreferredSize(new Dimension(1000, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);


        for (StateQWOP st : states) {
            panelRunner.updateState(st);
            panelRunner.repaint();

            try {
                Thread.sleep(35);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
