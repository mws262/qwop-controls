package flashqwop;

import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;
import game.IGameExternal;
import game.qwop.CommandQWOP;
import game.qwop.StateQWOP;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.types.UInt8;
import tflowtools.TensorflowGenericEvaluator;
import vision.CaptureQWOPWindow;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Version of QWOP Flash game that gets state estimation using a deep-learning vision model. The current pipeline.
 * 1. Screen capture happens in software by GStreamer (launched by this).
 * 2. GStreamer sends video feed to a fake "webcam" /dev/video77. V4L2Loopback handles this. This is done so that a
 * webcam can be substituted in more easily.
 * 3. A Java library, V4L4J, reads this video feed, and a callback occurs with each new frame.
 */
public class FlashGameVision implements IGameExternal<CommandQWOP, StateQWOP>, CaptureCallback {


    private final TensorflowGenericEvaluator tflowVision;

    private static final Logger logger = LogManager.getLogger(FlashGameVision.class);

    // Fields to put in and fetch from the tensorflow model.
    private final Map<String, Tensor<?>> netIn = new HashMap<>();
    private final List<String> netOut = new ArrayList<>();
    {
        netOut.add("prediction");
    }

    private final int[] resizeDim;

//    private final float[] positionMins = new float[35];
//    private final float[] positionMaxes = new float[35];

    private final AtomicInteger timestepCount = new AtomicInteger();

    private final String model = "./python/saves/modeldef.pb";
    private final String checkpoint = "./python/backup_medres_backgroundincl/model.ckpt";

    private String prevScreenHash;

    public FlashGameVision(String modelFile, String checkpointFile) throws IOException {
        // Video device should be arbitrarily added as video77. Check this. User needs to add it if it's not
        // available due to sudo.
        if (!(new File("/dev/video77").exists())) {
            throw new IOException("Could not find the video device to be used for capture. Run \"sudo modprobe " +
                    "v4l2loopback video_nr=77\"");
        }

        // Load the vision model and the trained checkpoint.
        tflowVision = new TensorflowGenericEvaluator(new File(modelFile));
        tflowVision.loadCheckpoint(checkpointFile);

        // Constants that are set inside the tensorflow model to make sure they match between training and evaluation.
        Session.Runner runner = tflowVision.getSession().runner();
        int[] cropDims = runner.fetch("crop_dim").run().get(0).copyTo(new int[4]); // (height offset to left top corner, width offset to left top corner, height, width)
        resizeDim = runner.fetch("img_resize_dims").run().get(0).copyTo(new int[2]); // (height, width)

        // Find the QWOP window.
        Rectangle qwopScreenLocation = null;
        try {
            qwopScreenLocation = CaptureQWOPWindow.locateQWOP();
        } catch (AWTException e) {
            e.printStackTrace();
        }

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
                        " use-damage=false ! videoconvert ! videoscale method=0 ! video/x-raw, format=I420, " +
                        "width=" + resizeDim[1] + ", " +
                        "height=" + resizeDim[0] + ", " +
                        "framerate=60/1 ! v4l2sink device=/dev/video77";
        logger.info(command);
        pb.command(command.split(" "));

        // Make sure that the g-streamer process gets killed when Java quits.
        try {
            Process p = pb.start();
            Runtime.getRuntime().addShutdownHook(new Thread(p::destroyForcibly));
        } catch (IOException e) {
            e.printStackTrace();
        }

//        // Load state rescalings. These were previously found from the states associated with the big image dataset and
//        // used during model training.
//        try (FileReader fr = new FileReader(new File("./python/mins.txt"));
//             BufferedReader br = new BufferedReader(fr)) {
//            String line;
//            int count = 0;
//            while ((line = br.readLine()) != null)
//                positionMins[count++] = Float.parseFloat(line);
//        }
//
//        try (FileReader fr = new FileReader(new File("./python/maxes.txt"));
//             BufferedReader br = new BufferedReader(fr)) {
//            String line;
//            int count = 0;
//            while ((line = br.readLine()) != null)
//                positionMaxes[count++] = Float.parseFloat(line);
//        }

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
        int w = Integer.getInteger("test.width", (int)qwopScreenLocation.getWidth());
        int h = Integer.getInteger("test.height", (int)qwopScreenLocation.getHeight());
        int std = Integer.getInteger("test.standard", 0);
        int ch = Integer.getInteger("test.channel", 0);

        try {
            VideoDevice vd = new VideoDevice(deviceProperty);
            FrameGrabber fg = vd.getBGRFrameGrabber(w, h, ch, std);
            fg.setCaptureCallback(this);
            fg.startCapture();
            Runtime.getRuntime().addShutdownHook(new Thread(fg::stopCapture));
        } catch (V4L4JException e) {
            e.printStackTrace();
        }

    }
    @Override
    public void command(CommandQWOP command) {

    }

    @Override
    public int getNumberOfChoices() {
        return CommandQWOP.NUM_COMMANDS;
    }

    @Override
    public StateQWOP getCurrentState() {
        return null;
    }

    @Override
    public boolean isFailed() {
        return false;
    }

    @Override
    public long getTimestepsThisGame() {
        return 0;
    }

    @Override
    public int getStateDimension() {
        return StateQWOP.STATE_SIZE;
    }

    @Override
    public void nextFrame(VideoFrame videoFrame) {
        // Grab the new video frame and toss it into a Tensor. TODO: before wasting time on this, make sure that the
        //  frame is actually new. If the framerate is slower than the game's, they could be identical
        ByteBuffer buff = videoFrame.getBuffer();

        // The incoming screen buffer gets hashed. This is used to determine if anything has changed since the last
        // frame, and is used to determine if a game timestep has occurred. todo is hashing more effective than just
        //  comparing bit by bit?
        String hash = DigestUtils.md5Hex(buff.array());
        if (!hash.equals(prevScreenHash)) {
            Tensor<UInt8> picTensor = Tensor.create(UInt8.class, new long[]{resizeDim[0], resizeDim[1], 3}, buff);

            netIn.put("input_eval", picTensor);
            List<Tensor<?>> output = tflowVision.evaluate(netIn, netOut);
            picTensor.close();

            Tensor<Float> result = output.get(0).expect(Float.class);
            long[] outputShape = result.shape();


            float[] reshapedResult = result.copyTo(new float[(int) outputShape[0]][(int) outputShape[1]])[0];
            output.forEach(Tensor::close);

            float[] stateVals = new float[StateQWOP.STATE_SIZE / 2];
            System.arraycopy(reshapedResult, 0, stateVals, 1, reshapedResult.length);

            StateQWOP st = StateQWOP.makeFromPositionArrayOnly(stateVals);
            prevScreenHash = hash;
            
            timestepCount.incrementAndGet();
        }

        // VERY VERY important. Will not get correct callback inputs without recycling the video frame.
        videoFrame.recycle();
    }

    @Override
    public void exceptionReceived(V4L4JException e) {
        e.printStackTrace();
    }
}
