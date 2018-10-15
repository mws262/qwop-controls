package data;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SavableFileIO<T> {

    /**
     * Store objects in ordered form. Only use if you require ordered. It's slower. Also preserves duplicates for
     * better or worse.
     **/
    public void storeObjectsOrdered(List<T> data, String fullFileName, boolean append) {

        OutputStream ops = null;
        ObjectOutputStream objOps = null;

        try {
            if (append && new File(fullFileName).isFile()) {
                ops = new FileOutputStream(fullFileName, true);
                objOps = new AppendingObjectOutputStream(ops);
            } else {
                File file = new File(fullFileName);
                file.getParentFile().mkdirs();
                ops = new FileOutputStream(file, false);
                objOps = new ObjectOutputStream(ops);
            }

            int count = 0;
            for (T d : data) {
                objOps.writeObject(d);
                count++;
                System.out.println("Wrote games to file: " + count + "/" + data.size());
            }
            objOps.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (objOps != null) objOps.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Store objects in ordered form. Only use if you require ordered. It's slower. Also preserves duplicates for
     * better or worse.
     **/
    public void storeObjectsOrdered(T data, String fullFileName, boolean append) {
        List<T> dataList = new ArrayList<>();
        dataList.add(data);
        storeObjectsOrdered(dataList, fullFileName, append);
    }

    /**
     * Store objects in unordered form. Will also not preserve duplicates.
     **/
    public void storeObjectsUnordered(HashSet<T> data, String fullFileName, boolean append) {

        OutputStream ops = null;
        ObjectOutputStream objOps = null;

        try {
            if (append && new File(fullFileName).isFile()) {
                ops = new FileOutputStream(fullFileName, true);
                objOps = new AppendingObjectOutputStream(ops);
            } else {
                File file = new File(fullFileName);
                file.getParentFile().mkdirs();
                ops = new FileOutputStream(file, false);
            }

            for (T d : data) {
                objOps.writeObject(d);
            }
            objOps.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (objOps != null) objOps.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Store objects in unordered form. Will also not preserve duplicates.
     **/
    public void storeObjectsUnordered(T data, String fullFileName, boolean append) {
        HashSet<T> dataList = new HashSet<>();
        dataList.add(data);
        storeObjectsUnordered(dataList, fullFileName, append);
    }

    /**
     * Load objects in unordered form. Will also NOT contain duplicates.
     **/
    public HashSet<T> loadObjectsUnordered(String fullFileName) {
        InputStream fileIs = null;
        ObjectInputStream objIs = null;
        int counter = 0;

        HashSet<T> dataList = new HashSet<>();
        try {
            final String dir = System.getProperty("user.dir");
            System.out.println("current directory: " + dir);
            fileIs = new FileInputStream(fullFileName);
            objIs = new ObjectInputStream(fileIs);
            boolean reading = true;
            while (reading) {
                try {
                    @SuppressWarnings("unchecked")
                    T obj = (T) objIs.readObject();
                    dataList.add(obj);
                    counter++;
                } catch (EOFException c) {
                    reading = false;
                }
            }

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (objIs != null) objIs.close();
            } catch (Exception ex) {

            }
        }
        System.out.println("Loaded " + counter + " objects from file " + fullFileName + ".");
        return dataList;
    }

    /**
     * Load objects in ordered form. Faster but could contain duplicates.
     **/
    public List<T> loadObjectsOrdered(String fullFileName) {
        return loadObjectsOrdered(new File(fullFileName));
    }

    /**
     * Load objects in ordered form. Faster but could contain duplicates.
     **/
    public List<T> loadObjectsOrdered(File file) {

        InputStream fileIs = null;
        ObjectInputStream objIs = null;
        int counter = 0;

        List<T> dataList = new ArrayList<>();
        try {
            fileIs = new FileInputStream(file);
            objIs = new ObjectInputStream(fileIs);
            boolean reading = true;
            while (reading) {
                try {
                    @SuppressWarnings("unchecked")
                    T obj = (T) objIs.readObject();
                    dataList.add(obj);
                    counter++;
                } catch (EOFException c) {
                    reading = false;
                }
            }

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (objIs != null) objIs.close();
                if (fileIs != null) fileIs.close();
            } catch (Exception ex) {

            }
        }
        System.out.println("Loaded " + counter + " objects from file " + file.getName() + ".");
        return dataList;
    }

    /**
     * Handle when we want to append to a save file rather than start anew.
     *
     * @author matt
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