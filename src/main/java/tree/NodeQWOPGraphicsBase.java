package tree;

import actions.Action;
import actions.IActionGenerator;
import com.jogamp.opengl.GL2;
import game.State;

import java.awt.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Explorable QWOP node which provides no additional functionality beyond drawing capabilities. If you don't need this,
 * use {@link NodeQWOPExplorableBase} for memory efficiency.
 *
 * See {@link NodeGenericBase} for an explanation of the ridiculous generics. It's not as bad as it looks.
 *
 * @param <N>
 */
public abstract class NodeQWOPGraphicsBase<N extends NodeQWOPGraphicsBase<N>> extends NodeQWOPExplorableBase<N> {
    /**
     * Determines whether very close lines/nodes will be drawn. Can greatly speed up UI for very dense trees.
     */
    private static final boolean limitDrawing = true;

    public static final Set<NodeQWOPGraphicsBase> pointsToDraw = ConcurrentHashMap.newKeySet(10000);
    private static final float nodeDrawFilterDistSq = 0.1f;
    public boolean notDrawnForSpeed = false;
    public boolean displayPoint = false;
    public boolean displayLine = true;

    public float[] nodeLocation = new float[3]; // Location that this node appears on the tree visualization
    float nodeAngle = 0; // Keep track of the angle that the line previous node to this node makes.
    float sweepAngle = 2f * (float) Math.PI;
    private float edgeLength = 1.f;

    public Color pointColor = Color.GREEN;

    private static final float lineBrightness = 0.85f;

    /**
     * If we want to bring out a certain part of the tree so it doesn't hide under other parts, give it a small z
     * offset.
     */
    private float nodeLocationZOffset = 0.f;

    public NodeQWOPGraphicsBase(State rootState, IActionGenerator actionGenerator) {
        super(rootState, actionGenerator);
    }

    public NodeQWOPGraphicsBase(State rootState) {
        super(rootState);
    }

    public NodeQWOPGraphicsBase(N parent, Action action, State state, IActionGenerator actionGenerator) {
        super(parent, action, state, actionGenerator);
    }


    /**
     * Draw the line connecting this node to its parent.
     */
    public void drawLine(GL2 gl) {
        // TODO override color
        if ((getTreeDepth() > 0) && displayLine) { // No lines for root.
            gl.glColor3fv(getColorFromTreeDepth(getTreeDepth()).getColorComponents(null), 0);
            gl.glVertex3d(getParent().nodeLocation[0], getParent().nodeLocation[1],
                    getParent().nodeLocation[2] + nodeLocationZOffset);
            gl.glVertex3d(nodeLocation[0], nodeLocation[1], nodeLocation[2] + nodeLocationZOffset);
        }
    }

    /**
     * Draw the node point if enabled
     */
    public void drawPoint(GL2 gl) {
        // TODO override color.
        if (displayPoint) {
            gl.glColor3fv(pointColor.getColorComponents(null), 0);
            gl.glVertex3d(nodeLocation[0], nodeLocation[1], nodeLocation[2] + nodeLocationZOffset);
        }
    }

    /**
     * Color the node scaled by depth in the tree. Totally for gradient pleasantness.
     */
    public static Color getColorFromTreeDepth(float depth) {
        return getColorFromScaledValue(depth, 10f, lineBrightness);
    }

    public static Color getColorFromScaledValue(float val, float max, float brightness) {
        float colorOffset = 0f;
        float scaledDepth = val / max;
        float H = scaledDepth * 0.38f + colorOffset;
        float S = 0.8f;
        return Color.getHSBColor(H, S, brightness);
    }


    private void calcNodePos() {
        //Angle of this current node -- parent node's angle - half the total sweep + some increment so that all will
        // span the required sweep.
        if (getTreeDepth() >= 0) {
            int possibleParentBranchSlots = getParent().getChildCount() + getParent().getUntriedActionCount();

            if (possibleParentBranchSlots > 1) { //Catch the div by 0

                int childNo = getIndexAccordingToParent();

                sweepAngle = (float) Math.max((getParent().sweepAngle / possibleParentBranchSlots)
                                * (1 + getTreeDepth() * 0.05f), 0.005);

                // This is to straighten out branches that are curving off to one side due to asymmetric expansion.
                // Acts like a controller to bring the angle towards the angle of the first node in this branch after
                // root.
                float angleAdj;
                N ancestor = getThis();
                while (ancestor.getTreeDepth() > 1) {
                    ancestor = ancestor.getParent();
                }
                angleAdj = -0.2f * (getParent().nodeAngle - ancestor.nodeAngle);

                if (childNo == 0) {
                    nodeAngle = getParent().nodeAngle + angleAdj;
                } else if (childNo % 2 == 0) {
                    nodeAngle = getParent().nodeAngle + sweepAngle * childNo / 2 + angleAdj;
                } else {
                    nodeAngle = getParent().nodeAngle - sweepAngle * (childNo + 1) / 2 + angleAdj;
                }
            } else {
                sweepAngle = getParent().sweepAngle; //Only reduce the sweep angle if the parent one had more than one child.
                nodeAngle = getParent().nodeAngle;
            }
        }

        nodeLocation[0] = (float) (getParent().nodeLocation[0] + edgeLength * Math.cos(nodeAngle));
        nodeLocation[1] = (float) (getParent().nodeLocation[1] + edgeLength * Math.sin(nodeAngle));
        nodeLocation[2] = 0f; // No out of plane stuff yet.

        // Since drawing speed is a UI bottleneck, we may want to filter out some of the points that are REALLY close.
        if (limitDrawing) {
            float xDiff;
            float yDiff;
            float zDiff;
            for (NodeQWOPGraphicsBase n : pointsToDraw) {
                xDiff = n.nodeLocation[0] - nodeLocation[0];
                yDiff = n.nodeLocation[1] - nodeLocation[1];
                zDiff = n.nodeLocation[2] - nodeLocation[2];
                // Actually distance squared to avoid sqrt
                if ((xDiff * xDiff + yDiff * yDiff + zDiff * zDiff) < nodeDrawFilterDistSq) {
                    notDrawnForSpeed = true;
                    return; // Too close. Turn it off and get out.
                }
            }
            notDrawnForSpeed = false;
            pointsToDraw.add(this);
        }
    }

    /**
     * Resets the angle at which child nodes will fan out from this node in the visualization. This must be done
     * before the children are created. The angle is set to 3pi/2.
     */
    public void resetSweepAngle() {
        sweepAngle = (float)(3.*Math.PI/2.);
    }
}
