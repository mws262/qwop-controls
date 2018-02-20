package main;

import java.util.ArrayList;

/**
 * Saving to file with full state and action data at every timestep.
 * Data formatting and crunching into a file is handled by inheriting
 * classes. This only covers the basic data collection from 
 * Negotiator.
 * 
 * @author matt
 *
 */

public abstract class DataSaver_Dense implements IDataSaver {

	/** Action buffer cleared once per game. **/
	protected ArrayList<Action> actionBuffer = new ArrayList<Action>();
	/** State buffer cleared once per game. **/
	protected ArrayList<State> stateBuffer = new ArrayList<State>();

	/** Number of games in between saves to file. **/
	protected int saveInterval = 100;
	
	public DataSaver_Dense() {
		actionBuffer.ensureCapacity(1800);
		stateBuffer.ensureCapacity(1800);
	}
	
	@Override
	public void reportGameInitialization(State initialState) {
		actionBuffer.clear();
		stateBuffer.clear();
		stateBuffer.add(initialState);

	}

	@Override
	public void reportTimestep(Action action, State state) {
		stateBuffer.add(state);
		actionBuffer.add(action);

	}
	
	@Override
	public void setSaveInterval(int numGames) {
		saveInterval = numGames;
	}

}
