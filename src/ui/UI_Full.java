package ui;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Vector3f;

import org.jblas.*;
import com.jogamp.opengl.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

import game.GameLoader;
import main.IUserInterface;
import main.Node;
import main.PanelPlot;
import main.PanelRunner;
import main.State;
import main.Transform_PCA;
import main.TreeWorker;
import main.Utility;


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

	/** Pane for the runner. **/
	PanelRunner_Animated runnerPanel = new PanelRunner_AnimatedAutoencoder();
	
	
	/** Pane for the snapshots of the runner. **/
	PanelRunner_Snapshot snapshotPane = new PanelRunner_Snapshot();

	/** Pane for comparing different states across the tree. **/
	PanelRunner_Comparison comparisonPane;

	/** Plots here. **/
	PanelPlot statePlotPane;
	
	PanelPlot pcaPlotPane;
	//DataPane dataPane_pca;

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

	/** Drawing offsets within the viewing panel (i.e. non-physical) **/
	public int xOffsetPixels_init = 960;
	public int xOffsetPixels = xOffsetPixels_init;
	public int yOffsetPixels = 100;

	/** Runner coordinates to pixels. **/
	public float runnerScaling = 10f;

	private final Color historyDrawColor = new Color(0.6f,0.6f,0.6f);
	private final Color appleGray = new Color(230,230,230);

	final Font QWOPLittle = new Font("Ariel", Font.BOLD,21);
	final Font QWOPBig = new Font("Ariel", Font.BOLD,28);


	/** Continuously update the estimate of the display loop time in milliseconds. **/
	private long avgLoopTime = MSPF;
	/** Filter the average loop time. Lower numbers gives more weight to the lower estimate, higher numbers gives more weight to the old value. **/
	private final float loopTimeFilter = 20f;
	private long lastIterTime = System.currentTimeMillis();
	private long currentGamesPlayed = 0;
	private long lastGamesPlayed = 0;
	/** Keep track of whether we sent a pause request to the tree. **/
	private boolean treePause = false;

	/********** Writing actions on the left pane. **********/
	/** Fonts used for drawing on the left side pane. **/
	//private final Font giantFont = new Font("Ariel",Font.BOLD,36);
	private final Font bigFont = new Font("Ariel", Font.BOLD, 16);
	private final Font littleFont = new Font("Ariel", Font.BOLD, 12);

	/********** Plots **********/
	public boolean plotColorsByDepth = true;

	/** State machine states for all UI **/
	public enum Status{
		IDLE_ALL, INITIALIZE, DRAW_ALL, VIEW_RUN
	}

	private Status currentStatus = Status.IDLE_ALL;
	private Status previousStatus = Status.IDLE_ALL;

	public UI_Full() {
		Container pane = this.getContentPane();
		pane.setLayout(new GridBagLayout());

		/**** Tabbed panes ****/
		GridBagConstraints dataConstraints = new GridBagConstraints();
		dataConstraints.fill = GridBagConstraints.HORIZONTAL;
		dataConstraints.gridx = 0;
		dataConstraints.gridy = 1;
		dataConstraints.weightx = 0.3;
		dataConstraints.weighty = 0.125; // Turn this up if the tree stuff starts to cover the tabs
		dataConstraints.ipady = (int)(0.28*windowHeight);
		dataConstraints.ipadx = (int)(windowWidth*0.5);

		/* Pane for all tabs */
		tabPane = new JTabbedPane();
		tabPane.setBorder(BorderFactory.createRaisedBevelBorder());

		/* Runner pane */   
		Thread runnerPanelThread = new Thread(runnerPanel);
		runnerPanelThread.start();
		tabPane.addTab("Run Animation", runnerPanel);

		/* Snapshot pane */
		tabPane.addTab("State Viewer", snapshotPane);

		/* Data pane */
		statePlotPane = new PanelPlot_States(6);
		tabPane.addTab("State Data Viewer", statePlotPane);
		pcaPlotPane = new PanelPlot_Transformed(new Transform_PCA(IntStream.range(0, 72).toArray()));
		tabPane.addTab("PCA Viewer", pcaPlotPane);

		/* State comparison pane */
		comparisonPane = new PanelRunner_Comparison();
		tabPane.addTab("State Compare", comparisonPane);

		/* Tree pane */
		treePane = new TreePane();

		pane.add(tabPane, dataConstraints);

		allTabbedPanes.add(runnerPanel);
		allTabbedPanes.add(snapshotPane);
		allTabbedPanes.add(statePlotPane);
		allTabbedPanes.add(pcaPlotPane);
		allTabbedPanes.add(comparisonPane);
		tabPane.addChangeListener(this);
		//Make sure the currently active tab is actually being updated.
		allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();

		/**** TREE PANE ****/
		GridBagConstraints treeConstraints = new GridBagConstraints();
		treeConstraints.fill = GridBagConstraints.HORIZONTAL;
		treeConstraints.gridx = 0;
		treeConstraints.gridy = 0;
		treeConstraints.weightx = 0.8;
		treeConstraints.weighty = 0.6;
		treeConstraints.ipady = (int)(windowHeight*0.8f);
		treeConstraints.ipadx = (int)(windowWidth*0.8f);

		treePane = new TreePane();

		treePane.setBorder(BorderFactory.createRaisedBevelBorder());
		pane.add(treePane,treeConstraints);
		/*******************/

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setPreferredSize(new Dimension(windowWidth, windowHeight));
		this.setContentPane(this.getContentPane());
		this.pack();
		this.setVisible(true); 
		//treePane.requestFocus();
		repaint();
	}
	
	public void addDebuggingTab(PanelRunner debugPanel) {
		tabPane.addTab("Debugging", debugPanel);
		allTabbedPanes.add(debugPanel);
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
				currentStatus = Status.INITIALIZE;
				break;
			case INITIALIZE:
				currentStatus = Status.DRAW_ALL;
				break;
			case DRAW_ALL:
				repaint();
				break;
			case VIEW_RUN:
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
					// TODO Auto-generated catch block
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
			if (runnerPanel.isActive()) runnerPanel.simRunToNode(selectedNode);

			if (snapshotPane.isActive()) snapshotPane.giveSelectedNode(selectedNode);
			if (comparisonPane.isActive()) comparisonPane.giveSelectedNode(selectedNode);
			if (statePlotPane.isActive()) statePlotPane.update(selectedNode); // Updates data being put on plots
			if (pcaPlotPane.isActive()) pcaPlotPane.update(selectedNode); // Updates data being put on plots
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		for (TabbedPaneActivator p: allTabbedPanes) {
			p.deactivateTab();
		}
		allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
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
//			tmp remove textRenderSmall.draw(negotiator.getGamesImported() + " Games imported", 20, panelHeight - 70);
			
			// Total games played
			textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);
			currentGamesPlayed = TreeWorker.getTotalGamesPlayed();
			textRenderSmall.draw(currentGamesPlayed + " total games", 20, panelHeight - 85);	
			
			textRenderSmall.setColor(0.1f, 0.7f, 0.1f, 1.0f);
			textRenderSmall.draw(Math.round(TreeWorker.getTotalTimestepsSimulated()/360)/10f + " hours simulated!", 20, panelHeight - 100);
			
			textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);
			gps = (int)((gps*(loopTimeFilter - 1f) + 1000f*(currentGamesPlayed - lastGamesPlayed) / (System.currentTimeMillis() - lastIterTime))/loopTimeFilter);
			textRenderSmall.draw(gps + " games/s", 20, panelHeight - 115);

			
			textRenderSmall.draw(Runtime.getRuntime().totalMemory()/1000000 + "MB used", 20, panelHeight - 145);
			textRenderSmall.draw(Runtime.getRuntime().maxMemory()/1000000 + "MB max", 20, panelHeight - 160);
			textRenderSmall.endRendering();
			lastGamesPlayed = currentGamesPlayed;
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
							rootNodes.get(0).calcNodePos_below();
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
						System.exit(0);
						break;
						
					case KeyEvent.VK_SPACE:
						if (runnerPanel.isActive()) {
							runnerPanel.pauseToggle();
						}
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
			if (snapshotPane.isActive() && mouseInside) {
				List<Node> snapshotLeaves = snapshotPane.getDisplayedLeaves();
				if (snapshotLeaves.size() > 0) {
					Node nearest = cam.nodeFromClick_set(mouseX, mouseY, snapshotLeaves, 50);
					if (nearest != null) {
						snapshotPane.giveSelectedFuture(nearest);
					}else{
						snapshotPane.giveSelectedFuture(null); // clear it out if the mouse is too far away from selectable nodes.
					}
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
			// TODO Auto-generated method stub
			return false;
		}

	}
//	/**
//	 * Pane for displaying plots of the states transformed using principle component analysis (PCA). A tab.
//	 * @author Matt
//	 */
//	public class DataPane_PCA extends DataPane implements KeyListener {
//		// Which pairs of eigenvalues we're plotting.
//		int[][] dataSelect;
//
//		private Node lastSelectedNode;
//
//		/** Data transformed by PCA **/
//		private PCATransformedData pcaPlotDat;
//
//		public boolean mouseIsIn = false;
//
//		public DataPane_PCA() {
//			super();
//			addKeyListener(this);
//			setFocusable(true);
//			dataSelect = new int[numberOfPlots][2];
//
//			// First set of plots are the 0 vs 1-6 eig
//			for (int i = 0; i < numberOfPlots; i++) {
//				dataSelect[i] = new int[]{0,i};
//			}
//		}
//
//		@Override
//		public void update() {
//			requestFocus();
//			setDatasets(dataSelect);
//			for (int i = 0; i < numberOfPlots; i++) {
//				JFreeChart chart = plotPanels[i].getChart();
//				chart.fireChartChanged();
//				// TODO: resizing plots axes
//				//XYPlot plot = (XYPlot)(plotPanels[i].getChart().getPlot());
//				//plot.getDomainAxis().setRange(range);
//				//plot.getRangeAxis().setRange(range);
//			}
//			XYPlot firstPlot = (XYPlot)plotPanels[0].getChart().getPlot();
//			if (!plotColorsByDepth) {
//				addCommandLegend(firstPlot);
//			}
//		}
//
//		private void setDatasets(int[][] dataSelect) {			
//			// Fetching new data.
//			ArrayList<Node> nodesBelow = new ArrayList<Node>();
//			if (selectedNode != null) {
//
//				// A state pair being added to the first plot.
//				XYPlot pl = (XYPlot)plotPanels[0].getChart().getPlot();
//
//				// Only update the plots shown, don't redo PCA calcs.
//				if (lastSelectedNode != null && lastSelectedNode.equals(selectedNode)) {
//					pcaPlotDat = (PCATransformedData)pl.getDataset();
//				}else{
//					selectedNode.getNodes_below(nodesBelow);
//					pcaPlotDat = new PCATransformedData(nodesBelow);
//					lastSelectedNode = selectedNode;
//				}
//
//				pl.setRenderer(pcaPlotDat.getRenderer());
//				pcaPlotDat.addSeries(0, dataSelect[0][0], dataSelect[0][1]);
//				pl.setDataset(pcaPlotDat);
//				pl.getDomainAxis().setLabel("Eig " + dataSelect[0][0] + " (" + Math.round(1000.*pcaPlotDat.evalsNormalized.get(dataSelect[0][0]))/10. + "%)");
//				pl.getRangeAxis().setLabel("Eig " + dataSelect[0][1] + " (" + Math.round(1000.*pcaPlotDat.evalsNormalized.get(dataSelect[0][1]))/10. + "%)");
//
//				for (int i = 1; i < dataSelect.length; i++) {
//					pl = (XYPlot)plotPanels[i].getChart().getPlot();
//					PCATransformedData pcaPlotDatNext = pcaPlotDat.duplicateWithoutRecalcPCA();
//					pl.setRenderer(pcaPlotDat.getRenderer());
//					pcaPlotDatNext.addSeries(0, dataSelect[i][0], dataSelect[i][1]);
//					pl.setDataset(pcaPlotDatNext);
//					pl.getDomainAxis().setLabel("Eig " + dataSelect[i][0] + " (" + Math.round(1000.*pcaPlotDat.evalsNormalized.get(dataSelect[i][0]))/10. + "%)");
//					pl.getRangeAxis().setLabel("Eig " + dataSelect[i][1] + " (" + Math.round(1000.*pcaPlotDat.evalsNormalized.get(dataSelect[i][1]))/10. + "%)");
//				}	
//			}
//		}
//
//		public void plotShift(int xShift, int yShift) {
//			if (xShift != 0 || yShift != 0) {
//				// First set of plots are the 0 vs 1-6 eig
//
//				XYPlot pl = (XYPlot)plotPanels[0].getChart().getPlot();
//				PCATransformedData dat = (PCATransformedData)pl.getDataset();
//				int totalEigs = dat.evals.length;
//
//				if (dataSelect[0][0] + xShift < 0 || dataSelect[numberOfPlots - 1][0] + xShift > totalEigs - 1) {
//					xShift = 0;
//				}
//
//				if (dataSelect[0][1] + yShift < 0 || dataSelect[numberOfPlots - 1][1] + yShift > totalEigs - 1) {
//					yShift = 0;
//				}
//
//				for (int i = 0; i < numberOfPlots; i++) {
//
//					dataSelect[i][0] = dataSelect[i][0] + xShift; // Clamp within the actual number of eigenvalues we have.
//					dataSelect[i][1] = dataSelect[i][1] + yShift;
//				}
//				update();
//			}
//		}
//
//		/** Get the last PCA data. **/
//		public PCATransformedData getPCAData() {
//			return pcaPlotDat;
//		}
//
//		@Override
//		public void keyTyped(KeyEvent e) {
//		}
//
//		@Override
//		public void keyPressed(KeyEvent e) {
//			switch(e.getKeyCode()) {
//			case KeyEvent.VK_UP:
//				plotShift(0,-1);
//				break;
//			case KeyEvent.VK_DOWN:
//				plotShift(0,1);
//				break;
//			case KeyEvent.VK_LEFT:
//				plotShift(-1,0);
//				break;
//			case KeyEvent.VK_RIGHT:
//				plotShift(1,0);
//				break;
//			case KeyEvent.VK_B:
//				plotColorsByDepth = !plotColorsByDepth;
//				break;
//			}	
//		}
//
//		@Override
//		public void keyReleased(KeyEvent e) {}
//
//		@Override
//		public void plotClicked(int plotIdx) {
//			// TODO Auto-generated method stub
//
//		}
//
//		@Override
//		public void activateTab() {
//			active = true;
//			// update(); // Don't update though. Make the user click a node before doing this.
//			requestFocus();
//		}
//
//		@Override
//		public boolean isActive() {
//			return active;
//		}
//	}

//
//		/** XYDataset gets data for plotting transformed data from PCA here. **/
//		public class PCATransformedData extends AbstractXYDataset{
//
//			/** Nodes to appear on the plot. **/
//			private ArrayList<Node> nodeList;
//
//			/** Eigenvectors found during SVD of the conditioned states. They represent the principle directions that explain most of the state variance. **/
//			private FloatMatrix evecs;
//
//			/** During SVD we find the eigenvalues, the weights for what portion of variance is explained by the corresponding eigenvector. **/
//			private FloatMatrix evals;
//
//			/** Normalized so the sum of the evals == 1 **/
//			private FloatMatrix evalsNormalized;
//
//			/** The conditioned dataset. Mean of each state is subtracted and divided by its standard deviation to give a variance of 1. **/
//			private FloatMatrix dataSet;
//
//			private PCAPlotRenderer renderer = new PCAPlotRenderer();
//
//			/** Specific series of data to by plotted. Integer is the plotindex, the matrix is 2xn for x-y plot. **/
//			private Map<Integer,FloatMatrix> tformedData = new HashMap<Integer,FloatMatrix>();
//
//			ArrayList<State.ObjectName> objectsUsed = new ArrayList<State.ObjectName>(Arrays.asList(State.ObjectName.values()));
//			ArrayList<State.StateName> statesUsed = new ArrayList<State.StateName>(Arrays.asList(State.StateName.values()));
//
//
//			public PCATransformedData(ArrayList<Node> nodes) {
//				super();
//				// Can blacklist things NOT to be PCA'd
//				statesUsed.remove(State.StateName.X);
//
//				nodeList = nodes;
//				doPCA();
//			}
//
//			private PCATransformedData() {
//				super();
//			}
//
//			/** Make another one of these but using the same PCA calculation. No data series are specified. **/
//			public PCATransformedData duplicateWithoutRecalcPCA() {
//				PCATransformedData duplicate = new PCATransformedData();
//				duplicate.dataSet = dataSet;
//				duplicate.nodeList = nodeList;
//				duplicate.evals = evals;
//				duplicate.evecs = evecs;
//				duplicate.evalsNormalized = evalsNormalized;
//				return duplicate;
//			}
//
//			/** PCA is already done when the object is created with a list of nodes. This determines which data, transformed according to which
//			 * eigenvalue, is plotted on which plot index.
//			 * @param plotIdxNum
//			 * @param eigForXAxis
//			 * @param eigForYAxis
//			 */
//			public void addSeries(int plotIdx, int eigForXAxis, int eigForYAxis) {
//				tformedData.put(plotIdx, dataSet.mmul(evecs.getColumns(new int[]{eigForXAxis,eigForYAxis})));	
//			}
//
//			/** Mostly for external use. Transform any data by the chosen PCA components. Must have done the PCA already! **/
//			public FloatMatrix transformDataset(ArrayList<Node> nodesToTransform, int[] chosenPCAComponents) {
//				FloatMatrix preppedDat = prepTrialNodeData(nodesToTransform, objectsUsed, statesUsed);
//				FloatMatrix lowDimData = preppedDat.mmul(evecs.getColumns(chosenPCAComponents));
//				return lowDimData;
//			}
//
//			@Override
//			public Number getX(int series, int item) {
//				return tformedData.get(series).get(item,0);
//			}
//
//			@Override
//			public Number getY(int series, int item) {
//				return tformedData.get(series).get(item,1);
//			}
//
//			/** Run PCA on all the states in the nodes stored here. **/
//			public void doPCA() {
//
//				dataSet = prepTrialNodeData(nodeList, objectsUsed, statesUsed);
//
//				FloatMatrix[] USV = Singular.fullSVD(dataSet);
//				evecs = USV[2]; // Eigenvectors
//				evals = USV[1].mul(USV[1]).div(dataSet.rows); // Eigenvalues
//				// Transforming with the first two eigenvectors
//				//tformedData = dataSet.mmul(evecs.getColumns(new int[]{pcaEigX,pcaEigY}));
//
//				// Also make the vector of normalized eigenvalues.
//				float evalSum = 0;
//				for (int i = 0; i < evals.length; i++) {
//					evalSum += evals.get(i);
//				}
//				evalsNormalized = new FloatMatrix(evals.length);
//				for (int i = 0; i < evals.length; i++) {
//					evalsNormalized.put(i, evals.get(i)/evalSum);
//				}
//			}
//
//			/** Unpack the state data from the nodes, pulling only the stuff we want. Condition data to variance 1, mean 0. **/
//			private FloatMatrix prepTrialNodeData(ArrayList<Node> nodes, 
//					ArrayList<State.ObjectName> includedObjects, 
//					ArrayList<State.StateName> includedStates) {
//
//
//				int numStates = includedObjects.size() * includedStates.size();
//				FloatMatrix dat = new FloatMatrix(nodes.size(), numStates);
//
//				// Iterate through all nodes
//				for (int i = 0; i < nodes.size(); i++) {
//					int colCounter = 0;
//					// Through all body parts...
//					for (State.ObjectName obj : includedObjects) {
//						// For each state of each body part.
//						for (State.StateName st : includedStates) {
//							dat.put(i, colCounter, nodes.get(i).state.getStateVarFromName(obj, st));
//							colCounter++;
//						}
//					}
//				}
//				conditionData(dat);
//				return dat;
//			}
//
//			/** Subtracts the mean for each variable, and converts to unit variance.
//			 *  Samples are rows, variables are columns. Alters matrix x.
//			 * @param x
//			 */
//			private void conditionData(FloatMatrix x) {
//
//				for (int i = 0; i < x.columns; i++) {
//					// Calculate the mean of a column.
//					float sum = 0;
//					for (int j = 0; j < x.rows; j++) {
//						sum += x.get(j,i);
//					}
//					// Subtract the mean out.
//					float avg = sum/x.rows;
//					for (int j = 0; j < x.rows; j++) {
//						float centered = x.get(j,i) - avg;
//						x.put(j,i,centered);
//					}
//					// Find the standard deviation for each column.
//					sum = 0;
//					for (int j = 0; j < x.rows; j++) {
//						sum += x.get(j,i) * x.get(j,i);
//					}
//					float std = (float)Math.sqrt(sum/(x.rows - 1));
//
//					// Divide the standard deviation out.
//					for (int j = 0; j < x.rows; j++) {
//						float unitVar = x.get(j,i)/std;
//						x.put(j,i,unitVar);
//					}	
//				}	
//			}
//
//			@Override
//			public int getItemCount(int series) {
//				return nodeList.size();
//			}
//
//			@Override
//			public int getSeriesCount() {
//				return tformedData.size();
//			}
//
//			@Override
//			public Comparable getSeriesKey(int series) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			public XYLineAndShapeRenderer getRenderer() {
//				return renderer;
//			}
//
//			public class PCAPlotRenderer extends XYLineAndShapeRenderer {
//				/** Color points by corresponding depth in the tree or by command leading to this point. **/
//				public boolean colorByDepth = plotColorsByDepth;
//
//				public PCAPlotRenderer() {
//					super(false, true); //boolean lines, boolean shapes
//					setSeriesShape( 0, new Rectangle2D.Double( -2.0, -2.0, 2.0, 2.0 ) );
//					setUseOutlinePaint(false);
//				}
//
//				@Override
//				public Paint getItemPaint(int series, int item) {
//					if (colorByDepth) {
//						return Node.getColorFromTreeDepth(nodeList.get(item).treeDepth);
//					}else{
//						Color dotColor = Color.RED;
//						switch (nodeList.get(item).treeDepth % 4) {
//						case 0:
//							dotColor = actionColor1;
//							break;
//						case 1:
//							dotColor = actionColor2;
//							break;
//						case 2:
//							dotColor = actionColor3;
//							break;
//						case 3:
//							dotColor = actionColor4;
//							break;
//						}
//						return dotColor;
//					}
//				}
//				@Override
//				public java.awt.Shape getItemShape(int row, int col) { // Dumb because box2d also has shape imported.
//					//				if (col == pane.selectedPoint) {
//					//					return (java.awt.Shape)BigMarker;
//					//				} else {
//					return super.getItemShape(row, col);
//					//				}
//				}
//			}
//		}
//	}


	@Override
	public void addRootNode(Node node) {
		rootNodes.add(node);
	}
}

