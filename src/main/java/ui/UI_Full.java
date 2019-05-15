package ui;

import tree.NodeQWOPGraphicsBase;
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
public class UI_Full extends JFrame implements ChangeListener, NodeSelectionListener, Runnable, IUserInterface {

    /**
     * Thread loop running?
     */
    private boolean running = true;

    /**
     * Verbose printing?
     */
    public boolean verbose = false;

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
    private ArrayList<TabbedPaneActivator> allTabbedPanes = new ArrayList<>();

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
        Container pane = getContentPane();

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

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.setPreferredSize(new Dimension(windowWidth, windowHeight));
        this.setContentPane(getContentPane());

        // Add toolbar icon.
        Toolkit kit = Toolkit.getDefaultToolkit();
        Image img = kit.createImage("src/main/resources/icons/QWOP_tree_ico.png");
        setIconImage(img);

        this.pack();
        this.setVisible(true);

        Utility.showOnScreen(this, 0, false); // Choose the monitor to display on, filling that monitor.
    }

    /**
     * Add a new tab to this frame's set of tabbed panels.
     *
     * @param newTab New tab to add to the existing set of tabbed panels in this frame.
     * @param name Name of the tab to display on the tab itself.
     */
    public void addTab(TabbedPaneActivator newTab, String name) {
        tabPane.addTab(name, (Component) newTab);
        allTabbedPanes.add(newTab);
        tabPane.revalidate();

        //Make sure the currently active tab is actually being updated.
        allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
    }

    /**
     * Remove a specified UI element which is part of a tabbed set of panels.
     *
     * @param tabToRemove Tab to be removed from the set. Throws if the tab is not part of the group.
     */
    public void removeTab(TabbedPaneActivator tabToRemove) {
        if (allTabbedPanes.contains(tabToRemove))
            allTabbedPanes.remove(tabToRemove);
        else
            throw new IllegalArgumentException("Tried to remove a UI tab which did not exist.");

        //Make sure the currently active tab is actually being updated.
        allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
    }

    @Override
    public void run() {
        while (running) {
            long currentTime = System.currentTimeMillis();
            repaint();

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
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    @Override
    public void nodeSelected(NodeQWOPGraphicsBase<?> selected) {
        if (selectedNode != null) { // Clear things from the old selected node.
            selectedNode.displayPoint = false;
            selectedNode.clearBranchColor();
            selectedNode.clearBranchZOffset();
        }
        selectedNode = selected;
        selectedNode.displayPoint = true;

        selectedNode.setOverridePointColor(Color.RED);
        selectedNode.setBranchZOffset(0.4f);

        for (TabbedPaneActivator panel : allTabbedPanes) {
            if (panel.isActive()) {
                panel.update(selectedNode);
            }
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (!allTabbedPanes.isEmpty()) {
            for (TabbedPaneActivator p : allTabbedPanes) {
                p.deactivateTab();
            }
            allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
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
