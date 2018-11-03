package goals.save_file_manipulation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import data.SparseDataToDenseTFRecord;

/**
 * Take a sparsely-saved datafile (i.e. states not recorded at every timestep), and turn it into a densely-saved
 * TFRecord binary which can be used here or in the Python TensorFlow stuff.
 *
 * @author matt
 */
public class MAIN_ConvertSparseToTFRecord {

    public static void main(String[] args) {

        File loadFile = new File("src/main/resources/saved_data/11_1_18/");
        File outFile = new File("./src/main/resources/saved_data/11_1_18/");
        String filterTerm = "";

        //noinspection ResultOfMethodCallIgnored
        outFile.mkdir();

        List<File> filesToConvert = new ArrayList<>();
        File[] files = loadFile.listFiles();
        for (File f : Objects.requireNonNull(files)) {
            if (f.toString().contains("SavableSingleGame") && f.toString().contains(filterTerm)) {
                filesToConvert.add(f);
            }
        }

        SparseDataToDenseTFRecord converter = new SparseDataToDenseTFRecord(outFile.getAbsolutePath());
        converter.trimLast = 4;
        converter.convert(filesToConvert, true);
    }
}
