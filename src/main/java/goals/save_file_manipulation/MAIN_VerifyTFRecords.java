package goals.save_file_manipulation;

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

    public static void main(String[] args) {

        File saveLoc = new File("src/main/resources/saved_data/11_2_18");

        File[] allFiles = saveLoc.listFiles();
        if (allFiles == null) throw new RuntimeException("Bad directory given: " + saveLoc.getName());

        List<File> exampleDataFiles = new ArrayList<>();
        for (File f : allFiles) {
            if (f.getName().contains("TFRecord") && !f.getName().contains("recovery")) {
                exampleDataFiles.add(f);
            }
        }
        System.out.println("Found " + exampleDataFiles.size() + " files to check.");

        List<File> badFiles = new ArrayList<>();
        for (File file : exampleDataFiles) {
            System.out.print("Checking " + file.getName() + "... ");
            try {
                List<SequenceExample> data = loadSequencesFromTFRecord(file);
                System.out.println("found " + data.size() + " sequences... ");
            } catch (IOException e) {
                badFiles.add(file);
                System.out.println("failed.");
                continue;
            }
            System.out.println("passed!");
        }
        System.out.println("Summary: ");
        if (badFiles.isEmpty()) {
            System.out.println("All files seem ok!");
        } else {
            System.out.println("The following files are bad:");
            for (File bad : badFiles) {
                System.out.println(bad.getName());
            }
        }
    }
}
