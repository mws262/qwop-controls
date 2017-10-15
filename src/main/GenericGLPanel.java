package main;
import java.awt.Dimension;
import java.awt.Graphics;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * This attempts to hold all the things I normally do when making a new panel that uses GL.
 * Extend this class and fill in any additional UI listeners/ things that should go in draw.
 * 
 * 
 * @author Matt
 *
 */

public class GenericGLPanel extends GLJPanel implements GLEventListener {
	
	/***** GL objects *****/
	/** If using openGL, we have to put a special GLCanvas inside the frame **/
	GLCanvas canvas;

	/** GLU is the line/point graphics **/
	GLU glu;

	/** GLUT is used for text rendering. **/
	GLUT glut = new GLUT();
	
	GL2 gl;
	
	/***** Theme colors *****/
	// Dark theme - default
	float[] darkBackground = {0.f/255.f, 43f/255.f, 54.f/255.f, 0.2f}; // Solarized background color
	float[] darkText = {0.7f, 0.7f, 0.7f, 1.0f}; // Gray color

	/***** Camera management *****/
	/** Camera manager for this scene **/
	CamManager cam;
	
	int panelWidth = 1920; 
	int panelHeight = 720;
	
	public GenericGLPanel() {
		// Canvas setup and sizing -- GL stuff exists in a CANVAS that we put in a PANEL (not fully understood TBH)
		GLProfile glp = GLProfile.getDefault();
		GLCapabilities caps = new GLCapabilities(glp);
		canvas = new GLCanvas(caps);
		canvas.setSize(new Dimension(panelWidth,panelHeight));
		//canvas.setMaximumSize(new Dimension(panelWidth,panelHeight));
		//canvas.setMinimumSize(new Dimension(panelWidth,panelHeight));
		
		// Listeners for user interaction
		canvas.addGLEventListener(this);
		
		this.add(canvas); // Add the canvas to the panel.
		
		// Default camera positioning.
		cam = new CamManager(panelWidth,panelHeight);
		
	}
	
	/** Paint for this JPanel pretty much just asks the sub-canvas to paint. Somewhat a relic of the old graphics **/
	public void paintComponent(Graphics g){
		if(canvas != null){   
			canvas.display();
		}
	}
	
	/** Set the size of this window **/
	public void setSize(int width, int height){
		panelWidth  = width;
		panelHeight = height;	
		
		canvas.setSize(new Dimension(panelWidth,panelHeight));
		canvas.setMaximumSize(new Dimension(panelWidth,panelHeight));
		canvas.setMinimumSize(new Dimension(panelWidth,panelHeight));
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		gl = drawable.getGL().getGL2();
		if (glu == null) glu = GLU.createGLU();
		
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		/* Run any camera updates. Note the camera updates are smoothed, 
		 * so calling this EVERY time is important even if we haven't commanded
		 * a change this step.
		 */
		cam.update(gl, glu);
	}
	
	@Override
	public void dispose(GLAutoDrawable drawable) {}

	@Override
	public void init(GLAutoDrawable drawable) {
		gl = drawable.getGL().getGL2();

		gl.setSwapInterval(1);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glLineWidth(2);

		gl.glEnable(GL2.GL_NORMALIZE);

		cam.initLighting(gl);

		//Line smoothing -- get rid of the pixelated look.
		gl.glEnable( GL2.GL_LINE_SMOOTH );
		gl.glEnable( GL2.GL_POLYGON_SMOOTH );
		gl.glHint( GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST );
		gl.glHint( GL2.GL_POLYGON_SMOOTH_HINT, GL2.GL_NICEST );
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

		gl.glEnable( GL2.GL_POINT_SMOOTH );

		// Background color -- this is the dark one.
		gl.glClearColor(darkBackground[0],darkBackground[1],darkBackground[2],darkBackground[3]);
	}
	
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		panelWidth  = width;
		panelHeight = height;	
		GL2 gl = drawable.getGL().getGL2();
		cam.setDims(gl, width, height);
	}
}
