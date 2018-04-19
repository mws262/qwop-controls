package ui;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Vector3f;

import com.jogamp.opengl.*;

import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

import main.IUserInterface;
import main.Node;
import main.TreeWorker;


/**
 * All UI stuff happens here and most of the analysis that individual panes show happens here too.
 * @author Matt
 */
@SuppressWarnings("serial")
public class UI_Full extends JFrame implements ChangeListener, Runnable, IUserInterface{

	/** Thread loop running? **/
	public boolean running = true;

	/** Verbose printing? **/
	public boolean verbose = false;

	/** Tree root nodes associated with this interface. **/
	ArrayList<Node> rootNodes = new ArrayList<Node>();

	/** Individual pane for the tree. **/
	TreePane treePane;

	/** Pane for the tabbed side of the interface. **/
	JTabbedPane tabPane;

	/** Selected node by user click/key **/
	Node selectedNode;

	/** List of panes which can be activated, deactivated. **/
	private ArrayList<TabbedPaneActivator> allTabbedPanes = new ArrayList<TabbedPaneActivator>(); //List of all panes in the tabbed part

	/** Window width **/
	public static int windowWidth = 1920;

	/** Window height **/
	public static int windowHeight = 1000;

	/** Attempted frame rate **/
	private int FPS = 25;

	/** Games played per second **/
	private int gps = 0;

	/** Usable milliseconds per frame **/
	private long MSPF = (long)(1f/FPS * 1000f);

	/** Continuously update the estimate of the display loop time in milliseconds. **/
	private long avgLoopTime = MSPF;
	/** Filter the average loop time. Lower numbers gives more weight to the lower estimate, higher numbers gives more weight to the old value. **/
	private final float loopTimeFilter = 8;
	private long lastIterTime = System.currentTimeMillis();
	private long totalGamesPlayed = 0;
	private long lastGamesPlayed = 0;

	/** Keep track of whether we sent a pause request to the tree. **/
	private boolean treePause = false;

	/** State machine states for all UI **/
	public enum Status{
		IDLE_ALL, DRAW_ALL
	}

	private Status currentStatus = Status.IDLE_ALL;
	private Status previousStatus = Status.IDLE_ALL;

	public UI_Full() {
		Container pane = this.getContentPane();
		/**** Tabbed panes ****/
		/* Add components to tabs */
		tabPane = new JTabbedPane();
		tabPane.setBorder(BorderFactory.createRaisedBevelBorder());
		tabPane.setPreferredSize(new Dimension(1080,250));
		tabPane.setMinimumSize(new Dimension(100,1));
		tabPane.addChangeListener(this);

		/**** TREE PANE ****/
		treePane = new TreePane();
		treePane.setBorder(BorderFactory.createRaisedBevelBorder());

		// This makes it have that dragable border between the tab and the tree sections.
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treePane, tabPane);
		splitPane.setResizeWeight(0.7);
		pane.add(splitPane);

		/*******************/
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.setPreferredSize(new Dimension(windowWidth, windowHeight));
		this.setContentPane(this.getContentPane());
		this.pack();
		this.setVisible(true); 
		//treePane.requestFocus();

		currentStatus = Status.DRAW_ALL; // Fire it up.
		
	}

	/** Add a new tab to this frame. **/
	public void addTab(TabbedPaneActivator newTab, String name) {
		tabPane.addTab(name, (Component)newTab);
		allTabbedPanes.add(newTab);
		tabPane.revalidate();
		
		//Make sure the currently active tab is actually being updated.
		allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();	
	}
	
	/** Add a new tab to this frame. **/
	public void removeTab(JPanel tabToRemove) {
		tabPane.remove((Component)tabToRemove);
		allTabbedPanes.remove(tabToRemove);

		//Make sure the currently active tab is actually being updated.
		allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();	
	}

	/* (non-Javadoc)
	 * @see main.IUserInterface#run()
	 */
	@Override
	public void run() {
		while (running) {
			long currentTime = System.currentTimeMillis();
			switch(currentStatus) {
			case IDLE_ALL:
				break;
			case DRAW_ALL:
				repaint();
				break;
			default:
				break;
			}

			if (verbose && currentStatus != previousStatus) {
				System.out.println(previousStatus + " -> " + currentStatus );
			}

			previousStatus = currentStatus;

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
	 * @see main.IUserInterface#kill()
	 */
	@Override
	public void kill() {
		running = false;
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));	
	}

	/* (non-Javadoc)
	 * @see main.IUserInterface#selectNode(main.Node)
	 */
	@Override
	public void selectNode(Node selected) {
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
			for (TabbedPaneActivator p: allTabbedPanes) {
				p.deactivateTab();
			}
			allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
		}
	}

	/**
	 * Pane for displaying the entire tree in OpenGL. Not part of the tabbed system.
	 * @author Matt
	 */
	public class TreePane extends GLPanelGeneric implements TabbedPaneActivator, GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

		/** For rendering text overlays. Note that textrenderer is for overlays while GLUT is for labels in world space **/
		TextRenderer textRenderBig;
		TextRenderer textRenderSmall;

		/** Currently tracked mouse x location in screen coordinates of the panel **/
		int mouseX;

		/** Currently tracked mouse x location in screen coordinates of the panel **/
		int mouseY;

		/** Is the mouse cursor inside the bounds of the tree panel? **/
		boolean mouseInside = false;

		public TreePane() {
			super();
			canvas.setFocusable(true);
			canvas.addKeyListener(this);
			canvas.addMouseListener(this);
			canvas.addMouseMotionListener(this);
			canvas.addMouseWheelListener(this);

			textRenderBig = new TextRenderer(new Font("Calibri", Font.BOLD, 36));
			textRenderSmall = new TextRenderer(new Font("Calibri", Font.PLAIN, 18));
		}

		@Override
		public void activateTab() {}
		@Override
		public void deactivateTab() {}
		@Override
		public void display(GLAutoDrawable drawable) {		
			super.display(drawable);

			float ptSize = 50f/cam.getZoomFactor(); //Let the points be smaller/bigger depending on zoom, but make sure to cap out the size!
			ptSize = Math.min(ptSize, 10f);

			for (Node node : rootNodes) {
				gl.glColor3f(1f, 0.1f, 0.1f);
				gl.glPointSize(ptSize);

				gl.glBegin(GL.GL_POINTS);
				node.drawNodes_below(gl);
				gl.glEnd();

				gl.glColor3f(1f, 1f, 1f);
				gl.glBegin(GL.GL_LINES);
				node.drawLines_below(gl);
				gl.glEnd();
			}

			// Draw games played and games/sec in upper left.
			textRenderBig.beginRendering(panelWidth, panelHeight);
			textRenderBig.setColor(0.7f, 0.7f, 0.7f, 1.0f);
			//tmp remove textRenderBig.draw(negotiator.getGamesPlayed() + " games", 20, panelHeight - 50);

			if (treePause) {
				textRenderBig.setColor(0.7f, 0.1f, 0.1f, 1.0f);	
				textRenderBig.draw("PAUSED", panelWidth/2, panelHeight - 50);
			}
			textRenderBig.endRendering();

			// ms/frame
			avgLoopTime = (long)(((loopTimeFilter - 1f) * avgLoopTime + 1f * (System.currentTimeMillis() - lastIterTime)) / loopTimeFilter); // Filter the loop time

			// Draw the FPS of the tree drawer at the moment.
			textRenderSmall.beginRendering(panelWidth, panelHeight);
			textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);

			int dekafps = (int)(10000./avgLoopTime); // Only multiplying by 10000 instead of 1000 to make decimal places as desired.
			textRenderSmall.draw( ( (Math.abs(dekafps) > 10000) ? "???" : dekafps/10f ) + " FPS", panelWidth - 75, panelHeight - 20);

			// Number of imported games
			textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);

			// Total games played
			textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);
			long totalTimestepsSimulated = TreeWorker.getTotalTimestepsSimulated();
			totalGamesPlayed = TreeWorker.getTotalGamesPlayed();
			textRenderSmall.draw(totalGamesPlayed + " total games", 20, panelHeight - 85);	

			textRenderSmall.draw(Math.round(totalTimestepsSimulated/9000f)/10f + " hours simulated!", 20, panelHeight - 100);

			textRenderSmall.setColor(0.1f, 0.7f, 0.1f, 1.0f);
			textRenderSmall.draw(Math.round(totalTimestepsSimulated/(double)totalGamesPlayed * 0.4)/10f + "s Avg. game length", 20, panelHeight - 115);

			textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);
			gps = (int)((gps*(loopTimeFilter - 1f) + 1000f*(totalGamesPlayed - lastGamesPlayed) / (System.currentTimeMillis() - lastIterTime))/loopTimeFilter);
			textRenderSmall.draw(gps + " games/s", 20, panelHeight - 130);

			textRenderSmall.setColor(0.1f, 0.7f, 0.1f, 1.0f);
			textRenderSmall.draw(Runtime.getRuntime().totalMemory()/1000000 + "MB used", 20, panelHeight - 145);
			textRenderSmall.draw(Runtime.getRuntime().maxMemory()/1000000 + "MB max", 20, panelHeight - 160);
			textRenderSmall.endRendering();
			lastGamesPlayed = totalGamesPlayed;
			lastIterTime = System.currentTimeMillis();		

		}

		/** Draw a text string using GLUT (for openGL rendering version of my stuff) **/
		public void drawString(String toDraw, float x, float y, float z, GL2 gl, GLUT glut ) 
		{
			// Fomat numbers with Java.
			NumberFormat format = NumberFormat.getNumberInstance();
			format.setMinimumFractionDigits(2);
			format.setMaximumFractionDigits(2);

			// Printing fonts, letters and numbers is much simpler with GLUT.
			// We do not have to use our own bitmap for the font.
			gl.glRasterPos3d(x,y,z);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, toDraw);
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (e.getWheelRotation() < 0) { //Negative mouse direction -> zoom in.
				cam.smoothZoom(0.9f, 5);
			}else{
				cam.smoothZoom(1.1f, 5);
			}
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
			if (e.isMetaDown()) {
				selectNode(cam.nodeFromClick(e.getX(), e.getY(), rootNodes));
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

				if(e.isMetaDown()) { //if we're using GL, then we'll move the camera with mac key + arrows
					switch( keyCode ) { 
					case KeyEvent.VK_UP: //Go out the branches of the tree

						cam.smoothRotateLong(0.1f, 5);
						break;
					case KeyEvent.VK_DOWN: //Go back towards root one level
						cam.smoothRotateLong(-0.1f, 5);
						break;
					case KeyEvent.VK_LEFT: //Go left along an isobranch (like that word?)
						cam.smoothRotateLat(0.1f, 5);
						break;
					case KeyEvent.VK_RIGHT : //Go right along an isobranch
						cam.smoothRotateLat(-0.1f, 5);
						break;
					case KeyEvent.VK_S : // toggle the score text at the end of all branches
						//TODO
						break;
					}
				}else if(e.isShiftDown()) {
					switch( keyCode ) { 
					case KeyEvent.VK_LEFT: //Go left along an isobranch (like that word?)
						cam.smoothTwist(0.1f, 5);
						break;
					case KeyEvent.VK_RIGHT : //Go right along an isobranch
						cam.smoothTwist(-0.1f, 5);
						break;
					}
				}else{
					switch( keyCode ) { 
					case KeyEvent.VK_UP: //Go out the branches of the tree
						arrowSwitchNode(0,1);
						break;
					case KeyEvent.VK_DOWN: //Go back towards root one level
						arrowSwitchNode(0,-1); 
						break;
					case KeyEvent.VK_LEFT: //Go left along an isobranch (like that word?)
						arrowSwitchNode(1,0);
						break;
					case KeyEvent.VK_RIGHT : //Go right along an isobranch
						arrowSwitchNode(-1,0);
						break;
					case KeyEvent.VK_P : //Pause everything except for graphics updates
						treePause = !treePause;
						if (treePause) {
							rootNodes.get(0).calcNodePosBelow();
						}else{
							//tmp remove negotiator.unpauseTree();
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
		public void keyReleased(KeyEvent e) {}

		@Override
		public void mouseMoved(MouseEvent e) {
			mouseX = e.getX();
			mouseY = e.getY();
			// If the snapshot pane is displaying stuff, this lets us potentially select some of the future nodes displayed in the snapshot pane.
			if (mouseInside) {}
			for (TabbedPaneActivator pane : allTabbedPanes) {
				if (pane.isActive() && pane.getClass().equals(PanelRunner_Snapshot.class)) {
					PanelRunner_Snapshot snapshotPane = (PanelRunner_Snapshot)pane;
					
					List<Node> snapshotLeaves = snapshotPane.getDisplayedLeaves();
					if (snapshotLeaves.size() > 0) {
						Node nearest = cam.nodeFromClick_set(mouseX, mouseY, snapshotLeaves, 50);
						if (nearest != null) {
							snapshotPane.giveSelectedFuture(nearest);
						}else{
							snapshotPane.giveSelectedFuture(null); // clear it out if the mouse is too far away from selectable nodes.
						}
					}
					break;
				}
			}
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

		// The following 2 methods are probably too complicated. when you push the arrow at the edge of one branch, this tries to jump to the nearest next branch node at the same depth.
		/** Called by key listener to change our focused node to the next adjacent one in the +1 or -1 direction **/
		private void arrowSwitchNode(int direction,int depth) {
			//Stupid way of getting this one's index according to its parent.
			if(selectedNode != null) {
				if (selectedNode.treeDepth == 0) { // At root, don't try to look at parent.
					// <nothing>
				}else{
					int thisIndex = selectedNode.parent.children.indexOf(selectedNode);
					//This set of logicals eliminates the edge cases, then takes the proposed action as default
					if (thisIndex == 0 && direction == -1) { //We're at the lowest index of this node and must head to a new parent node.
						ArrayList<Node> blacklist = new ArrayList<Node>(); //Keep a blacklist of nodes that already proved to be duds.
						blacklist.add(selectedNode);
						nextOver(selectedNode.parent,blacklist,1,direction,selectedNode.parent.children.indexOf(selectedNode),0);

					}else if (thisIndex == selectedNode.parent.children.size()-1 && direction == 1) { //We're at the highest index of this node and must head to a new parent node.
						ArrayList<Node> blacklist = new ArrayList<Node>();
						blacklist.add(selectedNode);
						nextOver(selectedNode.parent,blacklist, 1,direction,selectedNode.parent.children.indexOf(selectedNode),0);

					}else{ //Otherwise we can just switch nodes within the scope of this parent.
						selectNode(selectedNode.parent.children.get(thisIndex+direction));
					}
				}

				//These logicals just take the proposed motion (or not) and ignore any edges.
				if(depth == 1 && selectedNode.children.size()>0) { //Go further down the tree if this node has children
					selectNode(selectedNode.children.get(0));
				}else if(depth == -1 && selectedNode.treeDepth>0) { //Go up the tree if this is not root.
					selectNode(selectedNode.parent);
				}
				repaint();
			}
		}

		/** Take a node back a layer. Don't return to node past. Try to go back out by the deficit depth amount in the +1 or -1 direction left/right **/
		private boolean nextOver(Node current, ArrayList<Node> blacklist, int deficitDepth, int direction,int prevIndexAbove,int numTimesTried) { // numTimesTried added to prevent some really deep node for causing some really huge search through the whole tree. If we don't succeed in a handful of iterations, just fail quietly.
			numTimesTried++;
			boolean success = false;
			//TERMINATING CONDITIONS-- fail quietly if we get back to root with nothing. Succeed if we get back to the same depth we started at.
			if (deficitDepth == 0) { //We've successfully gotten back to the same level. Great.
				selectNode(current);
				return true;
			}else if(current.treeDepth == 0) {
				return true; // We made it back to the tree's root without any success. Just return.

			}else if(numTimesTried>100) {// If it takes >100 movements between nodes, we'll just give up.
				return true;
			}else{
				//CCONDITIONS WE NEED TO STEP BACKWARDS TOWARDS ROOT.
				//If this new node has no children OR it's 1 child is on the blacklist, move back up the tree.
				if((prevIndexAbove+1 == current.children.size() && direction == 1) || (prevIndexAbove == 0 && direction == -1)) {
					blacklist.add(current); 
					success = nextOver(current.parent,blacklist,deficitDepth+1,direction,current.parent.children.indexOf(current),numTimesTried); //Recurse back another node.
				}else if (!(current.children.size() >0) || (blacklist.contains(current.children.get(0)) && current.children.size() == 1)) { 
					blacklist.add(current); 
					success = nextOver(current.parent,blacklist,deficitDepth+1,direction,current.parent.children.indexOf(current),numTimesTried); //Recurse back another node.
				}else{

					//CONDITIONS WE NEED TO GO DEEPER:
					if(direction == 1) { //March right along this previous node.
						for (int i = prevIndexAbove+1; i<current.children.size(); i++) {
							success = nextOver(current.children.get(i),blacklist,deficitDepth-1,direction,-1,numTimesTried);
							if(success) {
								return true;
							}
						}
					}else if(direction == -1) { //March left along this previous node
						for (int i = prevIndexAbove-1; i>=0; i--) {
							success = nextOver(current.children.get(i),blacklist,deficitDepth-1,direction,current.children.get(i).children.size(),numTimesTried);
							if(success) {
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

	}

	@Override
	public void addRootNode(Node node) {
		rootNodes.add(node);
	}

	@Override
	public void clearRootNodes() {
		rootNodes.clear();
	}
}

