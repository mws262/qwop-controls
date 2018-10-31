package goals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import data.SparseDataToDenseTFRecord;

public class MAIN_ConvertSparseToTFRecord {

    public static void main(String[] args) {

        File loadFile = new File("./4_26_18/");
        File outFile = new File("./4_26_18/");
        String filterTerm = "steadyRunPrefix";

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
