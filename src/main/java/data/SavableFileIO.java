package data;

import java.io.*;
import java.util.*;

/**
 * Store and load serialized objects. Each of these savers can only handle saving and loading objects of one type.
 *
 * @param <T> Type of object manipulated.
 * @author matt
 */
public class SavableFileIO<T> {

    /**
     * Whether to display debugging/progress messages.
     */
    public boolean verbose = true;

    /**
     * Store objects to file.
     *
     * @param data     Objects matching the generic of this saver that will be sent to file.
     * @param saveFile File to save data to. If the file does not exist, it will be created as
     *                 will any folders needed to get to this new file.
     * @param append   Append data to an existing file (true), or make a new file (false). Will tolerate append being
     *                 true even if the file does not exist, and will create the file anyway.
     */
    public void storeObjects(Collection<T> data, File saveFile, boolean append) {

        if (!saveFile.isFile()) {
            if (saveFile.getParentFile().mkdirs() && verbose) {
                System.out.println("Made parent directory(s) before storing objects.");
            }
        }

        if (!append || !saveFile.exists()) { // If the file doesn't exist, or we just don't want to append, then
            // don't use the appending version of the output stream.
            try (ObjectOutputStream objOps = new ObjectOutputStream(new FileOutputStream(saveFile, false))) {
                dataToStream(data, objOps);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else { // Appending should instead use the version below which overrides the WriteStreamHeader method.
            try (ObjectOutputStream objOps = new AppendingObjectOutputStream(new FileOutputStream(saveFile, true))) {
                dataToStream(data, objOps);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Load objects from a save file. They will be put in the provided collection. The provided collection will not
     * be cleared before adding objects.
     *
     * @param file Saved file to load from.
     * @param collection Collection (e.g. {@link java.util.List}, {@link java.util.Set}) to put the loaded objects in.
     */
    public void loadObjectsToCollection(File file, Collection<T> collection) {
        int counter = 0;
        try (ObjectInputStream objIs = new ObjectInputStream(new FileInputStream(file))) {
            if (verbose) {
                final String dir = System.getProperty("user.dir");
                System.out.println("current directory: " + dir);
            }

            boolean reading = true;
            while (reading) {
                try {
                    @SuppressWarnings("unchecked")
                    T obj = (T) objIs.readObject();
                    collection.add(obj);
                    counter++;
                } catch (EOFException c) {
                    reading = false;
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        if (verbose) System.out.println("Loaded " + counter + " objects from file " + file.getName() + ".");
    }

    /**
     * Send objects to an output stream.
     *
     * @param data Collection of objects to send to output stream.
     * @param objOps Output stream. Could be the usual version or the appending version.
     * @throws IOException When writing to stream fails.
     */
    private void dataToStream(Collection<T> data, ObjectOutputStream objOps) throws IOException {
        int count = 0;
        for (T d : data) {
            objOps.writeObject(d);
            count++;
            if (verbose) System.out.println("Wrote games to file: " + count + "/" + data.size());
        }
        objOps.flush();
    }

    /**
     * Combine multiple files into one output file, eliminating any duplicates. Does not delete the original files.
     *
     * @param inputFiles Multiple files with saved objects that we wish to combine into a single file. All these
     *                   files must contain the same object type.
     * @param destination Output file for the combined objects found in inputFiles.
     */
    public void combineFiles(File[] inputFiles, File destination) {
        HashSet<T> loadedSet = new HashSet<>();
        // Load them all into the same set.
        for (File file : inputFiles) {
            loadObjectsToCollection(file, loadedSet);
        }
        storeObjects(loadedSet, destination, false);
    }

    /**
     * Get all files in the working directory with a specified file extension.
     * @param extension File extension. Do not include the dot before the extension.
     * @return List of files with the specified file extension.
     **/
    public static Set<File> getFilesByExtension(File directory, String extension) {
        File[] filesInDirectory = directory.listFiles();
        Objects.requireNonNull(filesInDirectory, "Unable to open the current directory.");
        Set<File> files = new HashSet<>();

        for (File file : filesInDirectory) {
            if (file.isFile()) {
                int indexOfLastSeparator = file.getName().lastIndexOf(".");
                // Only get the files with the specified file extension.
                if (file.getName().substring(indexOfLastSeparator).equalsIgnoreCase(extension)) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    /**
     * Print the size of a file in megabytes. For debugging stuff.
     * @param file File to check the size of.
     */
    public static void printFileSize(File file) {
        if (file.exists()) {
            double bytes = file.length();
            double kilobytes = (bytes / 1024);
            double megabytes = (kilobytes / 1024);
            System.out.println(file + " is: " + Math.round(megabytes * 100) / 100. + "Mb");
        }
    }

    /**
     * Hack for appending to files rather than starting from scratch.
     *
     * @see <a href="https://stackoverflow.com/questions/1194656/appending-to-an-objectoutputstream/1195078#1195078">Stack Overflow post.</a>
     */
    public class AppendingObjectOutputStream extends ObjectOutputStream {

        public AppendingObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void writeStreamHeader() throws IOException {
            // do not write a header, but reset:
            // this line added after another question
            // showed a problem with the original
            reset();
        }
    }
}
