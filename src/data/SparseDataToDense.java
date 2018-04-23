package data;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import game.GameLoader;
import main.Action;
import main.ActionQueue;
import savers.DataSaver_DenseTFRecord;

public class SparseDataToDense {

	/** Personal copy of the game. **/
	private GameLoader game = new GameLoader();

	/** Saver that this will use. Use a dense one most likely. **/
	private DataSaver_DenseTFRecord saver;

	/** Saves and loads. **/
	private SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();

	/** Queued commands, IE QWOP key presses **/
	public ActionQueue actionQueue = new ActionQueue();

	/** If we don't want to save data for the first or last actions in a sequence. **/
	public int trimFirst = 0;
	public int trimLast = 0;

	public SparseDataToDense(DataSaver_DenseTFRecord saver) {
		this.saver = saver;
	}

	/** Resim and convert. **/
	public void convert(List<File> files) {
		for (File file : files) {
			List<SaveableSingleGame> sparseGames = fileIO.loadObjectsOrdered(file.getAbsolutePath());
			for (SaveableSingleGame singleGame : sparseGames) {
				saver.filenameOverride = file.getName().split("\\.(?=[^\\.]+$)")[0];
				sim(singleGame);
			}
		}
		saver.reportStageEnding(null, null);	
	}

	private void simWithoutSave(Action[] actions) {
		actionQueue.clearAll();
		actionQueue.addSequence(actions);
		while (!actionQueue.isEmpty()) {
			boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
			Action action = actionQueue.peekThisAction();
			boolean Q = nextCommand[0];
			boolean W = nextCommand[1]; 
			boolean O = nextCommand[2];
			boolean P = nextCommand[3];
			game.stepGame(Q,W,O,P);
			if (game.getFailureStatus()) {
				System.out.println("Game saver is seeing a failed state");
			}
		}
	}

	private void simWithSave(Action[] actions) {
		actionQueue.clearAll();
		actionQueue.addSequence(actions);
		while (!actionQueue.isEmpty()) {
			boolean[] nextCommand = actionQueue.pollCommand(); // Get and remove the next keypresses
			Action action = actionQueue.peekThisAction();
			boolean Q = nextCommand[0];
			boolean W = nextCommand[1]; 
			boolean O = nextCommand[2];
			boolean P = nextCommand[3];
			game.stepGame(Q,W,O,P);
			saver.reportTimestep(action, game); // Key difference
			if (game.getFailureStatus()) {
				System.out.println("Game saver is seeing a failed state");
			}
		}
	}

	private void sim(SaveableSingleGame singleGame) {
		actionQueue.clearAll();
		Action[] gameActions = singleGame.actions;

		Action[] noSaveActions1 = Arrays.copyOfRange(gameActions, 0, trimFirst);
		Action[] saveActions = Arrays.copyOfRange(gameActions, trimFirst, gameActions.length - trimLast);
		Action[] noSaveActions2 = Arrays.copyOfRange(gameActions, gameActions.length - trimLast, gameActions.length);

		if (noSaveActions1.length + saveActions.length + noSaveActions2.length != gameActions.length)
			throw new RuntimeException("Split the game actions into an incorrect number which does not add up to the original total.");

		game.makeNewWorld();
		simWithoutSave(noSaveActions1);
		saver.reportGameInitialization(game.getCurrentState()); // Wait to initialize until the ignored section is done.
		simWithSave(saveActions);

		saver.reportGameEnding(null);
	}
}
