package savers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import game.GameLoader;
import game.State;
import actions.Action;
import tree.Node;

public interface IDataSaver {

    /**
     * Report initial state.
     **/
    void reportGameInitialization(State initialState);

    /**
     * Report intermediate nodes as they are being run. Useful for dense
     * saving of data for TFRecords or other.
     *
     * @param action
     * @param game
     */
    void reportTimestep(Action action, GameLoader game);

    /**
     * Get the final game state for this run.
     **/
    void reportGameEnding(Node endNode);

    /**
     * Called when the end of a TreeStage is reached. TargetNodes meaning is different depending on the saver
     * implementation.
     **/
    void reportStageEnding(Node rootNode, List<Node> targetNodes);

    /**
     * Set the number of games to collect in between saves.
     **/
    void setSaveInterval(int numGames);

    /**
     * Set where the files are saved. Defaults to working directory otherwise.
     **/
    void setSavePath(String fileLoc);

    /**
     * Get a fresh copy of this saver with the same settings.
     **/
    IDataSaver getCopy();

    /**
     * Generate a filename. Format is: [prefix]_YYYY-MM-DD_HH-mm-ss.[class name]
     **/
    static String generateFileName(String prefix, String className) {
        Date date = new Date();
        SimpleDateFormat dateFormat =
				new SimpleDateFormat("'" + prefix + "_'" + "yyyy-MM-dd_HH-mm-ss" + "'." + className + "'");
        String name = dateFormat.format(date);
        System.out.println("Generated file: " + name);

        return name;
    }
}
