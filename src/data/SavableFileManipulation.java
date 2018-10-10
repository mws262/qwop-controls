package data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

public class SavableFileManipulation {

    static String origin = "test3";
    static String destination = "test3";

    public static void main(String[] args) {
        //ArrayList<File> qwopFiles = getQWOPFiles();
        String[] filesToCombine = new String[]{"test1", "test2"};
        combineFiles(filesToCombine, "test_out");
    }

    /**
     * Get all .qwop files in the working directory.
     **/
    public static ArrayList<File> getQWOPFiles(String extension) {
        File folder = new File(".");
        File[] listOfFiles = folder.listFiles();
        ArrayList<File> qwopFiles = new ArrayList<>();
        System.out.println("Found the following QWOP files: ");
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {


                File f = listOfFile;
                int indexOfLastSeparator = f.getName().lastIndexOf(".");

                // Only get the .qwop files
                if (f.getName().substring(indexOfLastSeparator).equalsIgnoreCase(extension)) {
                    System.out.println("File " + f.getName());
                    qwopFiles.add(f);
                }
            } else if (listOfFile.isDirectory()) {
                //System.out.println("Directory " + listOfFiles[i].getName());
            }
        }
        return qwopFiles;
    }

    /**
     * Eliminate all duplicate runs in the given file and save to destination file.
     **/
    public static void eliminateDuplicateRuns(String origin, String destination) {
        File file = new File(origin);

        if (file.exists()) {
            double bytes = file.length();
            double kilobytes = (bytes / 1024);
            double megabytes = (kilobytes / 1024);
            System.out.println(Math.round(megabytes * 100) / 100. + " megabyte file input: " + origin);
        }

        SavableFileIO<SavableSingleGame> io = new SavableFileIO<>();
        HashSet<SavableSingleGame> loaded = io.loadObjectsUnordered(origin);
        io.storeObjectsUnordered(loaded, destination, false);


        file = new File(destination);
        if (file.exists()) {
            double bytes = file.length();
            double kilobytes = (bytes / 1024);
            double megabytes = (kilobytes / 1024);
            System.out.println(Math.round(megabytes * 100) / 100. + " megabyte file output: " + destination);
        }
    }

    /**
     * Combine multiple .qwop files into a single one containing the data of both with duplicates removed.
     **/
    public static void combineFiles(String[] inputFiles, String destination) {
        SavableFileIO<SavableSingleGame> io = new SavableFileIO<>();
        HashSet<SavableSingleGame> loaded = new HashSet<>();
        System.out.println("Combining .QWOP files: ");
        // Load them all into the same hashset.
        for (String file : inputFiles) {
            HashSet<SavableSingleGame> loadedSet = io.loadObjectsUnordered(file);
            loaded.addAll(loadedSet);
            //System.out.println("File " + file + " has " + loadedSet.size() + " games in it.");
        }
        System.out.println("Output file " + destination + " has " + loaded.size() + " games in it.");

        io.storeObjectsUnordered(loaded, destination, false);
    }

}
