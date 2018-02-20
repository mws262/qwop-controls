package main;

import java.util.ArrayList;

import data.SaveableDenseData;
import data.SaveableFileIO;
import data.SaveableSingleGame;

/**
 * Saver for sparse game information. Basically this includes actions
 * needed to recreate a run, but not full state information at every timestep.
 * @author matt
 *
 */

public class DataSaver_Sparse implements IDataSaver {

	/** File prefix. Goes in front of date. **/
	public String filePrefix = "qwop_sparse_java";
	
	/** Do not include dot before. **/
	public String fileExtension = "SaveableSingleGame";
	
	/** How many games in between saves. **/
	private int saveInterval = 100;
	
	/** Games since last file write. **/
	private int gamesSinceFile = 0;
	
	/** Handles class serialization and writing to file. **/
	private SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
	
	/** Buffered games awaiting file write. **/
	private ArrayList<SaveableSingleGame> saveBuffer = new ArrayList<SaveableSingleGame>();
	
	@Override
	public void reportGameInitialization(State initialState) {}

	@Override
	public void reportTimestep(Action action, State state) {}

	@Override
	public void reportGameEnding(Node endNode) {
		saveBuffer.add(new SaveableSingleGame(endNode));
		gamesSinceFile++;
		
		if (saveInterval == gamesSinceFile) {	
			fileIO.storeObjectsOrdered(saveBuffer, IDataSaver.generateFileName(filePrefix, fileExtension), false);
			saveBuffer.clear();
			gamesSinceFile = 0;
		}
	}

	@Override
	public void setSaveInterval(int numGames) {
		saveInterval = numGames;
	}

}
