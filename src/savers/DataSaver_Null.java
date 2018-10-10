package savers;

import java.util.List;

import game.GameLoader;
import game.State;
import main.Action;
import main.Node;

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
    public void reportTimestep(Action action, GameLoader game) {
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
    public DataSaver_Null clone() {
        return new DataSaver_Null();
    }

}
