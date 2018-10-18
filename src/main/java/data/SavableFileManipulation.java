package data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SavableFileManipulation {

    /**
     * Get all files in the working directory with a specified file extension.
     * @param extension File extension. Do not include the dot before the extension.
     * @return List of files with the specified file extension.
     **/
    public static List<File> getFilesByExtension(String extension) {
        File folder = new File(".");
        File[] listOfFiles = folder.listFiles();
        ArrayList<File> qwopFiles = new ArrayList<>();

        if (listOfFiles == null) {
            throw new NullPointerException("Unable to open the current directory.");
        }

        System.out.println("Found the following files: ");
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {

                int indexOfLastSeparator = listOfFile.getName().lastIndexOf(".");

                // Only get the files with the specified file extension.
                if (listOfFile.getName().substring(indexOfLastSeparator).equalsIgnoreCase(extension)) {
                    System.out.println("File " + listOfFile.getName());
                    qwopFiles.add(listOfFile);
                }
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
        HashSet<SavableSingleGame> loaded = new HashSet<>();
        io.loadObjectsToCollection(file, loaded);
        io.storeObjects(loaded, destination, false);

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
        // Load them all into the same set.
        for (String file : inputFiles) {
            HashSet<SavableSingleGame> loadedSet = new HashSet<>();
            io.loadObjectsToCollection(new File(file), loadedSet);
            loaded.addAll(loadedSet);
        }
        System.out.println("Output file " + destination + " has " + loaded.size() + " games in it.");

        io.storeObjects(loaded, destination, false);
    }

}
