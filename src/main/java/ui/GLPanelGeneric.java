package ui;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * This attempts to hold all the things I normally do when making a new panel that uses GL.
 * Extend this class and fill in any additional UI listeners/ things that should go in draw.
 *
 * @author Matt
 */

public class GLPanelGeneric extends GLJPanel implements GLEventListener, ComponentListener {

    /**
     * GLU is the line/point graphics
     **/
    private GLU glu;
    transient GL2 gl;
    transient GLUT glut = new GLUT();

    /**
     * Dark theme background. From the Solarized palette.
     */
    public static final float[] darkBackground = {0.f / 255.f, 43f / 255.f, 54.f / 255.f, 0.2f};

    /**
     * Gray text color.
     */
    public static final float[] darkText = {0.7f, 0.7f, 0.7f, 1.0f};

    /**
     * Camera manager for this scene
     */
    transient GLCamManager cam;

    int panelWidth = 1920;
    int panelHeight = 700;

    public GLPanelGeneric() {
        super(new GLCapabilities(GLProfile.getDefault()));
        setSize(new Dimension(panelWidth, panelHeight));
        // Listeners for user interaction
        addGLEventListener(this);
        addComponentListener(this);
        // Default camera positioning.
        cam = new GLCamManager(panelWidth, panelHeight);
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
    public void dispose(GLAutoDrawable drawable) {}

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
     */
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        cam = new GLCamManager(panelWidth, panelHeight);
    }

    /**
     * This applies to the whole panel. This one should handle resizing the canvas appropriately.
     */
    @Override
    public void componentResized(ComponentEvent e) {
        panelHeight = e.getComponent().getHeight();
        panelWidth = e.getComponent().getWidth();
        setSize(panelWidth, panelHeight);
        cam = new GLCamManager(panelWidth, panelHeight);
    }

    /**
     * Draw a text string using GLUT. This string will go to a position in world coordinates, not screen coordinates.
     */
    public void drawString(String toDraw, float x, float y, float z, Color color) {
        // Printing fonts, letters and numbers is much simpler with GLUT.
        // We do not have to use our own bitmap for the font.
        gl.glColor3f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
        gl.glRasterPos3d(x, y, z);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, toDraw);
    }

    @Override
    public void componentMoved(ComponentEvent e) {}

    @Override
    public void componentShown(ComponentEvent e) {}

    @Override
    public void componentHidden(ComponentEvent e) {}
}
