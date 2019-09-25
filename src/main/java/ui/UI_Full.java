package ui;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import game.state.IState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.annotate.JsonSetter;
import tree.TreeWorker;
import tree.Utility;
import tree.node.NodeGameGraphicsBase;
import ui.timeseries.PanelTimeSeries_WorkerLoad;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * All UI stuff happens here and most of the analysis that individual panes show happens here too.
 *
 * @author Matt
 */
public class UI_Full<C extends Command<?>, S extends IState> implements ChangeListener, NodeSelectionListener<C, S>,
        IUserInterface<C, S> {

    private final JFrame frame = new JFrame();

    /**
     * Individual pane for the tree.
     */
    private PanelTree<C, S> panelTree;

    /**
     * Pane for the tabbed side of the interface.
     */
    private final JTabbedPane tabPane;

    /**
     * Selected node by user click/key
     */
    private NodeGameGraphicsBase<?, C, S> selectedNode;

    /**
     * List of panes which can be activated, deactivated.
     */
    private List<TabbedPaneActivator<C, S>> tabbedPanes = new ArrayList<>();

    private final PanelTimeSeries_WorkerLoad<C, S> workerPanel;

    private final Logger logger = LogManager.getLogger(UI_Full.class);

    /**
     * Window width
     */
    public static int windowWidth = 1920;

    /**
     * Window height
     */
    public static int windowHeight = 1000;

    private Timer redrawTimer;

    public final int maxWorkersToMonitor;

    public UI_Full(@JsonProperty("maxWorkersToMonitor") int maxWorkersToMonitor) {
        this.maxWorkersToMonitor = maxWorkersToMonitor;
        Container pane = frame.getContentPane();

        /* Tabbed panes */
        tabPane = new JTabbedPane();
        tabPane.setBorder(BorderFactory.createRaisedBevelBorder());
        tabPane.setPreferredSize(new Dimension(1080, 150));
        tabPane.setMinimumSize(new Dimension(100, 1));
        tabPane.addChangeListener(this);

        /* Tree pane */
        panelTree = new PanelTree<>();
        panelTree.addNodeSelectionListener(this); // Add this UI as a listener for selections of nodes on the tree.

        // This makes it have that draggable border between the tab and the tree sections.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelTree, tabPane);
        splitPane.setResizeWeight(0.72);
        pane.add(splitPane);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setPreferredSize(new Dimension(windowWidth, windowHeight));
        frame.setContentPane(frame.getContentPane());

        // Add toolbar icon.
        Toolkit kit = Toolkit.getDefaultToolkit();
        Image img = kit.createImage("src/main/resources/icons/QWOP_tree_ico.png");
        frame.setIconImage(img);

        frame.pack();
        workerPanel = new PanelTimeSeries_WorkerLoad<>("Worker Load", maxWorkersToMonitor);
        addTab(workerPanel);
    }

    /**
     * Add a new tab to this frame's set of tabbed panels.
     *
     * @param newTab New tab to add to the existing set of tabbed panels in this frame.
     */
    public synchronized void addTab(TabbedPaneActivator<C, S> newTab) {
            tabPane.addTab(newTab.getName(), (Component) newTab);
            tabbedPanes.add(newTab);
            tabPane.revalidate();
    }

    /**
     * Remove a specified UI element which is part of a tabbed set of panels.
     *
     * @param tabToRemove Tab to be removed from the set. Throws if the tab is not part of the group.
     */
    public synchronized void removeTab(TabbedPaneActivator tabToRemove) {
        if (tabbedPanes.contains(tabToRemove))
            tabbedPanes.remove(tabToRemove);
        else
            throw new IllegalArgumentException("Tried to remove a UI tab which did not exist.");

        //Make sure the currently active tab is actually being updated.
        tabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
    }

    @JsonSetter
    public synchronized void setTabbedPanes(List<TabbedPaneActivator<C, S>> tabs) {
        for (TabbedPaneActivator<C, S> tab : tabs) {
            addTab(tab);
        }
    }

    @JsonGetter
    public synchronized List<TabbedPaneActivator<C, S>> getTabbedPanes() {
        return tabbedPanes;
    }

    @Override
    public void start() {
//        int selectedTab = tabPane.getSelectedIndex();
//        if (selectedTab >= 0 && selectedTab < tabbedPanes.size()) {// -1 if none selected.
//            tabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
//        } else {
//            if (!tabbedPanes.isEmpty()) {
//            }
//            logger.warn("Launched full UI with no tab selected by default.");
//        }

        if (!tabbedPanes.isEmpty()) {
            tabPane.setSelectedIndex(0);
            tabbedPanes.get(0).activateTab(); // Just activate the first tab. Forget the other nonsense.
        }
        frame.setVisible(true);
        Utility.showOnScreen(frame, 0, false); // Choose the monitor to display on, filling that monitor.

        redrawTimer = new Timer(35, e -> frame.repaint());
        redrawTimer.start();
    }

    @Override
    public void kill() {
        redrawTimer.stop();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    @Override
    public void nodeSelected(NodeGameGraphicsBase<?, C, S> selected) {
        if (selectedNode != null) { // Clear things from the old selected node.
            selectedNode.setOverridePointColor(null);
            selectedNode.clearBranchLineOverrideColor();
            selectedNode.clearBranchZOffset();
        }
        if (selected == null) {
            return;
        }
        selectedNode = selected;
        selectedNode.reenableIfNotDrawnForSpeed(); // If the node was previously disabled to avoid drawing too many
        // lines, then put it back in since it is annoying selecting "invisible" nodes.
        selectedNode.setOverridePointColor(Color.RED);
        selectedNode.setBranchZOffset(0.4f);
//        selectedNode.setLineBrightnessBelow(1f);

        for (TabbedPaneActivator<C, S> panel : tabbedPanes) {
            if (panel.isActive()) {
                panel.update(selectedNode);
            }
        }
    }

    @Override
    public synchronized void stateChanged(ChangeEvent e) {
        if (!tabbedPanes.isEmpty()) {
            for (TabbedPaneActivator p : tabbedPanes) {
                p.deactivateTab();
            }
            nodeSelected(null);
            int selected = tabPane.getSelectedIndex();
            if (selected >= 0 && selected < tabbedPanes.size()) {
                tabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
            }
        }
    }

    @Override
    public void addRootNode(NodeGameGraphicsBase<?, C, S> node) {
        panelTree.addRootNode(node);
    }

    @Override
    public void clearRootNodes() {
        panelTree.clearRootNodes();
    }

    @Override
    public void setActiveWorkers(List<TreeWorker<C, S>> treeWorkers) {
        workerPanel.setWorkers(treeWorkers);
    }
}
