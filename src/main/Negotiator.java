package main;
import java.util.ArrayList;

/*
 * Negotiates actions between real game and sim.
 * 
 */
public class Negotiator {

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
	boolean verbose_game = true;

	/** UI window reports all FSM changes. */
	boolean verbose_UI = false;

	/** Tree builder reports all FSM changes. */
	boolean verbose_tree = true;

	SaveableFileIO<SaveableSingleGame> saveableFileIO;

	/* First new node, after a sequence of tested ones, that we want to sim to. */
	Node requestedNodeToSimTo;

	/* Added node to sim. Continues an existing run. */
	Node requestedContinuedNodeToSim;

	/* Node currently being simmed. */
	Node nodeBeingSimmed;

	/* Are we running games in realtime or fast? */
	boolean runningGameRealtime = false;

	private String saveFileName;
	private boolean saveToFile = true;
	private boolean append = true;

	ArrayList<Node> activeRoots = new ArrayList<Node>();

	public Negotiator(FSM_Tree tree, FSM_UI ui, FSM_Game game,
			SaveableFileIO<SaveableSingleGame> fileIO, String saveFileName,
			boolean appendToExistingFile) {
		this.tree = tree;
		this.ui = ui;
		this.game = game;
		this.saveableFileIO = fileIO;
		this.saveFileName = saveFileName;
		append = appendToExistingFile;
	}

	public Negotiator(FSM_Tree tree, FSM_UI ui, FSM_Game game) {
		this.tree = tree;
		this.ui = ui;
		this.game = game;

		saveToFile = false;
	}

	/**
	 * Change the file we are writing tree data to. Also turn saving on if it
	 * was previously off.
	 **/
	public void changeSaveFile(String fileName, boolean appendToExisting) {
		append = appendToExisting;
		saveFileName = fileName;
		saveToFile = true;
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

	/***********************************/
	/********** FSM Changes ************/
	/***********************************/

	/**** For the actual tree ****/
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
				throw new RuntimeException("Tree tried to queue a sequence while the game wasn't idle.");
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

	/** Negotiator should keep track of added tree roots **/
	public void addTreeRoot(Node node) {
		activeRoots.add(node);
		ui.rootNodes.add(node);
		tree.rootNodes.add(node);
	}

	/** Save the the run to file. Currently called by the Tree FSM when it is ready. **/
	public void saveRunToFile(Node leafNode){
		if (saveToFile)
			saveableFileIO.storeObjects(new SaveableSingleGame(leafNode),
					saveFileName, append);
	}

	/** Stop an active realtime game (e.g. when a tab change occurs). **/
	public void killRealtimeRun(){
		game.killRealtimeRun();
	}

	/** Only doing this to keep all information flowing through negotiator. **/
	public int getGamesPlayed(){ return Node.getCreatedGameCount(); }
	public int getGamesImported(){ return Node.getImportedGameCount(); }
	public int getGamesTotal(){ return Node.getCreatedGameCount() + Node.getImportedGameCount(); }
	public float getTimeSimulated() { return game.getTimeSimulated(); }
	public Action[] getCurrentSequence() { return game.actionQueue.getActionsInCurrentRun(); }
	public int getCurrentActionIdx() { return game.actionQueue.getCurrentActionIdx(); }
	public float getGamesPerSecond() { return tree.currentGPS; }
	/**** For the visualization of the tree. ****/
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

	/** Let the game report that it is no longer simulating in real time and the tree may resume. **/
	public void reportEndOfRealTimeSim(){
		tree.unlockFSM(); // Resume tree actions.
		ui.runnerPane.clearWorldToView();
	}

	/**** For the QWOP game. ****/
	public void statusChange_Game(FSM_Game.Status status) {
		if (verbose_game)
			System.out.println("Game FSM: " + status);
		switch (status) {
		case IDLE:
			break;
		case LOCKED:
			break;
		case INITIALIZE:
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

	/** Game tells negotiator which keys are down currently. **/
	public void reportQWOPKeys(boolean Q, boolean W, boolean O, boolean P) {
		this.Q = Q;
		this.W = W;
		this.O = O;
		this.P = P;
	}
}