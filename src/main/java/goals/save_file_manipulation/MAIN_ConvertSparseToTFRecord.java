package goals.save_file_manipulation;

import com.google.common.base.Preconditions;
import data.SparseDataToDenseTFRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Take a sparsely-saved datafile (i.e. states not recorded at every timestep), and turn it into a densely-saved
 * TFRecord binary which can be used here or in the Python TensorFlow stuff.
 *
 * @author matt
 */
public class MAIN_ConvertSparseToTFRecord {

    private static final Logger logger = LogManager.getLogger(MAIN_ConvertSparseToTFRecord.class);

    public static void main(String[] args) throws FileNotFoundException {

        File loadFile = new File("./src/main/resources/saved_data/11_1_18/");
        File outFile = new File("./src/main/resources/saved_data/11_1_18/");
        String filterTerm = "";

        Preconditions.checkArgument(loadFile.isDirectory());
        Preconditions.checkArgument(outFile.isDirectory());

        if (!loadFile.exists()) {
            throw new FileNotFoundException("Could not find input file.");
        }

        if (outFile.mkdir()) {
            logger.info("Output directory created: " + outFile.getPath());
        }

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
