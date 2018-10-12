package data;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import game.GameLoader;
import main.Action;
import main.ActionQueue;
import savers.DataSaver_DenseTFRecord;

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
 */
public class SparseDataToDense {

    /**
     * Personal copy of the game.
     **/
    private final GameLoader game = new GameLoader();

    /**
     * Saver that this will use. Use a dense one most likely.
     **/
    private final DataSaver_DenseTFRecord saver = new DataSaver_DenseTFRecord();;

    /**
     * Saves and loads.
     **/
    private final SavableFileIO<SavableSingleGame> fileIO = new SavableFileIO<>();

    /**
     * Queued commands, IE QWOP key presses
     **/
    private final ActionQueue actionQueue = new ActionQueue();

    /**
     * If we don't want to save data for the first or last actions in a sequence.
     **/
    public int trimFirst = 0;
    public int trimLast = 0;

    public SparseDataToDense(String fileLoc) {
        if (!fileLoc.endsWith("/")) fileLoc = fileLoc + "/";
        saver.setSavePath(fileLoc);
    }

    /**
     * Resim and convert. saveBulk means that all will be combined into one file. Otherwise into many different.
     **/
    public void convert(List<File> files, boolean saveBulk) {

        if (saveBulk) {
            saver.setSaveInterval(-1);
            for (File file : files) {
                List<SavableSingleGame> sparseGames = fileIO.loadObjectsOrdered(file.getAbsolutePath());
                for (SavableSingleGame singleGame : sparseGames) {
                    sim(singleGame);
                }
            }
            saver.toFile();
        } else {
            saver.setSaveInterval(1);
            for (File file : files) {
                List<SavableSingleGame> sparseGames = fileIO.loadObjectsOrdered(file.getAbsolutePath());
                for (SavableSingleGame singleGame : sparseGames) {
                    saver.filenameOverride = file.getName().split("\\.(?=[^\\.]+$)")[0];
                    sim(singleGame);
                }
            }
        }

        saver.reportStageEnding(null, null);
    }

    private void simWithoutSave(Action[] actions) {
        actionQueue.clearAll();
        actionQueue.addSequence(actions);
        while (!actionQueue.isEmpty()) {
            boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
            boolean Q = nextCommand[0];
            boolean W = nextCommand[1];
            boolean O = nextCommand[2];
            boolean P = nextCommand[3];
            game.stepGame(Q, W, O, P);
            if (game.getFailureStatus()) {
                System.out.println("Game saver is seeing a failed state");
            }
        }
    }

    private void simWithSave(Action[] actions) {
        actionQueue.clearAll();
        actionQueue.addSequence(actions);
        while (!actionQueue.isEmpty()) {
            boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
            Action action = actionQueue.peekThisAction();
            boolean Q = nextCommand[0];
            boolean W = nextCommand[1];
            boolean O = nextCommand[2];
            boolean P = nextCommand[3];
            game.stepGame(Q, W, O, P);
            saver.reportTimestep(action, game); // Key difference
            if (game.getFailureStatus()) {
                System.out.println("Game saver is seeing a failed state");
            }
        }
    }

    private void sim(SavableSingleGame singleGame) {
        actionQueue.clearAll();
        Action[] gameActions = singleGame.actions;
        Action[] noSaveActions1 = Arrays.copyOfRange(gameActions, 0, trimFirst);
        Action[] saveActions = Arrays.copyOfRange(gameActions, trimFirst, gameActions.length - trimLast);
        Action[] noSaveActions2 = Arrays.copyOfRange(gameActions, gameActions.length - trimLast, gameActions.length);

        if (noSaveActions1.length + saveActions.length + noSaveActions2.length != gameActions.length)
            throw new RuntimeException("Split the game actions into an incorrect number which does not add up to the " +
                    "original total.");

        game.makeNewWorld();
        simWithoutSave(noSaveActions1);
        saver.reportGameInitialization(game.getCurrentState()); // Wait to initialize until the ignored section is done.
        simWithSave(saveActions);

        saver.reportGameEnding(null);
    }
}
