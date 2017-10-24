package data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import main.Action;
import main.FSM_Game;
import main.FSM_Game.Status;
import main.INegotiateGame;
import main.State;

public class ProcessDenseData implements INegotiateGame{
	
	private FSM_Game gameFSM = new FSM_Game();
	private Thread gameThread = new Thread(gameFSM); 
	private ArrayList<Action> actionBuffer = new ArrayList<Action>();
	private ArrayList<State> stateBuffer = new ArrayList<State>();
	private ArrayList<SaveableDenseData> denseDataBuffer = new ArrayList<SaveableDenseData>();
	private volatile boolean gameReady = true;
	private State initialState = FSM_Game.getInitialState();
	
	public ProcessDenseData() {
		gameThread.start();
		gameFSM.setNegotiator(this);
	}
	
	public void process(String[] filenames) {
		
		/**** Load the sparse runs we want to fill in. ****/
		SaveableFileIO<SaveableSingleGame> fileIn = new SaveableFileIO<SaveableSingleGame>();
		SaveableFileIO<SaveableDenseData> fileOut = new SaveableFileIO<SaveableDenseData>();
		
		/**** Loop through simulations ****/
		int counter = 0;
		for (int i = 0; i < filenames.length; i++) { // Loop through files.
			HashSet<SaveableSingleGame> gameData = fileIn.loadObjectsUnordered(filenames[i]);
			for (SaveableSingleGame singleGame : gameData) {
				System.out.println("Waiting for game to be ready.");
				while (!gameReady); // Wait for the game FSM to return to IDLE.	
				System.out.println("Ready!");
				gameFSM.addSequence(singleGame.actions); // Queue up the next games.
				stateBuffer.add(initialState);
				gameReady = false; // Mark the game FSM busy until IDLE is reached again.
				counter++;
				System.out.println("Games run: " + counter);
			}
		}
		
		// Hack to prevent concurrent modification. TODO yeah....
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/**** Write to file ****/
		fileOut.storeObjectsOrdered(denseDataBuffer, "denseData", false);
		
		
	}
	
	@Override
	public void reportGameStep(Action action) {
		actionBuffer.add(action); // Technically this is the action which GETS us to the current state, so we want it sort of grouped with the previous state since that is when it is applied.
		stateBuffer.add(gameFSM.getGameState());
	}

	@Override
	public void statusChange_Game(Status status) {
		System.out.println(status.toString());
		switch(status) {
		case FAILED:
			// Collect all the states and actions into a data object.
			denseDataBuffer.add(new SaveableDenseData(stateBuffer,actionBuffer));
			// Clear out for the next run to begin.
			stateBuffer.clear();
			actionBuffer.clear();
			gameReady = true;
			break;
		case IDLE:
			break;
		case INITIALIZE:
			break;
		case LOCKED:
			break;
		case RUNNING_SEQUENCE:
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
