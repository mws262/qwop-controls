package goals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import data.SparseDataToDense;

public class MAIN_ConvertSparseToTFRecord {

    public static void main(String[] args) {

        File loadFile = new File("./4_26_18/");
        File outFile = new File("./4_26_18/");
        String filterTerm = "steadyRunPrefix";

        outFile.mkdir();

        List<File> filesToConvert = new ArrayList<>();
        File[] files = loadFile.listFiles();
        for (File f : files) {
            if (f.toString().contains("SavableSingleGame") && f.toString().contains(filterTerm)) {
                filesToConvert.add(f);
            }
        }

        SparseDataToDense converter = new SparseDataToDense(outFile.getAbsolutePath());
        converter.trimLast = 4;
        converter.convert(filesToConvert, true);
    }
}
