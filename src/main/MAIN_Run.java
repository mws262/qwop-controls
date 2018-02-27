package main;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.SaveableDenseData;
import data.SaveableFileIO;
import data.SaveableSingleGame;
import com.beust.jcommander.*;

public class MAIN_Run implements Runnable{

	private static long ticTime;
	private static long tocTime;

	private static final boolean useTreePhysics = false;
	private static Negotiator negotiator;

	private int treesPlayed = 0;
	private int treesToPlay = 1000;
	private long secondsPerTree = 1000000;

	private long initTime;

	private String filePrefix = "sample1";

	/***** Command line arguments to parse. *****/
	@Parameter
	private List<String> parameters = new ArrayList<>();

	@Parameter(names={"--headless", "-hl"},  description = "Run without graphical interface.")
	private boolean headless = false;

	@Parameter(names={"--sampler", "-sm"},  description = "Pick how new nodes are selected [random, distribution, greedy, UCB].")
	private String sampler = "";
	
	@Parameter(names={"--saver", "-sv"},  description = "Pick how data is saved [tfrecord, java_dense, java_sparse, none].")
	private String saver = "";


	public MAIN_Run() {}

	public static void main(String[] args) {

		MAIN_Run manager = new MAIN_Run();

		JCommander.newBuilder()
		.addObject(manager)
		.build()
		.parse(args);

		Thread managerThread = new Thread(manager);
		managerThread.start();
	}

	@Override
	public void run() {
		while(true) {

			if (negotiator == null) {
				initTime = System.currentTimeMillis();
				doGames(filePrefix);
			}

			if ((System.currentTimeMillis() - initTime)/1000 >= secondsPerTree) {
				negotiator.globalDestruction();
				Node.maxDepthYet = 0;
				negotiator = null;
				treesPlayed++;
				if (treesPlayed >= treesToPlay) System.exit(0);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void doGames(String filePrefix) {

		/********************************************/		
		/******* Space of allowable actions. ********/
		/********************************************/

		/***** Space of allowed actions to sample ******/
		Distribution<Action> uniform_dist = new Distribution_Uniform();

		/********** Repeated action 1 -- no keys pressed. ***********/
		Integer[] durations1 = new Integer[]{5,6,7,8,9,10,11,12,13,14,15,16,17,18};
		boolean[][] keySet1 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durations1.length);

		//Distribution<Action> dist1 = new Distribution_Uniform();
		Distribution<Action> dist1 = new Distribution_Normal(10f,2f);
		ActionSet actionSet1 = ActionSet.makeActionSet(durations1, keySet1, dist1);

		/**********  Repeated action 2 -- W-O pressed ***********/
		Integer[] durations2 = new Integer[]{30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45};
		boolean[][] keySet2 = ActionSet.replicateKeyString(new boolean[]{false,true,true,false},durations2.length);

		//		Distribution<Action> dist2 = new Distribution_Uniform();
		Distribution<Action> dist2 = new Distribution_Normal(39f,3f);
		ActionSet actionSet2 = ActionSet.makeActionSet(durations2, keySet2, dist2);

		/**********  Repeated action 3 -- W-O pressed ***********/
		Integer[] durations3 = new Integer[]{5,6,7,8,9,10,11,12,13,14,15,16,17,18};
		boolean[][] keySet3 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durations3.length);

		//		Distribution<Action> dist3 = new Distribution_Uniform();
		Distribution<Action> dist3 = new Distribution_Normal(10f,2f);
		ActionSet actionSet3 = ActionSet.makeActionSet(durations3, keySet3, dist3);

		/**********  Repeated action 4 -- Q-P pressed ***********/
		Integer[] durations4 = new Integer[]{30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45};
		boolean[][] keySet4 = ActionSet.replicateKeyString(new boolean[]{true,false,false,true},durations4.length);

		//		Distribution<Action> dist4 = new Distribution_Uniform();
		Distribution<Action> dist4 = new Distribution_Normal(39f,3f);
		ActionSet actionSet4 = ActionSet.makeActionSet(durations4, keySet4, dist4);

		ActionSet[] repeatedActions = new ActionSet[] {actionSet1,actionSet2,actionSet3,actionSet4};


		/////// Action Exceptions ////////
		/********** Repeated action exceptions 1 -- no keys pressed. ***********/
		Integer[] durationsE1 = new Integer[]{1,2,3,4,5,6,7,8,9,10};
		boolean[][] keySetE1 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durationsE1.length);

		//Distribution<Action> distE1 = new Distribution_Uniform();
		Distribution<Action> distE1 = new Distribution_Normal(5f,1f);
		ActionSet actionSetE1 = ActionSet.makeActionSet(durationsE1, keySetE1, distE1);

		/**********  Repeated action exceptions 2 -- W-O pressed ***********/
		Integer[] durationsE2 = new Integer[]{27,28,29,30,31,32,33,34,35,36,37,38,39};
		boolean[][] keySetE2 = ActionSet.replicateKeyString(new boolean[]{false,true,true,false},durationsE2.length);

		Distribution<Action> distE2 = new Distribution_Normal(34f,2f);
		ActionSet actionSetE2 = ActionSet.makeActionSet(durationsE2, keySetE2, distE2);

		/**********  Repeated action exceptions 3 -- W-O pressed ***********/
		Integer[] durationsE3 = new Integer[]{20,21,22,23,24,25,26,27,28,29,30,31,32};
		boolean[][] keySetE3 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durationsE3.length);

		Distribution<Action> distE3 = new Distribution_Normal(24f,2f);
		ActionSet actionSetE3 = ActionSet.makeActionSet(durationsE3, keySetE3, distE3);

		/**********  Repeated action exceptions 4 -- Q-P pressed ***********/
		Integer[] durationsE4 = new Integer[]{45,46,47,48,49,50,51,52,53,54,55};
		boolean[][] keySetE4 = ActionSet.replicateKeyString(new boolean[]{true,false,false,true},durationsE4.length);

		Distribution<Action> distE4 = new Distribution_Normal(49f,2f);
		ActionSet actionSetE4 = ActionSet.makeActionSet(durationsE4, keySetE4, distE4);

		Map<Integer,ActionSet> actionExceptions = new HashMap<Integer,ActionSet>();
		actionExceptions.put(0, actionSetE1);
		actionExceptions.put(1, actionSetE2);
		actionExceptions.put(2, actionSetE3);
		actionExceptions.put(3, actionSetE4);


		// Define the specific way that these allowed actions are assigned as potential options for nodes.
		IActionGenerator actionGenerator = new ActionGenerator_FixedSequence(repeatedActions, actionExceptions);
		Node.potentialActionGenerator = actionGenerator;

		/****************************************/		
		/*********** Node evaluation ************/
		/****************************************/

		IEvaluationFunction evaluateRandom = new Evaluator_Random(); // Assigns a purely random score for diagnostics.
		IEvaluationFunction evaluateDistance = new Evaluator_Distance();
		IEvaluationFunction evaluateHandTuned = new Evaluator_HandTunedOnState();

		IEvaluationFunction currentEvaluator = evaluateHandTuned;

		/***********************************************/		
		/*********** Tree building strategy ************/
		/***********************************************/

		/******** Define how nodes are sampled from the above defined actions. *********/
		// Can be picked with command line argument --sampler or -sm
		ISampler currentSampler;
		switch (sampler.toLowerCase()) {
		case "random":
			currentSampler = new Sampler_Random(); // Random sampler does not need a value function as it acts blindly anyway.
			break;
		case "distribution":
			currentSampler = new Sampler_Distribution();
			break;
		case "greedy":
			currentSampler = new Sampler_Greedy(currentEvaluator); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
			break;
		case "ucb":
			currentSampler = new Sampler_UCB(currentEvaluator); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
			break;
		default:
			currentSampler = new Sampler_UCB(currentEvaluator);
		}

		/************************************************************/		
		/******* Decide how datasets are to be saved/loaded. ********/
		/************************************************************/

		// Can be picked with command line argument --sampler or -sm
		IDataSaver dataSaver;
		switch (saver.toLowerCase()) {
		case "tfrecord":
			dataSaver = new DataSaver_DenseTFRecord(); // Saves full state/action info, in Tensorflow-compatible TFRecord format.
			break;
		case "java_sparse":
			dataSaver = new DataSaver_Sparse(); // Saves just actions needed to recreate runs.
			break;
		case "java_dense":
			dataSaver = new DataSaver_DenseJava(); // Saves full state/action info, but in serialized java classes. // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
			break;
		case "none":
			dataSaver = new DataSaver_null(); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
			break;
		default:
			dataSaver = new DataSaver_null();
		}

		dataSaver.setSaveInterval(500);

		//ArrayList<SaveableSingleGame> loaded = io_sparse.loadObjectsOrdered("test_2017-10-25_16-25-38.SaveableSingleGame");

		//Node treeRoot = Node.makeNodesFromRunInfo(loaded, false);
		// If we don't want to append to an existing file, we should clear out anything existing with this name.
		//		clearExistingFile(sparseFileName);
		//		clearExistingFile(denseFileName);

		Node treeRoot = new Node(useTreePhysics);

		IUserInterface ui;
		if (headless) {
			ui = new UI_Headless();
		}else {
			ui = new UI_Full();
		}

		FSM_Tree tree = new FSM_Tree(currentSampler);
		FSM_Game game = new FSM_Game();

		/* Manage the tree, UI, and game. Start some threads. */
		negotiator = new Negotiator(tree, ui, game);

		//		negotiator.addDataSaver(sparseSaver);
		//		negotiator.addDataSaver(denseSaver);
		negotiator.addDataSaver(dataSaver);


		tree.setNegotiator(negotiator);
		ui.setNegotiator(negotiator);
		game.setNegotiator(negotiator);
		negotiator.addTreeRoot(treeRoot);

		Thread treeThread = new Thread(tree);
		Thread uiThread = new Thread(ui);
		Thread gameThread = new Thread(game); 
		//uiThread.setPriority(Thread.MAX_PRIORITY);

		/* Start processes */
		gameThread.start();
		uiThread.start();
		treeThread.start();

		System.out.println("All initialized.");
	}

	/** Matlab tic and toc functionality. **/
	public static void tic(){
		ticTime = System.nanoTime();
	}
	public static long toc(){
		tocTime = System.nanoTime();
		long difference = tocTime - ticTime;
		if (difference < 1000000000){
			System.out.println(Math.floor(difference/10000)/100 + " ms elapsed.");
		}else{
			System.out.println(Math.floor(difference/100000000.)/10. + " s elapsed.");
		}
		return difference;
	}

	/** Clear out an existing file. **/
	public static void clearExistingFile(String fileName) {
		File file = new File(fileName);
		try {
			boolean result = Files.deleteIfExists(file.toPath());
			if (result) System.out.println("Cleared file: " + file.getName());
		} catch (IOException e) {
			e.printStackTrace();
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
}
