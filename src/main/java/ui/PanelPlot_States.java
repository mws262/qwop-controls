package ui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.jfree.chart.plot.XYPlot;

import filters.NodeFilter_Downsample;
import game.State;
import tree.Node;
import tree.Utility;

/**
 * Pane for displaying plots of various state variables. Click each plot to pull up a menu for selecting data.
 * A tab.
 *
 * @author Matt
 */
public class PanelPlot_States extends PanelPlot implements ItemListener {

    private static final long serialVersionUID = 1L;

    /**
     * Maximum allowed datapoints. Will downsample if above. Prevents extreme lag.
     **/
    private NodeFilter_Downsample plotDownsampler = new NodeFilter_Downsample(5000);

    /**
     * Node from which states are referenced.
     **/
    private Node selectedNode;

    /**
     * Which plot index has an active menu.
     **/
    int activePlotIdx = 0;

    /**
     * Body parts associated with each plot and axis.
     **/
    private State.ObjectName[] plotObjectsX = new State.ObjectName[numberOfPlots];
    private State.ObjectName[] plotObjectsY = new State.ObjectName[numberOfPlots];

    /**
     * State variables associated with each plot and axis.
     **/
    private State.StateName[] plotStatesX = new State.StateName[numberOfPlots];
    private State.StateName[] plotStatesY = new State.StateName[numberOfPlots];

    /**
     * Drop down menus for the things to plot.
     **/
    private JComboBox<String> objListX;
    private JComboBox<String> stateListX;
    private JComboBox<String> objListY;
    private JComboBox<String> stateListY;

    /**
     * Menu for selecting which data is displayed.
     **/
    private final JDialog menu;

    private int countDataCollect = 0;

    public PanelPlot_States(int numPlots) {
        super(numPlots);
        // Make string arrays of the body part and state variable names.
        int count = 0;
        /** String names of the body parts. **/
        String[] objNames = new String[State.ObjectName.values().length];
        for (State.ObjectName obj : State.ObjectName.values()) {
            objNames[count] = obj.name();
            count++;
        }
        count = 0;
        /** String names of the state variables. **/
        String[] stateNames = new String[State.StateName.values().length];
        for (State.StateName st : State.StateName.values()) {
            stateNames[count] = st.name();
            count++;
        }

        // Initial plots to display
        for (int i = 0; i < numberOfPlots; i++) {
            plotObjectsX[i] = State.ObjectName.values()[Utility.randInt(0, numberOfPlots - 1)];
            plotStatesX[i] = State.StateName.values()[Utility.randInt(0, numberOfPlots - 1)];
            plotObjectsY[i] = State.ObjectName.values()[Utility.randInt(0, numberOfPlots - 1)];
            plotStatesY[i] = State.StateName.values()[Utility.randInt(0, numberOfPlots - 1)];
        }

        // Drop down menus
        objListX = new JComboBox<>(objNames);
        stateListX = new JComboBox<>(stateNames);
        objListY = new JComboBox<>(objNames);
        stateListY = new JComboBox<>(stateNames);

        objListX.addItemListener(this);
        stateListX.addItemListener(this);
        objListY.addItemListener(this);
        stateListY.addItemListener(this);

        menu = new JDialog(); // Pop up box for the menus.
        menu.setLayout(new GridLayout(2, 4));
        menu.add(new JLabel("X-axis", SwingConstants.CENTER));
        menu.getContentPane().add(objListX);
        menu.getContentPane().add(stateListX);
        menu.add(new JLabel("Y-axis", SwingConstants.CENTER));
        menu.getContentPane().add(objListY);
        menu.getContentPane().add(stateListY);

        menu.setAlwaysOnTop(true);
        menu.pack();
        menu.setVisible(false); // Start with this panel hidden.
    }

    @Override
    public void update(Node selectedNode) {
        this.selectedNode = selectedNode;
        // Fetching new data.
        List<Node> nodesBelow = new ArrayList<>();
        if (selectedNode != null) {
            selectedNode.getNodesBelow(nodesBelow);

            // Reduce the list size to something which renders quickly.
            plotDownsampler.filter(nodesBelow);

            Iterator<Entry<XYPlot, PlotDataset>> it = plotsAndData.entrySet().iterator();
            countDataCollect = 0;
            while (it.hasNext()) {
                Entry<XYPlot, PlotDataset> plotAndData = it.next();
                XYPlot pl = plotAndData.getKey();
                PlotDataset dat = plotAndData.getValue();

                Float[] xData =
						nodesBelow.stream().map(n -> n.getState().getStateVarFromName(plotObjectsX[countDataCollect],
								plotStatesX[countDataCollect])).toArray(Float[]::new); // Crazy new Java 8!
                Float[] yData =
						nodesBelow.stream().map(n -> n.getState().getStateVarFromName(plotObjectsY[countDataCollect],
								plotStatesY[countDataCollect])).toArray(Float[]::new);
                Color[] cData =
						nodesBelow.stream().map(n -> Node.getColorFromTreeDepth(n.getTreeDepth())).toArray(Color[]::new);

                float xLow = Arrays.stream(xData).min(Float::compare).get();
                float xHi = Arrays.stream(xData).max(Float::compare).get();

                float yLow = Arrays.stream(yData).min(Float::compare).get();
                float yHi = Arrays.stream(yData).max(Float::compare).get();

                pl.getDomainAxis().setRange(xLow - 0.05, xHi + 0.05); // Range gets whiney if you select one node and
				// try to set the range upper and lower to the same thing.
                pl.getRangeAxis().setRange(yLow - 0.05, yHi + 0.05);

                dat.addSeries(0, xData, yData, cData);
                pl.getRangeAxis().setLabel(plotObjectsX[countDataCollect].toString() + " " + plotStatesX[countDataCollect].toString());
                pl.getDomainAxis().setLabel(plotObjectsY[countDataCollect].toString() + " " + plotStatesY[countDataCollect].toString());
                countDataCollect++;
            }
            //addCommandLegend(firstPlot);
            applyUpdates();
        }
    }

    @Override
    public void deactivateTab() {
        super.deactivateTab();
        menu.setVisible(false);
    }

    @Override
    public void plotClicked(int plotIdx) {
        activePlotIdx = plotIdx;
        menu.setTitle("Select plot " + (plotIdx + 1) + " data.");
        int menuYOffset = -30; /** Offsets to put the data selection menu right above the correct panel. **/
        int menuXOffset = 30;
        menu.setLocation(plotPanels[plotIdx].getX() + menuXOffset, menuYOffset);
        objListX.setSelectedIndex(plotObjectsX[plotIdx].ordinal()); // Make the drop down menus match the current plots.
        objListY.setSelectedIndex(plotObjectsY[plotIdx].ordinal());
        stateListX.setSelectedIndex(plotStatesX[plotIdx].ordinal());
        stateListY.setSelectedIndex(plotStatesY[plotIdx].ordinal());
        menu.setVisible(true);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        int state = e.getStateChange();
        if (state == ItemEvent.SELECTED) {
            if (e.getSource() == objListX) {
                plotObjectsX[activePlotIdx] = State.ObjectName.valueOf((String) e.getItem());
            } else if (e.getSource() == objListY) {
                plotObjectsY[activePlotIdx] = State.ObjectName.valueOf((String) e.getItem());
            } else if (e.getSource() == stateListX) {
                plotStatesX[activePlotIdx] = State.StateName.valueOf((String) e.getItem());
            } else if ((e.getSource() == stateListY)) {
                plotStatesY[activePlotIdx] = State.StateName.valueOf((String) e.getItem());
            } else {
                throw new RuntimeException("Unknown item status in plots from: " + e.getSource().toString());
            }
            update(selectedNode);
        }
    }
}