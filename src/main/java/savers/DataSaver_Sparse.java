package savers;

import game.action.Action;
import data.SavableFileIO;
import data.SavableSingleGame;
import game.IGameInternal;
import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Saver for sparse game information. Basically this includes game.command
 * needed to recreate a run, but not full state information at every timestep.
 *
 * @author matt
 */

public class DataSaver_Sparse<C extends Command<?>, S extends IState> implements IDataSaver<C, S> {

    /**
     * File prefix. Goes in front of date.
     */
    public final String filePrefix = "qwop_sparse_java";

    /**
     * Do not include dot before.
     */
    public final String fileExtension = "SavableSingleGame";

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
    private SavableFileIO<SavableSingleGame<C, S>> fileIO = new SavableFileIO<>();

    /**
     * Buffered games awaiting file write.
     */
    private ArrayList<SavableSingleGame<C, S>> saveBuffer = new ArrayList<>();

    @Override
    public void reportGameInitialization(IState initialState) {}

    @Override
    public void reportTimestep(Action<C> action, IGameInternal<C, S> game) {}

    @Override
    public void reportGameEnding(NodeGameBase<?, C, S> endNode) {
        saveBuffer.add(new SavableSingleGame<>(endNode));
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
    public int getSaveInterval() {
        return saveInterval;
    }

    @Override
    public String getSavePath() {
        return fileLocation;
    }

    @Override
    public void reportStageEnding(NodeGameBase<?, C, S> rootNode, List<NodeGameBase<?, C, S>> targetNodes) {
        // If the save buffer still has stuff in it, save!
        if (!saveBuffer.isEmpty()) {
            File saveFile = new File(fileLocation + IDataSaver.generateFileName(filePrefix, fileExtension));
            fileIO.storeObjects(saveBuffer, saveFile, false);
        }
    }

    @Override
    public void finalizeSaverData() {}

    @Override
    public DataSaver_Sparse<C, S> getCopy() {
        DataSaver_Sparse<C, S> newSaver = new DataSaver_Sparse<>();
        newSaver.setSaveInterval(saveInterval);
        newSaver.setSavePath(fileLocation);
        return newSaver;
    }
}
