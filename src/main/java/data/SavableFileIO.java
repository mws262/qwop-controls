package data;

import java.io.*;
import java.util.Collection;

/**
 * Store and load serialized objects.
 *
 * @param <T> Type of object manipulated.
 *
 * @author matt
 */
public class SavableFileIO<T> {

    /**
     *  Whether to display debugging messages.
     */
    public boolean verbose = true;

    /**
     * Store objects to file.
     *
     * @param data Objects matching the generic of this saver that will be sent to file.
     * @param fullFileName Full name/path of the file to save to. If the file does not exist, it will be created as
     *                     will any folders needed to get to this new file.
     * @param append Append data to an existing file (true), or make a new file (false).
     */
    public void storeObjects(Collection<T> data, String fullFileName, boolean append) {

        File file = new File(fullFileName);
        if (!file.isFile()) {
            if (file.getParentFile().mkdirs() && verbose) {
                System.out.println("Made parent directory(s) before storing objects.");
            }
        }

        try (ObjectOutputStream objOps = new ObjectOutputStream(new FileOutputStream(fullFileName, append))){
            int count = 0;
            for (T d : data) {
                objOps.writeObject(d);
                count++;
                if (verbose) System.out.println("Wrote games to file: " + count + "/" + data.size());
            }
            objOps.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void loadObjectsToCollection(File file, Collection<T> collection) {
        int counter = 0;
        try (ObjectInputStream objIs = new ObjectInputStream(new FileInputStream(file))){
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
}
