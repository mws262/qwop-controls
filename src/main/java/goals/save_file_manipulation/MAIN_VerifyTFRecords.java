package goals.save_file_manipulation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tensorflow.example.SequenceExample;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static data.TFRecordDataParsers.loadSequencesFromTFRecord;

/**
 * Check to make sure TFRecords are ok. This just verifies that the files can be loaded, not that the data itself is
 * correct. Sometimes if the executable is terminated in the middle of writing a file, it can produce an incomplete
 * TFRecord which I don't know how to ignore in Tensorflow.
 *
 * @author matt
 */
public class MAIN_VerifyTFRecords {

    private static Logger logger = LogManager.getLogger(MAIN_VerifyTFRecords.class);

    public static void main(String[] args) {

        File saveLoc = new File("src/main/resources/saved_data/training_data");

        File[] allFiles = saveLoc.listFiles();
        if (allFiles == null) throw new RuntimeException("Bad directory given: " + saveLoc.getName());

        List<File> exampleDataFiles = new ArrayList<>();
        for (File f : allFiles) {
            if (f.getName().contains("TFRecord") && !f.getName().contains("recovery")) {
                exampleDataFiles.add(f);
            }
        }
        logger.info("Found " + exampleDataFiles.size() + " files to check.");

        List<File> badFiles = new ArrayList<>();
        for (File file : exampleDataFiles) {
            logger.info("Checking " + file.getName() + "... ");
            try {
                List<SequenceExample> data = loadSequencesFromTFRecord(file);
                logger.info("found " + data.size() + " sequences... ");
            } catch (IOException e) {
                badFiles.add(file);
                logger.warn(file.getName() + "failed");
                continue;
            }
            logger.info("passed!");
        }
        logger.info("Summary: ");
        if (badFiles.isEmpty()) {
            logger.info("All files seem ok!");
        } else {
            logger.warn("The following files are bad:");
            for (File bad : badFiles) {
                logger.warn(bad.getName());
            }
        }
    }
}
