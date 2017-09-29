import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Vector3f;

import org.jblas.FloatMatrix;
import org.jblas.Singular;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.common.XForm;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.DomainInfo;
import org.jfree.data.Range;
import org.jfree.data.RangeInfo;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

/*
 * 
 * 
 */

@SuppressWarnings("serial")
public class FSM_UI extends JFrame implements ChangeListener, Runnable{

	/** Negotiator acts like a listener. **/
	Negotiator negotiator;

	/** Thread loop running? **/
	public boolean running = true;

	/** Tree root nodes associated with this interface. **/
	ArrayList<TrialNodeMinimal> rootNodes = new ArrayList<TrialNodeMinimal>();

	/** Individual pane for the tree. **/
	TreePane treePane;

	/** Pane for the tabbed side of the interface. **/
	JTabbedPane tabPane;

	/** Pane for the runner. **/
	RunnerPane runnerPane;

	/** Pane for the snapshots of the runner. **/
	SnapshotPane snapshotPane;

	/** Plots here. **/
	DataPane dataPane;

	/** Selected node by user click/key **/
	TrialNodeMinimal selectedNode;

	/** List of panes which can be activated, deactivated. **/
	private ArrayList<TabbedPaneActivator> allTabbedPanes= new ArrayList<TabbedPaneActivator>(); //List of all panes in the tabbed part

	/** Window width **/
	public static int windowWidth = 1920;

	/** Window height **/
	public static int windowHeight = 1000;

	/** Attempted frame rate **/
	private int FPS = 25;

	/** Usable milliseconds per frame **/
	private long MSPF = (long)(1f/(float)FPS * 1000f);

	/** Drawing offsets within the viewing panel (i.e. non-physical) **/
	public int xOffsetPixels_init = 960;
	public int xOffsetPixels = xOffsetPixels_init;
	public int yOffsetPixels = 100;

	/** Runner coordinates to pixels. **/
	public float runnerScaling = 10f;

	private final Color defaultDrawColor = new Color(0.2f,0.2f,0.5f);
	private final Color historyDrawColor = new Color(0.6f,0.6f,0.6f);
	private final Color appleGray = new Color(230,230,230);

	final Font QWOPLittle = new Font("Ariel", Font.BOLD,21);
	final Font QWOPBig = new Font("Ariel", Font.BOLD,28);


	/** Continuously update the estimate of the display loop time in milliseconds. **/
	private long avgLoopTime = MSPF;
	/** Filter the average loop time. Lower numbers gives more weight to the lower estimate, higher numbers gives more weight to the old value. **/
	private final float loopTimeFilter = 100f;
	private long lastIterTime = System.currentTimeMillis();
	/** Have we turned the physics off due to slowness? **/
	private boolean physOn = false;
	/** Keep track of whether we sent a pause tree command back to negotiator. **/
	private boolean treePause = false;

	/********** Writing actions on the left pane. **********/
	/** Fonts used for drawing on the left side pane. **/
	private final Font giantFont = new Font("Ariel",Font.BOLD,36);
	private final Font bigFont = new Font("Ariel", Font.BOLD, 16);
	private final Font littleFont = new Font("Ariel", Font.BOLD, 12);

	/** Spacing for sequence number drawing on the left side panel. **/
	private final int vertTextSpacing = 18;
	private final int vertTextAnchor = 15;

	/** State machine states for all UI **/
	public enum Status{
		IDLE_ALL, INITIALIZE, DRAW_ALL, VIEW_RUN
	}

	private Status currentStatus = Status.IDLE_ALL;
	private Status previousStatus = Status.IDLE_ALL;

	public FSM_UI(){
		Container pane = this.getContentPane();
		pane.setLayout(new GridBagLayout());

		/**** Tabbed panes ****/
		GridBagConstraints dataConstraints = new GridBagConstraints();
		dataConstraints.fill = GridBagConstraints.HORIZONTAL;
		dataConstraints.gridx = 0;
		dataConstraints.gridy = 1;
		dataConstraints.weightx = 0.3;
		dataConstraints.weighty = 0.1;
		dataConstraints.ipady = (int)(0.28*windowHeight);
		dataConstraints.ipadx = (int)(windowWidth*0.5);

		/* Pane for all tabs */
		tabPane = new JTabbedPane();
		tabPane.setBorder(BorderFactory.createRaisedBevelBorder());

		/* Runner pane */   
		runnerPane = new RunnerPane();
		tabPane.addTab("Run Animation", runnerPane);

		/* Snapshot pane */
		snapshotPane = new SnapshotPane();
		tabPane.addTab("State Viewer", snapshotPane);

		/* Data pane */
		dataPane = new DataPane();
		tabPane.addTab("Data Viewer", dataPane);

		/* Tree pane */
		treePane = new TreePane();

		pane.add(tabPane, dataConstraints);

		allTabbedPanes.add(runnerPane);
		allTabbedPanes.add(snapshotPane);
		allTabbedPanes.add(dataPane);
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

	/** Main graphics loop. **/
	public void run(){
		while (running){
			long currentTime = System.currentTimeMillis();
			switch(currentStatus){
			case IDLE_ALL:
				currentStatus = Status.INITIALIZE;
				break;
			case INITIALIZE:
				currentStatus = Status.DRAW_ALL;
				break;
			case DRAW_ALL:
				if (physOn){
					Iterator<TrialNodeMinimal> iter = rootNodes.iterator();
					while (iter.hasNext()){
						iter.next().stepTreePhys(1);
					}
				}
				repaint();
				break;
			case VIEW_RUN:
				break;
			default:
				break;
			}

			if (currentStatus != previousStatus){
				negotiator.statusChange_UI(currentStatus);
			}

			previousStatus = currentStatus;

			long extraTime = MSPF - (System.currentTimeMillis() - currentTime);
			if (extraTime > 5){
				try {
					Thread.sleep(extraTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// Tree frames per sec
			avgLoopTime = (long)(((loopTimeFilter - 1f) * avgLoopTime + 1f * (System.currentTimeMillis() - lastIterTime)) / loopTimeFilter); // Filter the loop time
			lastIterTime = System.currentTimeMillis();		
		}
	}

	/** Pick a node for the UI to highlight and potentially display. **/
	public void selectNode(TrialNodeMinimal selected){
		boolean success = false; // We don't allow new node selection while a realtime game is being played. 
		if (negotiator != null) success = negotiator.uiNodeSelect(selected);
		if (success){
			if (selectedNode != null){ // Clear things from the old selected node.
				selectedNode.displayPoint = false;
				selectedNode.clearBranchColor();
				selectedNode.clearBranchZOffset();
			}
			selectedNode = selected;
			selectedNode.displayPoint = true;
			selectedNode.nodeColor = Color.RED;
			selectedNode.setBranchZOffset(0.4f);
			if (snapshotPane.active) snapshotPane.giveSelectedNode(selectedNode);
			if (dataPane.active) dataPane.update(); // Updates data being put on plots
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		for (TabbedPaneActivator p: allTabbedPanes){
			p.deactivateTab();
		}
		allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();
	}

	public void setNegotiator(Negotiator negotiator){
		this.negotiator = negotiator;
	}

	/** Draw the actions on the left side pane. **/
	private void drawActionString(int[] sequence, Graphics g){
		drawActionString(sequence, g, -1);
	}

	private void drawActionString(int[] sequence, Graphics g, int highlightIdx){
		g.setFont(bigFont);
		g.setColor(Color.BLACK);
		g.drawString("Selected sequence: ", 10, vertTextAnchor);
		g.setColor(Color.DARK_GRAY);

		int currIdx = 0;
		int lineNum = 1;
		while (currIdx < sequence.length - 1){
			String line = sequence[currIdx] + ",";

			if (currIdx == highlightIdx){
				g.setColor(Color.GREEN);
			}else{
				g.setColor(Color.DARK_GRAY);
			}
			g.drawString(line, 10 + (currIdx % 4)*50, vertTextAnchor + vertTextSpacing * (lineNum + 2));
			currIdx++;
			lineNum = currIdx/4 + 1;
		}

		// Draw the little keys above the column.
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.DARK_GRAY);
		g2.drawRoundRect(8, vertTextAnchor + 15, 30, 20, 5, 5);
		g2.drawRoundRect(8 + 49, vertTextAnchor + 15, 30, 20, 5, 5);
		g2.drawRoundRect(8 + 2*49, vertTextAnchor + 15, 30, 20, 5, 5);
		g2.drawRoundRect(8 + 3*49, vertTextAnchor + 15, 30, 20, 5, 5);

		g2.setColor(Color.LIGHT_GRAY);
		g2.fillRoundRect(8, vertTextAnchor + 15, 30, 20, 6, 6);
		g2.fillRoundRect(8 + 49, vertTextAnchor + 15, 30, 20, 6, 6);
		g2.fillRoundRect(8 + 2*49, vertTextAnchor + 15, 30, 20, 6, 6);
		g2.fillRoundRect(8 + 3*49, vertTextAnchor + 15, 30, 20, 6, 6);

		g.setFont(littleFont);
		g.setColor(Color.BLACK);
		g.drawString("- -", 12, vertTextAnchor + 30);
		g.drawString("W O", 12 + 49, vertTextAnchor + 30);
		g.drawString("- -", 12 + 2*49, vertTextAnchor + 30);
		g.drawString("Q P", 12 + 3*49, vertTextAnchor + 30);
	}

	/** Tree pane **/
	public class TreePane extends GenericGLPanel implements TabbedPaneActivator, GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

		/** For rendering text overlays. Note that textrenderer is for overlays while GLUT is for labels in world space **/
		TextRenderer textRenderBig;
		TextRenderer textRenderSmall;

		/** Currently tracked mouse x location in screen coordinates of the panel **/
		int mouseX;

		/** Currently tracked mouse x location in screen coordinates of the panel **/
		int mouseY;

		/** Is the mouse cursor inside the bounds of the tree panel? **/
		boolean mouseInside = false;

		public TreePane(){
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
			if (negotiator == null) return;

			super.display(drawable);

			float ptSize = 5f/cam.getZoomFactor(); //Let the points be smaller/bigger depending on zoom, but make sure to cap out the size!
			ptSize = Math.min(ptSize, 1f);

			for (TrialNodeMinimal node : rootNodes){
				gl.glColor3f(1f, 0.1f, 0.1f);
				gl.glPointSize(5*ptSize);

				gl.glBegin(GL2.GL_POINTS);
				node.drawNodes_below(gl);
				gl.glEnd();

				gl.glColor3f(1f, 1f, 1f);
				gl.glBegin(GL2.GL_LINES);
				node.drawLines_below(gl);
				gl.glEnd();
			}

			// Draw games played and games/sec in upper left.
			textRenderBig.beginRendering(panelWidth, panelHeight);
			textRenderBig.setColor(0.7f, 0.7f, 0.7f, 1.0f);
			textRenderBig.draw(negotiator.getGamesPlayed() + " games", 20, panelHeight - 50);

			if (treePause){
				textRenderBig.setColor(0.7f, 0.1f, 0.1f, 1.0f);	
				textRenderBig.draw("PAUSED", panelWidth/2, panelHeight - 50);
			}
			textRenderBig.endRendering();

			// Draw the FPS of the tree drawer at the moment.
			textRenderSmall.beginRendering(panelWidth, panelHeight);
			textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);
			int fps = (int)(10000./avgLoopTime);
			textRenderSmall.draw( ( (Math.abs(fps) > 10000) ? "???" : fps/10f ) + " FPS", panelWidth - 75, panelHeight - 20);
			// Physics on/off alert
			if (physOn){
				textRenderSmall.setColor(0.1f, 0.7f, 0.1f, 1.0f);
				textRenderSmall.draw("Tree physics on", panelWidth - 120, panelHeight - 35);
			}else{
				textRenderSmall.setColor(0.7f, 0.1f, 0.1f, 1.0f);
				textRenderSmall.draw("Tree physics off", panelWidth - 120, panelHeight - 35);
			}
			// Number of imported games
			textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);
			textRenderSmall.draw(negotiator.getGamesImported() + " Games imported", 20, panelHeight - 70);
			// Total games played
			textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);
			textRenderSmall.draw(negotiator.getGamesTotal() + " Total games", 20, panelHeight - 85);

			textRenderSmall.setColor(0.1f, 0.7f, 0.1f, 1.0f);
			textRenderSmall.draw(Math.round(negotiator.getTimeSimulated()/360)/10f + " hours simulated!", 20, panelHeight - 100);
			textRenderSmall.draw((int)negotiator.getGamesPerSecond() + " games/s", 20, panelHeight - 115);

			textRenderSmall.endRendering();

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
			if (e.getWheelRotation() < 0){ //Negative mouse direction -> zoom in.
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
			if (e.isMetaDown()){
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

			//Navigating the focused node tree
			int keyCode = e.getKeyCode();

			if(e.isMetaDown()){ //if we're using GL, then we'll move the camera with mac key + arrows
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
			}else if(e.isShiftDown()){
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
					if (treePause){
						negotiator.pauseTree();
					}else{
						negotiator.unpauseTree();
					}
					break;
				case KeyEvent.VK_C:
					negotiator.redistributeNodes();
					break;
				case KeyEvent.VK_V:
					physOn = !physOn;
					break;
				case KeyEvent.VK_ESCAPE:
					System.exit(0);
					break;
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
			if (snapshotPane.active && mouseInside){
				ArrayList<TrialNodeMinimal> snapshotLeaves = snapshotPane.getDisplayedLeaves();
				if (snapshotLeaves.size() > 0){
					TrialNodeMinimal nearest = cam.nodeFromClick_set(mouseX, mouseY, snapshotLeaves, 50);
					if (nearest != null){
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
		private void arrowSwitchNode(int direction,int depth){
			//Stupid way of getting this one's index according to its parent.
			if(selectedNode != null){
				if (selectedNode.treeDepth == 0){ // At root, don't try to look at parent.
					// <nothing>
				}else{
					int thisIndex = selectedNode.parent.children.indexOf(selectedNode);
					//This set of logicals eliminates the edge cases, then takes the proposed action as default
					if (thisIndex == 0 && direction == -1){ //We're at the lowest index of this node and must head to a new parent node.
						ArrayList<TrialNodeMinimal> blacklist = new ArrayList<TrialNodeMinimal>(); //Keep a blacklist of nodes that already proved to be duds.
						blacklist.add(selectedNode);
						nextOver(selectedNode.parent,blacklist,1,direction,selectedNode.parent.children.indexOf(selectedNode),0);

					}else if (thisIndex == selectedNode.parent.children.size()-1 && direction == 1){ //We're at the highest index of this node and must head to a new parent node.
						ArrayList<TrialNodeMinimal> blacklist = new ArrayList<TrialNodeMinimal>();
						blacklist.add(selectedNode);
						nextOver(selectedNode.parent,blacklist, 1,direction,selectedNode.parent.children.indexOf(selectedNode),0);

					}else{ //Otherwise we can just switch nodes within the scope of this parent.
						selectNode(selectedNode.parent.children.get(thisIndex+direction));
					}
				}

				//These logicals just take the proposed motion (or not) and ignore any edges.
				if(depth == 1 && selectedNode.children.size()>0){ //Go further down the tree if this node has children
					selectNode(selectedNode.children.get(0));
				}else if(depth == -1 && selectedNode.treeDepth>0){ //Go up the tree if this is not root.
					selectNode(selectedNode.parent);
				}
				repaint();
			}
		}

		/** Take a node back a layer. Don't return to node past. Try to go back out by the deficit depth amount in the +1 or -1 direction left/right **/
		private boolean nextOver(TrialNodeMinimal current, ArrayList<TrialNodeMinimal> blacklist, int deficitDepth, int direction,int prevIndexAbove,int numTimesTried){ // numTimesTried added to prevent some really deep node for causing some really huge search through the whole tree. If we don't succeed in a handful of iterations, just fail quietly.
			numTimesTried++;
			boolean success = false;
			//TERMINATING CONDITIONS-- fail quietly if we get back to root with nothing. Succeed if we get back to the same depth we started at.
			if (deficitDepth == 0){ //We've successfully gotten back to the same level. Great.
				selectNode(current);
				return true;
			}else if(current.treeDepth == 0){
				return true; // We made it back to the tree's root without any success. Just return.

			}else if(numTimesTried>100){// If it takes >100 movements between nodes, we'll just give up.
				return true;
			}else{
				//CCONDITIONS WE NEED TO STEP BACKWARDS TOWARDS ROOT.
				//If this new node has no children OR it's 1 child is on the blacklist, move back up the tree.
				if((prevIndexAbove+1 == current.children.size() && direction == 1) || (prevIndexAbove == 0 && direction == -1)){
					blacklist.add(current); 
					success = nextOver(current.parent,blacklist,deficitDepth+1,direction,current.parent.children.indexOf(current),numTimesTried); //Recurse back another node.
				}else if (!(current.children.size() >0) || (blacklist.contains(current.children.get(0)) && current.children.size() == 1)){ 
					blacklist.add(current); 
					success = nextOver(current.parent,blacklist,deficitDepth+1,direction,current.parent.children.indexOf(current),numTimesTried); //Recurse back another node.
				}else{

					//CONDITIONS WE NEED TO GO DEEPER:
					if(direction == 1){ //March right along this previous node.
						for (int i = prevIndexAbove+1; i<current.children.size(); i++){
							success = nextOver(current.children.get(i),blacklist,deficitDepth-1,direction,-1,numTimesTried);
							if(success){
								return true;
							}
						}
					}else if(direction == -1){ //March left along this previous node
						for (int i = prevIndexAbove-1; i>=0; i--){
							success = nextOver(current.children.get(i),blacklist,deficitDepth-1,direction,current.children.get(i).children.size(),numTimesTried);
							if(success){
								return true;
							}
						}
					}
				}
			}
			success = true;
			return success;
		}

	}

	/** Pane for the animated runner. **/
	public class RunnerPane extends JPanel implements TabbedPaneActivator {

		public int headPos;

		boolean active = false;

		private World world;

		public RunnerPane(){}

		public void paintComponent(Graphics g){
			if (!active) return;
			super.paintComponent(g);

			if (world != null){
				Body newBody = world.getBodyList();
				while (newBody != null){

					Shape newfixture = newBody.getShapeList();

					while(newfixture != null){

						if(newfixture.getType() == ShapeType.POLYGON_SHAPE){

							PolygonShape newShape = (PolygonShape)newfixture;
							Vec2[] shapeVerts = newShape.m_vertices;
							for (int k = 0; k<newShape.m_vertexCount; k++){

								XForm xf = newBody.getXForm();
								Vec2 ptA = XForm.mul(xf,shapeVerts[k]);
								Vec2 ptB = XForm.mul(xf, shapeVerts[(k+1) % (newShape.m_vertexCount)]);
								g.drawLine((int)(runnerScaling * ptA.x) + xOffsetPixels,
										(int)(runnerScaling * ptA.y) + yOffsetPixels,
										(int)(runnerScaling * ptB.x) + xOffsetPixels,
										(int)(runnerScaling * ptB.y) + yOffsetPixels);			    		
							}
						}else if (newfixture.getType() == ShapeType.CIRCLE_SHAPE){
							CircleShape newShape = (CircleShape)newfixture;
							float radius = newShape.m_radius;
							headPos = (int)(runnerScaling * newBody.getPosition().x);
							g.drawOval((int)(runnerScaling * (newBody.getPosition().x - radius) + xOffsetPixels),
									(int)(runnerScaling * (newBody.getPosition().y - radius) + yOffsetPixels),
									(int)(runnerScaling * radius * 2),
									(int)(runnerScaling * radius * 2));		

						}else if(newfixture.getType() == ShapeType.EDGE_SHAPE){

							EdgeShape newShape = (EdgeShape)newfixture;
							XForm trans = newBody.getXForm();

							Vec2 ptA = XForm.mul(trans, newShape.getVertex1());
							Vec2 ptB = XForm.mul(trans, newShape.getVertex2());
							Vec2 ptC = XForm.mul(trans, newShape.getVertex2());

							g.drawLine((int)(runnerScaling * ptA.x) + xOffsetPixels,
									(int)(runnerScaling * ptA.y) + yOffsetPixels,
									(int)(runnerScaling * ptB.x) + xOffsetPixels,
									(int)(runnerScaling * ptB.y) + yOffsetPixels);			    		
							g.drawLine((int)(runnerScaling * ptA.x) + xOffsetPixels,
									(int)(runnerScaling * ptA.y) + yOffsetPixels,
									(int)(runnerScaling * ptC.x) + xOffsetPixels,
									(int)(runnerScaling * ptC.y) + yOffsetPixels);			    		

						}else{
							System.out.println("Not found: " + newfixture.m_type.name());
						}
						newfixture = newfixture.getNext();
					}
					newBody = newBody.getNext();
				}
				//This draws the "road" markings to show that the ground is moving relative to the dude.
				for(int i = 0; i<this.getWidth()/69; i++){
					g.drawString("_", ((xOffsetPixels - xOffsetPixels_init-i * 70) % getWidth()) + getWidth(), yOffsetPixels + 92);
					keyDrawer(g, negotiator.Q,negotiator.W,negotiator.O,negotiator.P);
				}

				drawActionString(negotiator.getCurrentSequence(), g, negotiator.getCurrentActionIdx());

			}else{
				keyDrawer(g, false, false, false, false);
			}

			//    	g.drawString(dc.format(-(headpos+30)/40.) + " metres", 500, 110);
			xOffsetPixels = -headPos + xOffsetPixels_init;

		}

		public void setWorldToView(World world){
			this.world = world;
		}

		public void clearWorldToView(){
			world = null;
		}

		@Override
		public void activateTab() {
			active = true;
		}
		@Override
		public void deactivateTab() {
			active = false;
			negotiator.killRealtimeRun();
		}
		public void keyDrawer(Graphics g, boolean q, boolean w, boolean o, boolean p){

			int qOffset = (q ? 10:0);
			int wOffset = (w ? 10:0);
			int oOffset = (o ? 10:0);
			int pOffset = (p ? 10:0);

			int offsetBetweenPairs = getWidth()/4;
			int startX = -45;
			int startY = yOffsetPixels - 200;
			int size = 40;

			Font activeFont;
			FontMetrics fm;
			Graphics2D g2 = (Graphics2D)g;

			g2.setColor(Color.DARK_GRAY);
			g2.drawRoundRect(startX + 80 - qOffset/2, startY - qOffset/2, size + qOffset, size + qOffset, (size + qOffset)/10, (size + qOffset)/10);
			g2.drawRoundRect(startX + 160 - wOffset/2, startY - wOffset/2, size + wOffset, size + wOffset, (size + wOffset)/10, (size + wOffset)/10);
			g2.drawRoundRect(startX + 240 - oOffset/2 + offsetBetweenPairs, startY - oOffset/2, size + oOffset, size + oOffset, (size + oOffset)/10, (size + oOffset)/10);
			g2.drawRoundRect(startX + 320 - pOffset/2 + offsetBetweenPairs, startY - pOffset/2, size + pOffset, size + pOffset, (size + pOffset)/10, (size + pOffset)/10);

			g2.setColor(Color.LIGHT_GRAY);
			g2.fillRoundRect(startX + 80 - qOffset/2, startY - qOffset/2, size + qOffset, size + qOffset, (size + qOffset)/10, (size + qOffset)/10);
			g2.fillRoundRect(startX + 160 - wOffset/2, startY - wOffset/2, size + wOffset, size + wOffset, (size + wOffset)/10, (size + wOffset)/10);
			g2.fillRoundRect(startX + 240 - oOffset/2 + offsetBetweenPairs, startY - oOffset/2, size + oOffset, size + oOffset, (size + oOffset)/10, (size + oOffset)/10);
			g2.fillRoundRect(startX + 320 - pOffset/2 + offsetBetweenPairs, startY - pOffset/2, size + pOffset, size + pOffset, (size + pOffset)/10, (size + pOffset)/10);

			g2.setColor(Color.BLACK);

			//Used for making sure text stays centered.

			activeFont = q ? QWOPBig:QWOPLittle;
			g2.setFont(activeFont);
			fm = g2.getFontMetrics();
			g2.drawString("Q", startX + 80 + size/2-fm.stringWidth("Q")/2, startY + size/2+fm.getHeight()/3);


			activeFont = w ? QWOPBig:QWOPLittle;
			g2.setFont(activeFont);
			fm = g2.getFontMetrics();
			g2.drawString("W", startX + 160 + size/2-fm.stringWidth("W")/2, startY + size/2+fm.getHeight()/3);

			activeFont = o ? QWOPBig:QWOPLittle;
			g2.setFont(activeFont);
			fm = g2.getFontMetrics();
			g2.drawString("O", startX + 240 + size/2-fm.stringWidth("O")/2 + offsetBetweenPairs, startY + size/2+fm.getHeight()/3);

			activeFont = p ? QWOPBig:QWOPLittle;
			g2.setFont(activeFont);
			fm = g2.getFontMetrics();
			g2.drawString("P", startX + 320 + size/2-fm.stringWidth("P")/2 + offsetBetweenPairs, startY + size/2+fm.getHeight()/3);

		}
	}

	/** Pane for the fixed view of the runner at each node. **/
	public class SnapshotPane extends JPanel implements TabbedPaneActivator, MouseListener, MouseMotionListener, MouseWheelListener {

		/** Number of runner states in the past to display. **/
		public int numHistoryStatesDisplay = 10;

		/** Is this tab currently active? If not, don't run the draw loop. **/
		public boolean active = false;

		/** Highlight stroke for line drawing. **/
		Stroke normalStroke = new BasicStroke(0.5f);

		/** Highlight stroke for line drawing. **/
		Stroke boldStroke = new BasicStroke(2);

		/** Node we are focusing on displaying. **/
		private TrialNodeMinimal snapshotNode;

		TrialNodeMinimal highlightedRunNode;

		TrialNodeMinimal queuedFutureLeaf;

		Font floatingActionText = new Font("Ariel", Font.BOLD, 18);

		private int mouseX = 0;
		private int mouseY = 0;
		private boolean mouseIsIn = false;

		/** How close do we have to be (squared) from the chest of a single figure for it to be eligible for selection. **/
		float figureSelectThreshSq = 150;

		public SnapshotPane(){
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		private ArrayList<TrialNodeMinimal> focusLeaves = new ArrayList<TrialNodeMinimal>();
		private ArrayList<XForm[]> transforms = new ArrayList<XForm[]>();
		private ArrayList<Stroke> strokes = new ArrayList<Stroke>();
		private ArrayList<Color> colors = new ArrayList<Color>();

		Shape[] shapes;

		/** Assign a selected node for the snapshot pane to display. **/
		public void giveSelectedNode(TrialNodeMinimal node){
			transforms.clear();
			focusLeaves.clear();
			strokes.clear();
			colors.clear();

			shapes = QWOPGame.shapeList;

			/***** Focused node first *****/
			snapshotNode = node;
			XForm[] nodeTransform = snapshotNode.state.getTransforms();
			// Make the sequence centered around the selected node state.
			xOffsetPixels = xOffsetPixels_init + (int)(-runnerScaling * nodeTransform[1].position.x);
			transforms.add(nodeTransform);
			strokes.add(boldStroke);
			colors.add(Color.BLACK);
			focusLeaves.add(snapshotNode);

			/***** History nodes *****/
			TrialNodeMinimal historyNode = snapshotNode;
			for (int i = 0; i < numHistoryStatesDisplay; i++){
				if (historyNode.treeDepth > 0){
					historyNode = historyNode.parent;
					nodeTransform = historyNode.state.getTransforms();
					transforms.add(nodeTransform);
					strokes.add(normalStroke);
					colors.add(historyDrawColor);
					focusLeaves.add(historyNode);
				}
			}

			/***** Future leaf nodes *****/
			ArrayList<TrialNodeMinimal> descendants = new ArrayList<TrialNodeMinimal>();
			for (int i = 0; i < selectedNode.children.size(); i++){
				TrialNodeMinimal child = selectedNode.children.get(i);
				child.getLeaves(descendants);

				Color runnerColor = TrialNodeMinimal.getColorFromTreeDepth(i*10);
				child.setBranchColor(runnerColor); // Change the color on the tree too.

				for (TrialNodeMinimal descendant : descendants){
					if (descendant.state != null){
						focusLeaves.add(descendant);
						transforms.add(descendant.state.getTransforms());
						strokes.add(normalStroke);
						colors.add(runnerColor);
					}
				}
			}
		}

		private float getDistFromMouseSq(float x, float y){
			float xdist = (mouseX - (runnerScaling * x + xOffsetPixels));
			float ydist = (mouseY - (runnerScaling * y + yOffsetPixels));
			return xdist*xdist + ydist*ydist;
		}

		/** Draws the selected node state and potentially previous and future states. **/
		public void paintComponent(Graphics g){
			if (!active) return;
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;

			if (snapshotNode != null && snapshotNode.state != null){ // TODO this keeps the root node from throwing errors because I didn't assign it a state. We really should do that.

				float bestSoFar = Float.MAX_VALUE;
				int bestIdx = Integer.MIN_VALUE;

				// Figure out if the mouse close enough to highlight one state.
				if (mouseIsIn && mouseX > getWidth()/2){ // If we are mousing over this panel, see if we're hovering close enough over any particular dude state.

					// Check body first
					for (int i = 0; i < focusLeaves.size(); i++){
						float distSq = getDistFromMouseSq(focusLeaves.get(i).state.body.x,focusLeaves.get(i).state.body.y);
						if (distSq < bestSoFar  && distSq < figureSelectThreshSq){
							bestSoFar = distSq;
							bestIdx = i;
						}
					}
					// Then head
					if (bestIdx < 0){
						for (int i = 0; i < focusLeaves.size(); i++){
							float distSq = getDistFromMouseSq(focusLeaves.get(i).state.head.x,focusLeaves.get(i).state.head.y);
							if (distSq < bestSoFar  && distSq < figureSelectThreshSq){
								bestSoFar = distSq;
								bestIdx = i;
							}
						}
					}
					// Then both feet equally
					if (bestIdx < 0){
						for (int i = 0; i < focusLeaves.size(); i++){
							float distSq = getDistFromMouseSq(focusLeaves.get(i).state.lfoot.x,focusLeaves.get(i).state.lfoot.y);
							if (distSq < bestSoFar  && distSq < figureSelectThreshSq){
								bestSoFar = distSq;
								bestIdx = i;
							}
							distSq = getDistFromMouseSq(focusLeaves.get(i).state.rfoot.x,focusLeaves.get(i).state.rfoot.y);
							if (distSq < bestSoFar  && distSq < figureSelectThreshSq){
								bestSoFar = distSq;
								bestIdx = i;
							}

						}
					}
				}

				// Draw all non-highlighted runners.
				for (int i = transforms.size() - 1; i >= 0; i--){
					if (!mouseIsIn || bestIdx != i){
						if (highlightedRunNode != null && focusLeaves.get(i).treeDepth > selectedNode.treeDepth){ // Make the nodes after the selected one lighter if one is highlighted.
							drawRunner(g2, colors.get(i).brighter(), strokes.get(i), shapes, transforms.get(i));
						}else{
							drawRunner(g2, colors.get(i), strokes.get(i), shapes, transforms.get(i));
						}

					}
				}

				// Change things if one runner is selected.
				if (mouseIsIn && bestIdx >= 0){
					TrialNodeMinimal newHighlightNode = focusLeaves.get(bestIdx);
					changeFocusedFuture(g2, highlightedRunNode, newHighlightNode);
					highlightedRunNode = newHighlightNode;

					// Externally commanded pick, instead of mouse-picked.
				}else if(queuedFutureLeaf != null){
					changeFocusedFuture(g2, highlightedRunNode, queuedFutureLeaf);
					highlightedRunNode = queuedFutureLeaf;

				}else if (highlightedRunNode != null){ // When we stop mousing over, clear the brightness changes.
					highlightedRunNode.displayPoint = false;
					highlightedRunNode.nodeColor = Color.GREEN;
					rootNodes.get(0).resetLineBrightness_below();
					highlightedRunNode.clearBackwardsBranchZOffset();
					highlightedRunNode = null;
				}

				// Draw the sequence too.
				drawActionString(selectedNode.getSequence(), g);
			}
		}

		/** Change highlighting on both the tree and the snapshot when selections change. **/
		private void changeFocusedFuture(Graphics2D g2, TrialNodeMinimal oldFuture, TrialNodeMinimal newFuture){
			// Clear out highlights from the old node.
			if (oldFuture != null && !oldFuture.equals(newFuture)){
				oldFuture.clearBackwardsBranchZOffset();
				oldFuture.displayPoint = false;
				oldFuture.nodeColor = Color.GREEN;
			}

			// Add highlights to the new node if it's different or previous is nonexistant
			if (oldFuture == null || !oldFuture.equals(newFuture)){
				newFuture.displayPoint = true;
				newFuture.nodeColor = Color.ORANGE;
				newFuture.setBackwardsBranchZOffset(0.8f);
				newFuture.highlightSingleRunToThisNode(); // Tell the tree to highlight a section and darken others.
			}
			// Draw
			int idx = focusLeaves.indexOf(newFuture);
			if (idx > -1){ // Focus leaves no longer contains the no focus requested.
				try{
					drawRunner(g2, colors.get(idx).darker(), boldStroke, shapes, transforms.get(idx));

					TrialNodeMinimal currentNode = newFuture;

					// Also draw parent nodes back the the selected one to view the run that leads to the highlighted failure.
					int prevX = Integer.MAX_VALUE;
					while (currentNode.treeDepth > selectedNode.treeDepth){
						// Make color shades slightly alternate between subsequent move frames.
						Color everyOtherEvenColor = colors.get(idx).darker();
						if (currentNode.treeDepth % 2 == 0){
							everyOtherEvenColor = everyOtherEvenColor.darker();
						}
						drawRunner(g2, everyOtherEvenColor, boldStroke, shapes, currentNode.state.getTransforms());

						// Draw actions above their heads.
						g2.setFont(QWOPBig);
						g2.setFont(floatingActionText);
						g2.setColor(everyOtherEvenColor);
						prevX = Math.min((int)(runnerScaling * currentNode.state.head.x) + xOffsetPixels - 3,prevX - 25);

						g2.drawString(String.valueOf(currentNode.controlAction), 
								prevX, 
								Math.min((int)(runnerScaling * currentNode.state.head.y) + yOffsetPixels - 25, 45));

						currentNode = currentNode.parent;
					}
				}catch(IndexOutOfBoundsException e){
					// I don't really care tbh. Just skip this one.
				}
			}
		}

		/** Focus a single future leaf **/
		public void giveSelectedFuture(TrialNodeMinimal queuedFutureLeaf){
			this.queuedFutureLeaf = queuedFutureLeaf;
		}

		/** Draw the runner at a certain state. **/
		private void drawRunner(Graphics2D g, Color drawColor, Stroke stroke, Shape[] shapes, XForm[] transforms){

			for (int i = 0; i < shapes.length; i++){
				g.setColor(drawColor);
				g.setStroke(stroke);
				switch(shapes[i].getType()){
				case CIRCLE_SHAPE:
					CircleShape circleShape = (CircleShape)shapes[i];
					float radius = circleShape.getRadius();
					Vec2 circleCenter = XForm.mul(transforms[i], circleShape.getLocalPosition());
					g.drawOval((int)(runnerScaling * (circleCenter.x - radius) + xOffsetPixels),
							(int)(runnerScaling * (circleCenter.y - radius) + yOffsetPixels),
							(int)(runnerScaling * radius * 2),
							(int)(runnerScaling * radius * 2));
					break;
				case POLYGON_SHAPE:
					//Get both the shape and its transform.
					PolygonShape polygonShape = (PolygonShape)shapes[i];
					XForm transform = transforms[i];

					// Ground is black regardless.
					if (shapes[i].m_filter.groupIndex == 1){
						g.setColor(Color.BLACK);
						g.setStroke(normalStroke);
					}
					for (int j = 0; j < polygonShape.getVertexCount(); j++){ // Loop through polygon vertices and draw lines between them.
						Vec2 ptA = XForm.mul(transform, polygonShape.m_vertices[j]);
						Vec2 ptB = XForm.mul(transform, polygonShape.m_vertices[(j + 1) % (polygonShape.getVertexCount())]); //Makes sure that the last vertex is connected to the first one.
						g.drawLine((int)(runnerScaling * ptA.x) + xOffsetPixels,
								(int)(runnerScaling * ptA.y) + yOffsetPixels,
								(int)(runnerScaling * ptB.x) + xOffsetPixels,
								(int)(runnerScaling * ptB.y) + yOffsetPixels);		
					}
					break;
				default:
					break;
				}
			}
		}

		/** Get the list of leave nodes (failure states) that we're displaying in the snapshot pane. **/
		public ArrayList<TrialNodeMinimal> getDisplayedLeaves(){
			return focusLeaves;
		}

		@Override
		public void activateTab() {
			active = true;
		}
		@Override
		public void deactivateTab() {
			active = false;
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {}

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
			queuedFutureLeaf = null; // No longer using what the tree says is focused when the mouse is in this pane.
		}

		@Override
		public void mouseExited(MouseEvent e) {
			mouseIsIn = false;
		}
	}

	public class DataPane extends JPanel implements TabbedPaneActivator, ChartMouseListener{

		/** How many plots do we want to squeeze in there horizontally? **/
		private final int numberOfPlots = 6;

		/** Array of the numberOfPlots number of plots we make. **/
		public ChartPanel[] plotPanels = new ChartPanel[numberOfPlots];

		/** Is this tab active? Do we bother to do updates in other words. **/
		private boolean active = false;

		/** Formatting stuff for the dot markers. **/
		private MarkerFormat format = new MarkerFormat(false,true,this);

		/** The variables and nodes associated with the plots. **/
		private XYDataset[] plotData = new XYDataset[numberOfPlots];

		public int selectedPoint = -1;

		public DataPane(){
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			for (int i = 0; i < numberOfPlots; i++){
				JFreeChart chart = createChart(null,null); // Null means no title and no data yet too
				ChartPanel chartPanel = new ChartPanel(chart);
				chartPanel.addChartMouseListener(this);
				chartPanel.setPopupMenu(null);
				chartPanel.setDomainZoomable(false);
				chartPanel.setRangeZoomable(false);
				chartPanel.setVisible(true);
				plotPanels[i] = chartPanel;
				add(chartPanel);
			}
			pack();
		}

		/** Check if the bounds need expanding, tell JFreeChart to update, and set the bounds correctly **/
		public void update(){

			// Fetching new data.
			ArrayList<TrialNodeMinimal> nodesBelow = new ArrayList<TrialNodeMinimal>();
			if (selectedNode != null){
				selectedNode.getNodes_below(nodesBelow);
				
				// A state pair being added to the first plot.
				XYPlot pl = (XYPlot)plotPanels[0].getChart().getPlot();
				LinkStateCombination statePlotDat1 = new LinkStateCombination(nodesBelow);
				statePlotDat1.addSeries(0, CondensedStateInfo.ObjectName.BODY, CondensedStateInfo.StateName.TH, CondensedStateInfo.ObjectName.BODY, CondensedStateInfo.StateName.Y);
				pl.setDataset(statePlotDat1);
				
				
				// A PCA plot being added to the second plot.
				pl = (XYPlot)plotPanels[1].getChart().getPlot();
				PCATransformedData pcaPlotDat1 = new PCATransformedData(nodesBelow);
				pcaPlotDat1.addSeries(0, 0, 1);
				pl.setDataset(pcaPlotDat1);
				
				pl = (XYPlot)plotPanels[2].getChart().getPlot();
				PCATransformedData pcaPlotDat2 = new PCATransformedData(nodesBelow);
				pcaPlotDat2.addSeries(0, 0, 2);
				pl.setDataset(pcaPlotDat2);
				
				pl = (XYPlot)plotPanels[3].getChart().getPlot();
				PCATransformedData pcaPlotDat3 = new PCATransformedData(nodesBelow);
				pcaPlotDat3.addSeries(0, 0, 3);
				pl.setDataset(pcaPlotDat3);
				
				pl = (XYPlot)plotPanels[4].getChart().getPlot();
				PCATransformedData pcaPlotDat4 = new PCATransformedData(nodesBelow);
				pcaPlotDat4.addSeries(0, 0, 4);
				pl.setDataset(pcaPlotDat4);
				
				
				//pl.getRangeAxis().setLabel(obj2.toString() + " " + st2.toString());
				//pl.getDomainAxis().setLabel(obj1.toString() + " " + st1.toString());
			}

			for (int i = 0; i < numberOfPlots; i++){
				JFreeChart chart = plotPanels[i].getChart();
				chart.fireChartChanged();
				//XYPlot plot = (XYPlot)(plotPanels[i].getChart().getPlot());
				//plot.getDomainAxis().setRange(range); TODO
				//plot.getRangeAxis().setRange(range);
			}
		}
		/** My default settings for each plot. **/
		private JFreeChart createChart(XYDataset dataset,String name) {
			JFreeChart chart = ChartFactory.createScatterPlot(name,
					"X", "Y", dataset, PlotOrientation.VERTICAL, false, false, false);

			chart.setBackgroundPaint(appleGray);
			chart.setPadding(new RectangleInsets(-8,-12,-8,-5)); // Pack em in really tight.
			chart.setBorderVisible(false);

			XYPlot plot = (XYPlot) chart.getPlot();

			plot.setNoDataMessage("NO DATA");
			plot.setDomainZeroBaselineVisible(true);
			plot.setRangeZeroBaselineVisible(true);
			plot.setRenderer(format);
			plot.setBackgroundPaint(Color.WHITE); // Background of actual plotting area, not the surrounding border area.

			format.setSeriesShape( 0, new Rectangle2D.Double( -2.0, -2.0, 2.0, 2.0 ) );
			format.setUseOutlinePaint(false);

			NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
			domainAxis.setAutoRangeIncludesZero(false);
			domainAxis.setTickMarkInsideLength(2.0f);
			domainAxis.setTickMarkOutsideLength(0.0f);
			domainAxis.setLabelFont(littleFont);
			domainAxis.setRange(new Range(-10, 10));

			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			rangeAxis.setTickMarkInsideLength(2.0f);
			rangeAxis.setTickMarkOutsideLength(0.0f);
			rangeAxis.setLabelFont(littleFont);
			rangeAxis.setRange(new Range(-10, 10));
			return chart;
		}

		@Override
		public void chartMouseClicked(ChartMouseEvent event) {
			ChartEntity entity = event.getEntity();
			if (entity == null)
				return;
		}

		@Override
		public void chartMouseMoved(ChartMouseEvent event) {}

		@Override
		public void activateTab() {
			active = true;
			update();

		}
		@Override
		public void deactivateTab() {
			active = false;
		}

		public class MarkerFormat extends XYLineAndShapeRenderer {
			DataPane pane;
			Rectangle2D BigMarker = new Rectangle2D.Double( -5.0, -5.0, 10.0, 10.0 );
			Color SelectedColor = new Color(0.5f,1,0.5f);
			Color UnSelectedColor = new Color(1f,0.25f,0.25f);

			public MarkerFormat(boolean lines, boolean shapes, DataPane pane) {
				super(lines, shapes);
				this.pane = pane;
			}

			@Override
			public Paint getItemPaint(int row, int col) {
				if (col == pane.selectedPoint) {
					return SelectedColor;
				} else {
					return UnSelectedColor;
				}
			}
			@Override
			public java.awt.Shape getItemShape(int row, int col){ // Dumb because box2d also has shape imported.
				if (col == pane.selectedPoint) {
					return (java.awt.Shape)BigMarker;
				} else {
					return (java.awt.Shape) super.getItemShape(row, col);
				}

			}
		}

		/** XYDataset gets data for ploting states vs other states here. **/
		private class LinkStateCombination extends AbstractXYDataset{

			/** Nodes to appear on the plot. **/
			private final ArrayList<TrialNodeMinimal> nodeList;

			private Map<Integer,Pair> dataSeries = new HashMap<Integer,Pair>();

			public LinkStateCombination(ArrayList<TrialNodeMinimal> nodes){
				nodeList = nodes;
			}

			public void addSeries(int plotIdx, CondensedStateInfo.ObjectName objectX, CondensedStateInfo.StateName stateX,
					CondensedStateInfo.ObjectName objectY, CondensedStateInfo.StateName stateY){
				dataSeries.put(plotIdx, new Pair(plotIdx, objectX, stateX,
						objectY, stateY));
			}

			@Override
			public Number getX(int series, int item) {
				CondensedStateInfo state = nodeList.get(item).state; // Item is which node.
				Pair dat = dataSeries.get(series);
				return state.getStateVarFromName(dat.objectX, dat.stateX);
			}
			@Override
			public Number getY(int series, int item) {
				CondensedStateInfo state = nodeList.get(item).state; // Item is which node.
				Pair dat = dataSeries.get(series);
				return state.getStateVarFromName(dat.objectY, dat.stateY);
			}

			/** State + body part name pairs for looking up data. **/
			private class Pair{
				CondensedStateInfo.ObjectName objectX;
				CondensedStateInfo.StateName stateX;
				CondensedStateInfo.ObjectName objectY;
				CondensedStateInfo.StateName stateY;

				public Pair(int plotIdx, CondensedStateInfo.ObjectName objectX, CondensedStateInfo.StateName stateX,
						CondensedStateInfo.ObjectName objectY, CondensedStateInfo.StateName stateY){
					this.objectX = objectX;
					this.objectY = objectY;
					this.stateX = stateX;
					this.stateY = stateY;
				}
			}

			@Override
			public int getItemCount(int series) {
				return nodeList.size();
			}

			@Override
			public int getSeriesCount() {
				return dataSeries.size();
			}

			@Override
			public Comparable getSeriesKey(int series) {
				// TODO Auto-generated method stub
				return null;
			}
		}

		/** XYDataset gets data for plotting transformed data from PCA here. **/
		private class PCATransformedData extends AbstractXYDataset{

			/** Nodes to appear on the plot. **/
			private final ArrayList<TrialNodeMinimal> nodeList;

			/** Eigenvectors found during SVD of the conditioned states. They represent the principle directions that explain most of the state variance. **/
			private FloatMatrix evecs;

			/** During SVD we find the eigenvalues, the weights for what portion of variance is explained by the corresponding eigenvector. **/
			private FloatMatrix evals;

			/** The conditioned dataset. Mean of each state is subtracted and divided by its standard deviation to give a variance of 1. **/
			private FloatMatrix dataSet;

			/** Specific series of data to by plotted. Integer is the plotindex, the matrix is 2xn for x-y plot. **/
			private Map<Integer,FloatMatrix> tformedData = new HashMap<Integer,FloatMatrix>();


			public PCATransformedData(ArrayList<TrialNodeMinimal> nodes){
				nodeList = nodes;
				doPCA();
			}

			/** PCA is already done when the object is created with a list of nodes. This determines which data, transformed according to which
			 * eigenvalue, is plotted on which plot index.
			 * @param plotIdxNum
			 * @param eigForXAxis
			 * @param eigForYAxis
			 */
			public void addSeries(int plotIdx, int eigForXAxis, int eigForYAxis){
				tformedData.put(plotIdx, dataSet.mmul(evecs.getColumns(new int[]{eigForXAxis,eigForYAxis})));	
			}

			@Override
			public Number getX(int series, int item) {
				return tformedData.get(series).get(item,0);
			}

			@Override
			public Number getY(int series, int item) {
				return tformedData.get(series).get(item,1);
			}

			/** Run PCA on all the states in the nodes stored here. **/
			public void doPCA(){
				int numStates = CondensedStateInfo.ObjectName.values().length * CondensedStateInfo.StateName.values().length;
				dataSet = new FloatMatrix(nodeList.size(), numStates);

				// Iterate through all nodes
				for (int i = 0; i < nodeList.size(); i++){
					int colCounter = 0;
					// Through all body parts...
					for (CondensedStateInfo.ObjectName obj : CondensedStateInfo.ObjectName.values()){
						// For each state of each body part.
						for (CondensedStateInfo.StateName st : CondensedStateInfo.StateName.values()){
							dataSet.put(i, colCounter, nodeList.get(i).state.getStateVarFromName(obj, st));
							colCounter++;
						}
					}
				}
				conditionData(dataSet);
				FloatMatrix[] USV = Singular.fullSVD(dataSet);
				evecs = USV[2]; // Eigenvectors
				evals = USV[1].mul(USV[1]).div(dataSet.rows); // Eigenvalues
				// Transforming with the first two eigenvectors
				//tformedData = dataSet.mmul(evecs.getColumns(new int[]{pcaEigX,pcaEigY}));
			}

			/** Subtracts the mean for each variable, and converts to unit variance.
			 *  Samples are rows, variables are columns. Alters matrix x.
			 * @param x
			 */
			private void conditionData(FloatMatrix x){

				for (int i = 0; i < x.columns; i++){
					// Calculate the mean of a column.
					float sum = 0;
					for (int j = 0; j < x.rows; j++){
						sum += x.get(j,i);
					}
					// Subtract the mean out.
					float avg = sum/(float)x.rows;
					for (int j = 0; j < x.rows; j++){
						float centered = x.get(j,i) - avg;
						x.put(j,i,centered);
					}
					// Find the standard deviation for each column.
					sum = 0;
					for (int j = 0; j < x.rows; j++){
						sum += x.get(j,i) * x.get(j,i);
					}
					float std = (float)Math.sqrt(sum/(float)(x.rows - 1));

					// Divide the standard deviation out.
					for (int j = 0; j < x.rows; j++){
						float unitVar = x.get(j,i)/std;
						x.put(j,i,unitVar);
					}	
				}	
			}

			@Override
			public int getItemCount(int series) {
				return nodeList.size();
			}

			@Override
			public int getSeriesCount() {
				return tformedData.size();
			}

			@Override
			public Comparable getSeriesKey(int series) {
				// TODO Auto-generated method stub
				return null;
			}
		}
	}

	/** All panes should implement this so we can switch which is active at any given moment. **/
	private interface TabbedPaneActivator {
		public void activateTab();
		public void deactivateTab();
	}	
}

