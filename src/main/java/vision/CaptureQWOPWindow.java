package vision;

import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;
import game.qwop.StateQWOP;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tensorflow.Tensor;
import org.tensorflow.types.UInt8;
import tflowtools.TensorflowGenericEvaluator;
import tree.Utility;
import ui.runner.PanelRunner_SimpleState;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.*;

public class CaptureQWOPWindow extends JPanel implements Runnable {

    /*
    Top row color RGB: 82, 114, 137
    Bottom left: 62, 43, 22
    Bottom right: 32, 20, 1
     */

    /**
     * The blue-ish color on the top row of the game in compact form.
     */
    /*
    Note 12/22/19: These colors seem to have changed since the last time I used this. I have updated below. Previously
    the main color had been (82, 114, 137). I think that was on a different computer though. It is possible that
    rendering differences account for this. Perhaps a range of values should be used in the future if this needs to
    be more robust.
    Note 1/4/20: They do vary. Just keep adding to these arrays as needed.
     */
    private static final int[] topRowMainColor = {new Color(83, 114, 136).getRGB(), new Color(82, 114, 137).getRGB()};
    private static final int[] topRowEndColor = {topRowMainColor[0], topRowMainColor[1],
            new Color(77, 89, 106).getRGB(), new Color(77, 89, 107).getRGB()};

    /**
     * Game width at default scaling.
     */
    private static final int defaultGameWidth = 640;

    /**
     * Game height at default scaling.
     */
    private static final int defaultGameHeight = 400;

    /**
     * Detected game scaling relative to defaults.
     */
    private double gameStageScaling = 1.;

    /**
     * For capturing screen regions.
     */
    private Robot robot;

    /**
     * Frame for making sure it's capturing the correct region.
     */
    public JFrame frame;

    private Rectangle screenCapRegion;

    private static boolean debugFrame = false;

    private static final Logger logger = LogManager.getLogger(CaptureQWOPWindow.class);

    public CaptureQWOPWindow() {
        // Make new Robot for capturing screen pixels.
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        // Find the dimensions and coordinates to just capture the QWOP window.
        try {
            screenCapRegion = locateQWOP();
        } catch (AWTException e) {
            screenCapRegion = new Rectangle();
            logger.warn("QWOP window not found when constructing this capturer. I will attempt to find it next time " +
                    "capture is called.");
        }

        if (debugFrame) {
            frame = new JFrame();
            frame.setPreferredSize(new Dimension((int) (gameStageScaling * defaultGameWidth),
                    (int) (gameStageScaling * defaultGameHeight) + 35)); // Have to offset the height of the window top bar.
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setVisible(true);
            frame.add(this);
            frame.pack();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        try {
            g.drawImage(getGameCapture(), 0, 0, (int) (defaultGameWidth * gameStageScaling),
                    (int) (defaultGameHeight * gameStageScaling), null);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    public static Rectangle locateQWOP() throws AWTException {
        // See what monitors are available.
        final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        // Report all the monitors seen.
        logger.info("Found " + devices.length + " monitors.");

        // Go through one-by-one to find QWOP. Stop when a QWOP candidate is found.
        for (GraphicsDevice gd : devices) {
            Rectangle monitorBounds = gd.getDefaultConfiguration().getBounds();
            logger.info("Trying monitor " + gd.getIDstring() + ": " + monitorBounds.width + " x " + monitorBounds.height);

            Rectangle qwopBounds = findQWOPInBounds(monitorBounds);
            if (qwopBounds != null) { // If game is found, return the bound locations.
                logger.info("Found QWOP.");
                return qwopBounds;
            }
        }
        logger.warn("QWOP not found on any monitor.");
        throw new AWTException("QWOP could not be found on any available computer monitor. Make sure " +
                "no minimized windows could be blocking where the actual game is.");
    }

    @Nullable
    private static Rectangle findQWOPInBounds(Rectangle monitorBounds) throws AWTException {
        // Capture the whole monitor image and get the data buffer as pixels.
        BufferedImage img = new Robot().createScreenCapture(monitorBounds);

        // This array is basically all the rows (x-dimension) concatenated together.
        int[] pixels = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();

        boolean previouslyOnTopRow = false;
        int topRowStartPixel = 0;
        int topRowEndPixel = 0;

        for (int i = 0; i < pixels.length - 1; i++) {
            final int pix = i;
            if (!previouslyOnTopRow && Arrays.stream(topRowMainColor).anyMatch(c->(c==pixels[pix]))) {
                previouslyOnTopRow = true;
                topRowStartPixel = i;
            } else if (previouslyOnTopRow && (Arrays.stream(topRowEndColor).anyMatch(c->(c==pixels[pix])) && Arrays.stream(topRowEndColor).noneMatch(c->(c==pixels[pix + 1])))) {
                topRowEndPixel = i;
                break;
            }
        }

        int gameUpperCornerX = topRowStartPixel % monitorBounds.width;
        int gameUpperCornerY = topRowStartPixel / monitorBounds.width;
        double gameStageScaling = Math.max((topRowEndPixel - topRowStartPixel + 1) / (double) defaultGameWidth, 0.1);
        // +1 since the bounds are included. i.e. "fencepost problem"

        if ((topRowEndPixel - topRowStartPixel) <= 0) {
            logger.info("QWOP not found on this monitor.");
            return null;
        } else {
            logger.info("Found game width of: " + (topRowEndPixel - topRowStartPixel));
            logger.info("Top-right game coordinate: (" + gameUpperCornerX + ", " + gameUpperCornerY + ")");
            logger.info("Game scaling is: " + gameStageScaling);
        }

        // Have to add on start coordinates of the selected monitor. Can be weird in multi-monitor setups.
        return new Rectangle(gameUpperCornerX + (int)monitorBounds.getX(),
                gameUpperCornerY +  (int)monitorBounds.getY(), (int)(gameStageScaling * defaultGameWidth),
                (int)(gameStageScaling * defaultGameHeight));
    }

    private BufferedImage getGameCapture() throws AWTException {
        if (screenCapRegion.getX() > 0 && screenCapRegion.getY() > 0) {
            BufferedImage img = robot.createScreenCapture(screenCapRegion);
            boolean topMatch = Arrays.stream(topRowMainColor).anyMatch(c->(c==img.getRGB(0, 0)));
            if (!topMatch) {
                logger.warn("Screen cap region invalid. Locating the QWOP window again. RGB found at corner location " +
                        "actually was: " + img.getRGB(0, 0));
                screenCapRegion = locateQWOP();
                getGameCapture();
            }
            return img;
        } else {
            return null;
        }
    }

    public void saveImageToPNG(File file) throws AWTException {
        logger.info("cap");
        Utility.tic();
        BufferedImage img = getGameCapture();
        Utility.toc();

        if (img != null) {
            logger.info("save");
            Utility.tic();
            try {
                ImageIO.write(img, "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utility.toc();
        } else {
            logger.warn("Screenshot image acquired was not valid and was not saved.");
        }
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while(true) {
            repaint();
        }
    }

    public static void main(String[] args) throws V4L4JException, InterruptedException, IOException, AWTException {

        // OK.
        // 1. load kernel module: sudo modprobe v4l2loopback video_nr=77 (77 is not a magic number. Just good to pick
        // something not already reserved.)
        // 2. fake video source should be created by now: /dev/video77
        // 3. If not, or if needs to be reloaded. disable/reenable module. sudo modprobe -r v4l2loopback

//        CaptureQWOPWindow.debugFrame = true;
//        try {
//            locator.saveImageToPNG(new File("test.png"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        Thread drawerThread = new Thread(locator);
//        drawerThread.start();

        // Neural net for state estimation.
        TensorflowGenericEvaluator tflow = new TensorflowGenericEvaluator(new File("./python" +
                "/saves/modeldef.pb"));
        tflow.loadCheckpoint("./python/saves/model.ckpt");
        tflow.printTensorflowGraphOperations();

        Map<String, Tensor<?>> netIn = new HashMap<>();
        List<String> netOut = new ArrayList<>();
        netOut.add("prediction_rescaled");
//        netOut.add("png");

        Rectangle location = locateQWOP();

        // IMPORTANT: Best I can tell gstreamer with I420 format can only do widths divisible by 8. Height doesn't
        // matter like this. If you specify something that isn't a multiple of 8, it rounds down without saying
        // anything and your dimensions are fucked.
        int[] insets = {100, 60, 336, 336};
        int[] resizedDim = {336, 336};

        // Launch the video stream and send the output to the video device faked by v4l2loopback.
        ProcessBuilder pb = new ProcessBuilder();
        String command =
                "gst-launch-1.0 ximagesrc startx=" + ((int)location.getX() + insets[0]) +
                        " starty=" + ((int)location.getY() + insets[1]) +
                        " endx=" + ((int)location.getX() + insets[0] + insets[2]) +
                        " endy=" + ((int)location.getY() + insets[1] + insets[3]) +
                        " use-damage=false ! videoconvert ! videoscale method=0 ! video/x-raw" +
                        ",format=I420," +
                        "width=" + resizedDim[0] + "," + // (int)(location.getWidth()/4f) + ", " +
                        "height=" + resizedDim[1] + "," + // (int)(location.getHeight()/4f) + ", " +
                        "framerate=60/1" +
                        " ! v4l2sink device=/dev/video77";
        logger.info(command);
        pb.command(command.split(" "));
        //pb.inheritIO();

        try {
            Process p = pb.start();
            Runtime.getRuntime().addShutdownHook(new Thread(p::destroyForcibly));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread.sleep(2000); // TODO maybe do something better than just waiting 2 seconds for gstreamer to fire up.

        JFrame frame = new JFrame();
       // JLabel label = new JLabel();

        PanelRunner_SimpleState<StateQWOP> runnerPanel = new PanelRunner_SimpleState<>("Runner");
        runnerPanel.activateTab();
        frame.getContentPane().add(runnerPanel);

        //label.setPreferredSize(new Dimension(800, 800));
        //label.setVisible(false);
        //frame.getContentPane().add(label);
        frame.setPreferredSize(new Dimension(1000, 1000));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.pack();
        frame.setVisible(true);

        System.setProperty("test.device", "/dev/video77");
        String deviceProperty = System.getProperty("test.device");
        VideoDevice vd = new VideoDevice(deviceProperty);

//        System.setProperty("test.width", "340");
//        System.setProperty("test.height", "340");
        int w = resizedDim[0];
        int h = resizedDim[1];
        int std = Integer.getInteger("test.standard", 0);
        int ch = Integer.getInteger("test.channel", 0);
        FrameGrabber fg = vd.getRGBFrameGrabber(w, h, ch, std);

                // vd.getDeviceInfo().getFormatList().getNativeFormats().get(0));
        fg.setCaptureCallback(new CaptureCallback() {
            String hashPrev;
            @Override
            public void nextFrame(VideoFrame videoFrame) {

                // See if frame is a new one byte hashing the incoming image and comparing to previous.
                // This takes 1ms, compared to 8ms to evaluate with vision model and compare output states for
                // differences.
                byte[] frameBytes = videoFrame.getBytes();
                String hash = DigestUtils.md5Hex(frameBytes);

                if (!hash.equals(hashPrev)) {
                    ByteBuffer buff = videoFrame.getBuffer();

//                Tensor<UInt8> picTensor = Tensor.create(frameBytes, UInt8.class);
                    Tensor<UInt8> picTensor = Tensor.create(UInt8.class, new long[]{resizedDim[0], resizedDim[1], 3},
                            buff);
                    netIn.put("input_eval", picTensor);

                    List<Tensor<?>> output = tflow.evaluate(netIn, netOut);
                    picTensor.close();

//                    byte[] b = output.get(1).bytesValue();
//                    try (FileOutputStream fw = new FileOutputStream(new File("./data.png"))) {
//                        fw.write(b);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                    Tensor<Float> result = output.get(0).expect(Float.class);
                    long[] outputShape = result.shape();

                    float[] reshapedResult = result.copyTo(new float[(int) outputShape[0]][(int) outputShape[1]])[0];
                    output.forEach(Tensor::close);

                    float[] stateVals = new float[36];
                    stateVals[0] = 0;
                    for (int i = 0; i < reshapedResult.length; i++) {
                        stateVals[i + 1] = reshapedResult[i]; // reshapedResult[i] * (maxes[i] - mins[i]) + mins[i];
                    }

                    StateQWOP st = StateQWOP.makeFromPositionArrayOnly(stateVals);
                    runnerPanel.updateState(st);
                    hashPrev = hash;
                }

                //label.setIcon(new ImageIcon(bufferedImage));
                videoFrame.recycle();
            }

            @Override
            public void exceptionReceived(V4L4JException e) {
                e.printStackTrace();
            }
        });

        fg.startCapture();
        Runtime.getRuntime().addShutdownHook(new Thread(fg::stopCapture));
        Thread.sleep(50000);
        fg.stopCapture();

    }
}
