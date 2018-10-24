package ui;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import tree.Node;
import tree.TreeWorker;

import javax.swing.*;
import javax.vecmath.Vector3f;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Pane for displaying the entire tree in OpenGL. Not part of the tabbed system.
 *
 * @author Matt
 */
public class PanelTree extends GLPanelGeneric implements IUserInterface.TabbedPaneActivator, GLEventListener, MouseListener,
        MouseMotionListener, MouseWheelListener, KeyListener, ActionListener {

    /**
     * For rendering text overlays. Note that {@link TextRenderer} is for overlays while GLUT is for labels in world
     * space.
     **/
    private final TextRenderer textRenderBig = new TextRenderer(new Font("Calibri", Font.BOLD, 36));
    private final TextRenderer textRenderSmall = new TextRenderer(new Font("Calibri", Font.PLAIN, 18));

    /**
     * Tree root nodes associated with this interface.
     **/
    private ArrayList<Node> rootNodes = new ArrayList<>();

    /**
     * Currently selected {@link Node} on the tree.
     */
    private Node selectedNode;

    /**
     * Games played per second
     **/
    private int gps = 0;

    /**
     * Continuously update the estimate of the display loop time in milliseconds.
     **/
    private long avgLoopTime = (long) 1000f / 30; // Initial guess doesn't matter too much.
    private long lastIterTime = System.currentTimeMillis();
    private long lastGamesPlayed = 0;

    /**
     * Keep track of whether we sent a pause request to the tree.
     **/
    private boolean treePause = false; // TODO this doesn't work yet.

    /**
     * Currently tracked mouse x location in screen coordinates of the panel
     **/
    private int mouseX;

    /**
     * Currently tracked mouse x location in screen coordinates of the panel
     **/
    private int mouseY;

    /**
     * Is the mouse cursor inside the bounds of the tree panel?
     **/
    private boolean mouseInside = false;

    /**
     * Keep track of observers who are listening for selection of nodes on the tree. May not contain duplicates.
     */
    private Set<NodeSelectionListener> nodeSelectionListeners = new HashSet<>();

    /**
     * Button for resetting the camera view.
     */
    private JButton resetButton;

    public PanelTree() {
        super();
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.LEFT); // So the button goes to the top left corner.
        setLayout(layout);

        // Button for resetting the camera to default position/target.
        resetButton = new JButton("Reset camera");
        resetButton.setToolTipText("Reset the camera view back to the initial view if you're lost.");
        resetButton.addActionListener(this);
        resetButton.setBackground(new Color(255,255,255,100));
        
        add(resetButton);
    }

    /**
     * Add a listener for node selections. Multiple listeners may be added and each will be called on an event. There
     * may not be duplicate listeners.
     *
     * @param listener {@link NodeSelectionListener} to add to the tree visualizer.
     */
    public void addNodeSelectionListener(NodeSelectionListener listener) {
        if (nodeSelectionListeners.contains(listener))
            throw new IllegalArgumentException("The tree panel already has this listener assigned!");
        nodeSelectionListeners.add(listener);
    }

    /**
     * Remove a node selection listener. Others may still remain after one is removed. An exception will occur if the
     * listener does not exist.
     *
     * @param listener {@link NodeSelectionListener} to remove from the tree visualizer.
     */
    public void removeNodeSelectionListener(NodeSelectionListener listener) {
        if (nodeSelectionListeners.contains(listener))
            nodeSelectionListeners.remove(listener);
        else
            throw new IndexOutOfBoundsException("Cannot remove a listener which was not assigned in the first place.");
    }

    /**
     * Node selected by the UI. Send out notifications to all the listeners.
     *
     * @param node Selected node that will be broadcast to listeners.
     */
    private void selectNode(Node node) {
        selectedNode = node;
        for (NodeSelectionListener listener : nodeSelectionListeners) {
            listener.nodeSelected(node);
        }
    }

    @Override
    public void activateTab() {
    }

    @Override
    public void deactivateTab() {
    }

    public void addRootNode(Node node) {
        rootNodes.add(node);
    }

    public void clearRootNodes() {
        rootNodes.clear();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        super.display(drawable);

        float ptSize = 50f / cam.getZoomFactor(); //Let the points be smaller/bigger depending on zoom, but make
        // sure to cap out the size!
        ptSize = Math.min(ptSize, 10f);

        for (Node node : rootNodes) {
            gl.glColor3f(1f, 0.1f, 0.1f);
            gl.glPointSize(ptSize);

            gl.glBegin(GL.GL_POINTS);
            node.drawNodes_below(gl); // Recurses through the whole tree.
            gl.glEnd();

            gl.glColor3f(1f, 1f, 1f);
            gl.glBegin(GL.GL_LINES);
            node.drawLines_below(gl); // Recurses through the whole tree.
            gl.glEnd();
        }

        // Draw games played and games/sec in upper left.
        textRenderBig.beginRendering(panelWidth, panelHeight);
        textRenderBig.setColor(0.7f, 0.7f, 0.7f, 1.0f);
        //tmp remove textRenderBig.draw(negotiator.getGamesPlayed() + " games", 20, panelHeight - 50);

        if (treePause) { // TODO Fix pausing.
            textRenderBig.setColor(0.7f, 0.1f, 0.1f, 1.0f);
            textRenderBig.draw("PAUSED", panelWidth / 2, panelHeight - 50);
        }
        textRenderBig.endRendering();

        /*
         * Filter the average loop time. Lower numbers gives more weight to the lower estimate, higher numbers
         * gives more
         * weight to the old value.
         */
        float loopTimeFilter = 8;
        avgLoopTime =
                (long) (((loopTimeFilter - 1f) * avgLoopTime + 1f * (System.currentTimeMillis() - lastIterTime)) / loopTimeFilter); // Filter the loop time

        // Draw the FPS of the tree drawer at the moment.
        textRenderSmall.beginRendering(panelWidth, panelHeight);
        textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);

        int dekafps = (int) (10000. / avgLoopTime); // Only multiplying by 10000 instead of 1000 to make decimal
        // places as desired.
        textRenderSmall.draw(((Math.abs(dekafps) > 10000) ? "???" : dekafps / 10f) + " FPS", panelWidth - 75,
                panelHeight - 20);

        // Number of imported games
        textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);

        // Total games played
        textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);
        long totalTimestepsSimulated = TreeWorker.getTotalTimestepsSimulated();
        long totalGamesPlayed = TreeWorker.getTotalGamesPlayed();
        textRenderSmall.draw(totalGamesPlayed + " total games", 20, panelHeight - 85);

        textRenderSmall.draw(Math.round(totalTimestepsSimulated / 9000f) / 10f + " hours simulated!", 20,
                panelHeight - 100);

        textRenderSmall.setColor(0.1f, 0.7f, 0.1f, 1.0f);
        textRenderSmall.draw(Math.round(totalTimestepsSimulated / (double) totalGamesPlayed * 0.4) / 10f + "s Avg" +
                ". game length", 20, panelHeight - 115);

        textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);
        gps =
                (int) ((gps * (loopTimeFilter - 1f) + 1000f * (totalGamesPlayed - lastGamesPlayed) / (System.currentTimeMillis() - lastIterTime)) / loopTimeFilter);
        textRenderSmall.draw(gps + " games/s", 20, panelHeight - 130);

        textRenderSmall.setColor(0.1f, 0.7f, 0.1f, 1.0f);
        textRenderSmall.draw(Runtime.getRuntime().totalMemory() / 1000000 + "MB used", 20, panelHeight - 145);
        textRenderSmall.draw(Runtime.getRuntime().maxMemory() / 1000000 + "MB max", 20, panelHeight - 160);
        textRenderSmall.endRendering();
        lastGamesPlayed = totalGamesPlayed;
        lastIterTime = System.currentTimeMillis();
    }

    /**
     * Draw a text string using GLUT (for openGL rendering version of my stuff)
     **/
    public void drawString(String toDraw, float x, float y, float z, GL2 gl, GLUT glut) {
        // Format numbers with Java.
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);

        // Printing fonts, letters and numbers is much simpler with GLUT.
        // We do not have to use our own bitmap for the font.
        gl.glRasterPos3d(x, y, z);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, toDraw);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        cam.smoothZoom(1f + (float)e.getPreciseWheelRotation(), 3);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Vector3f relCamMove = cam.windowFrameToWorldFrameDiff(e.getX(), e.getY(), mouseX, mouseY);
        cam.smoothTranslateRelative(relCamMove, relCamMove, 5);
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.isControlDown()) {

            // Get all nodes below all roots.
            List<Node> nodesBelow = new ArrayList<>();
            for (Node node : rootNodes) {
                node.getNodesBelow(nodesBelow, true);
            }
            selectNode(cam.nodeFromClick_set(e.getX(), e.getY(), nodesBelow));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (mouseInside) {
            //Navigating the focused node tree
            int keyCode = e.getKeyCode();

            if (e.isMetaDown()) { //if we're using GL, then we'll move the camera with mac key + arrows
                switch (keyCode) {
                    case KeyEvent.VK_UP: //Go out the branches of the tree

                        cam.smoothRotateLong(0.1f, 5);
                        break;
                    case KeyEvent.VK_DOWN: //Go back towards root one level
                        cam.smoothRotateLong(-0.1f, 5);
                        break;
                    case KeyEvent.VK_LEFT: //Go left along an isobranch (like that word?)
                        cam.smoothRotateLat(0.1f, 5);
                        break;
                    case KeyEvent.VK_RIGHT: //Go right along an isobranch
                        cam.smoothRotateLat(-0.1f, 5);
                        break;
                    case KeyEvent.VK_S: // toggle the score text at the end of all branches
                        //TODO
                        break;
                }
            } else if (e.isShiftDown()) {
                switch (keyCode) {
                    case KeyEvent.VK_LEFT: //Go left along an isobranch (like that word?)
                        cam.smoothTwist(0.1f, 5);
                        break;
                    case KeyEvent.VK_RIGHT: //Go right along an isobranch
                        cam.smoothTwist(-0.1f, 5);
                        break;
                }
            } else {
                switch (keyCode) {
                    case KeyEvent.VK_UP: //Go out the branches of the tree
                        arrowSwitchNode(0, 1);
                        break;
                    case KeyEvent.VK_DOWN: //Go back towards root one level
                        arrowSwitchNode(0, -1);
                        break;
                    case KeyEvent.VK_LEFT: //Go left along an isobranch (like that word?)
                        arrowSwitchNode(1, 0);
                        break;
                    case KeyEvent.VK_RIGHT: //Go right along an isobranch
                        arrowSwitchNode(-1, 0);
                        break;
                    case KeyEvent.VK_P: //Pause everything except for graphics updates
                        treePause = !treePause;
                        if (treePause) {
                            rootNodes.get(0).calcNodePosBelow();
                        }
                        break;
                    case KeyEvent.VK_C:
                        //tmp remove negotiator.redistributeNodes();
                        break;
                    case KeyEvent.VK_B:
                        //tmp remove negotiator.toggleSampler();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        //System.exit(0);
                        break;

                    case KeyEvent.VK_SPACE:
//						if (runnerPanel.isActive()) {
//							runnerPanel.pauseToggle();
//						}
                        break;
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // These be hax. TODO
//        mouseX = e.getX();
//        mouseY = e.getY();
//        // If the snapshot pane is displaying stuff, this lets us potentially select some of the future nodes
//        // displayed in the snapshot pane.
//        if (mouseInside) {
//        }
//        for (IUserInterface.TabbedPaneActivator pane : allTabbedPanes) {
//            if (pane.isActive() && pane.getClass().equals(PanelRunner_Snapshot.class)) {
//                PanelRunner_Snapshot snapshotPane = (PanelRunner_Snapshot) pane;
//
//                List<Node> snapshotLeaves = snapshotPane.getDisplayedLeaves();
//                if (snapshotLeaves.size() > 0) {
//                    Node nearest = cam.nodeFromClick_set(mouseX, mouseY, snapshotLeaves);
//                    if (nearest != null) {
//                        snapshotPane.giveSelectedFuture(nearest);
//                    } else {
//                        snapshotPane.giveSelectedFuture(null); // clear it out if the mouse is too far away from
//                        // selectable nodes.
//                    }
//                }
//                break;
//            }
//        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseInside = true;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseInside = false;
    }

    // The following 2 methods are probably too complicated. when you push the arrow at the edge of one branch,
    // this tries to jump to the nearest next branch node at the same depth.

    /**
     * Called by key listener to change our focused node to the next adjacent one in the +1 or -1 direction
     **/
    private void arrowSwitchNode(int direction, int depth) {

        if (selectedNode != null) { // Do nothing if no node is selected to begin with.

            if (selectedNode.getTreeDepth() != 0) {
                int thisIndex = selectedNode.getIndexAccordingToParent();
                //This set of logicals eliminates the edge cases, then takes the proposed action as default
                if (thisIndex == 0 && direction == -1) { //We're at the lowest index of this node and must head
                    // to a new parent node.
                    ArrayList<Node> blacklist = new ArrayList<>(); //Keep a blacklist of nodes that already
                    // proved to be duds.
                    blacklist.add(selectedNode);
                    nextOver(selectedNode.getParent(), blacklist, 1, direction,
                            selectedNode.getIndexAccordingToParent(), 0);

                } else if (thisIndex == selectedNode.getSiblingCount() && direction == 1) { //We're at
                    // the highest index of this node and must head to a new parent node.
                    ArrayList<Node> blacklist = new ArrayList<>();
                    blacklist.add(selectedNode);
                    nextOver(selectedNode.getParent(), blacklist, 1, direction,
                            selectedNode.getIndexAccordingToParent(), 0);

                } else { //Otherwise we can just switch nodes within the scope of this parent.
                    selectNode(selectedNode.getParent().getChildByIndex(thisIndex + direction));
                }
            }

            //These logicals just take the proposed motion (or not) and ignore any edges.
            if (depth == 1 && selectedNode.getChildCount() > 0) { //Go further down the tree if this node has
                // children
                selectNode(selectedNode.getChildByIndex(0));
            } else if (depth == -1 && selectedNode.getTreeDepth() > 0) { //Go up the tree if this is not root.
                selectNode(selectedNode.getParent());
            }
            repaint();
        }
    }

    /**
     * Take a node back a layer. Don't return to node past. Try to go back out by the deficit depth amount in the
     * +1 or -1 direction left/right
     **/
    private boolean nextOver(Node current, ArrayList<Node> blacklist, int deficitDepth, int direction,
                             int prevIndexAbove, int numTimesTried) { // numTimesTried added to prevent some really
        // deep node for causing some really huge search through the whole tree. If we don't succeed in a handful
        // of iterations, just fail quietly.
        numTimesTried++;
        boolean success = false;

        //TERMINATING CONDITIONS-- fail quietly if we get back to root with nothing. Succeed if we get back to
        // the same depth we started at.
        if (deficitDepth == 0) { //We've successfully gotten back to the same level. Great.
            selectNode(current);
            return true;
        } else if (current.getTreeDepth() == 0) {
            return true; // We made it back to the tree's root without any success. Just return.

        } else if (numTimesTried > 100) {// If it takes >100 movements between nodes, we'll just give up.
            return true;
        } else {
            //CONDITIONS WE NEED TO STEP BACKWARDS TOWARDS ROOT.
            //If this new node has no children OR it's 1 child is on the blacklist, move back up the tree.
            if ((prevIndexAbove + 1 == current.getChildCount() && direction == 1) || (prevIndexAbove == 0 && direction == -1)) {
                blacklist.add(current);
                success = nextOver(current.getParent(), blacklist, deficitDepth + 1, direction,
                        current.getIndexAccordingToParent(), numTimesTried); //Recurse back another node.
            } else if (!(current.getChildCount() > 0) || (blacklist.contains(current.getChildByIndex(0)) && current.getChildCount() == 1)) {
                blacklist.add(current);
                success = nextOver(current.getParent(), blacklist, deficitDepth + 1, direction,
                        current.getIndexAccordingToParent(), numTimesTried); //Recurse back another node.
            } else {

                //CONDITIONS WE NEED TO GO DEEPER:
                if (direction == 1) { //March right along this previous node.
                    for (int i = prevIndexAbove + 1; i < current.getChildCount(); i++) {
                        success = nextOver(current.getChildByIndex(i), blacklist, deficitDepth - 1, direction, -1,
                                numTimesTried);
                        if (success) {
                            return true;
                        }
                    }
                } else if (direction == -1) { //March left along this previous node
                    for (int i = prevIndexAbove - 1; i >= 0; i--) {
                        success = nextOver(current.getChildByIndex(i), blacklist, deficitDepth - 1, direction,
                                current.getChildByIndex(i).getChildCount(), numTimesTried);
                        if (success) {
                            return true;
                        }
                    }
                }
            }
        }
        success = true;
        return success;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void update(Node node) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Reset the camera view back to initial view.
        if (e.getSource().equals(resetButton)) {
            cam = new GLCamManager(panelWidth, panelHeight);
        }
    }
}
