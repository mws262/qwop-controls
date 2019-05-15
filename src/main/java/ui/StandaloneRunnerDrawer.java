package ui;

import game.State;

import javax.swing.*;
import java.awt.*;

/**
 * Draw a runner at a given state. Makes its own panel pop up. Good for exploring and debugging without massive
 * overhead and code.
 *
 * @author matt
 */
public class StandaloneRunnerDrawer extends JFrame implements Runnable {

    /**
     * Make a new drawer which will have its own window and just display a single runner at a single pose.
     * @param state The state to draw the runner at.
     */
    public StandaloneRunnerDrawer(State state) {
        PanelRunner_SimpleState panel = new PanelRunner_SimpleState();
        panel.activateTab();
        getContentPane().add(panel);
        setPreferredSize(new Dimension(1000, 400));
        pack();
        setVisible(true);
        panel.updateState(state);

        Thread drawThread = new Thread(this);
        drawThread.start();
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while(true) {
            repaint();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
