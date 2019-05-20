package goals.phase_variable_testing;

import actions.ActionQueue;
import game.GameUnified;
import game.State;
import tree.NodeQWOPGraphicsBase;
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
public class MAIN_StateCombinations extends JFrame {
    /**
     * Number of outputs to plot. Needs to have a corresponding neural network .pb file!
     */
    int numOutputs = 1;

    public static void main(String[] args) {
        new MAIN_StateCombinations().run();
    }
    public void run() {
        // Vis makeNewWorld.
        PanelPlot_Simple plotPanel = new PanelPlot_Simple();
        plotPanel.activateTab();
        getContentPane().add(plotPanel);
        setPreferredSize(new Dimension(1000, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        GameUnified game = new GameUnified();

        ActionQueue actionQueue = ActionQueue.getSampleActions();

        List<State> stateList = new ArrayList<>();
        stateList.add(GameUnified.getInitialState());
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
            State st = game.getCurrentState();
            stateList.add(st);
        }

        float[] xData = new float[stateList.size()];
        for (int i = 0; i < stateList.size(); i++) {
            xData[i] = i;
        }

        List<float[]> xDataList = new ArrayList<>();
        List<float[]> yDataList = new ArrayList<>();


        xDataList.add(xData);
        float[] stVals = new float[stateList.size()];
        for (int i = 0; i < stateList.size(); i++) {
            stVals[i] = stateList.get(i).rthigh.getTh() - stateList.get(i).lthigh.getTh(); // Pick data out here.
        }
        yDataList.add(stVals);


        List<Color> colorList = new ArrayList<>();
        for (int i = 0; i < xDataList.size(); i++) {
            colorList.add(NodeQWOPGraphicsBase.getColorFromTreeDepth(i, NodeQWOPGraphicsBase.lineBrightnessDefault));
        }

        plotPanel.setPlotData(xDataList, yDataList, colorList, "timestep idx", "state value");
    }
}
