package data;

import game.GameUnified;
import game.action.Action;
import game.action.ActionQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import savers.DataSaver_DenseTFRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Convert from a sparse representation of QWOP runs to a dense representation by re-simulating.
 * <p>
 * There are several "densities" of QWOP game data saved. During giant searches, data is usually saved as [state,
 * keys, wait time], and only at key changes is data held (i.e. at nodes). All the in-between states are discarded.
 * For trajectory libraries, machine learning training, etc., we may want data which has the state at every timestep.
 * Hence, we pick the important sparse data out of a tree, and pass it to this class. It will re-simulate these runs,
 * log data at every timestep, and save this data in TFRecord form.
 *
 * @author matt
 *
 * See {@link SavableSingleGame}, {@link DataSaver_DenseTFRecord}.
 */
public class SparseDataToDenseTFRecord {

    /**
     * Interface to the game and Box2D physics for simulation game.action.
     */
    private final GameUnified game = new GameUnified();

    /**
     * Saver that this converted will use. Uses a dense saver (every timestep saved) most likely.
     */
    private final DataSaver_DenseTFRecord saver = new DataSaver_DenseTFRecord();

    /**
     * Saving and loading of Java objects.
     */
    private final SavableFileIO<SavableSingleGame> fileIO = new SavableFileIO<>();

    /**
     * Queued {@link Action}, i.e. durations and keypresses.
     */
    private final ActionQueue actionQueue = new ActionQueue();

    /**
     * If we don't want to save data for the first or last game.action in a sequence.
     */
    public int trimFirst = 0;
    public int trimLast = 0;

    private static Logger logger = LogManager.getLogger(SparseDataToDenseTFRecord.class);

    /**
     * Make a new converter for making binary TFRecord files from {@link SavableSingleGame} which were
     * previoiusly serialized to file by {@link SavableFileIO}.
     *
     * @param fileLoc Directory for saving converted data.
     */
    public SparseDataToDenseTFRecord(String fileLoc) {
        if (!fileLoc.endsWith("/")) fileLoc = fileLoc + "/";
        saver.setSavePath(fileLoc);
    }

    /**
     * Given files containing sparsely saved information (i.e. just at game.action transitions, not at every timestep),
     * convert to densely saved data by re-simulating.
     * @param files Files containing sparsely saved run data, {@link SavableSingleGame}.
     * @param saveBulk Whether to
     */
    public void convert(List<File> files, boolean saveBulk) {

        if (saveBulk) {
            saver.setSaveInterval(-1);
            for (File file : files) {
                List<SavableSingleGame> sparseGames = new ArrayList<>();
                fileIO.loadObjectsToCollection(file, sparseGames);
                for (SavableSingleGame singleGame : sparseGames) {
                    sim(singleGame);
                }
            }
            saver.toFile();
        } else {
            saver.setSaveInterval(1);
            for (File file : files) {
                List<SavableSingleGame> sparseGames = new ArrayList<>();
                fileIO.loadObjectsToCollection(file, sparseGames);
                for (SavableSingleGame singleGame : sparseGames) {
                    saver.filenameOverride = file.getName().split("\\.(?=[^.]+$)")[0];
                    sim(singleGame);
                }
            }
        }
        saver.reportStageEnding(null, null);
    }

    /**
     * Simulate some game.action without saving. Useful when some beginning game.action are being trimmed from the data.
     *
     * @param actions Array of {@link Action game.action} to simulate without saving.
     */
    private void simWithoutSave(Action[] actions) {
        actionQueue.clearAll();
        actionQueue.addSequence(actions);
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
            if (game.getFailureStatus()) {
                logger.warn("Game saver is seeing a failed state");
            }
        }
    }

    /**
     * Simulate game.action with data stored at every timestep.
     *
     * @param actions Array of {@link Action game.action} to simulate with saving of full state data at every timestep.
     */
    private void simWithSave(Action[] actions) {
        actionQueue.clearAll();
        actionQueue.addSequence(actions);
        while (!actionQueue.isEmpty()) {
            boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
            Action action = actionQueue.peekThisAction();
            game.step(nextCommand);
            saver.reportTimestep(action, game); // Key difference
            if (game.getFailureStatus()) {
                logger.warn("Game saver is seeing a failed state");
            }
        }
    }

    /**
     * Simulate a sparsely-saved game stored as a {@link SavableSingleGame}. Save data at every timestep which is
     * included between {@link SparseDataToDenseTFRecord#trimFirst} and {@link SparseDataToDenseTFRecord#trimLast}.
     *
     * @param singleGame Sparsely-saved data to re-simulate and convert.
     */
    private void sim(SavableSingleGame singleGame) {
        actionQueue.clearAll();
        Action[] gameActions = singleGame.actions;

        if (trimLast + trimFirst > gameActions.length) {
            throw new IndexOutOfBoundsException("Number of trimmed game.action (beginning + end) exceeds the total number" +
                    " of game.action in the run.");
        }

        // Divide the game.action up into ones which shouldn't be saved at the beginning, ones which should be saved in
        // the middle, and ones which shouldn't be saved at the end.
        Action[] noSaveActions1 = Arrays.copyOfRange(gameActions, 0, trimFirst);
        Action[] saveActions = Arrays.copyOfRange(gameActions, trimFirst, gameActions.length - trimLast);
        Action[] noSaveActions2 = Arrays.copyOfRange(gameActions, gameActions.length - trimLast, gameActions.length);

        // Total number of game.action should be preserved by trimming.
        assert noSaveActions1.length + saveActions.length + noSaveActions2.length == gameActions.length;

        game.makeNewWorld();
        if (noSaveActions1.length > 0) simWithoutSave(noSaveActions1);
        saver.reportGameInitialization(game.getCurrentState()); // Wait to initialize until the ignored section is done.
        simWithSave(saveActions);
        // No need to simulate the unsaved game.action at the end.

        saver.reportGameEnding(null);
    }
}
