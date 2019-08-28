package goals.phase_variable_testing;

import game.action.ActionQueue;
import game.qwop.GameQWOP;
import game.IGameInternal;
import game.state.IState;
import game.state.transform.Transform_Autoencoder;
import tree.node.NodeQWOPGraphicsBase;
import ui.scatterplot.PanelPlot_Simple;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Trying to see what sorts of things would work as a phase variable to indicate what part of the gait cycle we are
 * in. One logical choice is a neural network which compresses the full 72 state values to a single 1. This runs a
 * sample set of game.command through and spits out what the network says.
 *
 * @author matt
 */
public class MAIN_SingleVarAutoencoder extends JFrame {
    /**
     * Number of outputs to plot. Needs to have a corresponding neural network .pb file!
     */
    int numOutputs = 6;

    public static void main(String[] args) {
        new MAIN_SingleVarAutoencoder().run();
    }
    public void run() {
        // Vis resetGame.
        PanelPlot_Simple plotPanel = new PanelPlot_Simple("Plot");
        plotPanel.activateTab();
        getContentPane().add(plotPanel);
        setPreferredSize(new Dimension(1000, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        IGameInternal game = new GameQWOP();
        String modelDir = "src/main/resources/tflow_models/";
        Transform_Autoencoder autoencoder =
                new Transform_Autoencoder(modelDir + "AutoEnc_72to" + numOutputs + "_6layer.pb",
                        numOutputs);

        ActionQueue actionQueue = ActionQueue.getSampleActions();

        List<IState> stateList = new ArrayList<>();
        stateList.add(GameQWOP.getInitialState());
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
            IState st = game.getCurrentState();
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
            colorList.add(NodeQWOPGraphicsBase.getColorFromTreeDepth(i, NodeQWOPGraphicsBase.lineBrightnessDefault));
        }

        plotPanel.setPlotData(xDataList, yDataList, colorList, "timestep idx", "autoencoder output");
    }
}
