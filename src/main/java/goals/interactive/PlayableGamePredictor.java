package goals.interactive;

import game.qwop.GameQWOP;
import game.state.IState;
import game.qwop.StateQWOP;
import org.tensorflow.Tensor;
import tflowtools.TensorflowLoader;
import ui.runner.PanelRunner_SimpleState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

public class PlayableGamePredictor extends TensorflowLoader {

    private static final String stateInputName = "input/qwop_state_input";
    private static final String actionInputName = "input/qwop_action_input";
    private static final String internalStateInput = "rnn/full_internal_state_input";
    private static final String hiddenStateOutputName = "output/internal_state_output";
    private static final String stateOutputName = "output/state_output";

    private Tensor<Float> currentGameStateTensor;
    private Tensor<Float> currentInternalState;

    private IState currentGameState =  GameQWOP.getInitialState();

    /**
     * Load the computational graph from a .pb file and also make a new session.
     *
     * @param pbFile    Name of the graph save file (usually *.pb), including the file extension.
     * @param directory Directory name containing the graph save file.
     */
    public PlayableGamePredictor(String pbFile, String directory) {
        super(pbFile, directory);
    }

    public void reset() {
        currentGameStateTensor = null;
        currentInternalState = null;
    }

    public StateQWOP advance(boolean[] keys) {

        if (currentInternalState == null) { // First timestep we don't supply an internal state.

            // Convert the initial state into a tensor.
            float[][][] stateIn = new float[1][][]; // Awkward singleton dimensions: [sample no (1), timesteps (1), state
            stateIn[0] = flattenState(currentGameState);
            Tensor<Float> stateInputTensor = Tensor.create(stateIn, Float.class);

            List<Tensor<?>> result =
                    getSession().runner().feed(stateInputName + ":0", stateInputTensor)
                            .feed(actionInputName + ":0", makeActionTensor(keys))
                            .fetch(stateOutputName + ":0")
                            .fetch(hiddenStateOutputName + ":0")
                            .run();

            stateInputTensor.close();
            currentGameStateTensor = result.get(0).expect(Float.class);
            currentInternalState = result.get(1).expect(Float.class);
        } else {
            // For timesteps >0, feed the previous internal state back into the RNN.
            List<Tensor<?>> result =
                    getSession().runner().feed(stateInputName + ":0", currentGameStateTensor)
                            .feed(actionInputName + ":0", makeActionTensor(keys))
                            .feed(internalStateInput + ":0", currentInternalState)
                            .fetch(stateOutputName + ":0")
                            .fetch(hiddenStateOutputName + ":0")
                            .run();

            currentGameStateTensor = result.get(0).expect(Float.class);
            currentInternalState = result.get(1).expect(Float.class);

        }

        // Convert the game state tensor into a StateQWOP object.
        long[] outputShape = currentGameStateTensor.shape();
        float[] reshapedResult =
                currentGameStateTensor.copyTo(new float[(int) outputShape[0]]
                        [(int) outputShape[1]]
                        [(int) outputShape[2]])[0][0];

        currentGameStateTensor.close();
        currentInternalState.close();
        return new StateQWOP(reshapedResult, false);
    }

    private Tensor<Float> makeActionTensor(boolean[] keys) {
        float[][][] oneHotAction = new float[1][1][3];

        if (keys[0] && keys[3]) {
            oneHotAction[0][0][1] = 1f;
        }else if (keys[1] && keys[2]) {
            oneHotAction[0][0][0] = 1f;
        }else {
            oneHotAction[0][0][2] = 1f;
        }
        return Tensor.create(oneHotAction, Float.class);
    }

    public static void main(String[] args) {
        PlayableGamePredictor gp = new PlayableGamePredictor("frozen_model9.pb", "src/main/resources/tflow_models" +
                "/sim_models/");
        Runtime.getRuntime().addShutdownHook(new Thread(gp::close));

        JFrame frame = new JFrame();
        PanelRunner_SimpleState panelRunner = new PanelRunner_SimpleState("Runner");
        panelRunner.activateTab();
        frame.getContentPane().add(panelRunner);
        frame.setPreferredSize(new Dimension(1000, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        boolean keys[] = new boolean[4];

        frame.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_Q) {
                    keys[0] = true;
                }else if(event.getKeyCode() == KeyEvent.VK_W) {
                    keys[1] = true;
                }else if(event.getKeyCode() == KeyEvent.VK_O) {
                    keys[2] = true;
                }else if(event.getKeyCode() == KeyEvent.VK_P) {
                    keys[3] = true;
                }else if(event.getKeyCode() == KeyEvent.VK_R) {
                    gp.reset();
                }
            }

            @Override
            public void keyReleased(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_Q) {
                    keys[0] = false;
                }else if(event.getKeyCode() == KeyEvent.VK_W) {
                    keys[1] = false;
                }else if(event.getKeyCode() == KeyEvent.VK_O) {
                    keys[2] = false;
                }else if(event.getKeyCode() == KeyEvent.VK_P) {
                    keys[3] = false;
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });

        while (true) {
            long time1 = System.currentTimeMillis();
            panelRunner.updateState(gp.advance(keys));
            panelRunner.repaint();
            long time2 = System.currentTimeMillis();

            if ((time2 - time1) < 40) {
                try {
                    Thread.sleep(40 - (time2 - time1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
