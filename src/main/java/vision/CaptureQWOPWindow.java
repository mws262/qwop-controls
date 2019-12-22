package vision;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;

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
     */
    private static final int topRowMainColor = new Color(83, 114, 136).getRGB();
    private static final int topRowEndColor = new Color(77, 89, 106).getRGB();

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
     * The monitor index to look for the game in. 0 should be the main monitor. TODO make it auto-detect this.
     */
    private int monitorIdx;

    /** x,y,width,height of selected monitor. **/
    private Rectangle monitorBounds;

    /**
     * For capturing screen regions.
     */
    private Robot robot;

    /**
     * Frame for making sure it's capturing the correct region.
     */
    public JFrame frame;

    /**
     * Detected upper corner x of the game.
     */
    private int gameUpperCornerX;

    /**
     * Detected upper corner y of the game (note from top left corner).
     */
    private int gameUpperCornerY;

    private Rectangle screenCapRegion;

    private static boolean debugFrame = false;

    private static final Logger logger = LogManager.getLogger(CaptureQWOPWindow.class);

    public CaptureQWOPWindow(int monitorIdx) {
        this.monitorIdx = monitorIdx;

        // Figure out the size of the specified monitor.
        final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        logger.info("Found " + devices.length + " monitors.");
        for (GraphicsDevice dev : devices)
            logger.info(dev.getDefaultConfiguration().getBounds().width + " x " + dev.getDefaultConfiguration().getBounds().height);

        GraphicsDevice gd = devices[monitorIdx];
        monitorBounds = gd.getDefaultConfiguration().getBounds();

        // Make new Robot for capturing screen pixels.
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        // Find the dimensions and coordinates to just capture the QWOP window.
        locateQWOP();

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
        g.drawImage(getGameCapture(), 0, 0, (int) (defaultGameWidth * gameStageScaling),
                (int) (defaultGameHeight * gameStageScaling), null);
    }

    private void locateQWOP() {
        // Capture the whole monitor image and get the data buffer as pixels.
        BufferedImage img = robot.createScreenCapture(monitorBounds);

        // This array is basically all the rows (x-dimension) concatenated together.
        int[] pixels = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();

        boolean previouslyOnTopRow = false;
        int topRowStartPixel = 0;
        int topRowEndPixel = 0;

        // The data buffer collapses RGBA into a single 32bit integer. This Stack Overflow magic converts back.
        for (int i = 0; i < pixels.length; i++) {

            if (!previouslyOnTopRow && pixels[i] == topRowMainColor) {
                previouslyOnTopRow = true;
                topRowStartPixel = i;
            } else if (previouslyOnTopRow && (pixels[i] != topRowMainColor && pixels[i] != topRowEndColor)) {
                topRowEndPixel = i;
                break;
            }
        }

        gameUpperCornerX = topRowStartPixel % monitorBounds.width;
        gameUpperCornerY = topRowStartPixel / monitorBounds.width;
        gameStageScaling = Math.max((topRowEndPixel - topRowStartPixel) / (double) defaultGameWidth, 0.1);

        if ((topRowEndPixel - topRowStartPixel) == 0) {
            logger.warn("Game window not found. Check monitor number or move nearby browser instances.");
        } else {
            logger.info("Found game width of: " + (topRowEndPixel - topRowStartPixel));
            logger.info("Top-right game coordinate: (" + gameUpperCornerX + ", " + gameUpperCornerY + ")");
            logger.info("Game scaling is: " + gameStageScaling);
        }

        screenCapRegion = new Rectangle(gameUpperCornerX + (int)monitorBounds.getX(), // Have to add on start
                // coordinates of the selected monitor. Can be weird in multi-monitor setups.
                gameUpperCornerY + (int)monitorBounds.getY(),
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
            if (img.getRGB(0, 0) != topRowMainColor) {
                logger.warn("Screen cap region invalid. Locating the QWOP window again. RGB found at corner location " +
                        "actually was: " + img.getRGB(0, 0));
                locateQWOP();
                getGameCapture();
            }
            return img;
        } else {
            return null;
        }
    }

    public void saveImageToPNG(File file) throws IOException {
        BufferedImage img = getGameCapture();
        if (img != null) {
            ImageIO.write(img, "png", file);
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


    public static void main(String[] args) {
        CaptureQWOPWindow.debugFrame = true;
        CaptureQWOPWindow locator = new CaptureQWOPWindow(1);
        try {
            locator.saveImageToPNG(new File("test.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread drawerThread = new Thread(locator);
        drawerThread.start();
    }
}
