package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.JPanel;

import actions.Action;
import ui.IUserInterface.TabbedPaneActivator;

/**
 * Snapshot viewers of the runner and animations of the runner share a lot of features and dimensions.
 * Hence, this superclass handles certain shared drawing processes.
 *
 * @author matt
 */
public abstract class PanelRunner extends JPanel implements TabbedPaneActivator {

    private static final long serialVersionUID = 1L;

    /**
     * Should this panel be drawing or is it hidden.
     **/
    protected boolean active = false;

    /**
     * Fonts for drawing the keyboard keys when pressed/not pressed.
     **/
    protected final static Font smallFont = new Font("Ariel", Font.BOLD, 12);
    protected final static Font medFont = new Font("Ariel", Font.BOLD, 21);
    protected final static Font bigFont = new Font("Ariel", Font.BOLD, 28);

    /**
     * Normal stroke for line drawing.
     **/
    public final static Stroke normalStroke = new BasicStroke(0.5f);

    /**
     * Highlight stroke for line drawing.
     **/
    public final static Stroke boldStroke = new BasicStroke(2);

    /**
     * Faded out gray for drawing past states and such.
     **/
    public final static Color ghostGray = new Color(0.6f, 0.6f, 0.6f);

    /**
     * Drawing offsets within the viewing panel (i.e. non-physical)
     **/
    public int xOffsetPixels = 500;
    public int yOffsetPixels = 100;

    /**
     * Runner coordinates to pixels.
     **/
    public float runnerScaling = 10f;

    private final int startY = yOffsetPixels - 85;


    /**
     * Draw the pressed keys in the panel during running.
     **/
    protected void keyDrawer(Graphics g, boolean q, boolean w, boolean o, boolean p) {

        int qOffset = (q ? 10 : 0);
        int wOffset = (w ? 10 : 0);
        int oOffset = (o ? 10 : 0);
        int pOffset = (p ? 10 : 0);

        Font activeFont;
        FontMetrics fm;
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.DARK_GRAY);
        int size = 40;
        int startX = -45;
        g2.drawRoundRect(startX + 80 - qOffset / 2, startY - qOffset / 2, size + qOffset, size + qOffset,
                (size + qOffset) / 10, (size + qOffset) / 10);
        g2.drawRoundRect(startX + 160 - wOffset / 2, startY - wOffset / 2, size + wOffset, size + wOffset,
                (size + wOffset) / 10, (size + wOffset) / 10);
        /** Parameters for placing the "keys" graphically. **/
        int offsetBetweenPairs = 30;
        g2.drawRoundRect(startX + 240 - oOffset / 2 + offsetBetweenPairs, startY - oOffset / 2, size + oOffset,
                size + oOffset, (size + oOffset) / 10, (size + oOffset) / 10);
        g2.drawRoundRect(startX + 320 - pOffset / 2 + offsetBetweenPairs, startY - pOffset / 2, size + pOffset,
                size + pOffset, (size + pOffset) / 10, (size + pOffset) / 10);

        g2.setColor(Color.LIGHT_GRAY);
        g2.fillRoundRect(startX + 80 - qOffset / 2, startY - qOffset / 2, size + qOffset, size + qOffset,
                (size + qOffset) / 10, (size + qOffset) / 10);
        g2.fillRoundRect(startX + 160 - wOffset / 2, startY - wOffset / 2, size + wOffset, size + wOffset,
                (size + wOffset) / 10, (size + wOffset) / 10);
        g2.fillRoundRect(startX + 240 - oOffset / 2 + offsetBetweenPairs, startY - oOffset / 2, size + oOffset,
                size + oOffset, (size + oOffset) / 10, (size + oOffset) / 10);
        g2.fillRoundRect(startX + 320 - pOffset / 2 + offsetBetweenPairs, startY - pOffset / 2, size + pOffset,
                size + pOffset, (size + pOffset) / 10, (size + pOffset) / 10);

        g2.setColor(Color.BLACK);

        //Used for making sure text stays centered.

        activeFont = q ? bigFont : medFont;
        g2.setFont(activeFont);
        fm = g2.getFontMetrics();
        g2.drawString("Q", startX + 80 + size / 2 - fm.stringWidth("Q") / 2, startY + size / 2 + fm.getHeight() / 3);


        activeFont = w ? bigFont : medFont;
        g2.setFont(activeFont);
        fm = g2.getFontMetrics();
        g2.drawString("W", startX + 160 + size / 2 - fm.stringWidth("W") / 2, startY + size / 2 + fm.getHeight() / 3);

        activeFont = o ? bigFont : medFont;
        g2.setFont(activeFont);
        fm = g2.getFontMetrics();
        g2.drawString("O", startX + 240 + size / 2 - fm.stringWidth("O") / 2 + offsetBetweenPairs,
                startY + size / 2 + fm.getHeight() / 3);

        activeFont = p ? bigFont : medFont;
        g2.setFont(activeFont);
        fm = g2.getFontMetrics();
        g2.drawString("P", startX + 320 + size / 2 - fm.stringWidth("P") / 2 + offsetBetweenPairs,
                startY + size / 2 + fm.getHeight() / 3);
    }

    /**
     * Draw the actions on the left side pane.
     **/
    protected void drawActionString(Action[] sequence, Graphics g) {
        drawActionString(g, sequence, -1);
    }

    protected void drawActionString(Graphics g, Action[] sequence, int highlightIdx) {

        if (sequence.length == 0) return; // Happens when clicking root node.

        g.setFont(smallFont);
        // g.setColor(Color.BLACK);
        // g.drawString("Selected sequence: ", 10, vertTextAnchor);
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
            /** Spacing for sequence number drawing on the left side panel. **/
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
    public void activateTab() {
        active = true;
    }

    @Override
    public abstract void deactivateTab();

    @Override
    public boolean isActive() {
        return active;
    }

}
