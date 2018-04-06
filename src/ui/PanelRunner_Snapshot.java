package ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import filters.NodeFilter_Downsample;
import game.GameLoader;
import main.INodeFilter;
import main.Node;
import main.PanelRunner;

/**
* Displays fixed shots of the runner at selected nodes. Can also preview the past and future from these nodes. A tab.
* @author Matt
*/
public class PanelRunner_Snapshot extends PanelRunner implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;

	/** The node that is the current focus of this panel. **/
	private Node snapshotNode;
	
	/** Filter to keep from drawing too many and killing the graphics speed. **/
	private INodeFilter filter = new NodeFilter_Downsample(10);

	/** Potentially, a future node selected by hovering over its runner to display a specific sequence of actions in all the displayed futures. **/
	private Node highlightedFutureMousedOver;

	/** Externally selected version to be highlighted. Mostly just commanded by selecting tree nodes instead. **/
	private Node highlightedFutureExternal;

	private List<Node> focusLeaves = new ArrayList<Node>();
	private List<Object[]> transforms = new ArrayList<Object[]>();
	private List<Stroke> strokes = new ArrayList<Stroke>();
	private List<Color> colors = new ArrayList<Color>();

	/** Number of runner states in the past to display. **/
	public int numHistoryStatesDisplay = 25;

	/** How close do we have to be (squared) from the body of a single runner for it to be eligible for selection. **/
	float figureSelectThreshSq = 150;

	/** Current mouse location. **/
	private int mouseX = 0;
	private int mouseY = 0;

	/** X offset in frame pixel coordinates determined by the focused body's x coordinate. **/
	private int specificXOffset = 0;
	
	/** Mouse currently over this panel? **/
	private boolean mouseIsIn = false;

	private final GameLoader game = new GameLoader();
	
	public PanelRunner_Snapshot() {
		super();
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	/** Assign a selected node for the snapshot pane to display. **/
	public void giveSelectedNode(Node node) {
		transforms.clear();
		focusLeaves.clear();
		strokes.clear();
		colors.clear();

		/***** Focused node first *****/
		snapshotNode = node;
		Object[] nodeTransform = game.getXForms(snapshotNode.state);
		specificXOffset = (int)(runnerScaling * snapshotNode.state.body.x);
		// Make the sequence centered around the selected node state.
		//xOffsetNet = xOffsetPixels + (int)(-runnerScaling * nodeTransform[1].position.x);
		transforms.add(nodeTransform);
		strokes.add(boldStroke);
		colors.add(Color.BLACK);
		focusLeaves.add(node);

		/***** History nodes *****/
		Node historyNode = snapshotNode;
		for (int i = 0; i < numHistoryStatesDisplay; i++) {
			if (historyNode.treeDepth > 0) {
				historyNode = historyNode.parent;
				nodeTransform = game.getXForms(historyNode.state);
				transforms.add(nodeTransform);
				strokes.add(normalStroke);
				colors.add(ghostGray);
				focusLeaves.add(historyNode);
			}
		}

		/***** Future leaf nodes *****/
		List<Node> descendants = new ArrayList<Node>();
		for (int i = 0; i < snapshotNode.children.size(); i++) {
			Node child = snapshotNode.children.get(i);
			child.getLeaves(descendants);
			filter.filter(descendants);
			
			Color runnerColor = Node.getColorFromTreeDepth(i*10);
			child.setBranchColor(runnerColor); // Change the color on the tree too.

			for (Node descendant : descendants) {
				if (descendant.state != null) {
					focusLeaves.add(descendant);
					transforms.add(game.getXForms(descendant.state));
					strokes.add(normalStroke);
					colors.add(runnerColor);
				}
			}
		}
	}


	/** Draws the selected node state and potentially previous and future states. **/
	@Override
	public void paintComponent(Graphics g) {
		if (!active) return;
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		if (snapshotNode != null && snapshotNode.state != null) { 
			float bestSoFar = Float.MAX_VALUE;
			int bestIdx = Integer.MIN_VALUE;

			// Figure out if the mouse close enough to highlight one state.
			if (mouseIsIn && mouseX > getWidth()/2) { // If we are mousing over this panel, see if we're hovering close enough over any particular dude state.

				// Check body first
				for (int i = 0; i < focusLeaves.size(); i++) {
					float distSq = getDistFromMouseSq(focusLeaves.get(i).state.body.x,focusLeaves.get(i).state.body.y);
					if (distSq < bestSoFar  && distSq < figureSelectThreshSq) {
						bestSoFar = distSq;
						bestIdx = i;
					}
				}
				// Then head
				if (bestIdx < 0) { // Only goes to this if we didn't find a near-enough torso.
					for (int i = 0; i < focusLeaves.size(); i++) {
						float distSq = getDistFromMouseSq(focusLeaves.get(i).state.head.x,focusLeaves.get(i).state.head.y);
						if (distSq < bestSoFar  && distSq < figureSelectThreshSq) {
							bestSoFar = distSq;
							bestIdx = i;
						}
					}
				}
				// Then both feet equally
				if (bestIdx < 0) { // Only goes to this if we didn't find a near-enough torso OR head.
					for (int i = 0; i < focusLeaves.size(); i++) {
						float distSq = getDistFromMouseSq(focusLeaves.get(i).state.lfoot.x,focusLeaves.get(i).state.lfoot.y);
						if (distSq < bestSoFar  && distSq < figureSelectThreshSq) {
							bestSoFar = distSq;
							bestIdx = i;
						}
						distSq = getDistFromMouseSq(focusLeaves.get(i).state.rfoot.x,focusLeaves.get(i).state.rfoot.y);
						if (distSq < bestSoFar  && distSq < figureSelectThreshSq) {
							bestSoFar = distSq;
							bestIdx = i;
						}
					}
				}
			}

			// Draw all non-highlighted runners.
			for (int i = transforms.size() - 1; i >= 0; i--) {
				if (!mouseIsIn || bestIdx != i) {
					Color nextRunnerColor = (highlightedFutureMousedOver != null && focusLeaves.get(i).treeDepth > snapshotNode.treeDepth) ? colors.get(i).brighter() : colors.get(i); // Make the nodes after the selected one lighter if one is highlighted.
					game.drawExtraRunner(g2, transforms.get(i), "", runnerScaling, xOffsetPixels - specificXOffset, yOffsetPixels, nextRunnerColor, strokes.get(i));
				}
			}

			// Change things if one runner is selected.
			if (mouseIsIn && bestIdx >= 0) {
				Node newHighlightNode = focusLeaves.get(bestIdx);
				changeFocusedFuture(g2, highlightedFutureMousedOver, newHighlightNode);
				highlightedFutureMousedOver = newHighlightNode;

				// Externally commanded pick, instead of mouse-picked.
			}else if(highlightedFutureExternal != null) {
				changeFocusedFuture(g2, highlightedFutureMousedOver, highlightedFutureExternal);
				highlightedFutureMousedOver = highlightedFutureExternal;

			}else if (highlightedFutureMousedOver != null) { // When we stop mousing over, clear the brightness changes.
				highlightedFutureMousedOver.displayPoint = false;
				highlightedFutureMousedOver.nodeColor = Color.GREEN;
				snapshotNode.getRoot().resetLineBrightness_below();
				highlightedFutureMousedOver.clearBackwardsBranchZOffset();
				highlightedFutureMousedOver = null;
			}

			// Draw the sequence too.
			drawActionString(snapshotNode.getSequence(), g);
		}
	}

	/** Change highlighting on both the tree and the snapshot when selections change. **/
	private void changeFocusedFuture(Graphics2D g2, Node oldFuture, Node newFuture) {
		// Clear out highlights from the old node.
		if (oldFuture != null && !oldFuture.equals(newFuture)) {
			oldFuture.clearBackwardsBranchZOffset();
			oldFuture.displayPoint = false;
			oldFuture.nodeColor = Color.GREEN;
		}

		// Add highlights to the new node if it's different or previous is nonexistant
		if (oldFuture == null || !oldFuture.equals(newFuture)) {
			newFuture.displayPoint = true;
			newFuture.nodeColor = Color.ORANGE;
			newFuture.setBackwardsBranchZOffset(0.8f);
			newFuture.highlightSingleRunToThisNode(); // Tell the tree to highlight a section and darken others.
		}
		// Draw
		int idx = focusLeaves.indexOf(newFuture);
		if (idx > -1) { // Focus leaves no longer contains the no focus requested.
			try{
				game.drawExtraRunner(g2, transforms.get(idx), "", runnerScaling, xOffsetPixels - specificXOffset, yOffsetPixels, colors.get(idx).darker(), boldStroke);

				Node currentNode = newFuture;

				// Also draw parent nodes back the the selected one to view the run that leads to the highlighted failure.
				//int prevX = Integer.MAX_VALUE;
				while (currentNode.treeDepth > snapshotNode.treeDepth) {
					// Make color shades slightly alternate between subsequent move frames.
					Color everyOtherEvenColor = colors.get(idx).darker();
					if (currentNode.treeDepth % 2 == 0) {
						everyOtherEvenColor = everyOtherEvenColor.darker();
					}
					game.drawExtraRunner(g2, game.getXForms(currentNode.state), currentNode.getAction().toStringLite(), runnerScaling, xOffsetPixels - specificXOffset, yOffsetPixels, everyOtherEvenColor, boldStroke);
					currentNode = currentNode.parent;
				}
			}catch(IndexOutOfBoundsException e) {
				// I don't really care tbh. Just skip this one.
			}
		}
	}

	/** Distance of given coordinates from mouse location, squared. **/
	private float getDistFromMouseSq(float x, float y) {
		float xdist = (mouseX - (runnerScaling * x + xOffsetPixels - specificXOffset));
		float ydist = (mouseY - (runnerScaling * y + yOffsetPixels - specificXOffset));
		return xdist*xdist + ydist*ydist;
	}

	/** Get the list of leave nodes (failure states) that we're displaying in the snapshot pane. **/
	public List<Node> getDisplayedLeaves() {
		return focusLeaves;
	}
	
	/** Focus a single future leaf **/
	public void giveSelectedFuture(Node queuedFutureLeaf) {
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
	public void deactivateTab() {
		transforms.clear();
		focusLeaves.clear();
		strokes.clear();
		colors.clear();
		active = false;	
	}
}
