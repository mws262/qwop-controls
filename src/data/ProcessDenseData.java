package data;

import java.util.ArrayList;
import java.util.HashSet;

import main.Action;
import main.FSM_Game;
import main.FSM_Game.Status;
import main.INegotiateGame;
import main.State;

public class ProcessDenseData implements INegotiateGame{
	
	private FSM_Game gameFSM = new FSM_Game();
	private Thread gameThread = new Thread(gameFSM); 
	private ArrayList<SaveableDenseData> dataBuffer = new ArrayList<SaveableDenseData>();
	private boolean gameReady = true;
	
	public ProcessDenseData() {
		gameThread.start();
		gameFSM.setNegotiator(this);
	}
	
	public void process(String[] filenames) {
		
		/**** Load the sparse runs we want to fill in. ****/
		SaveableFileIO<SaveableSingleGame> fileIn = new SaveableFileIO<SaveableSingleGame>();
		SaveableFileIO<SaveableDenseData> fileOut = new SaveableFileIO<SaveableDenseData>();
		
		/**** Loop through simulations ****/
		
		for (int i = 0; i < filenames.length; i++) { // Loop through files.
			HashSet<SaveableSingleGame> gameData = fileIn.loadObjectsUnordered(filenames[i]);
			for (SaveableSingleGame singleGame : gameData) {
				while (!gameReady);
				
				gameFSM.addSequence(singleGame.actions);
				
				
			}
		}
		
		
		/**** Write to file ****/
		
		
		
	}
	
	
	public SaveableDenseData makeDenseDataFromRun(SaveableSingleGame game) {
		gameFSM.getInitialState();
		return null;
	}
	
	@Override
	public void reportGameStep(Action action) {
		gameFSM.getGameState();
	}

	@Override
	public void statusChange_Game(Status status) {
		switch(status) {
		case FAILED:
			break;
		case IDLE:
			gameReady = true;
			break;
		case INITIALIZE:
			break;
		case LOCKED:
			break;
		case RUNNING_SEQUENCE:
			gameReady = false;
			break;
		case WAITING:
			gameFSM.reportFall(); // We're never going to add extra actions after a run is queued here.
			break;
		default:
			break;
		}
	}

	@Override
	public void reportQWOPKeys(boolean Q, boolean W, boolean O, boolean P) {}

	@Override
	public void reportEndOfRealTimeSim() {}
}
