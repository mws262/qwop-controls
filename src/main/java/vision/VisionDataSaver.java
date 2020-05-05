package vision;

import flashqwop.IFlashStateListener;
import game.qwop.StateQWOP;
import game.state.IState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

<<<<<<< HEAD
=======
import java.awt.*;
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

/**
 * Manages taking screenshots of the QWOP game while also recording state information. This can be added as a
 * listener to {@link flashqwop.FlashQWOPServer}. This is, more or less, an expansion of the screenshot features
 * found in {@link VisionDataSaver}.
 *
 * @author matt
 */
public class VisionDataSaver implements IFlashStateListener {

    private Queue<File> capturesThisRun = new LinkedList<>();
    private Queue<IState> statesThisRun = new LinkedList<>();

    /**
     * Super-directory to which all individual run data directories will be saved.
     */
    private File captureDir;

    /**
     * Directory to which this run's screenshots and data are being saved.
     */
    private File runFile;

    /**
     * Handles finding the QWOP window and basic saving operations.
     */
    private CaptureQWOPWindow windowCapturer;

    /**
     * Keeps track of when a game has failed previously and is waiting for a resetGame to occur.
     */
    private boolean resetPending = true;

    /**
     * Keeps track of the number of runs since this execution began. This is used for labelling subdirectories.
     */
    private int runCounter = 0;

    private static final Logger logger = LogManager.getLogger(VisionDataSaver.class);

    /**
     * Make a new screenshot and state information saver with a specified save directory.
     */
    public VisionDataSaver(File captureDir, int monitorIndex) {
        this.captureDir = captureDir;
<<<<<<< HEAD
        windowCapturer = new CaptureQWOPWindow(monitorIndex);
=======
        windowCapturer = new CaptureQWOPWindow();
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449

        if (!captureDir.exists() || !captureDir.isDirectory()) {
            assert captureDir.mkdirs();
        }
    }

    /**
     * Make a new screenshot and state information saver with the default save directory.
     */
    public VisionDataSaver(int monitorIndex) {
        this(new File("vision_capture"), monitorIndex);
    }

    @Override
    public void stateReceived(int timestep, StateQWOP state) {
        /*
         * Four important cases:
         * 1. Timestep is 0, i.e. beginning of new game. Need to clear caches and make new directories.
         * 2. General case: unfailed, "middle" timestep. Take a picture and record state info.
         * 3. Game has just failed. Save state information to file.
         * 4. Game is still failed, waiting for a resetGame to occur. Do nothing.
         */

        // TODO: There's some amount of ambiguity over whether the game state is sent before or after the frame is
        //  drawn. This is based on the data being sent first, but I may have changed that since. Check this.

        if (!resetPending && state.isFailed()) {
            endOfGameSave();
            resetPending = true;
        }

        // 1. Flash game resetGame has just occurred.
        if (timestep == 0) {
            resetForNextRun();
            resetPending = false;
        } else if (resetPending) { // 4. Waiting for the game to resetGame. Do nothing.
            return;
        }

        // Hold state info.
        statesThisRun.add(state);

        // 2. Normal timestep. Take screen capture.
        if (timestep > 0) {
            takeCapture(timestep); // Take the screenshot image and save to file stamped with the timestep number.
        }
    }

    /**
     * Takes a screen capture of the QWOP game window and saves to a file stamped with the timestep number, e.g.
     * 'ts112.png'.
     * @param timestep Timestep number at which this is being called.
     */
    private void takeCapture(int timestep) {
        try {
            File nextCapture = new File(runFile.getPath() + "/ts" + (timestep - 1) + ".png");
            windowCapturer.saveImageToPNG(nextCapture);
            capturesThisRun.add(nextCapture);
<<<<<<< HEAD
        } catch (IOException e) {
=======
        } catch (AWTException e) {
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
            e.printStackTrace();
        }
    }

    /**
     * At the beginning of a new game, clear the caches and create a new directory for the run which
     * is just starting.
     */
    private void resetForNextRun() {
        capturesThisRun.clear();
        statesThisRun.clear();
        runFile = new File(captureDir.getPath() + "/run" + runCounter++);
        assert runFile.mkdirs();
    }

    /**
     * At the end of a game, format all the state info into a tab-separated file with a row for each timestep.
     */
    private void endOfGameSave() {

        StringBuilder str = new StringBuilder(1000);

        while (!capturesThisRun.isEmpty()) {
            File f = capturesThisRun.poll();
            str.append(f.getName()).append('\t');
            IState s = Objects.requireNonNull(statesThisRun.poll());
            str.append(formatState(s)).append("\r\n");
        }
        assert !statesThisRun.isEmpty(); // TODO: will be true if the data comes before the screen updates.

        try {
            Files.write(new File(runFile.getPath() + "/poses.dat").toPath(),
                    str.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Format the state portion of one line of the data log file. This differs from other state representations in
     * the project in that all the position coordinates come before the velocities.
     * @param state StateQWOP to format.
     * @return One-line String representing the state info.
     */
    private String formatState(IState state) {
        if (state instanceof StateQWOP) {
            StateQWOP s = (StateQWOP) state;
            float bodyX = s.body.getX();
            return bodyX + "\t" + s.body.getY() + "\t" + s.body.getTh() + "\t"
                    + (s.head.getX() - bodyX) + "\t" + s.head.getY() + "\t" + s.head.getTh() + "\t"
                    + (s.rthigh.getX() - bodyX) + "\t" + s.rthigh.getY() + "\t" + s.rthigh.getTh() + "\t"
                    + (s.lthigh.getX() - bodyX) + "\t" + s.lthigh.getY() + "\t" + s.lthigh.getTh() + "\t"
                    + (s.rcalf.getX() - bodyX) + "\t" + s.rcalf.getY() + "\t" + s.rcalf.getTh() + "\t"
                    + (s.lcalf.getX() - bodyX) + "\t" + s.lcalf.getY() + "\t" + s.lcalf.getTh() + "\t"
                    + (s.rfoot.getX() - bodyX) + "\t" + s.rfoot.getY() + "\t" + s.rfoot.getTh() + "\t"
                    + (s.lfoot.getX() - bodyX) + "\t" + s.lfoot.getY() + "\t" + s.lfoot.getTh() + "\t"
                    + (s.ruarm.getX() - bodyX) + "\t" + s.ruarm.getY() + "\t" + s.ruarm.getTh() + "\t"
                    + (s.luarm.getX() - bodyX) + "\t" + s.luarm.getY() + "\t" + s.luarm.getTh() + "\t"
                    + (s.rlarm.getX() - bodyX) + "\t" + s.rlarm.getY() + "\t" + s.rlarm.getTh() + "\t"
                    + (s.llarm.getX() - bodyX) + "\t" + s.llarm.getY() + "\t" + s.llarm.getTh() + "\t"

                    + s.body.getDx() + "\t" + s.body.getDy() + "\t" + s.body.getDth() + "\t"
                    + s.head.getDx() + "\t" + s.head.getDy() + "\t" + s.head.getDth() + "\t"
                    + s.rthigh.getDx() + "\t" + s.rthigh.getDy() + "\t" + s.rthigh.getDth() + "\t"
                    + s.lthigh.getDx() + "\t" + s.lthigh.getDy() + "\t" + s.lthigh.getDth() + "\t"
                    + s.rcalf.getDx() + "\t" + s.rcalf.getDy() + "\t" + s.rcalf.getDth() + "\t"
                    + s.lcalf.getDx() + "\t" + s.lcalf.getDy() + "\t" + s.lcalf.getDth() + "\t"
                    + s.rfoot.getDx() + "\t" + s.rfoot.getDy() + "\t" + s.rfoot.getDth() + "\t"
                    + s.lfoot.getDx() + "\t" + s.lfoot.getDy() + "\t" + s.lfoot.getDth() + "\t"
                    + s.ruarm.getDx() + "\t" + s.ruarm.getDy() + "\t" + s.ruarm.getDth() + "\t"
                    + s.luarm.getDx() + "\t" + s.luarm.getDy() + "\t" + s.luarm.getDth() + "\t"
                    + s.rlarm.getDx() + "\t" + s.rlarm.getDy() + "\t" + s.rlarm.getDth() + "\t"
                    + s.llarm.getDx() + "\t" + s.llarm.getDy() + "\t" + s.llarm.getDth() + "\t";
        } else {
            logger.warn("StateQWOP type passed in is currently not supported. Returning a blank string.");
            return "";
        }
    }
}
