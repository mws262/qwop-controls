package savers;

import game.State;
import main.Action;
import main.IDataSaver;
import main.Node;

/**
 * Data saver placeholder for when we don't wish to actually save anything.
 *
 * @author Matt
 *
 */
public class DataSaver_null implements IDataSaver {

	@Override
	public void reportGameInitialization(State initialState) {}

	@Override
	public void reportTimestep(Action action, State state) {}

	@Override
	public void reportGameEnding(Node endNode) {}

	@Override
	public void setSaveInterval(int numGames) {}

	@Override
	public void setSavePath(String fileLoc) {}

}
