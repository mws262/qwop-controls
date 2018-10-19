package savers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import data.SavableFileIO;
import data.SavableSingleGame;
import game.GameLoader;
import game.State;
import actions.Action;
import tree.Node;

/**
 * Saver for sparse game information. Basically this includes actions
 * needed to recreate a run, but not full state information at every timestep.
 *
 * @author matt
 */

public class DataSaver_Sparse implements IDataSaver {

    /**
     * File prefix. Goes in front of date.
     **/
    @SuppressWarnings("WeakerAccess")
    public String filePrefix = "qwop_sparse_java";

    /**
     * Do not include dot before.
     **/
    @SuppressWarnings("WeakerAccess")
    public String fileExtension = "SavableSingleGame";

    /**
     * File save location.
     **/
    private String fileLocation = "./";

    /**
     * How many games in between saves.
     **/
    private int saveInterval = 100;

    /**
     * Games since last file write.
     **/
    private int gamesSinceFile = 0;

    /**
     * Handles class serialization and writing to file.
     **/
    private SavableFileIO<SavableSingleGame> fileIO = new SavableFileIO<>();

    /**
     * Buffered games awaiting file write.
     **/
    private ArrayList<SavableSingleGame> saveBuffer = new ArrayList<>();

    @Override
    public void reportGameInitialization(State initialState) {
    }

    @Override
    public void reportTimestep(Action action, GameLoader game) {
    }

    @Override
    public void reportGameEnding(Node endNode) {
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
    public void reportStageEnding(Node rootNode, List<Node> targetNodes) {
        // If the save buffer still has stuff in it, save!
        if (!saveBuffer.isEmpty()) {
            File saveFile = new File(fileLocation + IDataSaver.generateFileName(filePrefix, fileExtension));
            fileIO.storeObjects(saveBuffer, saveFile, false);
        }
    }

    @Override
    public DataSaver_Sparse getCopy() {
        DataSaver_Sparse newSaver = new DataSaver_Sparse();
        newSaver.setSaveInterval(saveInterval);
        newSaver.setSavePath(fileLocation);
        return newSaver;
    }
}
