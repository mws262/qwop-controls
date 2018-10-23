package ui;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tree.Node;


/**
 * All UI stuff happens here and most of the analysis that individual panes show happens here too.
 *
 * @author Matt
 */
public class UI_Full extends JFrame implements ChangeListener, NodeSelectionListener, Runnable, IUserInterface {

    /**
     * Thread loop running?
     **/
    private boolean running = true;

    /**
     * Verbose printing?
     **/
    public boolean verbose = false;

    /**
     * Tree root nodes associated with this interface.
     **/
    private ArrayList<Node> rootNodes = new ArrayList<>();

    /**
     * Individual pane for the tree.
     **/
    private PanelTree panelTree;

    /**
     * Pane for the tabbed side of the interface.
     **/
    private JTabbedPane tabPane;

    /**
     * Selected node by user click/key
     **/
    private Node selectedNode;

    /**
     * List of panes which can be activated, deactivated.
     **/
    private ArrayList<TabbedPaneActivator> allTabbedPanes = new ArrayList<>(); //List of all panes
    // in the tabbed part

    /**
     * Window width
     **/
    public static int windowWidth = 1920;

    /**
     * Window height
     **/
    public static int windowHeight = 1000;

    /**
     * Attempted frame rate
     **/
    private int FPS = 25;

    /**
     * Usable milliseconds per frame
     **/
    private long MSPF = (long) (1f / FPS * 1000f);

    public UI_Full() {
        Container pane = getContentPane();

        /* Tabbed panes */
        tabPane = new JTabbedPane();
        tabPane.setBorder(BorderFactory.createRaisedBevelBorder());
        tabPane.setPreferredSize(new Dimension(1080, 250));
        tabPane.setMinimumSize(new Dimension(100, 1));
        tabPane.addChangeListener(this);

        /* TREE PANE */
        panelTree = new PanelTree();
        panelTree.addNodeSelectionListener(this); // Add this UI as a listener for selections of nodes on the tree.
//        panelTree.setBorder(BorderFactory.createRaisedBevelBorder());

        // This makes it have that dragable border between the tab and the tree sections.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelTree, tabPane);
        splitPane.setResizeWeight(0.7);
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
    }

    /**
     * Add a new tab to this frame.
     **/
    public void addTab(TabbedPaneActivator newTab, String name) {
        tabPane.addTab(name, (Component) newTab);
        allTabbedPanes.add(newTab);
        tabPane.revalidate();

        //Make sure the currently active tab is actually being updated.
        allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
    }

    /**
     * Add a new tab to this frame.
     **/
    public void removeTab(JPanel tabToRemove) {
        tabPane.remove(tabToRemove);
        allTabbedPanes.remove(tabToRemove);

        //Make sure the currently active tab is actually being updated.
        allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
    }

    /* (non-Javadoc)
     * @see ui.IUserInterface#run()
     */
    @Override
    public void run() {
        while (running) {
            long currentTime = System.currentTimeMillis();
            repaint();

            long extraTime = MSPF - (System.currentTimeMillis() - currentTime);
            if (extraTime > 5) {
                try {
                    Thread.sleep(extraTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see ui.IUserInterface#kill()
     */
    @Override
    public void kill() {
        running = false;
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    @Override
    public void nodeSelected(Node selected) {
        if (selectedNode != null) { // Clear things from the old selected node.
            selectedNode.displayPoint = false;
            selectedNode.clearBranchColor();
            selectedNode.clearBranchZOffset();
        }
        selectedNode = selected;
        selectedNode.displayPoint = true;
        selectedNode.nodeColor = Color.RED;
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
    public void addRootNode(Node node) {
        rootNodes.add(node);
        panelTree.addRootNode(node);
    }

    @Override
    public void clearRootNodes() {
        rootNodes.clear();
        panelTree.clearRootNodes();
    }
}
