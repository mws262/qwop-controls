package savers;

import game.action.Action;
import game.IGameInternal;
import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

import java.util.List;

/**
 * Data saver placeholder for when we don't wish to actually save anything.
 *
 * @author Matt
 */
public class DataSaver_Null<C extends Command<?>> implements IDataSaver<C> {

    @Override
    public void reportGameInitialization(IState initialState) {}

    @Override
    public void reportTimestep(Action<C> action, IGameInternal<C> game) {}

    @Override
    public void reportGameEnding(NodeGameBase<?, C> endNode) {}

    @Override
    public void setSaveInterval(int numGames) {}

    @Override
    public int getSaveInterval() {
        return 0;
    }

    @Override
    public void setSavePath(String fileLoc) {}

    @Override
    public String getSavePath() {
        return "";
    }

    @Override
    public void reportStageEnding(NodeGameBase<?, C> rootNode, List<NodeGameBase<?, C>> targetNodes) {}

    @Override
    public void finalizeSaverData() {}

    @Override
    public DataSaver_Null<C> getCopy() {
        return new DataSaver_Null<>();
    }
}
