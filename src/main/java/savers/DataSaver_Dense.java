package savers;

import game.action.Action;
import game.IGameInternal;
import game.action.Command;
import game.state.IState;
import tree.node.NodeQWOPBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Saving to file with full state and command data at every timestep.
 * Data formatting and crunching into a file is handled by inheriting
 * classes. This only covers the basic data collection from
 * Negotiator.
 *
 * @author matt
 */

public abstract class DataSaver_Dense<C extends Command<?>> implements IDataSaver<C> {

    /**
     * Action buffer cleared once per game.
     */
    final ArrayList<Action<C>> actionBuffer = new ArrayList<>();

    /**
     * StateQWOP buffer cleared once per game.
     */
    final ArrayList<IState> stateBuffer = new ArrayList<>();

    /**
     * Number of games in between saves to file.
     */
    private int saveInterval;

    /**
     * File save location.
     */
    String fileLocation = "./";

    DataSaver_Dense() {
        actionBuffer.ensureCapacity(1800);
        stateBuffer.ensureCapacity(1800);
    }

    @Override
    public void reportGameInitialization(IState initialState) {
        actionBuffer.clear();
        stateBuffer.clear();
        stateBuffer.add(initialState);
    }

    @Override
    public void reportTimestep(Action<C> action, IGameInternal<C> game) {
        stateBuffer.add(game.getCurrentState());
        actionBuffer.add(action);
    }

    @Override
    public void reportStageEnding(NodeQWOPBase<?, C> rootNode, List<NodeQWOPBase<?, C>> targetNodes) {
    }

    @Override
    public void setSaveInterval(int numGames) {
        saveInterval = numGames;
    }

    @Override
    public int getSaveInterval() {
        return saveInterval;
    }

    @Override
    public void setSavePath(String fileLoc) {
        this.fileLocation = fileLoc;
    }

    @Override
    public String getSavePath() {
        return fileLocation;
    }

    @Override
    public abstract DataSaver_Dense<C> getCopy();
}
