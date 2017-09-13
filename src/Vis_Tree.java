import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Vector3f;

import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * 
 * @author Matt
 *
 */

public class Vis_Tree implements TabbedPaneActivator{


	/** To create a new visualizer, must supply a starting tree and whether this is a slave panel **/
	public Vis_Tree() {
		//Create the actual JPanel for the tree.
		treePanel = new SingleTreePane();
	}


	/**
	 * Actual Jpanel itself that can be added as either a slave or a main tree display.
	 * 
	 * @author Matt
	 *
	 */
	class SingleTreePane extends GLJPanel implements TabbedPaneActivator, MouseListener, MouseMotionListener, MouseWheelListener,KeyListener,GLEventListener{

		private static final long serialVersionUID = 1L;

		//Instructions string gets displayed on the side of the main tree pane, but not the slave(s).
		public final String[] instructions = {
			"Left click & drag pans.",
			"Scroll wheel zooms.",
			"Right click & drag rotates individual branches",
			"Alt-click labels the control.",
			"Ctrl-click hides a branch.",
			"Meta-click selects a point for state viewer.",
			"Alt-scroll spaces or contracts branches hovered over.",
			"S turns score display on and off.",
			"Meta-S turns value display.",
			"Meta-arrows rotates camera.",
			"Alt-LR arrow twists camera",
			"P pauses tree expansion. Graphics remain.",
		};
		
		
		
		/** Format numbers to truncate decimal places when displaying distances traveled **/
		private DecimalFormat df = new DecimalFormat("#");

		/** Games/sec counter for display on the window. **/
		private int gamespersec = 0;

		/** Keep track of mouse coordinates within this panel. **/
		private int mouseX = 0;
		private int mouseY = 0;

		/** Does the camera follow the center of the newest tree? **/
		private boolean camFollow = true;

		/** GLU is the line/point graphics **/
		private GLU glu;

		private int width = 400; // I think these values are meaningless since they change as per the layout manager.
		private int height = 200;

		/** If using openGL, we have to put a special GLCanvas inside the frame **/
		private GLCanvas canvas;
		private CamManager cam;


		/** For rendering text overlays. Note that textrenderer is for overlays while GLUT is for labels in world space **/
		TextRenderer textRenderBig;
		TextRenderer textRenderSmall;

		public SingleTreePane(){

			this.setLayout(new BorderLayout());
			GLProfile glp = GLProfile.getDefault();
			GLCapabilities caps = new GLCapabilities(glp);
			canvas = new GLCanvas(caps);
			
			//Not sure why, but this keeps the pane from changing sizes every time I change tabs. The actual numbers seem pretty arbitrary.
			canvas.setSize(new Dimension(100,30));
			canvas.setMaximumSize(new Dimension(100,30));
			canvas.setMinimumSize(new Dimension(100,30));

			//Add listeners to the canvas rather than frame if we're using GL
			canvas.addGLEventListener(this);
			this.add(canvas);
			canvas.setFocusable(true);
			canvas.addKeyListener(this);
			canvas.addMouseListener(this);
			canvas.addMouseMotionListener(this);
			canvas.addMouseWheelListener(this);
			new GLUT();

			textRenderBig = new TextRenderer(new Font("Calibri", Font.BOLD, 36));
			textRenderSmall = new TextRenderer(new Font("Calibri", Font.PLAIN, 18));

			//If this is a slave window, we need to move the camera over.
			cam = new CamManager(width,height); //Default camera placement
		}

		/** Paint for this JPanel pretty much just asks the sub-canvas to paint. Somewhat a relic of the old graphics **/
		public void paintComponent(Graphics g){
			if(!pauseDraw && canvas != null){   
				canvas.display();
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {	

				focusedNode = cam.nodeFromClick(e.getX(), e.getY(), tree, oldToGLScaling, false);

			if (e.isAltDown()){ //alt click enables a label on this node
				focusedNode.LabelOn = true;
			}else if (e.isControlDown()){ //Control will hide this node and all its children.

				if(focusedNode !=null && focusedNode.TreeDepth > 1){ //Keeps stupid me from hiding everything in one click.
					focusedNode.hiddenNode = true;
					focusedNode.ParentNode.RemoveChild(focusedNode); //Try also just killing it from the tree search too.
				}
			}else if (e.isMetaDown()){

				if (SnapshotPane != null){
					SnapshotPane.setNode(focusedNode);
				}
				if(pathView != null){
					pathView.AddQueuedTrial(focusedNode);
				}
			}
		}
		@Override
		public void mouseEntered(MouseEvent e) {
			this.requestFocus();
		}
		@Override
		public void mouseExited(MouseEvent e) {}
		@Override
		public void mousePressed(MouseEvent e) {
			mouseX = e.getX();
			mouseY = e.getY();
			if (e.getButton() == MouseEvent.BUTTON3){ //Right click moves nodes.
					focusedNode = cam.nodeFromClick(e.getX(), e.getY(), tree, oldToGLScaling, true);
			}
		}
		@Override
		public void mouseReleased(MouseEvent e) {
		}
		@Override
		public void mouseDragged(MouseEvent e){
			//Note: if using old graphics, actually move the point coordinates. if GL, then just move the camera.
			if (e.getButton() == MouseEvent.BUTTON1){ //Left click drags the whole thing.
				Vector3f relCamMove = cam.windowFrameToWorldFrameDiff(e.getX(), e.getY(), mouseX, mouseY, oldToGLScaling, glScaling);

				cam.smoothTranslateRelative(relCamMove, relCamMove, 5);

			}else if (e.getButton() == MouseEvent.BUTTON3){ //Right click moves nodes.
				Vector3f clickedpt = cam.planePtFromRay(e.getX(), e.getY(),oldToGLScaling,0);
				double clickAngle = -Math.atan2((clickedpt.x-focusedNode.ParentNode.nodeLocation[0]),(clickedpt.y-focusedNode.ParentNode.nodeLocation[1]))+Math.PI/2.;
				clickAngle -= focusedNode.nodeAngle; //Subtract out the current angle.
				focusedNode.RotateBranch(clickAngle);
			}
			mouseX = e.getX();
			mouseY = e.getY();
		}
		@Override
		public void mouseMoved(MouseEvent e) {
		}

		@Override
		/** This zooms in and out. Also changes the size factor in OptionsHolder to keep things consistent. **/
		public  void mouseWheelMoved(MouseWheelEvent e){

			if (e.getWheelRotation()<0){ //Negative mouse direction -> zoom in.
				if(e.isAltDown()){
					if(slave){
						focusedNode = cam.nodeFromClick(e.getX(), e.getY(), tree, oldToGLScaling, true);
						focusedNode.SpaceBranchAlt(0.1);
					}else{
						focusedNode = cam.nodeFromClick(e.getX(), e.getY(), tree, oldToGLScaling, false);
						focusedNode.SpaceBranch(0.1);
					}

				}else{
					cam.smoothZoom(0.9f, 5);
					glScaling*=0.9;
				}
			}else{
				if(e.isAltDown()){
						focusedNode = cam.nodeFromClick(e.getX(), e.getY(), tree, oldToGLScaling, false);
						focusedNode.SpaceBranch(-0.1);
				}else{
					cam.smoothZoom(1.1f, 5);
					glScaling*=1.1;
				}
			}
		}

		// The following 2 methods are probably too complicated. when you push the arrow at the edge of one branch, this tries to jump to the nearest next branch node at the same depth.
		/** Called by key listener to change our focused node to the next adjacent one in the +1 or -1 direction **/
		private void arrowSwitchNode(int direction,int depth){
			//Stupid way of getting this one's index according to its parent.
			if(focusedNode != null){
				int thisIndex = focusedNode.ParentNode.GetChildIndex(focusedNode);
				//This set of logicals eliminates the edge cases, then takes the proposed action as default
				if (thisIndex == 0 && direction == -1){ //We're at the lowest index of this node and must head to a new parent node.
					ArrayList<TrialNode> blacklist = new ArrayList<TrialNode>(); //Keep a blacklist of nodes that already proved to be duds.
					blacklist.add(focusedNode);
					nextOver(focusedNode.ParentNode,blacklist,1,direction,focusedNode.ParentNode.GetChildIndex(focusedNode),0);

				}else if (thisIndex == focusedNode.ParentNode.NumChildren()-1 && direction == 1){ //We're at the highest index of this node and must head to a new parent node.
					ArrayList<TrialNode> blacklist = new ArrayList<TrialNode>();
					blacklist.add(focusedNode);
					nextOver(focusedNode.ParentNode,blacklist, 1,direction,focusedNode.ParentNode.GetChildIndex(focusedNode),0);

				}else{ //Otherwise we can just switch nodes within the scope of this parent.
					focusedNode = (focusedNode.ParentNode.GetChild(thisIndex+direction));
				}

				//These logicals just take the proposed motion (or not) and ignore any edges.
				if(depth == 1 && focusedNode.NumChildren()>0){ //Go further down the tree if this node has children
					focusedNode = focusedNode.GetChild(0);
				}else if(depth == -1 && focusedNode.TreeDepth>-1){ //Go up the tree if this is not root.
					focusedNode = focusedNode.ParentNode;
				}
				if(SnapshotPane != null){
					SnapshotPane.setNode(focusedNode);
					SnapshotPane.update();
				}
				repaint();
			}
		}

		/** Take a node back a layer. Don't return to node past. Try to go back out by the deficit depth amount in the +1 or -1 direction left/right **/
		private boolean nextOver(TrialNode current, ArrayList<TrialNode> blacklist, int deficitDepth, int direction,int prevIndexAbove,int numTimesTried){ // numTimesTried added to prevent some really deep node for causing some really huge search through the whole tree. If we don't succeed in a handful of iterations, just fail quietly.
			numTimesTried++;
			boolean success = false;
			//TERMINATING CONDITIONS-- fail quietly if we get back to root with nothing. Succeed if we get back to the same depth we started at.
			if (deficitDepth == 0){ //We've successfully gotten back to the same level. Great.
				focusedNode = current;
				return true;
			}else if(current.TreeDepth == 0){
				return true; // We made it back to the tree's root without any success. Just return.

			}else if(numTimesTried>100){// If it takes >100 movements between nodes, we'll just give up.
				return true;
			}else{
				//CCONDITIONS WE NEED TO STEP BACKWARDS TOWARDS ROOT.
				//If this new node has no children OR it's 1 child is on the blacklist, move back up the tree.
				if((prevIndexAbove+1 == current.NumChildren() && direction == 1) || (prevIndexAbove == 0 && direction == -1)){
					blacklist.add(current); 
					success = nextOver(current.ParentNode,blacklist,deficitDepth+1,direction,current.ParentNode.GetChildIndex(current),numTimesTried); //Recurse back another node.
				}else if (!(current.NumChildren() >0) || (blacklist.contains(current.GetChild(0)) && current.NumChildren() == 1)){ 
					blacklist.add(current); 
					success = nextOver(current.ParentNode,blacklist,deficitDepth+1,direction,current.ParentNode.GetChildIndex(current),numTimesTried); //Recurse back another node.
				}else{

					//CONDITIONS WE NEED TO GO DEEPER:
					if(direction == 1){ //March right along this previous node.
						for (int i = prevIndexAbove+1; i<current.NumChildren(); i++){
							success = nextOver(current.GetChild(i),blacklist,deficitDepth-1,direction,-1,numTimesTried);
							if(success){
								return true;
							}
						}
					}else if(direction == -1){ //March left along this previous node
						for (int i = prevIndexAbove-1; i>=0; i--){
							success = nextOver(current.GetChild(i),blacklist,deficitDepth-1,direction,current.GetChild(i).NumChildren(),numTimesTried);
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
					valDisplay = !valDisplay;
					scoreDisplay = false;
					break;
				}
			}else if(e.isAltDown()){
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
					arrowSwitchNode(-1,0);
					break;
				case KeyEvent.VK_RIGHT : //Go right along an isobranch
					arrowSwitchNode(1,0);
					break;
				case KeyEvent.VK_C : //toggle camera following of new trees.
					camFollow = !camFollow;
					break;
				case KeyEvent.VK_P : //Pause everything except for graphics updates
					if(e.isShiftDown()){
						OptionsHolder.pauseWithAdvance = !OptionsHolder.pauseWithAdvance;
					}else{
						System.out.println("Pause toggle.");
						pause  = !pause;
					}
					break;
				case KeyEvent.VK_S : 
					scoreDisplay = !scoreDisplay;
					valDisplay = false;
					break;
				}

			}
		}
		@Override
		public void keyReleased(KeyEvent e) {}

		@Override
		public void keyTyped(KeyEvent e) {
			switch(e.getKeyChar()){
			case 't': // explore specific branch by setting an override node.

				//Uncolor the previous subtree, if it exists.
				if(OverrideNode != null){
					OverrideNode.ColorChildren(Color.BLACK);
				}

				if(focusedNode != null && !focusedNode.FullyExplored){

					if(Override){
						Override = false;
					}else{
						setOverride(focusedNode);
					}
				}
				break;
			}
		}

		/** externally set the override node **/
		public void setOverride(TrialNode node){
			if(OverrideNode != null){
				OverrideNode.ColorChildren(Color.BLACK); // if we already have an override. then set this old one back to default colors.
			}
			focusedNode = node; //Change the focus to the specified new override node.
			OverrideNode = node; // make the given node our override also
			Override = true; // set override flag to true.
			OverrideNode.ColorChildren(Color.RED);
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
			glut.glutBitmapString(
					GLUT.BITMAP_HELVETICA_12, 
					toDraw);
		}

		int numtrees = 0;
		int count = 0;
		/** 
		 * Main event loop: OpenGL display
		 * advance. GLEventListener implementation.
		 */
		@Override
		public  void display(GLAutoDrawable drawable) {
			GL2 gl = drawable.getGL().getGL2();
			GLUT glut = new GLUT();

			//Background color that defaults when canvas is cleared.
			if (OptionsHolder.darkTheme){
				gl.glClearColor(0.1f,0.1f,0.2f,0.2f);
			}else{
				gl.glClearColor(1f,1f,1f,1f);
			}

			//clear it out.
			gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
			gl.glMatrixMode(GL2.GL_MODELVIEW);

			if (glu == null) glu = GLU.createGLU();

			/* All camera updates */
			if(tree == null){
				return;
			}
//			if( camFollow && tree != null ){
//				
//				glScaling = 3.5f;
//				TrialNode tn = tree.getRoot();
//				Vector3f tarpos = new Vector3f(oldToGLScaling*tn.nodeLocation[0]+5,oldToGLScaling*tn.nodeLocation[1],oldToGLScaling*tn.height);
//				Vector3f campos = new Vector3f(-5,-10,70);
//				campos.add(tarpos);
//				cam.smoothTranslateAbsolute(campos, tarpos, 20);
//				numtrees = 1;
//			}
			cam.update(gl, glu);	

			//If display of this particular tree is not on, then continue to the next.
			if(!tree.displayOn){
				return;
			}

			LineHolder Lines = tree.getLines(); // Grab each lineholder corresponding to each tree one at a time.
			//This is the GL version of the line drawing.
			float ptSize = 10*1/glScaling; //Let the points be smaller/bigger depending on zoom, but make sure to cap out the size!
			ptSize = Math.min(ptSize, 10);

			//Display text on the tree for value/score stuff if s and meta-s are pressed.
			if (scoreDisplay){
				for(int i = 0; i<Lines.NodeList.length; i++){
					if (Lines.NodeList[i][1].DeadEnd){
						gl.glColor3fv(getScoreColor(minDistScaling,maxDistScaling,-Lines.NodeList[i][1].rawScore).getColorComponents(null),0);

						drawString(df.format(Lines.NodeList[i][1].rawScore), oldToGLScaling*Lines.NodeList[i][1].nodeLocation[0], oldToGLScaling*Lines.NodeList[i][1].nodeLocation[1],oldToGLScaling*Lines.NodeList[i][1].height,gl,glut);
					}
				}
			}else if (valDisplay){
				for(int i = 0; i<Lines.NodeList.length; i++){
					if(Lines.NodeList[i][1].value != 0){
						gl.glColor3fv(getScoreColor(minValScaling,maxValScaling,Lines.NodeList[i][1].value).getColorComponents(null),0);
						drawString(df.format(Lines.NodeList[i][1].value),  oldToGLScaling*Lines.NodeList[i][1].nodeLocation[0], oldToGLScaling*Lines.NodeList[i][1].nodeLocation[1],oldToGLScaling*Lines.NodeList[i][1].height,gl,glut);
					}
				}

			}

			//Display the focused point in red.
			gl.glPointSize(5*ptSize);
			gl.glBegin(GL2.GL_POINTS);
			if(focusedNode != null){ //Color the focusedNode red.
				gl.glColor3f(1, 0, 0);
				gl.glVertex3d(oldToGLScaling*focusedNode.nodeLocation[0], oldToGLScaling*focusedNode.nodeLocation[1],  oldToGLScaling*focusedNode.height);		  	  
			}
			gl.glEnd();

			gl.glEnable(GL2.GL_POINT_SMOOTH);
			gl.glPointSize(ptSize);

			gl.glBegin(GL2.GL_POINTS);
			//GL version of the end dot drawing:

			for(int i = 0; i<Lines.NodeList.length; i++){
				if (Lines.NodeList[i][1] != null && !Lines.NodeList[i][1].TempFullyExplored && Lines.NodeList[i][1].DeadEnd && OptionsHolder.failTypeDisp ){
					if (Lines.NodeList[i][1].FailType == StateHolder.FailMode.BACK){ // Failures -- we fell backwards
						gl.glColor3f(0, 1, 1);
						gl.glVertex3d(oldToGLScaling*Lines.NodeList[i][1].nodeLocation[0], oldToGLScaling*Lines.NodeList[i][1].nodeLocation[1],  oldToGLScaling*Lines.NodeList[i][1].height);
					}else if(Lines.NodeList[i][1].FailType == StateHolder.FailMode.FRONT){ // Failures -- we fell forward.
						gl.glColor3f(1, 0, 1);
						gl.glVertex3d(oldToGLScaling*Lines.NodeList[i][1].nodeLocation[0], oldToGLScaling*Lines.NodeList[i][1].nodeLocation[1],  oldToGLScaling*Lines.NodeList[i][1].height);
					}
				}
				if(Lines.NodeList[i][1].TempFullyExplored && OptionsHolder.failTypeDisp && Lines.NodeList[i][1].NumChildren()==0){ //These are nodes that we've stopped at due to a depth limit, but COULD go further (not failures).
					gl.glColor3f(0.2f, 0.2f, 0.2f);
					gl.glVertex3d(oldToGLScaling*Lines.NodeList[i][1].nodeLocation[0], oldToGLScaling*Lines.NodeList[i][1].nodeLocation[1],  oldToGLScaling*Lines.NodeList[i][1].height);	 
				}
			}
			gl.glEnd();	

			gl.glLineWidth(3f);

			gl.glBegin(GL2.GL_LINES);
			//color information is stored in each vertex.
			gl.glColor3f(1.f,0.f,0.f);

			if(Lines.numLines>1){ //Catch the stupid case where we are paused before any games have run.
				for (int i = 0; i<Lines.numLines; i++){

					if(Lines.NodeList[i][1].nodeLocation == null && Lines.NodeList[i][1].nodeLocation[0] == 0 && Lines.NodeList[i][1].nodeLocation[1] == 0){ //If the x2 and y2 are 0, we've come to the end of actual lines.
						break;
					}
					if(!Lines.ColorList[i].equals(Color.BLACK)){
						gl.glColor3fv(Lines.ColorList[i].getColorComponents(null),0);
					}else{						
						gl.glColor3fv(getDepthColor(Lines.NodeList[i][1].TreeDepth,Lines.NodeList[i][1].height,false).getRGBColorComponents(null),0);
					}
					gl.glVertex3d(oldToGLScaling*Lines.NodeList[i][0].nodeLocation[0], oldToGLScaling*Lines.NodeList[i][0].nodeLocation[1], oldToGLScaling*Lines.NodeList[i][0].height);
					gl.glVertex3d(oldToGLScaling*Lines.NodeList[i][1].nodeLocation[0], oldToGLScaling*Lines.NodeList[i][1].nodeLocation[1], oldToGLScaling*Lines.NodeList[i][1].height);
				}
			}
			gl.glEnd();
			//Draw games played and games/sec in upper left.
			textRenderBig.beginRendering(width, height); 
			if(OptionsHolder.darkTheme){
				textRenderBig.setColor(0.7f, 0.7f, 0.7f, 1.0f); 
			}else{
				textRenderBig.setColor(0f, 0f, 0f, 1.0f); 
			}


			textRenderBig.draw(OptionsHolder.gamesPlayed + " Games played", 20, height-50);

			//Draw games/s
			if(countLastReport>reportEvery){
				countLastReport = 0; //Reset the counter.
				currTime = System.currentTimeMillis();
				gamespersec = (int)((OptionsHolder.gamesPlayed-lastGameNum)*1000./(currTime-lastTime));
				textRenderBig.draw(gamespersec + "  games/s", 20, height-85);
				lastGameNum = OptionsHolder.gamesPlayed;
				lastTime = currTime;
			}else{
				textRenderBig.draw(gamespersec + "  games/s", 20, height-85);
				countLastReport++;
			}
			textRenderBig.endRendering();


			if(OptionsHolder.darkTheme){
				textRenderSmall.setColor(0.8f, 0.8f, 0.8f, 0.7f); 
			}else{
				textRenderSmall.setColor(0f, 0f, 0f, 0.3f); 
			}
			//Also draw instructions.
			textRenderSmall.beginRendering(width, height);

			for (int i = 0; i<instructions.length; i++){		
				textRenderSmall.draw(instructions[instructions.length-1-i], 20, i*20+15);
			}
			textRenderSmall.endRendering();


		}

		@Override
		public void dispose(GLAutoDrawable arg0) {}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		//		System.err.println("INIT GL IS: " + gl.getClass().getName());

		gl.setSwapInterval(1);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glLineWidth(1);

		gl.glEnable(GL2.GL_NORMALIZE);

		cam.initLighting(gl);

		//Line smoothing -- get rid of the pixelated look.
		gl.glEnable( GL2.GL_LINE_SMOOTH );
		gl.glEnable( GL2.GL_POLYGON_SMOOTH );
		gl.glHint( GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST );
		gl.glHint( GL2.GL_POLYGON_SMOOTH_HINT, GL2.GL_NICEST );
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height){

		this.width  = width;
		this.height = height;	
		GL2 gl = drawable.getGL().getGL2();
		cam.setDims(gl, width, height);
	}
}

@Override
public void setInterval(int interval) {
	this.interval = interval;
}

@Override
public int getInterval() {
	return interval;
}

@Override
public void DoScheduled() {
	ScaleDist();
	ScaleVal();	
	if(activeTab){
		update();
	}
}
//TODO TEMP MOVE 5/4/17
@Override
public void DoEvery() {
//	if(activeTab){
//		update();
//	}
}

@Override
public void DoNow() {}

@Override
public void Disable() {}

@Override
public void ActivateTab() {
	activeTab = true;	
}

@Override
public void DeactivateTab() {
	activeTab = false;
}
}

