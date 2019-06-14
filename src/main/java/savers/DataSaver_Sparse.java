package savers;

import actions.Action;
import data.SavableFileIO;
import data.SavableSingleGame;
import game.IGameInternal;
import game.IState;
import tree.NodeQWOPBase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Saver for sparse game information. Basically this includes actions
 * needed to recreate a run, but not full state information at every timestep.
 *
 * @author matt
 */

public class DataSaver_Sparse implements IDataSaver {

    /**
     * File prefix. Goes in front of date.
     */
    public String filePrefix = "qwop_sparse_java";

    /**
     * Do not include dot before.
     */
    public String fileExtension = "SavableSingleGame";

    /**
     * File save location.
     */
    private String fileLocation = "./";

    /**
     * How many games in between saves.
     */
    private int saveInterval = 100;

    /**
     * Games since last file write.
     */
    private int gamesSinceFile = 0;

    /**
     * Handles class serialization and writing to file.
     */
    private SavableFileIO<SavableSingleGame> fileIO = new SavableFileIO<>();

    /**
     * Buffered games awaiting file write.
     */
    private ArrayList<SavableSingleGame> saveBuffer = new ArrayList<>();

    @Override
    public void reportGameInitialization(IState initialState) {}

    @Override
    public void reportTimestep(Action action, IGameInternal game) {}

    @Override
    public void reportGameEnding(NodeQWOPBase<?> endNode) {
        saveBuffer.add(new SavableSingleGame(endNode));
        gamesSinceFile++;

        if (saveInterval == gamesSinceFile) {
            File saveFile = new File(fileLocation + IDataSaver.generateFileName(filePrefix,
                    fileExtension));
            fileIO.storeObjects(saveBuffer, saveFile, false);
            saveBuffer.clear();
            gamesSinceFile = 0;
        }
    }

    @Override
    public void setSaveInterval(int numGames) {
        saveInterval = numGames;
    }

    @Override
    public void setSavePath(String fileLoc) {
        this.fileLocation = fileLoc;
    }

    @Override
    public void reportStageEnding(NodeQWOPBase<?> rootNode, List<NodeQWOPBase<?>> targetNodes) {
        // If the save buffer still has stuff in it, save!
        if (!saveBuffer.isEmpty()) {
            File saveFile = new File(fileLocation + IDataSaver.generateFileName(filePrefix, fileExtension));
            fileIO.storeObjects(saveBuffer, saveFile, false);
        }
    }

    @Override
    public void finalizeSaverData() {

    }

    @Override
    public DataSaver_Sparse getCopy() {
        DataSaver_Sparse newSaver = new DataSaver_Sparse();
        newSaver.setSaveInterval(saveInterval);
        newSaver.setSavePath(fileLocation);
        return newSaver;
    }
}
