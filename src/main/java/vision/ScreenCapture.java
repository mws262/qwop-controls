package vision;

import org.jcodec.api.awt.AWTSequenceEncoder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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
     * Take a frame from the specified {@link JPanel}.
     *
     * @param container JPanel to take a snapshot from.
     * @throws IOException Unable to encode the captured image.
     */
    public void takeFrameFromContainer(Container container) throws IOException {
        BufferedImage bi = new BufferedImage(container.getWidth(), container.getHeight(), 1);
        Graphics2D g = bi.createGraphics();
        container.print(g);
        encoder.encodeImage(bi);
        g.dispose();
    }

    /**
     * Closes out the encoder, finalizing the video.
     *
     * @throws IOException Video capture is not successfully finalized to file.
     */
    public void finalize() throws IOException {
        encoder.finish();
    }
}