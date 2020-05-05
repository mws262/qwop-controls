package vision;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
<<<<<<< HEAD
=======
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tree.Utility;
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
<<<<<<< HEAD
=======
import java.util.Arrays;
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449

public class CaptureQWOPWindow extends JPanel implements Runnable {

    /*
    Top row color RGB: 82, 114, 137
    Bottom left: 62, 43, 22
    Bottom right: 32, 20, 1
     */

    /**
<<<<<<< HEAD
     * The blue-ish color on the top row of the game in RGB.
     */
    private static final int[] topRowColor = new int[]{82, 114, 137};
    /**
     * The blue-ish color on the top row of the game in compact form.
     */
    private static final int topRowColorSingleInt = -11373943;
=======
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
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449

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
<<<<<<< HEAD
     * The monitor index to look for the game in. 0 should be the main monitor. TODO make it auto-detect this.
     */
    private int monitorIdx;

    /** x,y,width,height of selected monitor. **/
    private Rectangle monitorBounds;

    /**
=======
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
     * For capturing screen regions.
     */
    private Robot robot;

    /**
     * Frame for making sure it's capturing the correct region.
     */
    public JFrame frame;

<<<<<<< HEAD
    /**
     * Detected upper corner x of the game.
     */
    private int gameUpperCornerX;

    /**
     * Detected upper corner y of the game (note from top left corner).
     */
    private int gameUpperCornerY;

=======
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
    private Rectangle screenCapRegion;

    private static boolean debugFrame = false;

    private static final Logger logger = LogManager.getLogger(CaptureQWOPWindow.class);

<<<<<<< HEAD
    public CaptureQWOPWindow(int monitorIdx) {
        this.monitorIdx = monitorIdx;

        // Figure out the size of the specified monitor.
        final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        logger.info("Found " + devices.length + " monitors.");

        GraphicsDevice gd = devices[monitorIdx];
        monitorBounds = gd.getDefaultConfiguration().getBounds();

=======
    public CaptureQWOPWindow() {
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
        // Make new Robot for capturing screen pixels.
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        // Find the dimensions and coordinates to just capture the QWOP window.
<<<<<<< HEAD
        locateQWOP();
=======
        try {
            screenCapRegion = locateQWOP();
        } catch (AWTException e) {
            screenCapRegion = new Rectangle();
            logger.warn("QWOP window not found when constructing this capturer. I will attempt to find it next time " +
                    "capture is called.");
        }
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449

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
<<<<<<< HEAD
        g.drawImage(getGameCapture(), 0, 0, (int) (defaultGameWidth * gameStageScaling),
                (int) (defaultGameHeight * gameStageScaling), null);
    }

    private void locateQWOP() {
        // Capture the whole monitor image and get the data buffer as pixels.
        BufferedImage img = robot.createScreenCapture(monitorBounds);
=======
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

>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
        // This array is basically all the rows (x-dimension) concatenated together.
        int[] pixels = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();

        boolean previouslyOnTopRow = false;
        int topRowStartPixel = 0;
        int topRowEndPixel = 0;

<<<<<<< HEAD
        // The data buffer collapses RGBA into a single 32bit integer. This Stack Overflow magic converts back.
        for (int i = 0; i < pixels.length; i++) {

            if (!previouslyOnTopRow && pixels[i] == topRowColorSingleInt) {
                previouslyOnTopRow = true;
                topRowStartPixel = i;
            } else if (previouslyOnTopRow && pixels[i] != topRowColorSingleInt) {
                previouslyOnTopRow = false;
=======
        for (int i = 0; i < pixels.length - 1; i++) {
            final int pix = i;
            if (!previouslyOnTopRow && Arrays.stream(topRowMainColor).anyMatch(c->(c==pixels[pix]))) {
                previouslyOnTopRow = true;
                topRowStartPixel = i;
            } else if (previouslyOnTopRow && (Arrays.stream(topRowEndColor).anyMatch(c->(c==pixels[pix])) && Arrays.stream(topRowEndColor).noneMatch(c->(c==pixels[pix + 1])))) {
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
                topRowEndPixel = i;
                break;
            }
        }

<<<<<<< HEAD
        gameUpperCornerX = topRowStartPixel % monitorBounds.width;
        gameUpperCornerY = topRowStartPixel / monitorBounds.width;
        gameStageScaling = Math.max((topRowEndPixel - topRowStartPixel) / (double) defaultGameWidth, 0.1);

        if ((topRowEndPixel - topRowStartPixel) == 0) {
            logger.warn("Game window not found. Check monitor number or move nearby browser instances.");
=======
        int gameUpperCornerX = topRowStartPixel % monitorBounds.width;
        int gameUpperCornerY = topRowStartPixel / monitorBounds.width;
        double gameStageScaling = Math.max((topRowEndPixel - topRowStartPixel + 1) / (double) defaultGameWidth, 0.1);
        // +1 since the bounds are included. i.e. "fencepost problem"

        if ((topRowEndPixel - topRowStartPixel) <= 0) {
            logger.info("QWOP not found on this monitor.");
            return null;
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
        } else {
            logger.info("Found game width of: " + (topRowEndPixel - topRowStartPixel));
            logger.info("Top-right game coordinate: (" + gameUpperCornerX + ", " + gameUpperCornerY + ")");
            logger.info("Game scaling is: " + gameStageScaling);
        }

<<<<<<< HEAD
        screenCapRegion = new Rectangle(gameUpperCornerX, gameUpperCornerY,
                (int) (gameStageScaling * defaultGameWidth), (int) (gameStageScaling * defaultGameHeight));
    }

    private int[] getRGB(int color) {
        int[] rgb = new int[3];

        // Assuming TYPE_INT_RGB, TYPE_INT_ARGB or TYPE_INT_ARGB_PRE
        // For TYPE_INT_BGR, you need to reverse the colors.
        rgb[0] = ((color >> 16) & 0xff); // red;
        rgb[1] = ((color >>  8) & 0xff); // green;
        rgb[2] = (color & 0xff); // blue;

        return rgb;
    }

    private BufferedImage getGameCapture() {
        if (screenCapRegion.getX() > 0 && screenCapRegion.getY() > 0) {
            BufferedImage img = robot.createScreenCapture(screenCapRegion);
            if (img.getRGB(0, 0) != topRowColorSingleInt) {
                locateQWOP();
=======
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
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
                getGameCapture();
            }
            return img;
        } else {
            return null;
        }
    }

<<<<<<< HEAD
    public void saveImageToPNG(File file) throws IOException {
        BufferedImage img = getGameCapture();
        if (img != null) {
            ImageIO.write(img, "png", file);
=======
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
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
        } else {
            logger.warn("Screenshot image acquired was not valid and was not saved.");
        }
    }

    @Override
    public void run() {
<<<<<<< HEAD
        //noinspection InfiniteLoopStatement
        while(true) {
            repaint();
        }
    }


    public static void main(String[] args) {
        CaptureQWOPWindow.debugFrame = true;
        CaptureQWOPWindow locator = new CaptureQWOPWindow(0);
        try {
            locator.saveImageToPNG(new File("test.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread drawerThread = new Thread(locator);
        drawerThread.start();
    }
=======
        while(debugFrame) {
            repaint();
        }
    }
>>>>>>> 3aca6a7e233ee0daea77c6a3abea920fe53b0449
}
