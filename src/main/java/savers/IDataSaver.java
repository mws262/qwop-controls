package savers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.IGameInternal;
import game.action.Action;
import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DataSaver_Null.class, name = "null"),
        @JsonSubTypes.Type(value = DataSaver_DenseTFRecord.class, name = "dense_tfrecord"),
        @JsonSubTypes.Type(value = DataSaver_DenseJava.class, name = "dense_java"),
        @JsonSubTypes.Type(value = DataSaver_Sparse.class, name = "sparse"),
        @JsonSubTypes.Type(value = DataSaver_StageSelected.class, name = "stage_selected")

})
public interface IDataSaver<C extends Command<?>, S extends IState> {

    /**
     * Report initial state.
     */
    void reportGameInitialization(S initialState);

    /**
     * Report intermediate nodes as they are being run. Useful for dense
     * saving of data for TFRecords or other.
     *
     * @param action Current command being run.
     * @param game Instance of the game used for simulation.
     */
    void reportTimestep(Action<C> action, IGameInternal<C, S> game);

    /**
     * Get the final game state for this run.
     */
    void reportGameEnding(NodeGameBase<?, C, S> endNode);

    /**
     * Called when the end of a TreeStage is reached. TargetNodes meaning is different depending on the saver
     * implementation.
     */
    void reportStageEnding(NodeGameBase<?, C, S> rootNode, List<NodeGameBase<?, C, S>> targetNodes);

    /**
     * Store and dump any buffered data, often when a stage has ended but nothing specific needs to be reported.
     */
    void finalizeSaverData();

    /**
     * Set the number of games to collect in between saves.
     */
    void setSaveInterval(int numGames);

    int getSaveInterval();

    /**
     * Set where the files are saved. Defaults to working directory otherwise.
     */
    void setSavePath(String fileLoc);

    String getSavePath();

    /**
     * Get a fresh copy of this saver with the same settings.
     */
    @JsonIgnore
    IDataSaver<C, S> getCopy();

    /**
     * Generate a filename. Format is: [prefix]_YYYY-MM-DD_HH-mm-ss.[class name]
     */
    static String generateFileName(String prefix, String className) {
        Date date = new Date();
        SimpleDateFormat dateFormat =
				new SimpleDateFormat("'" + prefix + "_'" + "yyyy-MM-dd_HH-mm-ss" + "'." + className + "'");
        return dateFormat.format(date);
    }
}
