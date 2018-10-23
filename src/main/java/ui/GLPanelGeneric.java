package ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * This attempts to hold all the things I normally do when making a new panel that uses GL.
 * Extend this class and fill in any additional UI listeners/ things that should go in draw.
 *
 * @author Matt
 */

@SuppressWarnings("serial")
public class GLPanelGeneric extends GLJPanel implements GLEventListener, ComponentListener {

    /* GL objects */
    /**
     * If using openGL, we have to put a GLCanvas inside the panel.
     **/
    GLCanvas canvas;

    /**
     * GLU is the line/point graphics
     **/
    private GLU glu;

    GL2 gl;

    /* Theme colors */
    // Dark theme - default
    public static final float[] darkBackground = {0.f / 255.f, 43f / 255.f, 54.f / 255.f, 0.2f}; // Solarized
	// background color
    public static final float[] darkText = {0.7f, 0.7f, 0.7f, 1.0f}; // Gray color

    /**
     * Camera manager for this scene
     **/
    GLCamManager cam;

    int panelWidth = 1920;
    int panelHeight = 700;

    public GLPanelGeneric() {
        // Canvas setup and sizing -- GL stuff exists in a CANVAS that we put in a PANEL (not fully understood TBH)
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        canvas = new GLCanvas(caps);
        canvas.setSize(new Dimension(panelWidth, panelHeight));
        //canvas.setMaximumSize(new Dimension(panelWidth,panelHeight));
        //canvas.setMinimumSize(new Dimension(panelWidth,panelHeight));

        // Listeners for user interaction
        canvas.addGLEventListener(this);
        addComponentListener(this);
        this.add(canvas); // Add the canvas to the panel.

        // Default camera positioning.
        cam = new GLCamManager(panelWidth, panelHeight);

    }

    /**
     * Paint for this JPanel pretty much just asks the sub-canvas to paint. Somewhat a relic of the old graphics
     **/
    @Override
    public void paintComponent(Graphics g) {
        if (canvas != null) {
            canvas.display();
        }
    }

    /**
     * Set the size of this window
     **/
    @Override
    public void setSize(int width, int height) {
        panelWidth = width;
        panelHeight = height;

        canvas.setSize(new Dimension(panelWidth, panelHeight));
        //canvas.setMaximumSize(new Dimension(panelWidth,panelHeight));
        canvas.setMinimumSize(new Dimension(100, 100));
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        gl = drawable.getGL().getGL2();
        if (glu == null) glu = GLU.createGLU();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        /* Run any camera updates. Note the camera updates are smoothed,
         * so calling this EVERY time is important even if we haven't commanded
         * a change this step.
         */
        cam.update(gl, glu);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        gl = drawable.getGL().getGL2();

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glLineWidth(2);

        gl.glEnable(GLLightingFunc.GL_NORMALIZE);

        cam.initLighting(gl);

        //Line smoothing -- get rid of the pixelated look.
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glEnable(GL2GL3.GL_POLYGON_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glHint(GL2GL3.GL_POLYGON_SMOOTH_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glEnable(GL2ES1.GL_POINT_SMOOTH);

        // Background color -- this is the dark one.
        gl.glClearColor(darkBackground[0], darkBackground[1], darkBackground[2], darkBackground[3]);
    }

    /**
     * This reshape only relates to the canvas. It won't get correct width and height values. Tends to just return the
     * current values.
     **/
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        cam = new GLCamManager(panelWidth, panelHeight);
        //cam.setDims(gl, width, height);
    }

    /**
     * This applies to the whole panel. This one should handle resizing the canvas appropriately.
     **/
    @Override
    public void componentResized(ComponentEvent e) {
        panelHeight = e.getComponent().getHeight();
        panelWidth = e.getComponent().getWidth();
        setSize(panelWidth, panelHeight);
        cam = new GLCamManager(panelWidth, panelHeight);
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }
}
