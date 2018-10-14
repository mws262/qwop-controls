package main;

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
 * @author matt
 */
public class Utility {

    private static long ticTime;

    /**
     * Random number generator for new node selection
     **/
    private final static Random rand = new Random();

    /**
     * Generate a random integer between two values, inclusive.
     **/
    public static int randInt(int min, int max) {
        if (min > max)
            throw new IllegalArgumentException("Random int sampler should be given a minimum value which is less than" +
					" or equal to the given max value.");
        return rand.nextInt((max - min) + 1) + min;
    }

    /**
     * Matlab tic and toc functionality.
     **/
    public static void tic() {
        ticTime = System.nanoTime();
    }

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

    public static void stringToLogFile(String contents, String outPath) throws IOException {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(outPath))) {
            writer.write(contents);
        }
    }

    /**
     * Write some part of a file to a log. Begin logging with !LOG_START and end with
     * !LOG_END. This can be done multiple times in the same file.
     **/
    public static void sectionToLogFile(String inPath, String outPath) throws IOException {
        boolean collecting = false;
        String divider = "**************************************************************";

        try(BufferedReader reader = new BufferedReader(new FileReader(inPath));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outPath))) {
            while (reader.ready()) {
                String nextLine = reader.readLine();

                if (nextLine.contains("!LOG_START")) {
                    collecting = true;
                    writer.write(divider + "\n");
                } else if (nextLine.contains("!LOG_STOP")) {
                    collecting = false;
                    writer.write(divider + "\n");
                }
                if (collecting) {
                    writer.write(nextLine + "\n");
                }
            }
        }
    }

    /**
     * Clear out an existing file.
     **/
    public static void clearExistingFile(String fileName) {
        File file = new File(fileName);
        try {
            boolean result = Files.deleteIfExists(file.toPath());
            if (result) System.out.println("Cleared file: " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a filename. Format is: [prefix]_YYYY-MM-DD_HH-mm-ss.[class name]
     **/
    public static String generateFileName(String prefix, String className) {
        Date date = new Date();
        SimpleDateFormat dateFormat =
				new SimpleDateFormat("'" + prefix + "_'" + "yyyy-MM-dd_HH-mm-ss" + "'." + className + "'");
        String name = dateFormat.format(date);
        System.out.println("Generated file: " + name);

        return name;
    }

    /**
     * Get a timestamp in string form.
     **/
    public static String getTimestamp() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return dateFormat.format(date);
    }

    /**
     * Load a configuration file.
     **/
    public static Properties loadConfigFile(File file) {
        Properties prop = new Properties();
        try(FileInputStream fis = new FileInputStream(file)) {
            prop.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop;
    }

    public static String getExcutionPath() {
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
}
