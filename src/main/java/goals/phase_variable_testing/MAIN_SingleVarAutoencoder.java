package goals.phase_variable_testing;

import actions.Action;
import actions.ActionQueue;
import game.GameLoader;
import game.State;
import transformations.Transform_Autoencoder;
import tree.Node;
import ui.PanelPlot_Simple;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Trying to see what sorts of things would work as a phase variable to indicate what part of the gait cycle we are
 * in. One logical choice is a neural network which compresses the full 72 state values to a single 1. This runs a
 * sample set of actions through and spits out what the network says.
 *
 * @author matt
 */
public class MAIN_SingleVarAutoencoder extends JFrame {
    /**
     * Number of outputs to plot. Needs to have a corresponding neural network .pb file!
     */
    int numOutputs = 1;

    public static void main(String[] args) {
        new MAIN_SingleVarAutoencoder().run();
    }
    public void run() {
        // Vis setup.
        PanelPlot_Simple plotPanel = new PanelPlot_Simple();
        plotPanel.activateTab();
        getContentPane().add(plotPanel);
        setPreferredSize(new Dimension(1000, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        GameLoader game = new GameLoader();
        String modelDir = "src/main/resources/tflow_models/";
        Transform_Autoencoder autoencoder =
                new Transform_Autoencoder(modelDir + "AutoEnc_72to" + String.valueOf(numOutputs) + "_6layer.pb",
                        numOutputs);

        ActionQueue actionQueue = getSampleActions();

        List<State> stateList = new ArrayList<>();
        stateList.add(GameLoader.getInitialState());
        while (!actionQueue.isEmpty()) {
            game.stepGame(actionQueue.pollCommand());
            State st = game.getCurrentState();
            stateList.add(st);
        }

        List<float[]> tformedSt = autoencoder.transform(stateList);

        float[] xData = new float[stateList.size()];
        for (int i = 0; i < stateList.size(); i++) {
            xData[i] = i;
        }

        List<float[]> xDataList = new ArrayList<>();
        List<float[]> yDataList = new ArrayList<>();
        for (int i = 0; i < tformedSt.get(0).length; i++) {
            yDataList.add(new float[tformedSt.size()]);
            xDataList.add(xData);
        }

        for (int i = 0; i < tformedSt.size(); i++) { // For each state transformed.
            for (int j = 0; j < tformedSt.get(i).length; j++) { // For each reduced state var.
                yDataList.get(j)[i] = tformedSt.get(i)[j];

            }
        }

        List<Color> colorList = new ArrayList<>();
        for (int i = 0; i < xDataList.size(); i++) {
            colorList.add(Node.getColorFromTreeDepth(i));
        }

        plotPanel.setPlotData(xDataList, yDataList, colorList, "timestep idx", "autoencoder output");
    }

    static ActionQueue getSampleActions() {
        // Ran MAIN_Search_LongRun to get these. 19 steps.
        ActionQueue actionQueue = new ActionQueue();
        actionQueue.addAction(new Action(1, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(34, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(19, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(45, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(38, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(5, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(31, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(21, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(21, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(14, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(35, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(10, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(23, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(23, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(13, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(24, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(22, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(18, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(23, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(24, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(21, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(20, new boolean[]{false, true, true, false}));
        actionQueue.addAction(new Action(19, new boolean[]{false, false, false, false}));
        actionQueue.addAction(new Action(21, new boolean[]{true, false, false, true}));

        actionQueue.addAction(new Action(16, new boolean[]{false, false, false, false}));

        return actionQueue;
    }
}
