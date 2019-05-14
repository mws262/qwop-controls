package ui;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.glu.GLU;
import tree.NodeQWOPGraphicsBase;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handle all camera motion including smooth motions, click-to-coordinate mapping, etc.
 * Update should be called in every graphics draw.
 *
 * @author Matt
 */
public class GLCamManager {
    /**
     * Keep track of the current zoom factor. Absolute number is not significant. Relative to initial zoom.
     */
    private float zoomFactor = 1f;

    /**
     * Vector from camera position to target position
     */
    private Vector3f eyeToTarget = new Vector3f();

    /**
     * Position of the camera.
     */
    private Vector3f eyePos = new Vector3f(8, 0, 100);

    /**
     * Position of the camera's focus.
     */
    private Vector3f targetPos = new Vector3f(8, 0, 0);

    /**
     * Define world coordinate's up.
     */
    private Vector3f upVec = new Vector3f(0, 1f, 0);

    /**
     * View frustum angle.
     */
    private float viewAng = 40;

    /**
     * Camera rotation. Updated every display cycle.
     */
    private float[] modelViewMat = new float[16];

    /**
     * Width of window.
     */
    private float width;

    /**
     * Height of window.
     */
    private float height;

    /**
     * Position of the light. Fixed at the location of the camera.
     */
    public static float[] lightPos = {0f, 0f, 0f, 1f};
    public static float[] lightAmbient = {0f, 0f, 0f, 1f};
    public static float[] lightDiffuse = {0.9f, 0.9f, 0.9f, 1f};
    public static float[] lightSpecular = {1f, 1f, 1f, 1f};

    /* Queued camera movements. The convention is step size and number of needed steps */
    private ArrayList<Vector3f> eyeIncrement = new ArrayList<>();
    private ArrayList<Integer> eyeSteps = new ArrayList<>();

    private ArrayList<Vector3f> targetIncrement = new ArrayList<>();
    private ArrayList<Integer> targetSteps = new ArrayList<>();

    private ArrayList<Float> longitudeIncrement = new ArrayList<>();
    private ArrayList<Integer> longitudeSteps = new ArrayList<>();

    private ArrayList<Float> latitudeIncrement = new ArrayList<>();
    private ArrayList<Integer> latitudeSteps = new ArrayList<>();

    private ArrayList<Float> zoomIncrement = new ArrayList<>();
    private ArrayList<Integer> zoomSteps = new ArrayList<>();

    private ArrayList<Float> twistIncrement = new ArrayList<>();
    private ArrayList<Integer> twistSteps = new ArrayList<>();

    // temporary var for when adding up camera movements.
    private Vector3f netMovement = new Vector3f();

    private Vector3f clickVec = new Vector3f(0, 0, 0); // Vector ray of the mouse click.
    private Vector3f EyeToPoint = new Vector3f(0, 0, 0); // vector from camera to a selected point.

    /**
     * Provide camera's target, etc.
     */
    public GLCamManager(float width, float height, Vector3f eyePos, Vector3f targetPos) {
        this.eyePos = eyePos;
        this.targetPos = targetPos;
        this.width = width;
        this.height = height;
    }

    /**
     * Use default camera position, target, etc
     */
    public GLCamManager(float width, float height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Setup Lighting
     */
    public void initLighting(GL2 gl) {
        // SETUP LIGHTING
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPos, 0);
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_AMBIENT, lightAmbient, 0);
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_DIFFUSE, lightDiffuse, 0);
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_SPECULAR, lightSpecular, 0);
        gl.glEnable(GLLightingFunc.GL_LIGHT0);
    }

    /**
     * Update all camera views and bookkeeping info. Any queued camera movements will be incremented.
     */
    public void update(GL2 gl, GLU glu) {

        /* Do all the queued actions. **/
        // Sum eye movements.
        netMovement.scale(0);
        for (int i = 0; i < eyeIncrement.size(); i++) {
            netMovement.add(eyeIncrement.get(i));
            eyeSteps.set(i, eyeSteps.get(i) - 1);

            //Get rid of this movement increment once we've done the specified number of movements.
            if (eyeSteps.get(i) == 0) {
                eyeSteps.remove(i);
                eyeIncrement.remove(i);
            }
        }
        // Put the net change back into the actual position.
        eyePos.add(netMovement);

        // Sum target movements.
        netMovement.scale(0);
        for (int i = 0; i < targetIncrement.size(); i++) {
            netMovement.add(targetIncrement.get(i));
            targetSteps.set(i, targetSteps.get(i) - 1);

            //Get rid of this movement increment once we've done the specified number of movements.
            if (targetSteps.get(i) == 0) {
                targetSteps.remove(i);
                targetIncrement.remove(i);
            }
        }
        // Put the net change back into the actual position.
        targetPos.add(netMovement);

        // Sum longitude movements.
        float net = 0;
        for (int i = 0; i < longitudeIncrement.size(); i++) {
            net += (longitudeIncrement.get(i));
            longitudeSteps.set(i, longitudeSteps.get(i) - 1);

            //Get rid of this movement increment once we've done the specified number of movements.
            if (longitudeSteps.get(i) == 0) {
                longitudeSteps.remove(i);
                longitudeIncrement.remove(i);
            }
        }

        // Put the net change back into the actual position.
        rotateLongitude(net);

        // Sum latitude movements.
        net = 0;
        for (int i = 0; i < latitudeIncrement.size(); i++) {
            net += (latitudeIncrement.get(i));
            latitudeSteps.set(i, latitudeSteps.get(i) - 1);

            //Get rid of this movement increment once we've done the specified number of movements.
            if (latitudeSteps.get(i) == 0) {
                latitudeSteps.remove(i);
                latitudeIncrement.remove(i);
            }
        }

        // Put the net change back into the actual position.
        rotateLatitude(net);

        // Sum twist movements.
        net = 0;
        for (int i = 0; i < twistIncrement.size(); i++) {
            net += (twistIncrement.get(i));
            twistSteps.set(i, twistSteps.get(i) - 1);

            //Get rid of this movement increment once we've done the specified number of movements.
            if (twistSteps.get(i) == 0) {
                twistSteps.remove(i);
                twistIncrement.remove(i);
            }
        }

        // Put the net change back into the actual position.
        twistCW(net);

        // Put together all zooms
        net = -1;
        for (int i = 0; i < zoomIncrement.size(); i++) {
            net *= zoomIncrement.get(i);
            zoomSteps.set(i, zoomSteps.get(i) - 1);

            if (zoomSteps.get(i) == 0) {
                zoomSteps.remove(i);
                zoomIncrement.remove(i);
            }
        }

        // Now do the zoom:
        eyeToTarget.sub(targetPos, eyePos); // Find vector from the camera eye to the target pos
        eyeToTarget.scale(net);
        eyePos.add(eyeToTarget, targetPos);
        zoomFactor *= -net; // MATT ADD 8/25/17

        upVec.cross(eyeToTarget, upVec);
        upVec.cross(upVec, eyeToTarget);
        upVec.normalize();

        // Actually change the camera settings now.
        // Camera perspective.
        gl.glLoadIdentity();

        // View frustum far plane distance.
        float farPlane = 10000;
        // View frustum near plane distance.
        float nearPlane = 5;
        glu.gluPerspective(viewAng, width / height, nearPlane, farPlane);
        glu.gluLookAt(eyePos.x, eyePos.y, eyePos.z, targetPos.x, targetPos.y, targetPos.z, upVec.x, upVec.y, upVec.z);
        gl.glPopMatrix();
        gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelViewMat, 0);
    }

    /**
     * Change window dims
     */
    public void setDims(GL2 gl, int width, int height) {
        height = Math.max(height, 1); // avoid height=0;
        width = Math.max(width, 1); // avoid height=0;
        this.width = width;
        this.height = height;

        gl.glViewport(0, 0, width, height);
    }

    /**
     * Transform a 2d coordinate click in window coordinates to a 3d coordinate in the world frame.
     */
    public Vector3f windowFrameToWorldFrameDiff(int mouseXnew, int mouseYnew, int mouseXold, int mouseYold) {
        //Find x transformed from front plane coordinates (click) to world camera coordinates
        eyeToTarget.sub(targetPos, eyePos);
        // Temporary. just don't want to keep reallocating memory.
        Vector3f temp1 = (Vector3f) upVec.clone();
        temp1.scale((mouseYnew - mouseYold) * zoomFactor * 0.1f); // Hand-tuned the multiplier.
        Vector3f temp2 = new Vector3f();

        //Find y transformed
        temp2.cross(upVec, eyeToTarget);
        temp2.normalize();
        temp2.scale((mouseXnew - mouseXold) * zoomFactor * 0.1f);
        temp1.add(temp2);

        return temp1;
    }

    /**
     * Twist up vector by provided angle
     */
    public void twistCW(float radians) {
        if (radians != 0f) {
            eyeToTarget.sub(targetPos);
            Vector3f perp = new Vector3f();
            perp.set(eyeToTarget);

            perp.cross(perp, upVec); // Perpendicular to the up vector in the plane of the camera.

            if (perp.dot(perp) > 0) {
                perp.normalize();
            } else {
                return;
            }

            perp.scale((float) Math.sin(radians));
            upVec.scale((float) Math.cos(radians));
            upVec.add(perp);
        }
    }

    /**
     * Zoom in/zoom out. Also spread out over a certain number of update calls to smooth it out.
     *
     * @param zoomFactor How the current zoom factor is changed, multiplicatively, by this zoom. So, 1 is no change,
     *                   <1 is zoom in. >1 is zoom out.
     * @param speed Number of timesteps that this change is spread over.
     */

    public void smoothZoom(float zoomFactor, int speed) {
        eyeToTarget.sub(targetPos, eyePos);
        float distToTarSq = eyeToTarget.dot(eyeToTarget);
        zoomFactor = Float.min(zoomFactor, 1.1f);
        zoomFactor = Float.max(zoomFactor, 0.9f);
        if ((zoomFactor > 1 || distToTarSq > 500) && // Don't allow zooming in too far or zooming out too far.
                (zoomFactor < 1 || distToTarSq < 1e6)) {
            float zoomIncrement = (float) Math.pow(zoomFactor, 1. / speed);
            this.zoomIncrement.add(zoomIncrement);
            zoomSteps.add(speed);
        }
    }

    /**
     * Rotate the camera about the axis it's aiming
     */
    public void smoothTwist(float zoomFactor, int speed) {
        twistIncrement.add(zoomFactor / speed);
        twistSteps.add(speed);
    }

    /**
     * Orbit camera longitudinally by a magnitude in radians with a speed factor of 'speed.'
     * The speed means how many update calls does it take to achieve the change.
     */
    public void smoothRotateLong(float magnitude, int speed) {
        longitudeIncrement.add(magnitude / speed); //find the magnitude of rotation per step
        longitudeSteps.add(speed);
    }

    /**
     * Orbit camera latitudinally by a magnitude in radians with a speed factor of 'speed'.
     * The speed means how many update calls does it take to achieve the change.
     */
    public void smoothRotateLat(float magnitude, int speed) {
        latitudeIncrement.add(magnitude / speed); //find the magnitude of rotation per step
        latitudeSteps.add(speed);
    }

    /**
     * Move camera eye and target positions to absolute positions given. This will be done in speed number of update
     * calls.
     */
    public void smoothTranslateAbsolute(Vector3f campos, Vector3f tarpos, int speed) {
        Vector3f eye = new Vector3f();
        eye.set(campos);
        Vector3f target = (Vector3f) tarpos.clone();

        // If we move absolutely, then remove all previously queued camera translations.
        targetIncrement.clear();
        targetSteps.clear();
        eyeIncrement.clear();
        eyeSteps.clear();

        // Find the difference between where we are and where we want to go.
        eye.sub(eyePos);
        target.sub(targetPos);

        // Scale such that this will be completed in speed-number of update calls.
        eye.scale(1f / speed);
        target.scale(1f / speed);

        eyeIncrement.add(eye); // Find the magnitude of rotation per step.
        eyeSteps.add(speed);

        targetIncrement.add(target); // Find the magnitude of rotation per step.
        targetSteps.add(speed);
    }

    /**
     * Move camera eye and target by offset amounts given. This will be done in speed number of update calls.
     */
    public void smoothTranslateRelative(Vector3f campos, Vector3f tarpos, int speed) {
        Vector3f eye = (Vector3f) campos.clone();
        Vector3f target = (Vector3f) tarpos.clone();

        // Scale such that this will be completed in speed-number of update calls.
        eye.scale(1f / speed);
        target.scale(1f / speed);

        eyeIncrement.add(eye); //find the magnitude of rotation per step
        eyeSteps.add(speed);

        targetIncrement.add(target); //find the magnitude of rotation per step
        targetSteps.add(speed);
    }

    /**
     * User interaction to rotate the camera latitudinally. Magnitude of rotation is in radians and may be negative.
     */
    public void rotateLatitude(float magnitude) {

        //Vector from target to eye:
        Vector3f distVec = new Vector3f();
        distVec.sub(eyePos, targetPos);

        //Axis to the left in the camera world. Magnitude provided by user.
        AxisAngle4f rotation = new AxisAngle4f(modelViewMat[1], modelViewMat[5], modelViewMat[9], magnitude);

        Matrix3f rotMat = new Matrix3f();
        rotMat.set(rotation);

        //Transform the target to eye vector
        rotMat.transform(distVec);

        //Add back to get absolute position and set this to be the eye position of the camera.
        eyePos.add(targetPos, distVec);
    }

    /**
     * User interaction to rotate the camera longitudinally. Magnitude of rotation is in radians and may be negative.
     */
    public void rotateLongitude(float magnitude) {

        if (magnitude != 0f) {
            // Vector from target to eye:
            Vector3f distVec = new Vector3f();
            distVec.sub(eyePos, targetPos);

            // Axis to the left in the camera world. Magnitude provided by user.
            AxisAngle4f rotation = new AxisAngle4f(modelViewMat[0], modelViewMat[4], modelViewMat[8], magnitude);
            // TODO fix singularity.
            Matrix3f rotMat = new Matrix3f();
            rotMat.set(rotation);

            // Transform the target to eye vector
            rotMat.transform(distVec);

            // Add back to get absolute position and set this to be the eye position of the camera.
            eyePos.add(targetPos, distVec);
        }
    }

    /**
     * Find a vector which represents the click ray in 3D space. Mostly stolen from my cloth simulator.
     */
    private Vector3f clickVector(int mouseX, int mouseY) {
        //Frame height in world dimensions (not pixels)
        float frameHeight;
        float frameWidth;

        //Click position in world dimensions
        float xClick;
        float yClick;

        //Vector of click direction.
        Vector3f clickVec;

        //Vector of eye position to target position.
        Vector3f camVec = new Vector3f(0, 0, 0);

        //Camera locally defined to face in y-direction:
        Vector3f localCamLookat = new Vector3f(0, 1, 0);

        Vector3f localCamUp = new Vector3f(1, 0, 0);

        // Axis and angle of rotation from world coordinates to camera coordinates.
        Vector3f rotAxis = new Vector3f(0, 0, 0);
        float transAngle;

        //Rotation from world coordinates to camera coordinates in both axis angle and matrix forms.
        AxisAngle4f camToGlobalRot = new AxisAngle4f(0, 0, 0, 0);
        Matrix3f RotMatrix = new Matrix3f(0, 0, 0, 0, 0, 0, 0, 0, 0);


        //Frame height and width in world dimensions.
        frameHeight = (float) (2 * Math.tan(viewAng / 180.0 * Math.PI / 2.0));
        frameWidth = frameHeight * width / height;
        // Position in world dimensions of click on front viewing plane. Center is defined as zero.
        xClick = frameWidth * (mouseX - width / 2) / width;
        yClick = frameHeight * (mouseY - height / 2) / height;

        // Vector of click in camera coordinates.
        clickVec = new Vector3f(-yClick, 1, -xClick);
        clickVec.normalize();

        // Camera facing origin in world coordinates.
        camVec.sub(targetPos, eyePos);
        camVec.normalize();

        //Find transformation -- world frame <-> cam frame
        // Two step process. First I align the camera facing vector direction.
        // Second, I align the "up" vector.

        //1st rotation
        rotAxis.cross(localCamLookat, camVec);
        rotAxis.normalize();
        transAngle = (float) Math.acos(localCamLookat.dot(camVec));
        camToGlobalRot.set(rotAxis, transAngle);
        RotMatrix.set(camToGlobalRot);

        //2nd rotation
        RotMatrix.transform(localCamUp);
        RotMatrix.transform(clickVec);

        rotAxis.cross(localCamUp, upVec);
        rotAxis.normalize();
        transAngle = (float) Math.acos(localCamUp.dot(upVec));
        camToGlobalRot.set(rotAxis, transAngle);
        RotMatrix.set(camToGlobalRot);

        //Transform the click vector to world coordinates
        RotMatrix.transform(clickVec);
        return clickVec;
    }

    /**
     * Take a click vector, find the nearest node to this line.
     *
     * @param clickVec Click direction as defined by the view frustum.
     * @param root     Root of a tree. All the nodes in this tree will be evaluated to find the closest.
     * @return Nearest node to the click ray.
     * @see GLCamManager#clickVector(int, int)
     */
    public NodeQWOPGraphicsBase<?> nodeFromRay(Vector3f clickVec, NodeQWOPGraphicsBase<?> root) {
        // Determine which point is closest to the clicked ray.
        ArrayList<NodeQWOPGraphicsBase<?>> nodeList = new ArrayList<>();
        root.recurseDownTreeInclusive(nodeList::add);
        return nodeFromRay_set(clickVec, nodeList);
    }

    /**
     * Take a click vector, find the nearest node to this line.
     */
    private NodeQWOPGraphicsBase<?> nodeFromRay_set(Vector3f clickVec, List<NodeQWOPGraphicsBase<?>> nodes) {
        NodeQWOPGraphicsBase<?> closestNodeInList = null;
        double smallestDistance = Double.MAX_VALUE;
        double tanDist;
        double normDistSq;

        for (NodeQWOPGraphicsBase<?> node : nodes) {
            //Vector from eye to a vertex.
            Vector3f nodePos = new Vector3f(node.nodeLocation[0], node.nodeLocation[1], node.nodeLocation[2]);
            EyeToPoint.sub(nodePos, eyePos);

            tanDist = EyeToPoint.dot(clickVec);
            normDistSq = EyeToPoint.lengthSquared() - tanDist * tanDist;

            if (normDistSq < smallestDistance) {
                smallestDistance = normDistSq;
                closestNodeInList = node;
            }
        }
        Objects.requireNonNull(closestNodeInList);

        return closestNodeInList;
    }

    /**
     * Return the closest node to a click.
     */
    public NodeQWOPGraphicsBase<?> nodeFromClick(int mouseX, int mouseY, NodeQWOPGraphicsBase<?> root) {
        clickVec = clickVector(mouseX, mouseY);
        return nodeFromRay(clickVec, root);
    }

    /**
     * Given a set of nodes
     */
    public NodeQWOPGraphicsBase<?> nodeFromClick_set(int mouseX, int mouseY, List<NodeQWOPGraphicsBase<?>> snapshotLeaves) {
        clickVec = clickVector(mouseX, mouseY);
        return nodeFromRay_set(clickVec, snapshotLeaves);
    }

    /**
     * Take a click vector, find the coordinates of the projected point at a given level.
     * Note: assumes trees always stay perpendicular to the z-axis.
     */
    public Vector3f planePtFromRay(int mouseX, int mouseY, float levelset) {
        // Determine which point is closest to the clicked ray.

        clickVec = clickVector(mouseX, mouseY); //Make a copy so scaling doesn't do weird things further up.

        float multiplier = (levelset - eyePos.z) / clickVec.z; // How many clickvecs does it take to reach the plane?

        clickVec.scale(multiplier); //scale so it reaches from eye to clicked point

        clickVec.add(eyePos); // Add the eye position so we get the actual clicked point.
        return clickVec;
    }

    /**
     * Keeping zoom factor private to keep meddling without using the correct functions.
     */
    public float getZoomFactor() {
        return zoomFactor;
    }
}
