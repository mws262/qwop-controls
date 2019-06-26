package ui;

import tree.node.NodeQWOPGraphicsBase;
import tree.Utility;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

/**
 * All UI stuff happens here and most of the analysis that individual panes show happens here too.
 *
 * @author Matt
 */
public class UI_Full implements ChangeListener, NodeSelectionListener, Runnable, IUserInterface {

    private final JFrame frame = new JFrame();

    /**
     * Thread loop running?
     */
    private boolean running = true;

    /**
     * Individual pane for the tree.
     */
    private PanelTree panelTree;

    /**
     * Pane for the tabbed side of the interface.
     */
    private JTabbedPane tabPane;

    /**
     * Selected node by user click/key
     */
    private NodeQWOPGraphicsBase<?> selectedNode;

    /**
     * List of panes which can be activated, deactivated.
     */
    public ArrayList<TabbedPaneActivator> tabbedPanes = new ArrayList<>();

    /**
     * Window width
     */
    public static int windowWidth = 1920;

    /**
     * Window height
     */
    public static int windowHeight = 1000;

    /**
     * Attempted frame rate
     */
    private int targetFramesPerSecond = 25;

    /**
     * Usable milliseconds per frame
     */
    private long millisecondsPerFrame = (long) (1f / targetFramesPerSecond * 1000f);

    public UI_Full() {
        Container pane = frame.getContentPane();

        /* Tabbed panes */
        tabPane = new JTabbedPane();
        tabPane.setBorder(BorderFactory.createRaisedBevelBorder());
        tabPane.setPreferredSize(new Dimension(1080, 150));
        tabPane.setMinimumSize(new Dimension(100, 1));
        tabPane.addChangeListener(this);

        /* Tree pane */
        panelTree = new PanelTree();
        panelTree.addNodeSelectionListener(this); // Add this UI as a listener for selections of nodes on the tree.

        // This makes it have that draggable border between the tab and the tree sections.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelTree, tabPane);
        splitPane.setResizeWeight(0.85);
        pane.add(splitPane);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setPreferredSize(new Dimension(windowWidth, windowHeight));
        frame.setContentPane(frame.getContentPane());

        // Add toolbar icon.
        Toolkit kit = Toolkit.getDefaultToolkit();
        Image img = kit.createImage("src/main/resources/icons/QWOP_tree_ico.png");
        frame.setIconImage(img);

        frame.pack();
        frame.setVisible(true);

        Utility.showOnScreen(frame, 0, false); // Choose the monitor to display on, filling that monitor.
    }

    /**
     * Add a new tab to this frame's set of tabbed panels.
     *
     * @param newTab New tab to add to the existing set of tabbed panels in this frame.
     */
    public void addTab(TabbedPaneActivator newTab) {
        tabPane.addTab(newTab.getName(), (Component) newTab);
        tabbedPanes.add(newTab);
        tabPane.revalidate();

        //Make sure the currently active tab is actually being updated.
        tabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
    }

    /**
     * Remove a specified UI element which is part of a tabbed set of panels.
     *
     * @param tabToRemove Tab to be removed from the set. Throws if the tab is not part of the group.
     */
    public void removeTab(TabbedPaneActivator tabToRemove) {
        if (tabbedPanes.contains(tabToRemove))
            tabbedPanes.remove(tabToRemove);
        else
            throw new IllegalArgumentException("Tried to remove a UI tab which did not exist.");

        //Make sure the currently active tab is actually being updated.
        tabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
    }

    @Override
    public void run() {
        while (running) {
            long currentTime = System.currentTimeMillis();
            frame.repaint();

            long extraTime = millisecondsPerFrame - (System.currentTimeMillis() - currentTime);
            if (extraTime > 5) {
                try {
                    Thread.sleep(extraTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void kill() {
        running = false;
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    @Override
    public void nodeSelected(NodeQWOPGraphicsBase<?> selected) {
        if (selectedNode != null) { // Clear things from the old selected node.
            selectedNode.displayPoint = false;
            selectedNode.clearBranchLineOverrideColor();
            selectedNode.clearBranchZOffset();
        }
        selectedNode = selected;
        selectedNode.reenableIfNotDrawnForSpeed(); // If the node was previously disabled to avoid drawing too many
        // lines, then put it back in since it is annoying selecting "invisible" nodes.
        selectedNode.displayPoint = true;
        selectedNode.setOverridePointColor(Color.RED);
        selectedNode.setBranchZOffset(0.4f);

        for (TabbedPaneActivator panel : tabbedPanes) {
            if (panel.isActive()) {
                panel.update(selectedNode);
            }
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (!tabbedPanes.isEmpty()) {
            for (TabbedPaneActivator p : tabbedPanes) {
                p.deactivateTab();
            }
            tabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
        }
    }

    @Override
    public void addRootNode(NodeQWOPGraphicsBase<?> node) {
        panelTree.addRootNode(node);
    }

    @Override
    public void clearRootNodes() {
        panelTree.clearRootNodes();
    }
}
