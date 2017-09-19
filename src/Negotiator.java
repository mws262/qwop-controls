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
	boolean verbose_game = false;

	/** UI window reports all FSM changes. */
	boolean verbose_UI = false;

	/** Tree builder reports all FSM changes. */
	boolean verbose_tree = false;

	QWOP_fileIO<CondensedRunInfo> fileIO;

	/* First new node, after a sequence of tested ones, that we want to sim to. */
	TrialNodeMinimal requestedNodeToSimTo;

	/* Added node to sim. Continues an existing run. */
	TrialNodeMinimal requestedContinuedNodeToSim;

	/* Node currently being simmed. */
	TrialNodeMinimal nodeBeingSimmed;

	/* Are we running games in realtime or fast? */
	boolean runningGameRealtime = false;

	private String saveFileName;
	private boolean saveToFile = true;
	private boolean append = true;

	ArrayList<TrialNodeMinimal> activeRoots = new ArrayList<TrialNodeMinimal>();

	public int gamesPlayed = 0;

	public Negotiator(FSM_Tree tree, FSM_UI ui, FSM_Game game,
			QWOP_fileIO<CondensedRunInfo> fileIO, String saveFileName,
			boolean appendToExistingFile) {
		this.tree = tree;
		this.ui = ui;
		this.game = game;
		this.fileIO = fileIO;
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
		TrialNodeMinimal.useTreePhysics = false;
		while (TrialNodeMinimal.stepping);
		System.out.println("Activate");
		activeRoots.get(0).calcNodePos_below();
		activeRoots.get(0).initTreePhys_below();
		TrialNodeMinimal.useTreePhysics = true;
	}

	/***********************************/
	/********** FSM Changes ************/
	/***********************************/

	/**** For the actual tree ****/
	public void statusChange_tree(FSM_Tree.Status status) {
		if (verbose_tree)
			System.out.println("Tree FSM: " + status);
		switch (status) {
		case ADD_NODE:
			break;
		case DO_PREDETERMINED:
			break;
		case EVALUATE_GAME:
			break;
		case EXHAUSTED:
			tree.kill();
			break;
		case WAITING_FOR_PREDETERMINED:
			FSM_Game.Status gameStatusPredet = game.getFSMStatusAndLock(); // Stop the FSM while we do this.
			if (gameStatusPredet == FSM_Game.Status.IDLE){
				game.addSequence_old(tree.targetNodeToTest.getSequence(),false);
			}else{
				throw new RuntimeException("Tree tried to queue a predetermined sequence while the game wasn't idle.");
			}
			game.unlockFSM();
			
			break;
		case WAITING_FOR_SINGLE:
			FSM_Game.Status gameStatusSingle = game.getFSMStatusAndLock(); // Stop the FSM while we do this.
			if (gameStatusSingle == FSM_Game.Status.WAITING){
				game.addAction_old(tree.targetNodeToTest.controlAction);
			}else{
				throw new RuntimeException("Tree tried to queue another single action while the game wasn't WAITING. Game was: " + game.getFSMStatus().toString());
			}
			game.unlockFSM();
			break;
		case IDLE:
			
			break;
		case INITIALIZE_TREE:
			//activeRoots.get(0).stepTreePhys(1000);
			break;
		default:
			break;
		}
	}

	/** Negotiator should keep track of added tree roots **/
	public void addTreeRoot(TrialNodeMinimal node) {
		activeRoots.add(node);
		ui.rootNodes.add(node);
		tree.rootNodes.add(node);
	}
	
	/** Save the the run to file. Currently called by the Tree FSM when it is ready. **/
	public void saveRunToFile(TrialNodeMinimal leafNode){
		if (saveToFile)
			fileIO.storeObjects(new CondensedRunInfo(leafNode),
					saveFileName, append);
	}

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
	public void uiNodeSelect(TrialNodeMinimal node) {
		if (ui.snapshotPane.active) {

		} else if (ui.runnerPane.active) {
			// Run a one-off, real-time game.
			System.out.println(node.treeDepth);
			tree.latchAtFSMStatus(FSM_Tree.Status.IDLE);
			game.runSingleRealtime(node.getSequence());
			//game.addSequence_old(node.getSequence(), true);
		}
	}
	
	/** Let the game report that it is no longer simulating in real time and the tree may resume. **/
	public void reportEndOfRealTimeSim(){
		System.out.println("JH");
		tree.unlockFSM(); // Resume tree actions.
		
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
			FSM_Tree.Status treeStatusWaiting = tree.getFSMStatus();
			
			if (treeStatusWaiting == FSM_Tree.Status.WAITING_FOR_PREDETERMINED
			|| treeStatusWaiting == FSM_Tree.Status.WAITING_FOR_SINGLE){
				tree.giveGameState(game.getGameState()); // Give the tree a game for it to 
			}else if (game.isRealtime()){
				ui.runnerPane.setWorldToView(null); // If it was a realtime game, turn of vis afterwards.
			}else{
				throw new RuntimeException("Game is waiting, but the tree isn't ready to do more stuff. Tree is in status: " + tree.currentStatus.toString());
			}
			break;
		case FAILED:
			FSM_Tree.Status treeStatusFailed = tree.getFSMStatus();
			
			if (treeStatusFailed == FSM_Tree.Status.WAITING_FOR_PREDETERMINED
			|| treeStatusFailed == FSM_Tree.Status.WAITING_FOR_SINGLE){
				tree.giveGameState(game.getGameState()); // Give the tree a game for it to 
			}else{
				// Could also be ending a realtime run. Eliminated the exception for now.
				//throw new RuntimeException("Game is failed, but the tree isn't ready to do more stuff. Tree is in status: " + tree.currentStatus.toString());
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