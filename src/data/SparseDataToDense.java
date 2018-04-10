package data;

import java.io.File;
import java.util.List;

import game.GameLoader;
import main.Action;
import main.ActionQueue;
import main.IDataSaver;

public class SparseDataToDense {
	
	/** Personal copy of the game. **/
	private GameLoader game = new GameLoader();
	
	/** Saver that this will use. Use a dense one most likely. **/
	private IDataSaver saver;
	
	/** Saves and loads. **/
	private SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
	
	/** Queued commands, IE QWOP key presses **/
	public ActionQueue actionQueue = new ActionQueue();
	
	public SparseDataToDense(IDataSaver saver) {
		this.saver = saver;
	}
	
	/** Resim and convert. **/
	public void convert(List<File> files) {
		for (File file : files) {
			List<SaveableSingleGame> sparseGames = fileIO.loadObjectsOrdered(file.getName());
			for (SaveableSingleGame singleGame : sparseGames) {
				sim(singleGame);
			}
		}
		saver.reportStageEnding(null, null);
		
		// MATT START WORKING HERE 4/10
		
	}
	
	private void sim(SaveableSingleGame singleGame) {
		actionQueue.clearAll();
		actionQueue.addSequence(singleGame.actions);
		game.makeNewWorld();
		saver.reportGameInitialization(GameLoader.getInitialState());
		
		while (!actionQueue.isEmpty()) {
			Action action = actionQueue.peekThisAction();
			boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
			boolean Q = nextCommand[0];
			boolean W = nextCommand[1]; 
			boolean O = nextCommand[2];
			boolean P = nextCommand[3];
			game.stepGame(Q,W,O,P);
			saver.reportTimestep(action, game);
		}
		saver.reportGameEnding(null);

	}
}
