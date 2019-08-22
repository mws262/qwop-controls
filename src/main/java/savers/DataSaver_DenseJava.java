package savers;

import data.SavableDenseData;
import data.SavableFileIO;
import game.action.Command;
import tree.node.NodeQWOPBase;

import java.io.File;
import java.util.ArrayList;

/**
 * Saves data at every timestep. Old saver which serializes java classes. Useful for manipulating data in java, not
 * good for TensorFlow stuff. Another disadvantage is that if any of the Java code, packages, or names change, then
 * the data might become unloadable.
 *
 * @author matt
 */
public class DataSaver_DenseJava <C extends Command<?>> extends DataSaver_Dense<C> {

    /**
     * File prefix. Goes in front of date.
     */
    public static final String filePrefix = "qwop_dense_java";

    /**
     * Do not include dot before.
     */
    public static final String fileExtension = "SavableDenseData";

    /**
     * Games since last save.
     */
    private int saveCounter = 0;

    /**
     * Handles class serialization and writing to file.
     */
    private final SavableFileIO<SavableDenseData<C>> fileIO = new SavableFileIO<>();

    /**
     * Buffered games waiting to be written to file.
     */
    private final ArrayList<SavableDenseData<C>> saveBuffer = new ArrayList<>();

    @Override
    public void reportGameEnding(NodeQWOPBase<?, C> endNode) {
        // Collect all the states and game.action into a data object.
        saveBuffer.add(new SavableDenseData<>(stateBuffer, actionBuffer));
        saveCounter++;

        if (getSaveInterval() == saveCounter) {
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
        if (getSaveInterval() == 0) {
            File saveFile = new File(fileLocation + IDataSaver.generateFileName(filePrefix,
                    fileExtension));
            fileIO.storeObjects(saveBuffer, saveFile, false);
        }
    }

    @Override
    public DataSaver_DenseJava<C> getCopy() {
        DataSaver_DenseJava<C> newSaver = new DataSaver_DenseJava<>();
        newSaver.setSaveInterval(getSaveInterval());
        newSaver.setSavePath(fileLocation);
        return newSaver;
    }
}
