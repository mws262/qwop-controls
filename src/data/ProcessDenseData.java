package data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import game.State;
import main.Action;
import main.FSM_Game;
import main.FSM_Game.Status;
import main.INegotiateGame;

/**
 * Convert sparse run data, ie with only state and action at the transitions, to dense, with data
 * at every timestep.
 * 
 * @author matt
 *
 */
public class ProcessDenseData implements INegotiateGame{

	/** Interface to the QWOP game. **/
	private FSM_Game gameFSM = new FSM_Game();
	/** Separate thread for the game. **/
	private Thread gameThread = new Thread(gameFSM);

	private ArrayList<Action> actionBuffer = new ArrayList<Action>();
	private ArrayList<State> stateBuffer = new ArrayList<State>();
	private ArrayList<SaveableDenseData> denseDataBuffer = new ArrayList<SaveableDenseData>();

	/** Initial state that the runner starts at. First element of every data object. **/
	private State initialState = FSM_Game.getInitialState();
	/** Is the game ready to take a new sequence yet? **/
	private volatile boolean gameReady = true;

	public ReentrantLock negotiatorLock = new ReentrantLock();
	public Condition negotiatorFree = negotiatorLock.newCondition();
	
	public ProcessDenseData() {
		gameThread.start();
		gameFSM.setNegotiator(this);
	}

	//TODO: add batch size.
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
				if (counter > 0) gameFSM.unlockFSM();
				counter++;
				System.out.println("Games run: " + counter);
			}
		}

		while (!gameReady); // Wait for the game FSM to return to IDLE.	

		/**** Write to file ****/
		fileOut.storeObjectsOrdered(denseDataBuffer, "denseData.SaveableDenseData", false);

		System.exit(0);
	}

	public void process(Map<Float,SaveableSingleGame> sparseGames, int batchSize) {

		SaveableFileIO<SaveableDenseData> fileOut = new SaveableFileIO<SaveableDenseData>();

		/**** Loop through simulations ****/
		int counter = 0;
		
		Iterator<Map.Entry<Float, SaveableSingleGame>> iter = sparseGames.entrySet().iterator();
		
		for (int i = 0; i < sparseGames.size(); i += batchSize) {
			for (int j = i; j < Math.min(i + batchSize, sparseGames.size()); j++) {
				
				SaveableSingleGame game = iter.next().getValue();
				
				while (!gameReady);
				
				gameReady = false; // Mark the game FSM busy until IDLE is reached again.
				gameFSM.addSequence(game.actions); // Queue up the next games.
				stateBuffer.add(initialState);
				counter++;
				System.out.println("Games run: " + counter + "/" + sparseGames.size());				
			} 

			while (!gameReady); // Wait for the game FSM to return to IDLE.	
			/**** Write to file ****/
			fileOut.storeObjectsOrdered(denseDataBuffer, generateFileName("denseData","SaveableDenseData"), false);
			denseDataBuffer.clear();
			System.out.println("Batch saved.");
		}
	}

	/** Generate a filename. Format is: [prefix]_YYYY-MM-DD_HH-mm-ss.[class name]**/
	public static String generateFileName(String prefix, String className) {
		Date date = new Date() ;
		SimpleDateFormat dateFormat = new SimpleDateFormat("'" + prefix + "_'" + "yyyy-MM-dd_HH-mm-ss" + "'." +  className + "'") ;
		String name = dateFormat.format(date);
		System.out.println("Generated file: " + name);

		return name;
	}
	
	@Override
	public void reportGameStep(Action action) {
		actionBuffer.add(action); // Technically this is the action which GETS us to the current state, so we want it sort of grouped with the previous state since that is when it is applied.
		stateBuffer.add(gameFSM.getGameState());
	}

	@Override
	public void statusChange_Game(Status status) {
		//System.out.println(status.toString());
		switch(status) {
		case FAILED:
			// Collect all the states and actions into a data object.
			SaveableDenseData denseDat = new SaveableDenseData(stateBuffer,actionBuffer);
			if (denseDat.getAction() != null && denseDat.getAction().length > 0) {
				denseDataBuffer.add(denseDat);
			}
			// Clear out for the next run to begin.
			stateBuffer.clear();
			actionBuffer.clear();
			break;
		case IDLE:
			gameReady = true;
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
