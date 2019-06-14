package game;

import actions.Action;
import actions.ActionQueue;
import tflowtools.TensorflowLoader;
import org.tensorflow.Tensor;
import ui.PanelRunner_SimpleState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GamePredictor extends TensorflowLoader {

    private String stateInputName = "input/qwop_state_input";
    private String actionInputName = "input/qwop_action_input";

    private String internalStateInput = "rnn/full_internal_state_input";

    private String hiddenStateOutputName = "output/internal_state_output";
    private String stateOutputName = "output/state_output";

    /**
     * Load the computational graph from a .pb file and also make a new session.
     *
     * @param pbFile    Name of the graph save file (usually *.pb), including the file extension.
     * @param directory Directory name containing the graph save file.
     */
    public GamePredictor(String pbFile, String directory) {
        super(pbFile, directory);
    }

    public List<IState> predictSimulation(IState initialState, ActionQueue actions) {

        List<IState> resultStates = new ArrayList<>();

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

        resultStates.add(new State(reshapedResult, false));

        // Second evaluation, we have an internal state to feed in.
        while (!actions.isEmpty()) {
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

            resultStates.add(new State(reshapedResult, false));

        }
        return resultStates;
    }

    /**
     * Turn an Action into a 3 element, one-hot tensor. This is explicitly for the WO, QP [NONE] key combinations.
     * Any order is allowed, but only those three key configurations.
     *
     * @param keys Action to convert into a one-hot tensor.
     * @return 3 element float tensor with one-hot representation of the action.
     */
    private Tensor<Float> makeActionTensor(boolean[] keys) {
        float[][][] oneHotAction = new float[1][1][3];

        // WO
        oneHotAction[0][0][0] = keys[1] && keys[2] ? 1f : 0f;
        oneHotAction[0][0][1] = keys[0] && keys[3] ? 1f : 0f;
        oneHotAction[0][0][2] = !keys[1] && !keys[2] && !keys[3] && !keys[0] ? 1f : 0f;

        return Tensor.create(oneHotAction, Float.class);
    }

    public static void main(String[] args) {
        GamePredictor gp = new GamePredictor("frozen_model.pb", "src/main/resources/tflow_models");

        IState initState = GameUnified.getInitialState();
        Action singleAction = new Action(1000, false, true, true, false);

        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addAction(singleAction);
//        ActionQueue actionQueue = CompareWarmStartToColdBase.getSampleActions();

        List<IState> states = gp.predictSimulation(initState, actionQueue);

        JFrame frame = new JFrame();
        PanelRunner_SimpleState panelRunner = new PanelRunner_SimpleState();
        panelRunner.activateTab();
        frame.getContentPane().add(panelRunner);
        frame.setPreferredSize(new Dimension(1000, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);


        for (IState st : states) {
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
