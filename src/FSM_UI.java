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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.GLU;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Vector3f;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.common.XForm;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

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

	/** Selected node by user click/key **/
	TrialNodeMinimal selectedNode;

	/** List of panes which can be activated, deactivated. **/
	private ArrayList<TabbedPaneActivator> allTabbedPanes= new ArrayList<TabbedPaneActivator>(); //List of all panes in the tabbed part

	/** Window width **/
	public static int windowWidth = 1920;

	/** Window height **/
	public static int windowHeight = 1000;

	/** Attempted frame rate **/
	private int FPS = 45;

	/** Usable milliseconds per frame **/
	private long MSPF = (long)(1f/(float)FPS * 1000f);

	/** Drawing offsets within the viewing panel (i.e. non-physical) **/
	public int xOffsetPixels_init = 250;
	public int xOffsetPixels = xOffsetPixels_init;
	public int yOffsetPixels = 800;

	/** Runner coordinates to pixels. **/
	public float runnerScaling = 10f;

	private Color defaultDrawColor = new Color(0f,0f,0f);
	private Color historyDrawColor = new Color(0.6f,0.6f,0.6f);
	final Font QWOPLittle = new Font("Ariel", Font.BOLD,21);
	final Font QWOPBig = new Font("Ariel", Font.BOLD,28);

	/** After how many loops do we update the tree physics? Negative means multiple times, positive means we only update every n loops **/
	private int updatePhysFreq = 1;
	/** Keeps track of how many display loops have occurred since the last tree physics update. **/
	private int physDrawCounter = 0;
	/** Continuously update the estimate of the display loop time in milliseconds. **/
	private long avgLoopTime = MSPF;
	/** Filter the average loop time. Lower numbers gives more weight to the lower estimate, higher numbers gives more weight to the old value. **/
	private final float loopTimeFilter = 100f;
	/** How fast does the loop time error accumulate before we change the physics update rate. **/
	private final float ki_phys = 0.5f;
	/** Keeps track of cummulative loop time error. **/
	private float accumulator_phys = 0f;
	/** Value at which the time error accumulator resets and changes the phys update frequency. **/
	private final float accumulator_phys_MAX = 100f;
	/** At what draw frequency do we scrap the physics. Bigger means we wait longer. **/
	private final int physOffThreshold = 12;
	/** Have we turned the physics off due to slowness? **/
	private boolean physOn = true;
	/** Keep track of whether we sent a pause tree command back to negotiator. **/
	private boolean treePause = false;

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
		dataConstraints.gridy = 0;
		dataConstraints.weightx = 0.3;
		dataConstraints.ipady = (int)(0.9*windowHeight);
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

		/* Tree pane */
		treePane = new TreePane();

		pane.add(tabPane, dataConstraints);

		allTabbedPanes.add(runnerPane);
		allTabbedPanes.add(snapshotPane);
		tabPane.addChangeListener(this);

		//Make sure the currently active tab is actually being updated.
		allTabbedPanes.get(tabPane.getSelectedIndex()).activateTab();

		/**** TREE PANE ****/
		GridBagConstraints treeConstraints = new GridBagConstraints();
		treeConstraints.fill = GridBagConstraints.HORIZONTAL;
		treeConstraints.gridx = 10;
		treeConstraints.gridy = 0;
		treeConstraints.weightx = 0.8;
		treeConstraints.ipady = (int)(windowHeight*0.95);
		treeConstraints.ipadx = (int)(windowWidth*0.8);

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

			// Update frequency of physics updates to keep the framerate about constant.
			avgLoopTime = (long)(((loopTimeFilter - 1f) * avgLoopTime + 1f * (System.currentTimeMillis() - currentTime)) / loopTimeFilter); // Filter the loop time

			long extraTime = System.currentTimeMillis() - currentTime;
			if (extraTime > 5){
				try {
					Thread.sleep(extraTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	/** Pick a node for the UI to highlight and potentially display. **/
	public void selectNode(TrialNodeMinimal selected){
		if (selectedNode != null) selectedNode.displayPoint = false;
		selectedNode = selected;
		selectedNode.displayPoint = true;
		selectedNode.nodeColor = Color.RED;
		if (negotiator != null) negotiator.uiNodeSelect(selectedNode);
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

	/** Tree pane **/
	public class TreePane extends GenericGLPanel implements TabbedPaneActivator, GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

		/** For rendering text overlays. Note that textrenderer is for overlays while GLUT is for labels in world space **/
		TextRenderer textRenderBig;
		TextRenderer textRenderSmall;

		/** Currently tracked mouse x location in screen coordinates of the panel **/
		int mouseX;

		/** Currently tracked mouse x location in screen coordinates of the panel **/
		int mouseY;

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

			float ptSize = 10f/cam.getZoomFactor(); //Let the points be smaller/bigger depending on zoom, but make sure to cap out the size!
			ptSize = Math.min(ptSize, 8);

			for (TrialNodeMinimal node : rootNodes){
				gl.glColor3f(1f, 0.1f, 0.1f);
				gl.glPointSize(3*ptSize);

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
			textRenderBig.draw(negotiator.gamesPlayed + " Games played", 20, panelHeight-50);
			textRenderBig.endRendering();

			// Draw the FPS of the tree drawer at the moment.
			textRenderSmall.beginRendering(panelWidth, panelHeight);
			textRenderSmall.setColor(0.7f, 0.7f, 0.7f, 1.0f);
			int fps = (int)(10000./avgLoopTime);
			textRenderSmall.draw( ( (Math.abs(fps) > 1000) ? "???" : fps/10f ) + " FPS", panelWidth - 75, panelHeight - 20);
			if (physOn){
				textRenderSmall.setColor(0.1f, 0.7f, 0.1f, 1.0f);
				textRenderSmall.draw("Tree physics on", panelWidth - 120, panelHeight - 35);
			}else{
				textRenderSmall.setColor(0.7f, 0.1f, 0.1f, 1.0f);
				textRenderSmall.draw("Tree physics off", panelWidth - 120, panelHeight - 35);
			}

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
		}
		@Override
		public void mouseReleased(MouseEvent e) {}
		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}

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
					g.drawString("_", ((xOffsetPixels - xOffsetPixels_init-i * 70) % getWidth()) + getWidth(), yOffsetPixels + 100);
					keyDrawer(g, negotiator.Q,negotiator.W,negotiator.O,negotiator.P);
				}
			}else{
				keyDrawer(g, false, false, false, false);
			}

			// Divider line.
			g.setColor(historyDrawColor);
			g.fill3DRect(0, yOffsetPixels - 230, getWidth(), 5, true);
			//    	g.drawString(dc.format(-(headpos+30)/40.) + " metres", 500, 110);
			xOffsetPixels = -headPos + xOffsetPixels_init;

		}

		public void setWorldToView(World world){
			this.world = world;
		}

		@Override
		public void activateTab() {
			active = true;
		}
		@Override
		public void deactivateTab() {
			active = false;
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
	public class SnapshotPane extends JPanel implements TabbedPaneActivator {

		/** Number of runner states in the past to display. **/
		public int numHistoryStatesDisplay = 5;

		/** Is this tab currently active? If not, don't run the draw loop. **/
		public boolean active = false;

		public SnapshotPane(){}

		public void paintComponent(Graphics g){
			if (!active) return;
			super.paintComponent(g);

			Shape[] shapes;
			XForm[] transforms;

			if (selectedNode != null && selectedNode.state != null){ // TODO this keeps the root node from throwing errors because I didn't assign it a state. We really should do that.
				TrialNodeMinimal drawNode = selectedNode;
				shapes = QWOPGame.shapeList;
				transforms = drawNode.state.getTransforms();

				xOffsetPixels = xOffsetPixels_init + (int)(-runnerScaling * transforms[1].position.x);
				drawRunner(g, defaultDrawColor, shapes, transforms);

				for (int i = 0; i < numHistoryStatesDisplay; i++){
					if (drawNode.treeDepth > 1){
						drawNode = drawNode.parent;
						transforms = drawNode.state.getTransforms();
						drawRunner(g, historyDrawColor, shapes, transforms);	
					}else{
						break;
					}
				}


			}
		}

		/** Draw the runner at a certain state. **/
		private void drawRunner(Graphics g, Color drawColor, Shape[] shapes, XForm[] transforms){

			g.setColor(drawColor);
			for (int i = 0; i < shapes.length; i++){
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
		@Override
		public void activateTab() {
			active = true;
		}
		@Override
		public void deactivateTab() {
			active = false;
		}
	}

	/** All panes should implement this so we can switch which is active at any given moment. **/
	public interface TabbedPaneActivator {
		public void activateTab();
		public void deactivateTab();
	}
}