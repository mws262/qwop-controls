package data;

import java.io.*;
import java.util.Collection;

/**
 * Store and load serialized objects.
 *
 * @param <T> Type of object manipulated.
 * @author matt
 */
public class SavableFileIO<T> {

    /**
     * Whether to display debugging messages.
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

        if (!append || !saveFile.exists()) {
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

    private void dataToStream(Collection<T> data, ObjectOutputStream objOps) throws IOException {
        int count = 0;
        for (T d : data) {
            objOps.writeObject(d);
            count++;
            if (verbose) System.out.println("Wrote games to file: " + count + "/" + data.size());
        }
        objOps.flush();
    }

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
