package savers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import data.SavableFileIO;
import data.SavableSingleGame;
import game.GameThreadSafe;
import game.State;
import actions.Action;
import tree.Node;

/**
 * Save save any runs specified by the TreeStage at the end of it.
 * This data is saved sparsely.
 *
 * @author matt
 */
public class DataSaver_StageSelected implements IDataSaver {

    /**
     * File prefix. Goes in front of date.
     */
    @SuppressWarnings("WeakerAccess")
    public String filePrefix = "qwop_stage_sparse_java";

    /**
     * Do not include dot before.
     */
    @SuppressWarnings("WeakerAccess")
    public String fileExtension = "SavableSingleGame";

    /**
     * If this string is not empty, use this as the filename instead.
     */
    public String overrideFilename = "";

    /**
     * Handles class serialization and writing to file.
     */
    private SavableFileIO<SavableSingleGame> fileIO = new SavableFileIO<>();

    /**
     * Buffered games awaiting file write.
     */
    private ArrayList<SavableSingleGame> saveBuffer = new ArrayList<>();

    /**
     * File save location.
     */
    private String fileLocation = "./";

    @Override
    public void reportGameInitialization(State initialState) {
    }

    @Override
    public void reportTimestep(Action action, GameThreadSafe game) {
    }

    @Override
    public void reportGameEnding(Node endNode) {
    }

    @Override
    public void reportStageEnding(Node rootNode, List<Node> targetNodes) {
        for (Node tar : targetNodes) {
            saveBuffer.add(new SavableSingleGame(tar));
        }

        String successStatus = "";
        if (targetNodes.isEmpty()) { // If we couldn't possible achieve the objective, just save the root node run
        	// and flag as unsuccessful in the filename.
            successStatus = "_unsuccessful";
            saveBuffer.add(new SavableSingleGame(rootNode));
        }

        if (overrideFilename.isEmpty()) {
            File saveFile = new File(fileLocation + IDataSaver.generateFileName(filePrefix + successStatus,
                    fileExtension));
            fileIO.storeObjects(saveBuffer, saveFile, false);
        } else {
            File saveFile = new File(fileLocation + overrideFilename + successStatus + "." + fileExtension);
            fileIO.storeObjects(saveBuffer, saveFile, false);

        }
        saveBuffer.clear();
        System.out.println("Saved " + targetNodes.size() + " runs sparsely to file at the end of the stage.");
    }

    @Override
    public void finalizeSaverData() {}

    @Override
    public void setSaveInterval(int numGames) {} // Not applicable for once-per-stage saving.

    @Override
    public void setSavePath(String fileLoc) {
        this.fileLocation = fileLoc;
    }

    @Override
    public IDataSaver getCopy() {
        DataSaver_StageSelected newSaver = new DataSaver_StageSelected();
        newSaver.setSavePath(fileLocation);
        return newSaver;
    }
}
