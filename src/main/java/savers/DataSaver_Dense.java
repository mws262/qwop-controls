package savers;

import game.action.Action;
import game.IGameInternal;
import game.state.IState;
import tree.node.NodeQWOPBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Saving to file with full state and action data at every timestep.
 * Data formatting and crunching into a file is handled by inheriting
 * classes. This only covers the basic data collection from
 * Negotiator.
 *
 * @author matt
 */

public abstract class DataSaver_Dense implements IDataSaver {

    /**
     * Action buffer cleared once per game.
     */
    ArrayList<Action> actionBuffer = new ArrayList<>();
    /**
     * State buffer cleared once per game.
     */
    ArrayList<IState> stateBuffer = new ArrayList<>();

    /**
     * Number of games in between saves to file.
     */
    int saveInterval;

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
    public void reportTimestep(Action action, IGameInternal game) {
        stateBuffer.add(game.getCurrentState());
        actionBuffer.add(action);
    }

    @Override
    public void reportStageEnding(NodeQWOPBase<?> rootNode, List<NodeQWOPBase<?>> targetNodes) {
    }

    @Override
    public void setSaveInterval(int numGames) {
        saveInterval = numGames;
    }

    @Override
    public void setSavePath(String fileLoc) {
        this.fileLocation = fileLoc;
    }

    @Override
    public abstract DataSaver_Dense getCopy();
}
