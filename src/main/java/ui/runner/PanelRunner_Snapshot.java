package ui.runner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Action;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.IStateQWOP.ObjectName;
import game.qwop.StateQWOP;
import game.state.IState;
import tree.node.NodeQWOPExplorableBase;
import tree.node.NodeQWOPGraphicsBase;
import tree.node.filter.INodeFilter;
import tree.node.filter.NodeFilter_Downsample;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays fixed shots of the runner at selected nodes. Can also preview the past and future from these nodes. A tab.
 *
 * @author Matt
 */
public class PanelRunner_Snapshot extends PanelRunner implements MouseListener, MouseMotionListener {
    /**
     * The node that is the current focus of this panel.
     */
    private NodeQWOPGraphicsBase<?, CommandQWOP> snapshotNode;

    /**
     * Filter to keep from drawing too many and killing the graphics speed.
     */
    private INodeFilter filter = new NodeFilter_Downsample(50);

    /**
     * Potentially, a future node selected by hovering over its runner to display a specific sequence of game.command in
     * all the displayed futures.
     */
    private NodeQWOPGraphicsBase<?, CommandQWOP> highlightedFutureMousedOver;

    /**
     * Externally selected version to be highlighted. Mostly just commanded by selecting tree nodes instead.
     */
    private NodeQWOPGraphicsBase<?, CommandQWOP> highlightedFutureExternal;

    private List<NodeQWOPGraphicsBase<?, CommandQWOP>> focusLeaves = new ArrayList<>();
    private List<IState> states = new ArrayList<>();
    private List<Stroke> strokes = new ArrayList<>();
    private List<Color> colors = new ArrayList<>();

    /**
     * Number of runner states in the past to display.
     */
    public int numHistoryStatesDisplay = 25;

    /**
     * How close do we have to be (squared) from the body of a single runner for it to be eligible for selection.
     */
    private float figureSelectThreshSq = 150;

    /**
     * Current mouse location.
     */
    private int mouseX = 0;
    private int mouseY = 0;

    /**
     * X offset in frame pixel coordinates determined by the focused body's x coordinate.
     */
    private int specificXOffset = 0;

    /**
     * Mouse currently over this panel?
     */
    private boolean mouseIsIn = false;

    private final String name;

    public PanelRunner_Snapshot(@JsonProperty("name") String name) {
        super();
        this.name = name;
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     * Assign a selected node for the snapshot pane to display.
     */
    @Override
    public void update(NodeQWOPGraphicsBase<?, CommandQWOP> node) {
        states.clear();
        focusLeaves.clear();
        strokes.clear();
        colors.clear();

        /* Focused node first */
        snapshotNode = node;
        specificXOffset = (int) (runnerScaling * snapshotNode.getState().getCenterX());
        states.add(snapshotNode.getState());
        strokes.add(boldStroke);
        colors.add(Color.BLACK);
        focusLeaves.add(node);

        /* History nodes */
        NodeQWOPGraphicsBase<?, CommandQWOP> historyNode = snapshotNode;
        for (int i = 0; i < numHistoryStatesDisplay; i++) {
            if (historyNode.getTreeDepth() > 0) {
                historyNode = historyNode.getParent();
                states.add(historyNode.getState());
                strokes.add(normalStroke);
                colors.add(ghostGray);
                focusLeaves.add(historyNode);
            }
        }

        /* Future leaf nodes */
        List<NodeQWOPGraphicsBase<?, CommandQWOP>> descendants = new ArrayList<>();
        for (int i = 0; i < snapshotNode.getChildCount(); i++) {
            NodeQWOPGraphicsBase<?, CommandQWOP> child = snapshotNode.getChildByIndex(i);

            child.recurseDownTreeInclusive(n->{
                if (n.getChildCount() == 0) {
                    descendants.add(n);
                }
            });
            filter.filter(descendants);

            Color runnerColor = NodeQWOPGraphicsBase.getColorFromTreeDepth(i * 10, NodeQWOPGraphicsBase.lineBrightnessDefault);
            child.setOverrideBranchColor(runnerColor); // Change the color on the tree too.


            for (NodeQWOPGraphicsBase<?, CommandQWOP> descendant : descendants) {
                focusLeaves.add(descendant);
                states.add(descendant.getState());
                strokes.add(normalStroke);
                colors.add(runnerColor);
            }
        }
    }

    /**
     * Draws the selected node state and potentially previous and future states.
     */
    @Override
    public void paintComponent(Graphics g) {
        if (!active) return;
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (snapshotNode != null && snapshotNode.getState() != null) {
            float bestSoFar = Float.MAX_VALUE;
            int bestIdx = Integer.MIN_VALUE;

            // Figure out if the mouse close enough to highlight one state.
            if (mouseIsIn && mouseX > getWidth() / 2) { // If we are mousing over this panel, see if we're hovering
                // close enough over any particular dude state.

                // Check body first
                for (int i = 0; i < focusLeaves.size(); i++) {
                    float distSq = getDistFromMouseSq(focusLeaves.get(i).getState().getCenterX(),
                            // TODO fix cast
                            ((StateQWOP) focusLeaves.get(i).getState()).getStateVariableFromName(ObjectName.BODY).getY());
                    if (distSq < bestSoFar && distSq < figureSelectThreshSq) {
                        bestSoFar = distSq;
                        bestIdx = i;
                    }
                }
                // Then head
                if (bestIdx < 0) { // Only goes to this if we didn't find a near-enough torso.
                    for (int i = 0; i < focusLeaves.size(); i++) {
                        float distSq = getDistFromMouseSq(focusLeaves.get(i).getState().getCenterX(),
                                // TODO fix cast
                                ((StateQWOP) focusLeaves.get(i).getState()).getStateVariableFromName(ObjectName.HEAD).getY());
                        if (distSq < bestSoFar && distSq < figureSelectThreshSq) {
                            bestSoFar = distSq;
                            bestIdx = i;
                        }
                    }
                }
                // Then both feet equally
                if (bestIdx < 0) { // Only goes to this if we didn't find a near-enough torso OR head.
                    for (int i = 0; i < focusLeaves.size(); i++) {
                        float distSq =
                                getDistFromMouseSq(((StateQWOP) focusLeaves.get(i).getState()).getStateVariableFromName(ObjectName.LFOOT).getX(),
                                        ((StateQWOP) focusLeaves.get(i).getState()).getStateVariableFromName(ObjectName.LFOOT).getY());
                        if (distSq < bestSoFar && distSq < figureSelectThreshSq) {
                            bestSoFar = distSq;
                            bestIdx = i;
                        }
                        // TODO fix cast
                        distSq =
                                getDistFromMouseSq(((StateQWOP) focusLeaves.get(i).getState()).getStateVariableFromName(ObjectName.RFOOT).getX(),
                                        ((StateQWOP) focusLeaves.get(i).getState()).getStateVariableFromName(ObjectName.RFOOT).getY());
                        if (distSq < bestSoFar && distSq < figureSelectThreshSq) {
                            bestSoFar = distSq;
                            bestIdx = i;
                        }
                    }
                }
            }

            // Draw all non-highlighted runners.
            for (int i = states.size() - 1; i >= 0; i--) {
                if (!mouseIsIn || bestIdx != i) {
                    Color nextRunnerColor =
                            (highlightedFutureMousedOver != null && focusLeaves.get(i).getTreeDepth() > snapshotNode.getTreeDepth()) ? colors.get(i).brighter() : colors.get(i); // Make the nodes after the selected one lighter if one is highlighted.
                    GameQWOP.drawExtraRunner(g2, states.get(i), "", runnerScaling, xOffsetPixels - specificXOffset,
                            yOffsetPixels, nextRunnerColor, strokes.get(i));
                }
            }

            // TODO reintroduce this with new nodes.
            // Change things if one runner is selected.
            if (mouseIsIn && bestIdx >= 0) {
                NodeQWOPGraphicsBase<?, CommandQWOP> newHighlightNode = focusLeaves.get(bestIdx);
                changeFocusedFuture(g2, highlightedFutureMousedOver, newHighlightNode);
                highlightedFutureMousedOver = newHighlightNode;

                // Externally commanded pick, instead of mouse-picked.
            } else if (highlightedFutureExternal != null) {
                changeFocusedFuture(g2, highlightedFutureMousedOver, highlightedFutureExternal);
                highlightedFutureMousedOver = highlightedFutureExternal;

            } else if (highlightedFutureMousedOver != null) { // When we stop mousing over, clear the brightness
                // changes.
                highlightedFutureMousedOver.displayPoint = false;
                snapshotNode.getRoot().setLineBrightnessBelow(NodeQWOPGraphicsBase.lineBrightnessDefault);
                highlightedFutureMousedOver.clearBackwardsBranchZOffset();
                highlightedFutureMousedOver = null;
            }

            // Draw the sequence too.
            if (snapshotNode.getTreeDepth() > 0) {
                List<Action<CommandQWOP>> actionList = new ArrayList<>();
                snapshotNode.getSequence(actionList);
                drawActionString(actionList.toArray(new Action[0]), g);
            }
        }
    }

    /**
     * Change highlighting on both the tree and the snapshot when selections change.
     */
    private void changeFocusedFuture(Graphics2D g2, NodeQWOPGraphicsBase<?, CommandQWOP> oldFuture,
                                     NodeQWOPGraphicsBase<?, CommandQWOP> newFuture) {

        // TODO
        // Clear out highlights from the old node.
        if (oldFuture != null && !oldFuture.equals(newFuture)) {
            oldFuture.clearBackwardsBranchZOffset();
            //oldFuture.clearBranchLineOverrideColor();
            oldFuture.displayPoint = false;
        }

        // Add highlights to the new node if it's different or previous is nonexistent
        if (oldFuture == null || !oldFuture.equals(newFuture)) {
            newFuture.displayPoint = true;
//            newFuture.nodeColor = Color.ORANGE;
            newFuture.setBackwardsBranchZOffset(0.8f);
            newFuture.highlightSingleRunToThisNode(); // Tell the tree to highlight a section and darken others.
        }
        // Draw
        int idx = focusLeaves.indexOf(newFuture);
        if (idx > -1) { // Focus leaves no longer contains the no focus requested.
            try {
                GameQWOP.drawExtraRunner(g2, states.get(idx), "", runnerScaling, xOffsetPixels - specificXOffset,
                        yOffsetPixels, colors.get(idx).darker(), boldStroke);

                NodeQWOPExplorableBase<?, CommandQWOP> currentNode = newFuture;

                // Also draw parent nodes back the the selected one to view the run that leads to the highlighted
                // failure.
                //int prevX = Integer.MAX_VALUE;
                while (currentNode.getTreeDepth() > snapshotNode.getTreeDepth()) {
                    // Make color shades slightly alternate between subsequent move frames.
                    Color everyOtherEvenColor = colors.get(idx).darker();
                    if (currentNode.getTreeDepth() % 2 == 0) {
                        everyOtherEvenColor = everyOtherEvenColor.darker();
                    }
                    GameQWOP.drawExtraRunner(g2, currentNode.getState(),
                            Integer.toString(currentNode.getAction().getTimestepsRemaining()),
                            runnerScaling, xOffsetPixels - specificXOffset, yOffsetPixels, everyOtherEvenColor, boldStroke);
                    currentNode = currentNode.getParent();
                }
            } catch (IndexOutOfBoundsException e) {
                // I don't really care tbh. Just skip this one.
                // TODO Fix this shit?
            }
        }
    }

    /**
     * Distance of given coordinates from mouse location, squared.
     */
    private float getDistFromMouseSq(float x, float y) {
        float xDistance = (mouseX - (runnerScaling * x + xOffsetPixels - specificXOffset));
        float yDistance = (mouseY - (runnerScaling * y + yOffsetPixels));
        return xDistance * xDistance + yDistance * yDistance;
    }

    /**
     * Get the list of leave nodes (failure states) that we're displaying in the snapshot pane.
     */
    @JsonIgnore
    public List<NodeQWOPGraphicsBase<?, CommandQWOP>> getDisplayedLeaves() {
        return focusLeaves;
    }

    /**
     * Focus a single future leaf
     */
    public void giveSelectedFuture(NodeQWOPGraphicsBase<?, CommandQWOP> queuedFutureLeaf) {
        this.highlightedFutureExternal = queuedFutureLeaf;
    }

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseIsIn = true;
        highlightedFutureExternal = null; // No longer using what the tree says is focused when the mouse is in this pane.
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseIsIn = false;
    }

    @Override
    public void activateTab() {
        active = true;
    }

    @Override
    public void deactivateTab() {
        states.clear();
        focusLeaves.clear();
        strokes.clear();
        colors.clear();
        active = false;
    }

    @Override
    public String getName() {
        return name;
    }
}
