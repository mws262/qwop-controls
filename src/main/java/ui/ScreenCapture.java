package ui;

import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.JPanel;

import org.jcodec.api.awt.AWTSequenceEncoder;

/**
 * Video capture and saving. Currently encodes at 25fps.
 *
 * @author matt
 */
public class ScreenCapture {

    private AWTSequenceEncoder encoder;
    private Robot robot;

    /**
     * Make a new screen capturer that will save to the specified file.
     *
     * @param saveFile File that the video will be saved to.
     */
    public ScreenCapture(File saveFile) {
        try {
            this.encoder = AWTSequenceEncoder.create25Fps(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    /**
     * Take a video frame and encode it with the rest of the video based on a section of the screen specified.
     *
     * @param rectangle Rectangle on screen to take a frame of.
     * @throws IOException Unable to encode the image.
     */
    public void takeFrame(Rectangle rectangle) throws IOException {
        BufferedImage image = robot.createScreenCapture(rectangle);
        encoder.encodeImage(image);
    }

    /**
     * Take a frame from the specified {@link JPanel}. TODO: make sure that the dimensions generalize properly.
     *
     * @param panel JPanel to take a snapshot from.
     * @throws IOException Unable to encode the captured image.
     */
    public void takeFrameFromJPanel(JPanel panel) throws IOException {
        BufferedImage bi = new BufferedImage(1440, 720, 1); // This is evil hacks
        Graphics2D g = bi.createGraphics();
        panel.print(g);
        encoder.encodeImage(bi);
        g.dispose();
    }

    /**
     * Closes out the encoder, finalizing the video.
     *
     * @throws IOException
     */
    public void finalize() throws IOException {
        encoder.finish();
    }
}