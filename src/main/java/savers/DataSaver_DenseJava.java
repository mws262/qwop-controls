package savers;

import data.SavableDenseData;
import data.SavableFileIO;
import tree.NodeQWOPBase;

import java.io.File;
import java.util.ArrayList;

/**
 * Saves data at every timestep. Old saver which serializes java classes. Useful for manipulating data in java, not
 * good for TensorFlow stuff. Another disadvantage is that if any of the Java code, packages, or names change, then
 * the data might become unloadable.
 *
 * @author matt
 */
public class DataSaver_DenseJava extends DataSaver_Dense {

    /**
     * File prefix. Goes in front of date.
     */
    @SuppressWarnings("WeakerAccess")
    public String filePrefix = "qwop_dense_java";

    /**
     * Do not include dot before.
     */
    @SuppressWarnings("WeakerAccess")
    public String fileExtension = "SavableDenseData";

    /**
     * Games since last save.
     */
    private int saveCounter = 0;

    /**
     * Handles class serialization and writing to file.
     */
    private SavableFileIO<SavableDenseData> fileIO = new SavableFileIO<>();

    /**
     * Buffered games waiting to be written to file.
     */
    private ArrayList<SavableDenseData> saveBuffer = new ArrayList<>();

    @Override
    public void reportGameEnding(NodeQWOPBase<?> endNode) {
        // Collect all the states and actions into a data object.
        saveBuffer.add(new SavableDenseData(stateBuffer, actionBuffer));
        saveCounter++;

        if (saveInterval == saveCounter) {
            File saveFile = new File(fileLocation + IDataSaver.generateFileName(filePrefix,
                    fileExtension));
            fileIO.storeObjects(saveBuffer, saveFile, false);
            saveCounter = 0;
        }
        // Clear out for the next run to begin.
        stateBuffer.clear();
        actionBuffer.clear();
    }

    @Override
    public void finalizeSaverData() {
        if (saveInterval == 0) {
            File saveFile = new File(fileLocation + IDataSaver.generateFileName(filePrefix,
                    fileExtension));
            fileIO.storeObjects(saveBuffer, saveFile, false);
        }
    }

    @Override
    public DataSaver_DenseJava getCopy() {
        DataSaver_DenseJava newSaver = new DataSaver_DenseJava();
        newSaver.setSaveInterval(saveInterval);
        newSaver.setSavePath(fileLocation);
        return newSaver;
    }
}
