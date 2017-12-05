package main;
import java.util.ArrayList;

import data.SaveableDenseData;
import data.SaveableFileIO;
import data.SaveableSingleGame;

/*
 * Negotiates actions between real game and sim.
 * 
 */
public class Negotiator implements INegotiateGame {

	/** Handles building the tree and adding nodes. **/
	FSM_Tree tree;

	/** All visuals and external interaction. **/
	FSM_UI ui;

	/** Communication with the physics engine and entering commands. **/
	FSM_Game game;

	/** Currently pressed keys in game. **/
	boolean Q = false;
	boolean W = false;
	boolean O = false;
	boolean P = false;

	/** Box2D game interface reports all FSM changes. */
	boolean verbose_game = false;

	/** UI window reports all FSM changes. */
	boolean verbose_UI = false;

	/** Tree builder reports all FSM changes. */
	boolean verbose_tree = false;

	/* First new node, after a sequence of tested ones, that we want to sim to. */
	Node requestedNodeToSimTo;

	/* Added node to sim. Continues an existing run. */
	Node requestedContinuedNodeToSim;

	/* Node currently being simmed. */
	Node nodeBeingSimmed;

	/* Are we running games in realtime or fast? */
	boolean runningGameRealtime = false;

	/** Right now, only a single node is used, but in the future, we may have multiple active roots. **/
	ArrayList<Node> activeRoots = new ArrayList<Node>();

	/********* File saving -- sparse *********/
	private boolean save_sparse = false; // Is saving even on? Performance improvement if off.
	private int saveInterval_sparse = 100;
	private int saveCounter_sparse = 0;
	private ArrayList<SaveableSingleGame> saveBuffer_sparse = new ArrayList<SaveableSingleGame>();

	private SaveableFileIO<SaveableSingleGame> fileIOSparse;
	private String saveFileName_sparse;
	private boolean append_sparse = true;

	/********* File saving -- dense **********/
	private boolean save_dense = false; // Warning: produces large large files.
	private int saveInterval_dense = 100;
	private int saveCounter_dense = 0;
	
	private ArrayList<Action> actionBuffer_dense = new ArrayList<Action>();
	private ArrayList<State> stateBuffer_dense = new ArrayList<State>();
	private ArrayList<SaveableDenseData> saveBuffer_dense = new ArrayList<SaveableDenseData>();

	private SaveableFileIO<SaveableDenseData> fileIODense;
	private String saveFileName_dense;
	private boolean append_dense = true;
	
	/** Initial state that the runner starts at. First element of every data object. **/
	private State initialState = FSM_Game.getInitialState();

	/** Basic negotiator which communicates between the tree, ui, and game FSMs. No file saving. **/
	public Negotiator(FSM_Tree tree, FSM_UI ui, FSM_Game game) {
		this.tree = tree;
		this.ui = ui;
		this.game = game;

		save_sparse = false;
		save_dense = false;
	}

	/** Make a negotiator which also saves results to file. Saves the states and actions at transitions, which
	 * can be used to reconstruct a tree.
	 */
	public Negotiator(FSM_Tree tree, FSM_UI ui, FSM_Game game,
			SaveableFileIO<SaveableSingleGame> fileIOSparse, String saveFileName_sparse) {
		this.tree = tree;
		this.ui = ui;
		this.game = game;
		this.fileIOSparse = fileIOSparse;
		this.saveFileName_sparse = saveFileName_sparse;

		save_sparse = true;
		save_dense = false;
	}

	/** Make a negotiator which also saves results to file. 1) Saves the states and actions at transitions, which
	 * can be used to reconstruct a tree. 2) Saves dense results, action and state at every timestep, future used
	 * for learning.
	 */
	public Negotiator(FSM_Tree tree, FSM_UI ui, FSM_Game game,
			SaveableFileIO<SaveableSingleGame> fileIOSparse, String saveFileName_sparse,
			SaveableFileIO<SaveableDenseData> fileIODense, String saveFileName_dense) {
		this.tree = tree;
		this.ui = ui;
		this.game = game;
		this.fileIOSparse = fileIOSparse;
		this.saveFileName_sparse = saveFileName_sparse;

		this.fileIODense = fileIODense;
		this.saveFileName_dense = saveFileName_dense;

		save_sparse = true;
		save_dense = true;
	}

	/***********************************/
	/********** FSM Changes ************/
	/***********************************/
	
	public void statusChange_tree(FSM_Tree.Status status) {
		if (verbose_tree) System.out.println("Tree FSM: " + status);
	
		FSM_Game.Status gameStatus;
	
		switch (status) {
		case IDLE:
			break;
		case INITIALIZE:
			break;
		case TREE_POLICY:
			break;
		case TREE_POLICY_WAITING:
			gameStatus = game.getFSMStatusAndLock(); // Stop the FSM while we do this.
			if (gameStatus == FSM_Game.Status.IDLE){
				game.addSequence(tree.targetNodeToTest.getSequence());
			}else{
				System.out.println("Warning Tree tried to queue a sequence while the game wasn't idle");
				//throw new RuntimeException("Tree tried to queue a sequence while the game wasn't idle. Game was: " + gameStatus.toString());
			}
			game.unlockFSM();
			break;
		case EXPANSION_POLICY:
			break;
		case EXPANSION_POLICY_WAITING:
			gameStatus = game.getFSMStatusAndLock(); // Stop the FSM while we do this.
			if (gameStatus == FSM_Game.Status.WAITING){
				game.actionQueue.clearAll();
				game.addAction(tree.targetNodeToTest.getAction());
			}else if (tree.targetNodeToTest.treeDepth == 1 && gameStatus == FSM_Game.Status.IDLE){ // The case where the tree policy is skipped because we're starting at the root.
				game.actionQueue.clearAll();
				game.addAction(tree.targetNodeToTest.getAction());
			}else {
				throw new RuntimeException("Tree tried to queue another single action while the game wasn't WAITING. Game was: " + game.getFSMStatus().toString());
			}
			game.unlockFSM();
			break;
		case ROLLOUT_POLICY:
			break;
		case ROLLOUT_POLICY_WAITING:
			gameStatus = game.getFSMStatusAndLock(); // Stop the FSM while we do this.
			if (gameStatus == FSM_Game.Status.WAITING){
				game.actionQueue.clearAll();
				game.addAction(tree.targetNodeToTest.getAction());
			}else{
				throw new RuntimeException("Tree tried to queue another single action while the game wasn't WAITING. Game was: " + game.getFSMStatus().toString());
			}
			game.unlockFSM();
			break;
		case EVALUATE_GAME:
			break;
		case EXHAUSTED:
			tree.kill();
			break;
		default:
			break;
		}
	}

	/* (non-Javadoc)
	 * @see main.INegotiator#statusChange_Game(main.FSM_Game.Status)
	 */
	@Override
	public void statusChange_Game(FSM_Game.Status status) {
		if (verbose_game)
			System.out.println("Game FSM: " + status);
		switch (status) {
		case IDLE:
			break;
		case LOCKED:
			break;
		case INITIALIZE:
			if (save_dense) {
				stateBuffer_dense.add(initialState);
			}
			break;
		case RUNNING_SEQUENCE:
			if (game.isRealtime()){
				ui.runnerPane.setWorldToView(game.getWorld());
			}
			break;
		case WAITING:
			// What is the tree doing?
			switch(tree.getFSMStatus()) {
			case TREE_POLICY_WAITING:
				tree.giveGameState(null);
				break;
			case EXPANSION_POLICY_WAITING:
				tree.giveGameState(game.getGameState()); // Give the tree a game for it to 
				break;
			case ROLLOUT_POLICY_WAITING:
				tree.giveGameState(game.getGameState()); // Give the tree a game for it to 
				break;
			default:
				if (game.isRealtime()){
					ui.runnerPane.setWorldToView(null); // If it was a realtime game, turn off vis afterwards.
				}else {
					throw new RuntimeException("Game is waiting, but the tree isn't ready to do more stuff. Tree is in status: " + tree.currentStatus.toString());
				}
			}
			break;
		case FAILED:	
			if (save_dense) {
				// Collect all the states and actions into a data object.
				saveBuffer_dense.add(new SaveableDenseData(stateBuffer_dense,actionBuffer_dense));
				saveCounter_dense++;
				
				if (saveInterval_dense == saveCounter_dense) {
					fileIODense.storeObjectsOrdered(saveBuffer_dense, saveFileName_dense, append_dense);
					saveCounter_dense = 0;
				}
				// Clear out for the next run to begin.
				stateBuffer_dense.clear();
				actionBuffer_dense.clear();
			}
			
			switch(tree.getFSMStatus()) {
			case TREE_POLICY_WAITING:
				throw new RuntimeException("Tree policy should never visit previously found failed states.");
			case EXPANSION_POLICY_WAITING:
				tree.giveGameState(game.getGameState()); // Assign the failed state to this node.
				break;
			case ROLLOUT_POLICY_WAITING:
				tree.giveGameState(game.getGameState()); // Give rollout the state for evaluation.
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}
	}

	public void statusChange_UI(FSM_UI.Status status) {
		if (verbose_UI)
			System.out.println("UI FSM: " + status);
		switch (status) {
		case DRAW_ALL:
			break;
		case IDLE_ALL:
			break;
		case INITIALIZE:
			break;
		case VIEW_RUN:
			break;
		default:
			break;
		}
	}

	/****** Pausers/stoppers *******/
	
	/** Stop EVERYTHING. **/
	public void globalDestruction() {
		game.kill();
		tree.kill();
		ui.kill();
	}
	
	/** Negotiator should keep track of added tree roots **/
	public void addTreeRoot(Node node) {
		activeRoots.add(node);
		ui.rootNodes.add(node);
		tree.rootNodes.add(node);
	}

	/** Save the the run to file. Currently called by the Tree FSM when it is ready. 
	 *  Dense saver piggybacks this to save when triggered also. 
	 */
	public void saveRunToFile(Node leafNode){
		//MAIN_test.tic();
		if (save_sparse) {
			saveBuffer_sparse.add(new SaveableSingleGame(leafNode));
			if (saveCounter_sparse == saveInterval_sparse - 1) {
				fileIOSparse.storeObjectsOrdered(saveBuffer_sparse,
						saveFileName_sparse, append_sparse);
				saveBuffer_sparse.clear();
				saveCounter_sparse = 0;
			}else {
				saveCounter_sparse++;
			}
			//	MAIN_test.toc();
		}
	}

	/** Pause everything but the game things. **/
	public void pauseGame(){
		activeRoots.get(0).calcNodePos_below();
		game.getFSMStatusAndLock();
	}

	public void pauseTree(){
		tree.latchAtFSMStatus(FSM_Tree.Status.IDLE);
	}

	public void unpauseTree(){
		tree.unlockFSM();
	}

	/** Stop an active realtime game (e.g. when a tab change occurs). **/
	public void killRealtimeRun(){
		game.killRealtimeRun();
	}

	/******* UI Commands ********/
	
	public void redistributeNodes(){
		if (Node.useTreePhysics){
			Node.useTreePhysics = false;
			while (Node.stepping);
			activeRoots.get(0).calcNodePos_below();
			activeRoots.get(0).initTreePhys_below();
			Node.useTreePhysics = true;	
		}else{
			activeRoots.get(0).calcNodePos_below();
		}
	}

	/** Node selected by clicking the tree. **/
	public boolean uiNodeSelect(Node node) {
		if (ui.snapshotPane.active) {
	
		} else if (ui.runnerPane.active) {
			if (!game.isRealtime() && node.treeDepth > 0){ // Can't be a root node.
				// Run a one-off, real-time game.
				tree.latchAtFSMStatus(FSM_Tree.Status.IDLE);
				game.runSingleRealtime(node.getSequence());
				return true;
			}else{
				return false; // Lets the UI know that we couldn't run a new pt right now.
			}
		}
		return true;
	}

	/****** Run stats ******/
	
	/** Only doing this to keep all information flowing through negotiator. **/
	public int getGamesPlayed(){ return Node.getCreatedGameCount(); }
	public int getGamesImported(){ return Node.getImportedGameCount(); }
	public int getGamesTotal(){ return Node.getCreatedGameCount() + Node.getImportedGameCount(); }
	public float getTimeSimulated() { return game.getTimeSimulated(); }
	public Action[] getCurrentSequence() { return game.actionQueue.getActionsInCurrentRun(); }
	public int getCurrentActionIdx() { return game.actionQueue.getCurrentActionIdx(); }
	public float getGamesPerSecond() { return tree.currentGPS; }


	/******* Reporting finished stuff *******/
	
	/** Let the game report that it is no longer simulating in real time and the tree may resume. **/
	@Override
	public void reportEndOfRealTimeSim(){
		tree.unlockFSM(); // Resume tree actions.
		ui.runnerPane.clearWorldToView();
	}

	/** Game tells negotiator which keys are down currently. **/
	@Override
	public void reportQWOPKeys(boolean Q, boolean W, boolean O, boolean P) {
		this.Q = Q;
		this.W = W;
		this.O = O;
		this.P = P;
	}

	boolean usePredictor = true;
	Tensorflow_Predictor pred = new Tensorflow_Predictor();
	@Override
	public void reportGameStep(Action action) {
		if (usePredictor) {
			System.out.println(Math.round(pred.getPrediction(game.getGameState())));
		}
		if (save_dense) {
			actionBuffer_dense.add(action); // Technically this is the action which GETS us to the current state, so we want it sort of grouped with the previous state since that is when it is applied.
			State thisState = game.getGameState();
			stateBuffer_dense.add(thisState);
		}
	}

	private boolean toggler = true;
	public void toggleSampler() {
		IEvaluationFunction evaluateRandom = new Evaluator_Random(); // Assigns a purely random score for diagnostics.
		IEvaluationFunction evaluateDistance = new Evaluator_Distance();
		IEvaluationFunction evaluateHandTuned = new Evaluator_HandTunedOnState();
		
		IEvaluationFunction currentEvaluator = evaluateDistance;
		
		/***********************************************/		
		/*********** Tree building strategy ************/
		/***********************************************/
		
		/******** Define how nodes are sampled from the above defined actions. *********/
		ISampler samplerRandom = new Sampler_Random(); // Random sampler does not need a value function as it acts blindly anyway.
		ISampler samplerGreedy = new Sampler_Greedy(currentEvaluator); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
		ISampler samplerUCB = new Sampler_UCB(currentEvaluator); // Upper confidence bound for trees sampler. More principled way of assigning weight for exploration/exploitation.
		if (toggler) {
			tree.changeSampler(samplerGreedy);
			System.out.println("Greedy");
			toggler = !toggler;
		}else {
			tree.changeSampler(samplerUCB);
			System.out.println("UCB");
			toggler = !toggler;
		}
		
	}
}