package goals.phase_variable_testing;

import game.action.ActionQueue;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.IStateQWOP.ObjectName;
import game.qwop.StateQWOP;
import tree.node.NodeGameGraphicsBase;
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
public class MAIN_StateCombinations extends JFrame {

    public static void main(String[] args) {
        new MAIN_StateCombinations().run();
    }
    public void run() {
        // Vis resetGame.
        PanelPlot_Simple plotPanel = new PanelPlot_Simple("Runner");
        plotPanel.activateTab();
        getContentPane().add(plotPanel);
        setPreferredSize(new Dimension(1000, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);

        GameQWOP game = new GameQWOP();

        ActionQueue<CommandQWOP> actionQueue = ActionQueue.getSampleActions();

        List<StateQWOP> stateList = new ArrayList<>();
        stateList.add((StateQWOP) GameQWOP.getInitialState());
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
            StateQWOP st = (StateQWOP) game.getCurrentState();
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
            stVals[i] =
                    stateList.get(i).getStateVariableFromName(ObjectName.RTHIGH).getTh() - stateList.get(i).getStateVariableFromName(ObjectName.LTHIGH).getTh(); // Pick data out here.
        }
        yDataList.add(stVals);


        List<Color> colorList = new ArrayList<>();
        for (int i = 0; i < xDataList.size(); i++) {
            colorList.add(NodeGameGraphicsBase.getColorFromTreeDepth(i, NodeGameGraphicsBase.lineBrightnessDefault));
        }

        plotPanel.setPlotData(xDataList, yDataList, colorList, "timestep idx", "state value");
    }
}
