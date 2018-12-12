package savers;

import java.util.List;

import game.GameThreadSafe;
import game.State;
import actions.Action;
import tree.Node;

/**
 * Data saver placeholder for when we don't wish to actually save anything.
 *
 * @author Matt
 */
public class DataSaver_Null implements IDataSaver {

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
    public void setSaveInterval(int numGames) {
    }

    @Override
    public void setSavePath(String fileLoc) {
    }

    @Override
    public void reportStageEnding(Node rootNode, List<Node> targetNodes) {
    }

    @Override
    public void finalizeSaverData() {
    }

    @Override
    public DataSaver_Null getCopy() {
        return new DataSaver_Null();
    }
}
