package hardware;

import game.qwop.CommandQWOP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * For testing the round-trip latency of the solenoid keypresser robot. This opens a window that is just listening
 * for keypresses. Then it sends serial commands to the microcontroller to press a certain key. It reports the time
 * between sending the command and receiving the keystroke back on the computer end. It reports this time for both
 * pressing and depressing the key. It repeats this a number of times.
 *
 * Experimentally, these numbers will be somewhere between 10ms and 40ms. Ideally, something like 15-20ms. Outside
 * the broader range something is probably wrong. Even too short of keypresses might indicate strange behavior.
 *
 * @author matt
 */
public class TimeTester {

    /**
     * For communication with the solenoid-controlling microcontroller.
     */
    private KeypusherSerialConnection serial;

    /**
     * Whether the commanded press command has completed.
     */
    private volatile boolean donePress = false;

    /**
     * Whether the commanded depress command has completed.
     */
    private volatile boolean doneDepress = false;

    /**
     * For logging the press and depress times.
     */
    private static final Logger logger = LogManager.getLogger(TimeTester.class);

    public TimeTester() {
        // Make a window that listens for keystrokes.
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setFocusable(true);
        frame.add(new JPanel());
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) { donePress = true; }
            @Override
            public void keyReleased(KeyEvent e) { doneDepress = true; }
        }); // Listen for user input.

        frame.setPreferredSize(new Dimension(600, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        frame.toFront();

        // Open a connection to the microcontroller and turn off all solenoids.
        try {
            serial = new KeypusherSerialConnection();
            Thread.sleep(400);
            serial.command(CommandQWOP.NONE);
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test how long it takes for a keypress to occur after it has been commanded. Make sure that the window does not
     * lose focus during the test.
     * @param testIterations Number of times to press and release the key.
     * @return Average response time for both press and depress commands. I think it is more informative to look at
     * the log messages, since sometimes a strange spike in times, or a big mismatch between press and depress can
     * indicate that hardware tuning is needed.
     */
    public long testKeyTime(CommandQWOP commandToTest, int testIterations) {
        long msSpent = 0;
        for (int i = 0; i < testIterations; i++) {
            donePress = false;
            serial.command(commandToTest);

            long startTimePress = System.currentTimeMillis();

            while (!donePress) {}

            long endTimePress = System.currentTimeMillis();
            long pressTime = endTimePress - startTimePress;
            logger.info("milliseconds to push key down: " + pressTime);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            doneDepress = false;
            serial.command(CommandQWOP.NONE);
            long startTimeDepress = System.currentTimeMillis();

            while (!doneDepress) {}

            long endTimeDepress = System.currentTimeMillis();
            long depressTime = endTimeDepress - startTimeDepress;

            logger.info("milliseconds to release key : " + (endTimeDepress - startTimeDepress));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            msSpent += pressTime;
            msSpent += depressTime;
        }
        return msSpent / (2 * testIterations); // Press and depress times are in there hence the 2.
    }

    public static void main(String[] args) {
        TimeTester tt = new TimeTester();
        tt.testKeyTime(CommandQWOP.P, 10); // Change out the keys here or chain tests together here.

        System.exit(0);
    }
}
