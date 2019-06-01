package hardware;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class TimeTester {

    KeypusherSerialConnection serial;

    private volatile boolean donePress = false;
    private volatile boolean doneDepress = false;

    public TimeTester() {
        JFrame frame = new JFrame(); // New frame to hold and manage the QWOP JPanel.
        frame.add(new JPanel());
        frame.addKeyListener(new Listener()); // Listen for user input.
        frame.setPreferredSize(new Dimension(600, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        try {
            serial = new KeypusherSerialConnection();
            Thread.sleep(400);
            serial.command(false, false,false, false);
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public long testKeyTime(int testTimes) {
        for (int i = 0; i < testTimes; i++) {
            donePress = false;
            serial.command(false, false, true, false);

            long startTimePress = System.currentTimeMillis();

            while (!donePress) {
            }

            long endTimePress = System.currentTimeMillis();

            System.out.println("milliseconds to push key down: " + (endTimePress - startTimePress));

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            doneDepress = false;
            serial.command(false, false, false, false);
            long startTimeDepress = System.currentTimeMillis();

            while (!doneDepress) {
            }

            long endTimeDepress = System.currentTimeMillis();

            System.out.println("milliseconds to release key : " + (endTimeDepress - startTimeDepress));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }


    private class Listener implements KeyListener {

        public Listener() {
        }

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            donePress = true;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            doneDepress = true;
        }
    }

    public static void main(String[] args) {
        TimeTester tt = new TimeTester();


        tt.testKeyTime(10);

        System.exit(0);
    }
}
