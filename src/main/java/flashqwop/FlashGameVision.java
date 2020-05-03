package flashqwop;

import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;
import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import game.IGameExternal;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.QWOPConstants;
import game.qwop.StateQWOP;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.types.UInt8;
import tflowtools.TensorflowGenericEvaluator;
import ui.runner.PanelRunner_SimpleState;
import vision.CaptureQWOPWindow;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Version of QWOP Flash game that gets state estimation using a deep-learning vision model. The current pipeline.
 * 1. Screen capture happens in software by GStreamer (launched by this).
 * 2. GStreamer sends video feed to a fake "webcam" /dev/video77. V4L2Loopback handles this. This is done so that a
 * webcam can be substituted in more easily.
 * 3. A Java library, V4L4J, reads this video feed, and a callback occurs with each new frame.
 *
 * sudo modprobe v4l2loopback video_nr=77
 */
public class FlashGameVision implements IGameExternal<CommandQWOP, StateQWOP>, CaptureCallback {

    private FlashQWOPServer server;

    private StateQWOP currentState = GameQWOP.getInitialState();

    private final TensorflowGenericEvaluator tflowVision;

    private static final Logger logger = LogManager.getLogger(FlashGameVision.class);

    // Fields to put in and fetch from the tensorflow model.
    private final String netInputName = "input_eval";
    private final String netOutputName = "prediction_rescaled";
    private final Map<String, Tensor<?>> netIn = new HashMap<>();
    private final List<String> netOut = new ArrayList<>();
    {
        netOut.add(netOutputName);
        // netOut.add("png");

    }

    private final AtomicInteger timestepsProcessed = new AtomicInteger();

    private float[] prevPositionCoords = new float[StateQWOP.STATE_SIZE/2];

    /**
     * Hashes the bytes in each received frame. A difference in hash indicates that a new, non-repeat frame
     * has been received. The frames should be coming in at a substantially higher framerate than the game is
     * running out.
     */
    private String hashPrev;

    /**
     * Width of incoming capture in pixels.
     */
    private final int captureWidth;

    /**
     * Height of incoming capture in pixels.
     */
    private final int captureHeight;

    /**
     * Length of a section of a row of pixels to be used for matching between frames.
     */
    private final int chunkSize = 30;

    /**
     * The pixel index where the section of pixels for matching starts.
     */
    private final int chunkStart;

    /**
     * The index of the row of pixels to be used for matching between frames. Top of capture is row 0. Should
     * be near the bottom of the image at the textured ground.
     */
    private final int rowToMatch;

    /**
     * It's only worth looking for the matching chunk within a certain +/- pixel range of where it was before
     * . The runner can only ever move so fast. This window represents the maximum change in pixels between
     * frames (velocity-ish) in the forward or backward directions.
     */
    private final int searchWindow = 30;

    /**
     * Conversion factor found in MATLAB using least squares to match velocities directly sent to the game to
     * pixel-estimated velocities.
     */
    private final float pixelsToVelocity = 1.3476f;

    /**
     * Section of a row of pixels preserved between frames for matching to find the velocity of the runner.
     * These are converted to grayscale.
     */
    private final float[] groundChunk;
    /**
     * Entire pixel row of the ground from the current frame. Used for matching with a chunk of pixels
     * preserved from the previous frame. These values are converted to grayscale.
     */
    private final float[] grayScaleCurrent;

    public FlashGameVision(String modelFile, String checkpointFile) throws IOException {
        // 1. load kernel module: sudo modprobe v4l2loopback video_nr=77 (77 is not a magic number. Just good to pick
        // something not already reserved.)
        // 2. fake video source should be created by now: /dev/video77
        // 3. If not, or if needs to be reloaded. disable/reenable module. sudo modprobe -r v4l2loopback

        // Video device should be arbitrarily added as video77. Check this. User needs to add it if it's not
        // available due to sudo.
        if (!(new File("/dev/video77").exists())) {
            throw new IOException("Could not find the video device to be used for capture. Run \"sudo modprobe " +
                    "v4l2loopback video_nr=77\"");
        }

        // Load the vision model and the trained checkpoint.
        tflowVision = new TensorflowGenericEvaluator(new File(modelFile));
        tflowVision.loadCheckpoint(checkpointFile);

        if (Streams.stream(tflowVision.getGraph().operations()).noneMatch(op -> op.name().contains(netOutputName)))
            throw new IllegalArgumentException("Loaded TensorFlow model does not contain the required output: " + netOutputName);

        if (Streams.stream(tflowVision.getGraph().operations()).noneMatch(op -> op.name().contains(netInputName)))
            throw new IllegalArgumentException("Loaded TensorFlow model does not contain the required input: " + netInputName);


        // Constants that are set inside the tensorflow model to make sure they match between training and evaluation.
        Session.Runner runner = tflowVision.getSession().runner();
        List<Tensor<?>> netConsts = runner.fetch("crop_dim").fetch("img_resize_dims").run();
        int[] cropDims = netConsts.get(0).copyTo(new int[4]); // (height offset to left top corner, width offset to left top corner, height, width)
        int[] resizeDim = netConsts.get(1).copyTo(new int[2]); // (height, width) FIXME check this order
        captureHeight = resizeDim[0];
        captureWidth = resizeDim[1];
        rowToMatch = captureHeight - 3; // Some pixel near the bottom where the ground texture should be.

        if (captureWidth <= 0 || captureHeight <= 0)
            throw new IllegalArgumentException("Zero or negative capture dimensions imported from the TensorFlow model. " +
                    "Height: " + captureHeight + " , Width: " + captureWidth + ".");

        if (captureWidth <= chunkSize + 2 * searchWindow)
            throw new IllegalArgumentException("Screen capture width specified by the TensorFlow model is too small " +
                    "for the parameters specified here for torso X-velocity estimation. Change one or the other.");

        if (captureHeight < rowToMatch)
            throw new IllegalArgumentException("Screen capture height specified by the TensorFlow model is less than " +
                    "row index to be used for the torso X-velocity estimate. Change the row index to match a section " +
                    "of ground, and ensure that the capture is correct.");

        // Assign stuff for velocity estimation based on ground movement.
        chunkStart = (captureWidth - chunkSize) / 2;
        groundChunk = new float[chunkSize];
        grayScaleCurrent = new float[captureWidth];

        // Find the QWOP window.
        Rectangle qwopScreenLocation = null;
        try {
            qwopScreenLocation = CaptureQWOPWindow.locateQWOP();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        Objects.requireNonNull(qwopScreenLocation);

        // TODO fix to handle rescalings of the game outside the normal 640x400 dimensions. For now, we need this for
        //  the cropping to be consistent with what the tflow model expects.
        int expectedWidth = 640;
        int expectedHeight = 400;
        if (qwopScreenLocation.getWidth() != expectedWidth || qwopScreenLocation.getHeight() != expectedHeight) {
            logger.warn("Detected game dimensions don't match expected. Found: " + qwopScreenLocation.getWidth() + "x" + qwopScreenLocation.getHeight());
        }

        // Launch the video stream and send the output to the video device faked by v4l2loopback.
        ProcessBuilder pb = new ProcessBuilder();
        String command =
                "gst-launch-1.0 ximagesrc startx=" + ((int)qwopScreenLocation.getX() + cropDims[1]) +
                        " starty=" + ((int)qwopScreenLocation.getY() + cropDims[0]) +
                        " endx=" + ((int)qwopScreenLocation.getX() + cropDims[1] + cropDims[3]) +
                        " endy=" + ((int)qwopScreenLocation.getY() + cropDims[0] + cropDims[2]) +
                        " use-damage=false ! videoconvert ! videoscale method=0 ! video/x-raw, format=RGB, " +
                        "width=" + captureWidth + ", " +
                        "height=" + captureHeight + ", " +
                        "framerate=45/1 ! v4l2sink device=/dev/video77";
        logger.info(command);
        pb.command(command.split(" "));

        // Make sure that the g-streamer process gets killed when Java quits.
        try {
            Process p = pb.start();
            Runtime.getRuntime().addShutdownHook(new Thread(p::destroyForcibly));
        } catch (IOException e) {
            e.printStackTrace();
        }

        server = new FlashQWOPServer(2900);
        // TODO: This is necessary to make sure that the g-streamer command line launch is done before we try to use
        //  it. This is really hacky and should be fixed somehow.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Set up V4L4J, which can read from the g-streamer feed on video77.
        System.setProperty("test.device", "/dev/video77");
        String deviceProperty = System.getProperty("test.device");
        int w = Integer.getInteger("test.width", captureWidth);
        int h = Integer.getInteger("test.height", captureHeight);
        int std = Integer.getInteger("test.standard", 0);
        int ch = Integer.getInteger("test.channel", 0);

        try {
            VideoDevice vd = new VideoDevice(deviceProperty);
            FrameGrabber fg = vd.getRGBFrameGrabber(w, h, std, ch);
            fg.setCaptureCallback(this);
            fg.startCapture();
            Runtime.getRuntime().addShutdownHook(new Thread(fg::stopCapture));
        } catch (V4L4JException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void command(CommandQWOP command) {
        server.command(command);
    }

    @Override
    public int getNumberOfChoices() {
        return CommandQWOP.NUM_COMMANDS;
    }

    @Override
    public StateQWOP getCurrentState() {
        return currentState;
    }

    @Override
    public boolean isFailed() {
        return server.isFailed();
    }

    @Override
    public long getTimestepsThisGame() {
        return timestepsProcessed.get();
    }

    @Override
    public int getStateDimension() {
        return StateQWOP.STATE_SIZE;
    }

    @Override
    public void nextFrame(VideoFrame videoFrame) {
        // About the structure of this byte array.
        // 1. Signed values from -128 to 127 (normal 8 bit signed integer). Each pixel gets 3 bytes.
        // 2. Order is [r1, g1, b1, r2, g2, b2,...] rather than [r1, r2, ..., g1, g2, ..., b1, b2,...]
        // 3. Pixels are ordered going along the rows.
        byte[] frameBytes = videoFrame.getBytes();

        // See if frame is a new one byte hashing the incoming image and comparing to previous.
        // This takes 1ms, compared to 8ms to evaluate with vision model and compare output states for
        // differences.
        String hash = DigestUtils.md5Hex(frameBytes);

        if (!hash.equals(hashPrev)) {
            ByteBuffer buff = videoFrame.getBuffer();
            float[] positionCoords = estimatePositionCoordinates(buff);
//
            float xVelocity = estimateBodyXVelocity(frameBytes);
            float[] velocityCoords = estimateBodyVelocities(positionCoords);

            // Add the torso x velocity back in to all the body parts.
            for (int i = 0; i < velocityCoords.length; i += 3) {
                velocityCoords[i] += xVelocity;
            }

            currentState = StateQWOP.makeFromPositionVelocityArrays(positionCoords, velocityCoords, false);
            hashPrev = hash;
            timestepsProcessed.incrementAndGet();

            File outputfile = new File("image.png");
            try {
                ImageIO.write(videoFrame.getBufferedImage(), "png", outputfile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        videoFrame.recycle();
    }

    @Override
    public void exceptionReceived(V4L4JException e) {
        e.printStackTrace();
    }

    /**
     * Estimate the positions/angles of all the body parts of the runner. This uses a convolutional neural network
     * (CNN) trained in TensorFlow.
     * @param buff Buffered capture of the screen section from V4L4J.
     * @return 36-element array of positions and angles associated with the runner body parts. Note that element 0,
     * the torso x-coordinate, will always be 0. The torso is centered on the screen anyway, and control algorithms
     * should be agnostic to this.
     */
    private float[] estimatePositionCoordinates(ByteBuffer buff) {
        Preconditions.checkNotNull(buff);

        Tensor<UInt8> picTensor = Tensor.create(UInt8.class, new long[]{captureWidth, captureHeight, 3},
                buff);
        netIn.put(netInputName, picTensor);

        List<Tensor<?>> output = tflowVision.evaluate(netIn, netOut);
        picTensor.close();

        Tensor<Float> result = output.get(0).expect(Float.class);
        long[] outputShape = result.shape();
        float[] reshapedResult = result.copyTo(new float[(int) outputShape[0]][(int) outputShape[1]])[0];

//        try (FileOutputStream out = new FileOutputStream("./testtest.png")) {
//            out.write(output.get(1).bytesValue());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        output.forEach(Tensor::close);

        float[] stateVals = new float[StateQWOP.STATE_SIZE / 2];
        System.arraycopy(reshapedResult, 0, stateVals, 1, reshapedResult.length);
        return stateVals;
    }

    /**
     * Estimate the torso x-direction velocity. This can't be estimated by the neural net since the body stays
     * centered on the screen anyway. This is estimated by looking at the two most recent frames and seeing how many
     * pixels the ground texture has been offset between them.
     * @param frameBytes Byte array representing the screen capture from V4L4J. Every sequential three bytes
     *                   represents one pixel, its R, G, B values.
     * @return Estimated horizontal torso velocity. Positive is rightward.
     */
    private float estimateBodyXVelocity(byte[] frameBytes) {
        Preconditions.checkArgument(frameBytes.length == 3 * captureHeight * captureWidth);

        // Grab a single row of the image. Convert to grayscale.
        for (int i = captureWidth * rowToMatch * 3, j = 0; i < captureWidth * (rowToMatch + 1) * 3; i += 3, j++) {
            grayScaleCurrent[j] =
                    0.3f * ((float) frameBytes[i] - (float) Byte.MIN_VALUE) +
                            0.59f * ((float) frameBytes[i + 1] - (float) Byte.MIN_VALUE) +
                            0.11f * ((float) frameBytes[i + 2] - (float) Byte.MIN_VALUE); // RGB to grayscale conversion.
        }

        // We can't do a pixel difference on the first timestep. Just record that pixel chunk and return 0 velocity
        // if this is the case.
        if (timestepsProcessed.get() > 0) {
            // Calculate cross-correlation-ish for various offsets of a chunk of pixels from the previous
            // timestep.
            int bestIdxMatch = chunkStart;
            double bestCorrelation = -Double.MAX_VALUE;
            for (int i = chunkStart - searchWindow; i <= chunkStart + searchWindow; i++) {
                double sectionMagnitude = 0;
                double crossCorrelation = 0;

                for (int j = i, k = 0; j < i + chunkSize; j++, k++) {
                    sectionMagnitude += grayScaleCurrent[j] * grayScaleCurrent[j];
                    crossCorrelation += grayScaleCurrent[j] * groundChunk[k];
                }
                sectionMagnitude = Math.sqrt(sectionMagnitude);
                crossCorrelation /= sectionMagnitude;

                if (crossCorrelation > bestCorrelation) {
                    bestCorrelation = crossCorrelation;
                    bestIdxMatch = i;
                }
            }
            // Copy a chunk of a row to be used for matching between frames.
            System.arraycopy(grayScaleCurrent, chunkStart, groundChunk, 0, chunkSize);
            return -pixelsToVelocity * (bestIdxMatch - chunkStart);
        } else {
            // Copy a chunk of a row to be used for matching between frames.
            System.arraycopy(grayScaleCurrent, chunkStart, groundChunk, 0, chunkSize);
            return 0;
        }
    }

    /**
     * Estimate the body part velocities by finite differencing with the previous timestep.
     * TODO: maybe do something cubic or higher order.
     * @param positionState 36-element array of body-part position/angles.
     * @return 36-element body-part velocity estimates. Elements positions correspond to inputs.
     */
    private float[] estimateBodyVelocities(float[] positionState) {
        Preconditions.checkArgument(positionState.length == StateQWOP.STATE_SIZE / 2);

        // First timestep, there's no finite differencing to do. Just return 0 for all velocities.
        if (timestepsProcessed.get() > 0) {
            float[] velocities = new float[StateQWOP.STATE_SIZE/2];

            // Horizontal velocity for the body can't be estimated from the
            for (int i = 1; i < positionState.length; i++) {
                velocities[i] = (positionState[i] - prevPositionCoords[i]) / QWOPConstants.timestep;
            }

            return velocities;
        } else {
            System.arraycopy(positionState, 0, prevPositionCoords, 0, StateQWOP.STATE_SIZE / 2);
            return new float[StateQWOP.STATE_SIZE / 2];
        }
    }


    public static void main(String[] args) throws IOException {
        final String model = "./python/saves/modeldef.pb";
        final String checkpoint = "./python/saves/model.ckpt";

        FlashGameVision game = new FlashGameVision(model, checkpoint);

        JFrame frame = new JFrame();
        PanelRunner_SimpleState<StateQWOP> runnerPanel = new PanelRunner_SimpleState<>("Runner");
        runnerPanel.activateTab();
        frame.getContentPane().add(runnerPanel);
        frame.setPreferredSize(new Dimension(1000, 1000));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.pack();
        frame.setVisible(true);

        Timer timer = new Timer(30, e -> runnerPanel.updateState(game.getCurrentState()));
        timer.start();


//        Map<String, Tensor<?>> netIn = new HashMap<>();
//        List<String> netOut = new ArrayList<>();
//        netOut.add("png"); // prediction_rescaled");
//
//        float[] stateVals = new float[36];
//        for (int i = 0; i < 200; i++) {
//            netIn.put("img_filename", Tensors.create("./vision_capture/run1/ts" + i + ".png"));
//
//            List<Tensor<?>> output = game.tflowVision.evaluate(netIn, netOut);
//        try (FileOutputStream out = new FileOutputStream("./testtest1.png")) {
//            out.write(output.get(0).bytesValue());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        output.get(0).close();

//            Tensor<Float> result = output.get(0).expect(Float.class);
//            long[] outputShape = result.shape();
//            float[] reshapedResult = result.copyTo(new float[(int) outputShape[0]][(int) outputShape[1]])[0];
//            System.arraycopy(reshapedResult, 0, stateVals, 1, reshapedResult.length);
//            output.get(0).close();
//            runnerPanel.updateState(StateQWOP.makeFromPositionArrayOnly(stateVals));
//        }

    }
}
