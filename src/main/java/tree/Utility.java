package tree;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

/**
 * Various shared static utility methods.
 *
 * @author matt
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Utility {

    /**
     * Internal holder of the last time {@link Utility#tic()} was called.
     */
    private static long ticTime;

    /**
     * Random number generator for new node selection
     */
    private final static Random rand = new Random();

    /**
     * Generate a random integer between two values, inclusive.
     *
     * @param min Minimum in the range of possible integers generated.
     * @param max Maximum in the range of possible integers generated.
     * @return A randomly generated integer in the specified range.
     * @see Random#nextInt()
     */
    public static int randInt(int min, int max) {
        if (min > max)
            throw new IllegalArgumentException("Random int sampler should be given a minimum value which is less than" +
                    " or equal to the given max value.");
        return rand.nextInt((max - min) + 1) + min;
    }

    /**
     * Start timing. tic and toc are basically equivalent to the MATLAB versions. Because this is static, any piece
     * of code which calls these will be messing with the same timer. This can be useful, but means that if we want
     * to time many processes at once, we should use a different timing approach.
     */
    public static void tic() {
        ticTime = System.nanoTime();
    }

    /**
     * Return the elapsed time in nanoseconds since the last time {@link Utility#tic()} was called. Also prints a
     * human-readable seconds or milliseconds count.
     *
     * @return Nanoseconds since last tic call.
     */
    public static long toc() {
        long tocTime = System.nanoTime();
        long difference = tocTime - ticTime;
        if (difference < Long.MAX_VALUE) {
            System.out.println(Math.floor(difference / 10000.) / 100 + " ms elapsed.");
        } else {
            System.out.println(Math.floor(difference / 100000000.) / 10. + " s elapsed.");
        }
        return difference;
    }

    /**
     * Write a string to file.
     *
     * @param contents String to be written in the file.
     * @param outPath  Full path and filename of the log file.
     * @throws IOException File cannot be opened or written to.
     */
    public static void stringToLogFile(String contents, String outPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outPath))) {
            writer.write(contents);
        }
    }

    /**
     * Write some part of a file to a log. Begin logging with !LOG_START and end with !LOG_END. This can be done
     * multiple times in the same file. This is useful when we want to record the settings specified in a configuration
     * file to a log file.
     *
     * @param inPath  Full path and filename of the file to selectively send to log.
     * @param outPath Full path and filename of the log file.
     * @throws IOException File cannot be opened or written to.
     */
    public static void sectionToLogFile(String inPath, String outPath) throws IOException {
        boolean collecting = false;

        String divider = "**************************************************************";

        try (BufferedReader reader = new BufferedReader(new FileReader(inPath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outPath))) {

            while (reader.ready()) {
                String nextLine = reader.readLine();
                if (nextLine.contains("!LOG_START")) {
                    collecting = true;
                    writer.write(divider + "\n");
                } else if (nextLine.contains("!LOG_STOP")) {
                    if (!collecting)
                        System.out.println("WARNING: sectionToLogFile found mismatched !LOG_START and !LOG_STOP commands. This is" +
                                " tolerated, but could represent a larger problem.");
                    collecting = false;
                    writer.write(divider + "\n");
                }
                if (collecting) {
                    writer.write(nextLine + "\n");
                }
            }
        }
        if (collecting)
            System.out.println("WARNING: sectionToLogFile found mismatched !LOG_START and !LOG_STOP commands. This is" +
                    " tolerated, but could represent a larger problem.");
    }

    /**
     * Delete a specified file, if it exists.
     *
     * @param fileName File we wish to delete.
     * @return True if the file was deleted by this method; false if the file could not be deleted or did not exist.
     */
    public static boolean clearExistingFile(String fileName) {
        boolean success = false;
        File file = new File(fileName);
        try {
            success = Files.deleteIfExists(file.toPath());
            if (success) System.out.println("Cleared file: " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return success;
    }

    /**
     * Generate a timestamped filename. Format is: [prefix]_YYYY-MM-DD_HH-mm-ss.[fileExtension].
     *
     * @param prefix        String to place at the beginning of the filename.
     * @param fileExtension String to place at the end of the file name as a file extension.
     * @return String for a timestamped file name.
     * @see Utility#getTimestamp()
     */
    public static String generateFileName(String prefix, String fileExtension) {
        Date date = new Date();
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("'" + prefix + "_'" + "yyyy-MM-dd_HH-mm-ss" + "'." + fileExtension + "'");
        return dateFormat.format(date);
    }

    /**
     * Get a timestamp in string form.
     */
    public static String getTimestamp() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return dateFormat.format(date);
    }

    /**
     * Load a configuration file.
     *
     * @param file File (not filename) of the config file we wish to load.
     * @return Properties object which is a {@link java.util.Hashtable} containing property names and property
     * settings as key/value pairs.
     */
    public static Properties loadConfigFile(File file) {
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            prop.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop;
    }

    /**
     * Returns the path that the code is being executed from. Can be useful since relative paths can get screwy
     * depending on whether the code is running in the IDE, command line, .jar, etc.
     *
     * @return String name of the path the the code is executing from.
     */
    @Deprecated
    public static String getExecutionPath() {
        String path = "";
        try {
            path = Utility.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        if (path.endsWith(".jar")) { // Executing from packaged jar.
            int lastDiv = path.lastIndexOf("/");
            path = path.substring(0, lastDiv + 1);
            path = path.replaceFirst("^/(.:/)", "$1"); // Needed to strip drive letter off of the front of the path in Windows.
            path = path.replace("//", ""); // If we end up with a double slashed concatenated after all these replacements.
        } else { // Running in eclipse or something.
            path += "../../";
        }
        return path;
    }

    /**
     * Make a JFrame go to the specified monitor (by index) and match its size. Choose whether to make it full screen
     * or not.
     * @param frame Frame to move and resize.
     * @param screen Index of the monitor to use.
     * @param fullScreen Maximize to completely full screen? This will not have an upper toolbar.
     *
     * @see <a href="https://stackoverflow.com/questions/4627553/show-jframe-in-a-specific-screen-in-dual-monitor
     * -configuration">StackOverflow thread</a>
     */
    public static void showOnScreen(JFrame frame, int screen, boolean fullScreen) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        if( screen > -1 && screen < gd.length ) {
            if (fullScreen) {
                gd[screen].setFullScreenWindow( frame );
            } else {
                frame.setLocation(gd[screen].getDefaultConfiguration().getBounds().x, gd[screen].getDefaultConfiguration().getBounds().y + frame.getY());
            }
        } else if( gd.length > 0 ) {
            if (fullScreen) {
                gd[0].setFullScreenWindow( frame );
            } else {
                frame.setLocation(gd[0].getDefaultConfiguration().getBounds().x, gd[0].getDefaultConfiguration().getBounds().y + frame.getY());
            }
        } else {
            throw new RuntimeException( "No Screens Found" );
        }
        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
    }
}
