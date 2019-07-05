package ui.runner;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.action.Action;
import ui.IUserInterface.TabbedPaneActivator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;

/**
 * Snapshot viewers of the runner and animations of the runner share a lot of features and dimensions.
 * Hence, this superclass handles certain shared drawing processes.
 *
 * @author matt
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PanelRunner_Animated.class, name = "runner_animated"),
        @JsonSubTypes.Type(value = PanelRunner_Snapshot.class, name = "runner_snapshot"),
        @JsonSubTypes.Type(value = PanelRunner_MultiState.class, name = "runner_multistate"),
        @JsonSubTypes.Type(value = PanelRunner_SimpleState.class, name = "runner_simplestate"),
        @JsonSubTypes.Type(value = PanelRunner_AnimatedTransformed.class, name = "runner_animated_transformed"),
        @JsonSubTypes.Type(value = PanelRunner_Comparison.class, name = "runner_comparison"),
        @JsonSubTypes.Type(value = PanelRunner_AnimatedFromStates.class, name = "runner_animated_states")
})
public abstract class PanelRunner extends JPanel implements TabbedPaneActivator, ComponentListener {

    /**
     * Should this panel be drawing or is it hidden.
     */
    transient protected boolean active = false;

    /**
     * Fonts for drawing the keyboard keys when pressed/not pressed.
     */
    protected final static Font smallFont = new Font("Ariel", Font.BOLD, 12);
    protected final static Font medFont = new Font("Ariel", Font.BOLD, 21);
    protected final static Font bigFont = new Font("Ariel", Font.BOLD, 28);

    /**
     * Normal stroke for line drawing.
     */
    public final static Stroke normalStroke = new BasicStroke(0.5f);

    /**
     * Highlight stroke for line drawing.
     */
    public final static Stroke boldStroke = new BasicStroke(2);

    transient public Stroke customStroke;

    /**
     * Faded out gray for drawing past states and such.
     */
    public final static Color ghostGray = new Color(0.6f, 0.6f, 0.6f);

    /**
     * Drawing offsets within the viewing panel (i.e. non-physical)
     */
    transient public int xOffsetPixels = 500;
    transient public int yOffsetPixels = 100;

    /**
     * Runner coordinates to pixels.
     */
    transient public float runnerScaling = 10f;

    private final int startX = -45;
    private final int startY = yOffsetPixels - 85;
    private float keyScaling;

    public PanelRunner() {
        this.addComponentListener(this);
    }

    protected void keyDrawer(Graphics g, boolean q, boolean w, boolean o, boolean p) {
        keyDrawer(g, q, w, o, p, startX, startY, 30, (int) keyScaling);
    }

    /**
     * Draw the pressed keys in the panel during running.
     */
    public static void keyDrawer(Graphics g, boolean q, boolean w, boolean o, boolean p, int xOffset, int yOffset,
                             int offsetBetweenPairs, int size) {
        int qOffset = (q ? 10 : 0);
        int wOffset = (w ? 10 : 0);
        int oOffset = (o ? 10 : 0);
        int pOffset = (p ? 10 : 0);

        Font bigFont = PanelRunner.bigFont.deriveFont(PanelRunner.bigFont.getSize() * (size/40f));
        Font medFont = PanelRunner.medFont.deriveFont(PanelRunner.medFont.getSize() * (size/40f));
        Font activeFont;
        FontMetrics fm;
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.DARK_GRAY);
        g2.drawRoundRect(xOffset + size * 2 - qOffset / 2, yOffset - qOffset / 2, size + qOffset, size + qOffset,
                (size + qOffset) / 10, (size + qOffset) / 10);
        g2.drawRoundRect(xOffset + size * 4 - wOffset / 2, yOffset - wOffset / 2, size + wOffset, size + wOffset,
                (size + wOffset) / 10, (size + wOffset) / 10);

        /* Parameters for placing the "keys" graphically. */
        g2.drawRoundRect(xOffset + size * 6 - oOffset / 2 + offsetBetweenPairs, yOffset - oOffset / 2, size + oOffset,
                size + oOffset, (size + oOffset) / 10, (size + oOffset) / 10);
        g2.drawRoundRect(xOffset + size * 8 - pOffset / 2 + offsetBetweenPairs, yOffset - pOffset / 2, size + pOffset,
                size + pOffset, (size + pOffset) / 10, (size + pOffset) / 10);

        g2.setColor(Color.LIGHT_GRAY);
        g2.fillRoundRect(xOffset + size * 2 - qOffset / 2, yOffset - qOffset / 2, size + qOffset, size + qOffset,
                (size + qOffset) / 10, (size + qOffset) / 10);
        g2.fillRoundRect(xOffset + size * 4 - wOffset / 2, yOffset - wOffset / 2, size + wOffset, size + wOffset,
                (size + wOffset) / 10, (size + wOffset) / 10);
        g2.fillRoundRect(xOffset + size * 6 - oOffset / 2 + offsetBetweenPairs, yOffset - oOffset / 2, size + oOffset,
                size + oOffset, (size + oOffset) / 10, (size + oOffset) / 10);
        g2.fillRoundRect(xOffset + size * 8 - pOffset / 2 + offsetBetweenPairs, yOffset - pOffset / 2, size + pOffset,
                size + pOffset, (size + pOffset) / 10, (size + pOffset) / 10);

        g2.setColor(Color.BLACK);

        // Used for making sure text stays centered.
        activeFont = q ? bigFont : medFont;
        g2.setFont(activeFont);
        fm = g2.getFontMetrics();
        g2.drawString("Q", xOffset + size * 2 + size / 2 - fm.stringWidth("Q") / 2,
                yOffset + size / 2 + fm.getHeight() / 3);

        activeFont = w ? bigFont : medFont;
        g2.setFont(activeFont);
        fm = g2.getFontMetrics();
        g2.drawString("W", xOffset + size * 4 + size / 2 - fm.stringWidth("W") / 2,
                yOffset + size / 2 + fm.getHeight() / 3);

        activeFont = o ? bigFont : medFont;
        g2.setFont(activeFont);
        fm = g2.getFontMetrics();
        g2.drawString("O", xOffset + size * 6 + size / 2 - fm.stringWidth("O") / 2 + offsetBetweenPairs,
                yOffset + size / 2 + fm.getHeight() / 3);

        activeFont = p ? bigFont : medFont;
        g2.setFont(activeFont);
        fm = g2.getFontMetrics();
        g2.drawString("P", xOffset + size * 8 + size / 2 - fm.stringWidth("P") / 2 + offsetBetweenPairs,
                yOffset + size / 2 + fm.getHeight() / 3);
    }

    /**
     * Draw the game.action on the left side pane.
     */
    protected void drawActionString(Action[] sequence, Graphics g) {
        drawActionString(g, sequence, -1);
    }

    protected static void drawActionString(Graphics g, Action[] sequence, int highlightIdx) {

        if (sequence.length == 0) return; // Happens when clicking root node.

        g.setFont(smallFont);
        g.setColor(Color.DARK_GRAY);

        int currIdx = 0;
        int lineNum = 1;
        int vertTextAnchor = 60;
        do {
            String line = sequence[currIdx].getTimestepsTotal() + ",";

            if (currIdx == highlightIdx) {
                g.setColor(Color.GREEN);
            } else {
                g.setColor(Color.DARK_GRAY);
            }
            // Spacing for sequence number drawing on the left side panel.
            int vertTextSpacing = 18;
            g.drawString(line, 10 + (currIdx % 4) * 50 + lineNum / 7 * 210,
                    vertTextAnchor + vertTextSpacing * (lineNum % 7 + 2)); // Wrap horizontally after 7 lines

            currIdx++;
            lineNum = currIdx / 4 + 1;

        } while (currIdx < sequence.length);

        // Draw the little keys above the column.
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.DARK_GRAY);
        g2.drawRoundRect(8, vertTextAnchor + 15, 30, 20, 5, 5);
        g2.drawRoundRect(8 + 49, vertTextAnchor + 15, 30, 20, 5, 5);
        g2.drawRoundRect(8 + 2 * 49, vertTextAnchor + 15, 30, 20, 5, 5);
        g2.drawRoundRect(8 + 3 * 49, vertTextAnchor + 15, 30, 20, 5, 5);

        g2.setColor(Color.LIGHT_GRAY);
        g2.fillRoundRect(8, vertTextAnchor + 15, 30, 20, 6, 6);
        g2.fillRoundRect(8 + 49, vertTextAnchor + 15, 30, 20, 6, 6);
        g2.fillRoundRect(8 + 2 * 49, vertTextAnchor + 15, 30, 20, 6, 6);
        g2.fillRoundRect(8 + 3 * 49, vertTextAnchor + 15, 30, 20, 6, 6);

        g.setFont(smallFont);
        g.setColor(Color.BLACK);
        g.drawString("- -", 12, vertTextAnchor + 30);
        g.drawString("W O", 12 + 49, vertTextAnchor + 30);
        g.drawString("- -", 12 + 2 * 49, vertTextAnchor + 30);
        g.drawString("Q P", 12 + 3 * 49, vertTextAnchor + 30);
    }

    @Override
    public abstract void activateTab();

    @Override
    public abstract void deactivateTab();

    @Override
    public boolean isActive() {
        return active;
    }

    // Thanks: https://stackoverflow.com/questions/2027613/how-to-draw-a-directed-arrow-line-in-java
    public static Shape createArrowShape(Point fromPt, Point toPt, float thickness) {
        Polygon arrowPolygon = new Polygon();

        // Values are somewhat arbitrary since scaling happens later. Just want them large enough so integer rounding
        // doesn't get problematic.
        arrowPolygon.addPoint(-600, (int) (thickness /2f));
        arrowPolygon.addPoint((int) (450 - thickness), (int) (thickness /2f));
        arrowPolygon.addPoint((int) (450 - thickness), (int) (3 * thickness /2f));
        arrowPolygon.addPoint(600,0);
        arrowPolygon.addPoint((int) (450 - thickness),(int) (-3 * thickness /2f));
        arrowPolygon.addPoint((int) (450 - thickness), (int) (-thickness /2f));
        arrowPolygon.addPoint(-600, (int) (-thickness /2f));

        Point midPoint = midpoint(fromPt, toPt);

        double rotate = Math.atan2(toPt.y - fromPt.y, toPt.x - fromPt.x);

        AffineTransform transform = new AffineTransform();
        transform.translate(midPoint.x, midPoint.y);
        double ptDistance = fromPt.distance(toPt);
        double scale = ptDistance / 1200.0; // 12 because it's the length of the arrow polygon.
        transform.scale(scale, scale);
        transform.rotate(rotate);

        return transform.createTransformedShape(arrowPolygon);
    }


    public static Shape createArrowShape(float angle, float length, Point toPt, float thickness) {
        Polygon arrowPolygon = new Polygon();

        // Values are somewhat arbitrary since scaling happens later. Just want them large enough so integer rounding
        // doesn't get problematic.
        arrowPolygon.addPoint(-1200, (int) (thickness /2f));
        arrowPolygon.addPoint((int) (-150 - thickness), (int) (thickness /2f));
        arrowPolygon.addPoint((int) (-150 - thickness), (int) (3 * thickness /2f));
        arrowPolygon.addPoint(0,0);
        arrowPolygon.addPoint((int) (-150 - thickness),(int) (-3 * thickness /2f));
        arrowPolygon.addPoint((int) (-150 - thickness), (int) (-thickness /2f));
        arrowPolygon.addPoint(-1200, (int) (-thickness /2f));

//        Point midPoint = new Point((int) (toPt.x  + Math.cos(length)), (int) (toPt.y + Math.sin(length)));

        AffineTransform transform = new AffineTransform();
        transform.translate(toPt.x, toPt.y);
        double scale = length / 1200.0; // 12 because it's the length of the arrow polygon.
        transform.scale(scale, scale);
        transform.rotate(angle);

        return transform.createTransformedShape(arrowPolygon);
    }

    private static Point midpoint(Point p1, Point p2) {
        return new Point((int)((p1.x + p2.x)/2.0),
                (int)((p1.y + p2.y)/2.0));
    }

    @Override
    public void componentResized(ComponentEvent e) {
        this.xOffsetPixels = getWidth()/2;
        this.yOffsetPixels = getHeight()/2;
        runnerScaling = getHeight()/25f;
        keyScaling = Math.min(getHeight() / 6f, getWidth() / 30f);
    }

    @Override
    public void componentMoved(ComponentEvent e) {}

    @Override
    public void componentShown(ComponentEvent e) {}

    @Override
    public void componentHidden(ComponentEvent e) {}
}
