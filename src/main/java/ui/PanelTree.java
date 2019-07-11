package ui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.awt.TextRenderer;
import tree.TreeWorker;
import tree.node.NodeQWOPGraphicsBase;

import javax.swing.*;
import javax.vecmath.Vector3f;
import java.awt.*;
import java.awt.event.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pane for displaying the entire tree in OpenGL. Not part of the tabbed system.
 *
 * @author Matt
 */
public class PanelTree extends GLPanelGeneric implements IUserInterface.TabbedPaneActivator, GLEventListener, MouseListener,
        MouseMotionListener, MouseWheelListener, KeyListener, ActionListener {

    /**
     * For rendering text overlays. Large 36pt font. Note that {@link TextRenderer} is for overlays while GLUT is for
     * labels in world space.
     */
    private final TextRenderer textRenderBig = new TextRenderer(new Font("Calibri", Font.BOLD, 36));

    /**
     * For rendering text overlays. Small 18pt font. Note that {@link TextRenderer} is for overlays while GLUT is for
     * labels in world space.
     */
    private final TextRenderer textRenderSmall = new TextRenderer(new Font("Calibri", Font.PLAIN, 18));

    /**
     * Tree root nodes associated with this interface.
     */
    private ArrayList<NodeQWOPGraphicsBase<?>> rootNodes = new ArrayList<>();

    /**
     * Currently selected {@link NodeQWOPGraphicsBase} on the tree.
     */
    private NodeQWOPGraphicsBase<?> selectedNode;

    /**
     * Games played per second
     */
    private int gps = 0;

    /**
     * Continuously update the estimate of the display loop time in milliseconds.
     */
    private long avgLoopTime = (long) 1000f / 30; // Initial guess doesn't matter too much.

    /**
     * Last system draw time in milliseconds.
     */
    private long lastIterTime = System.currentTimeMillis();

    /**
     * Number of games played at last drawing. Used to estimate games played per second.
     */
    private long lastGamesPlayed = 0;

    /**
     * Keep track of whether we sent a pause request to the tree.
     */
    private boolean treePause = false;

    /**
     * Currently tracked mouse x location in screen coordinates of the panel
     */
    private int mouseX;

    /**
     * Currently tracked mouse x location in screen coordinates of the panel
     */
    private int mouseY;

    /**
     * Is the mouse cursor inside the bounds of the tree panel?
     */
    private boolean mouseInside = false;

    /**
     * Keep track of observers who are listening for selection of nodes on the tree. May not contain duplicates.
     */
    private Set<NodeSelectionListener> nodeSelectionListeners = new HashSet<>();

    /**
     * Button for resetting the camera view.
     */
    private JButton resetButton;

    private int [] aiVertexBufferIndices = new int [] {-1};

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
        resetButton.setBackground(new Color(255, 255, 255, 100));
        add(resetButton);
    }

    /**
     * Add a listener for node selections. Multiple listeners may be added and each will be called on an event. There
     * may not be duplicate listeners.
     *
     * @param listener {@link NodeSelectionListener} to add to the tree visualizer.
     */
    void addNodeSelectionListener(NodeSelectionListener listener) {
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
    private void selectNode(NodeQWOPGraphicsBase<?> node) {
        selectedNode = node;
        for (NodeSelectionListener listener : nodeSelectionListeners) {
            listener.nodeSelected(node);
        }
    }

    /**
     * Add the root of a tree for drawing. Note that {@link NodeQWOPGraphicsBase} locations are dictated by {@link NodeQWOPGraphicsBase#nodeLocation}.
     *
     * @param node A root node whose tree we want to draw. Does not literally need to be a zero-depth tree root, just
     *             some node with children we want to draw.
     */
    public void addRootNode(NodeQWOPGraphicsBase<?> node) {
        rootNodes.add(node);
    }

    /**
     * Remove all root nodes from the list to draw. Will result in no trees being drawn.
     */
    public void clearRootNodes() {
        rootNodes.clear();
    }

    @Override
    public void activateTab() {}

    @Override
    public void deactivateTab() {}

    private long lastUpdateTree = System.currentTimeMillis();
    @Override
    public void display(GLAutoDrawable drawable) {
        if (treePause) {
            textRenderBig.beginRendering(panelWidth, panelHeight);
            textRenderBig.setColor(0.7f, 0.7f, 0.7f, 1.0f);
            textRenderBig.setColor(0.7f, 0.1f, 0.1f, 1.0f);
            textRenderBig.draw("DRAWING PAUSED", panelWidth / 2 - 150, panelHeight - 50);
            textRenderBig.endRendering();
            return;
        }

        super.display(drawable);

        final float ptSize = Math.min(50f / cam.getZoomFactor(), 10f); //Let the points be smaller/bigger depending on
        // zoom, but make sure to cap out the size!

//        for (NodeQWOPGraphicsBase<?> node : rootNodes) {
//            node.drawLinesBelow(gl);
////            node.recurseDownTreeInclusive( n -> {
////                n.drawLabel(gl, glut);
////
////                gl.glColor3f(1f, 0.1f, 0.1f);
////                gl.glPointSize(ptSize);
////
////                gl.glBegin(GL.GL_POINTS);
////                n.drawPoint(gl); // Recurses through the whole tree.
////                gl.glEnd();
////
////                gl.glColor3f(1f, 1f, 1f);
////                gl.glBegin(GL.GL_LINES);
////                n.drawLine(gl); // Recurses through the whole tree.
////                gl.glEnd();
////            });
//        }


        List<NodeQWOPGraphicsBase<?>> nodeList = new ArrayList<>();
        for (NodeQWOPGraphicsBase<?> node : rootNodes) {
            node.recurseDownTreeExclusive(nodeList::add);
        }
//        if (nodeList.size() > 1) {
        if (System.currentTimeMillis() - this.lastUpdateTree > 100 * (float)(avgLoopTime * avgLoopTime)/ (25f * 25f)) {
            aiVertexBufferIndices[0] = -1;
            int[] aiNumOfVertices = createAndFillVertexBuffer(gl, nodeList);
            lastUpdateTree = System.currentTimeMillis();
        }
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, aiVertexBufferIndices[0]);
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
            gl.glVertexPointer(3, GL.GL_FLOAT, 6 * Buffers.SIZEOF_FLOAT, 0);
            gl.glColorPointer(3, GL.GL_FLOAT, 6 * Buffers.SIZEOF_FLOAT, 3 * Buffers.SIZEOF_FLOAT);
            gl.glDrawArrays(GL2.GL_LINES, 0, nodeList.size() * 2);
            gl.glDrawArrays(GL2.GL_POINTS, nodeList.size() * 2, nodeList.size() * 3);
            // disable arrays once we're done
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
//        }

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

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (!treePause)
            cam.smoothZoom(1f + (float) e.getPreciseWheelRotation(), 3);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!treePause) {
            Vector3f relCamMove = cam.windowFrameToWorldFrameDiff(e.getX(), e.getY(), mouseX, mouseY);
            cam.smoothTranslateRelative(relCamMove, relCamMove, 5);
            mouseX = e.getX();
            mouseY = e.getY();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!treePause && e.isControlDown()) {

            // Get all nodes below all roots.
            List<NodeQWOPGraphicsBase<?>> nodesBelow = new ArrayList<>();
            for (NodeQWOPGraphicsBase<?> node : rootNodes) {
                node.recurseDownTreeInclusive(nodesBelow::add);
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
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (mouseInside) {
            //Navigating the focused node tree
            int keyCode = e.getKeyCode();

            if (keyCode == KeyEvent.VK_P) // Pause graphics updates.
                treePause = !treePause;

            if (!treePause) {
                if (e.isAltDown()) {
                    switch (keyCode) {
                        case KeyEvent.VK_UP:
                            cam.smoothRotateLong(0.1f, 5);
                            break;
                        case KeyEvent.VK_DOWN:
                            cam.smoothRotateLong(-0.1f, 5);
                            break;
                        case KeyEvent.VK_LEFT:
                            cam.smoothRotateLat(0.1f, 5);
                            break;
                        case KeyEvent.VK_RIGHT:
                            cam.smoothRotateLat(-0.1f, 5);
                            break;
                        default:
                            // Nothing.
                            break;
                    }
                } else if (e.isShiftDown()) {
                    switch (keyCode) {
                        case KeyEvent.VK_LEFT:
                            cam.smoothTwist(0.1f, 5);
                            break;
                        case KeyEvent.VK_RIGHT:
                            cam.smoothTwist(-0.1f, 5);
                            break;
                        case KeyEvent.VK_ESCAPE:
                            System.exit(0);
                            break;
                        default:
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
                        default:
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

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
    public void mouseReleased(MouseEvent e) {}

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
     */
    private void arrowSwitchNode(int direction, int depth) {

        if (selectedNode != null) { // Do nothing if no node is selected to begin with.

            if (selectedNode.getTreeDepth() != 0) {
                int thisIndex = selectedNode.getIndexAccordingToParent();
                //This set of logicals eliminates the edge cases, then takes the proposed action as default
                if (thisIndex == 0 && direction == -1) { //We're at the lowest index of this node and must head
                    // to a new parent node.
                    ArrayList<NodeQWOPGraphicsBase<?>> blacklist = new ArrayList<>(); //Keep a blacklist of nodes that already
                    // proved to be duds.
                    blacklist.add(selectedNode);
                    nextOver(selectedNode.getParent(), blacklist, 1, direction,
                            selectedNode.getIndexAccordingToParent(), 0);
                } else if (thisIndex == selectedNode.getSiblingCount() && direction == 1) { //We're at
                    // the highest index of this node and must head to a new parent node.
                    ArrayList<NodeQWOPGraphicsBase<?>> blacklist = new ArrayList<>();
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
     * +1 or -1 direction left/right -- TODO this is an old mess.
     */
    private boolean nextOver(NodeQWOPGraphicsBase<?> current, ArrayList<NodeQWOPGraphicsBase<?>> blacklist, int deficitDepth, int direction,
                             int prevIndexAbove, int numTimesTried) { // numTimesTried added to prevent some really
        // deep node for causing some really huge search through the whole tree. If we don't succeed in a handful
        // of iterations, just fail quietly.
        numTimesTried++;
        boolean success;

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
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isActive() {
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Reset the camera view back to initial view.
        if (e.getSource().equals(resetButton)) {
            cam = new GLCamManager(panelWidth, panelHeight);
        }
    }

    // Altered from: https://wadeawalker.wordpress.com/2010/10/17/tutorial-faster-rendering-with-vertex-buffer-objects/
    private int [] createAndFillVertexBuffer(GL2 gl2, List<NodeQWOPGraphicsBase<?>> nodeList) {

        int [] aiNumOfVertices = new int [] {nodeList.size() * 3}; // Enough space for two vertices for
        // lines and one vertex for a point for each line (even if not all are used).

        // create vertex buffer object if needed
        if( aiVertexBufferIndices[0] == -1 ) {
            // check for VBO support
            if(    !gl2.isFunctionAvailable( "glGenBuffers" )
                    || !gl2.isFunctionAvailable( "glBindBuffer" )
                    || !gl2.isFunctionAvailable( "glBufferData" )
                    || !gl2.isFunctionAvailable( "glDeleteBuffers" ) ) {
                throw new RuntimeException( "Vertex buffer objects not supported." );
            }

            gl2.glGenBuffers( 1, aiVertexBufferIndices, 0 );

            // create vertex buffer data store without initial copy
            gl2.glBindBuffer( GL.GL_ARRAY_BUFFER, aiVertexBufferIndices[0] );
            gl2.glBufferData( GL.GL_ARRAY_BUFFER,
                    aiNumOfVertices[0] * 3 * Buffers.SIZEOF_FLOAT * 2,
                    null,
                    GL2.GL_DYNAMIC_DRAW );
        }

        // map the buffer and write vertex and color data directly into it
        gl2.glBindBuffer( GL.GL_ARRAY_BUFFER, aiVertexBufferIndices[0] );
        ByteBuffer bytebuffer = gl2.glMapBuffer( GL.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY );
        FloatBuffer floatbuffer = bytebuffer.order( ByteOrder.nativeOrder() ).asFloatBuffer();

        for( NodeQWOPGraphicsBase<?> node : nodeList ) {
            node.addLineToBuffer(floatbuffer);
        }
        for( NodeQWOPGraphicsBase<?> node : nodeList ) {
            node.addPointToBuffer(floatbuffer);
        }

        gl2.glUnmapBuffer( GL.GL_ARRAY_BUFFER );

        return aiNumOfVertices;
    }
}
