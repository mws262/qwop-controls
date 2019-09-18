package savers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import data.SavableFileIO;
import data.SavableSingleGame;
import game.IGameInternal;
import game.action.Action;
import game.action.Command;
import game.state.IState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tree.node.NodeGameBase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Save save any runs specified by the TreeStage at the end of it.
 * This data is saved sparsely.
 *
 * @author matt
 */
public class DataSaver_StageSelected<C extends Command<?>, S extends IState> implements IDataSaver<C, S> {

    /**
     * File prefix. Goes in front of date.
     */
    public final String filePrefix = "qwop_stage_sparse_java";

    /**
     * Do not include dot before.
     */
    public final String fileExtension = "SavableSingleGame";

    /**
     * If this string is not empty, use this as the filename instead.
     */
    public String overrideFilename;

    /**
     * Handles class serialization and writing to file.
     */
    private SavableFileIO<SavableSingleGame<C, S>> fileIO = new SavableFileIO<>();

    /**
     * Buffered games awaiting file write.
     */
    private ArrayList<SavableSingleGame<C, S>> saveBuffer = new ArrayList<>();

    /**
     * File save location.
     */
    private String fileLocation = "./";

    private static final Logger logger = LogManager.getLogger(DataSaver_StageSelected.class);

    @Override
    public void reportGameInitialization(IState initialState) {}

    @Override
    public void reportTimestep(Action<C> action, IGameInternal<C, S> game) {}

    @Override
    public void reportGameEnding(NodeGameBase<?, C, S> endNode) {}

    @Override
    public void reportStageEnding(NodeGameBase<?, C, S> rootNode, List<NodeGameBase<?, C, S>> targetNodes) {
        for (NodeGameBase<?, C, S> tar : targetNodes) {
            saveBuffer.add(new SavableSingleGame<>(tar));
        }

        String successStatus = "";
        if (targetNodes.isEmpty()) { // If we couldn't possible achieve the objective, just save the root node run
        	// and flag as unsuccessful in the filename.
            successStatus = "_unsuccessful";
            saveBuffer.add(new SavableSingleGame<>(rootNode));
        }

        if (overrideFilename == null || overrideFilename.isEmpty()) {
            File saveFile = new File(fileLocation + IDataSaver.generateFileName(filePrefix + successStatus,
                    fileExtension));
            fileIO.storeObjects(saveBuffer, saveFile, false);
        } else {
            File saveFile = new File(fileLocation + overrideFilename + successStatus + "." + fileExtension);
            fileIO.storeObjects(saveBuffer, saveFile, false);

        }
        saveBuffer.clear();
        logger.info("Saved " + targetNodes.size() + " runs sparsely to file at the end of the stage.");
    }

    @Override
    public void finalizeSaverData() {}

    @JsonIgnore
    @Override
    public void setSaveInterval(int numGames) {} // Not applicable for once-per-stage saving.

    @Override
    public void setSavePath(String fileLoc) {
        this.fileLocation = fileLoc;
    }

    @JsonIgnore
    @Override
    public int getSaveInterval() {
        return 0;
    }

    @Override
    public String getSavePath() {
        return fileLocation;
    }

    @Override
    public IDataSaver<C, S> getCopy() {
        DataSaver_StageSelected<C, S> newSaver = new DataSaver_StageSelected<>();
        newSaver.setSavePath(fileLocation);
        return newSaver;
    }
}
