package goals.save_file_manipulation;

import data.TFRecordDataParsers;
import data.TFRecordWriter;
import org.tensorflow.example.SequenceExample;

import java.io.*;
import java.util.List;

/**
 * Take a (potentially) large TFRecord file of densely saved QWOP run data, and strip one run off, saving it to its
 * own file. This can be useful if there is a single run we want to use as a test case.
 *
 * @author matt
 */
public class MAIN_PullRunOffTFRecord {

    public static void main(String[] args) {
        try {
            saveIndividualRunToFile(new File("src/main/resources/saved_data/denseTF_2018-05-01_08-38-39.TFRecord"),
                    new File("src/main/resources/saved_data/example_run.tfrecord"), 0);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load runs from a TFRecord file, save a specified one to its own file.
     * @param inFile Input file to load runs from.
     * @param outFile Output file to save the selected run to.
     * @param runIndex Index of the run to save to its own file.
     * @throws FileNotFoundException Input file could not be found, or was not a TFRecord.
     */
    private static void saveIndividualRunToFile(File inFile, File outFile, int runIndex) throws FileNotFoundException {

        if (!inFile.exists()) {
            throw new FileNotFoundException("Could not find the specified file.");
        }

        if (!inFile.getName().toLowerCase().contains("tfrecord")) {
            throw new FileNotFoundException("Looks like the input file is not the correct type.");
        }

        // Read all the sequences from a file.
        List<SequenceExample> dataSeries = null;
        try {
            dataSeries = TFRecordDataParsers.loadSequencesFromTFRecord(inFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (dataSeries.isEmpty()) {
            throw new IndexOutOfBoundsException("Could not locate any runs in the TFRecord file specified.");
        }else if (dataSeries.size() - 1 < runIndex) {
            throw new IndexOutOfBoundsException("Specified run index is out of bounds of the found runs in the " +
                    "TFRecord file.");
        }

        SequenceExample chosenRun = dataSeries.get(runIndex);

        try (FileOutputStream fOut = new FileOutputStream(outFile)) {
            TFRecordWriter.writeToStream(chosenRun.toByteArray(), fOut);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
