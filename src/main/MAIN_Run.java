package main;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.beust.jcommander.*;

import TreeStages.TreeStage_FixedGames;
import TreeStages.TreeStage_MaxDepth;
import TreeStages.TreeStage_MinDepth;
import TreeStages.TreeStage_SearchForever;
import distributions.Distribution_Normal;
import distributions.Distribution_Uniform;
import evaluators.Evaluator_Distance;
import evaluators.Evaluator_HandTunedOnState;
import evaluators.Evaluator_Random;
import samplers.Sampler_Deterministic;
import samplers.Sampler_Distribution;
import samplers.Sampler_Greedy;
import samplers.Sampler_Random;
import samplers.Sampler_UCB;
import savers.DataSaver_DenseJava;
import savers.DataSaver_DenseTFRecord;
import savers.DataSaver_Sparse;
import savers.DataSaver_null;
import ui.UI_Full;
import ui.UI_Headless;

class Settings {
	@Parameter
	List<String> parameters = new ArrayList<>();

	@Parameter(names={"--headless", "-hl"},  description = "Run without graphical interface.")
	boolean headless = false;

	@Parameter(names={"--sampler", "-sm"},  description = "Pick how new nodes are selected [random, distribution, greedy, UCB].")
	String sampler = "";

	@Parameter(names={"--saver", "-sv"}, variableArity = true, description = "Pick how data is saved [tfrecord, java_dense, java_sparse, none].")
	List<String> saver = Arrays.asList("none", "200");
}

public class MAIN_Run implements Runnable{

	private int treesPlayed = 0;
	private int treesToPlay = 1000;
	private long secondsPerTree = 1000000;

	private long initTime;

	private Settings settings;


	public MAIN_Run(Settings settings) {
		this.settings = settings;
	}

	public static void main(String[] args) {

		Settings settings = new Settings();
		MAIN_Run manager = new MAIN_Run(settings);
		JCommander.newBuilder()
		.addObject(settings)
		.build()
		.parse(args);

		Thread managerThread = new Thread(manager);
		managerThread.start();
	}

	@Override
	public void run() {

		initTime = System.currentTimeMillis();
		doGames();

		while(true) {
			// Do managey things here.
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void doGames() {

		/********************************************/		
		/******* Space of allowable actions. ********/
		/********************************************/

		/***** Space of allowed actions to sample ******/
		Distribution<Action> uniform_dist = new Distribution_Uniform();

		/********** Repeated action 1 -- no keys pressed. ***********/
		//		Integer[] durations1 = new Integer[]{11,12,13,14};
		
		Integer[] durations1 = IntStream.range(1, 20).boxed().toArray(Integer[] :: new);
		boolean[][] keySet1 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durations1.length);

		//Distribution<Action> dist1 = new Distribution_Uniform();
		Distribution<Action> dist1 = new Distribution_Normal(10f,2f);
		ActionSet actionSet1 = ActionSet.makeActionSet(durations1, keySet1, dist1);

		/**********  Repeated action 2 -- W-O pressed ***********/
		//		Integer[] durations2 = new Integer[]{36,37,38,39,40,41};
		Integer[] durations2 = IntStream.range(30, 60).boxed().toArray(Integer[] :: new);
		boolean[][] keySet2 = ActionSet.replicateKeyString(new boolean[]{false,true,true,false},durations2.length);

		//		Distribution<Action> dist2 = new Distribution_Uniform();
		Distribution<Action> dist2 = new Distribution_Normal(39f,3f);
		ActionSet actionSet2 = ActionSet.makeActionSet(durations2, keySet2, dist2);

		/**********  Repeated action 3 -- W-O pressed ***********/
		//		Integer[] durations3 = new Integer[]{5,6,7,8,9,10,11};
		Integer[] durations3 = IntStream.range(1, 20).boxed().toArray(Integer[] :: new);
		boolean[][] keySet3 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durations3.length);

		//		Distribution<Action> dist3 = new Distribution_Uniform();
		Distribution<Action> dist3 = new Distribution_Normal(10f,2f);
		ActionSet actionSet3 = ActionSet.makeActionSet(durations3, keySet3, dist3);

		/**********  Repeated action 4 -- Q-P pressed ***********/
		//		Integer[] durations4 = new Integer[]{35,36,37,38,39,40};
		Integer[] durations4 = IntStream.range(30, 60).boxed().toArray(Integer[] :: new);
		boolean[][] keySet4 = ActionSet.replicateKeyString(new boolean[]{true,false,false,true},durations4.length);

		//		Distribution<Action> dist4 = new Distribution_Uniform();
		Distribution<Action> dist4 = new Distribution_Normal(39f,3f);
		ActionSet actionSet4 = ActionSet.makeActionSet(durations4, keySet4, dist4);
		ActionSet[] repeatedActions = new ActionSet[] {actionSet1,actionSet2,actionSet3,actionSet4};

		/////// Action Exceptions ////////
		/********** Repeated action exceptions 1 -- no keys pressed. ***********/
		//		Integer[] durationsE1 = new Integer[]{1,2,3,4,5,6};
		Integer[] durationsE1 = IntStream.range(1, 25).boxed().toArray(Integer[] :: new);
		boolean[][] keySetE1 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durationsE1.length);

		//Distribution<Action> distE1 = new Distribution_Uniform();
		Distribution<Action> distE1 = new Distribution_Normal(5f,1f);
		ActionSet actionSetE1 = ActionSet.makeActionSet(durationsE1, keySetE1, distE1);

		/**********  Repeated action exceptions 2 -- W-O pressed ***********/
		//		Integer[] durationsE2 = new Integer[]{30,31,32,33,34,35};
		Integer[] durationsE2 = IntStream.range(20, 50).boxed().toArray(Integer[] :: new);
		boolean[][] keySetE2 = ActionSet.replicateKeyString(new boolean[]{false,true,true,false},durationsE2.length);

		Distribution<Action> distE2 = new Distribution_Normal(34f,2f);
		ActionSet actionSetE2 = ActionSet.makeActionSet(durationsE2, keySetE2, distE2);

		/**********  Repeated action exceptions 3 -- W-O pressed ***********/
		//		Integer[] durationsE3 = new Integer[]{24,25,26,27,28};
		Integer[] durationsE3 = IntStream.range(10, 45).boxed().toArray(Integer[] :: new);
		boolean[][] keySetE3 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durationsE3.length);

		Distribution<Action> distE3 = new Distribution_Normal(24f,2f);
		ActionSet actionSetE3 = ActionSet.makeActionSet(durationsE3, keySetE3, distE3);

		/**********  Repeated action exceptions 4 -- Q-P pressed ***********/
		//		Integer[] durationsE4 = new Integer[]{46,47,48,49,50,51};
		Integer[] durationsE4 = IntStream.range(25, 65).boxed().toArray(Integer[] :: new);
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

		IEvaluationFunction currentEvaluator = evaluateDistance;

		/***********************************************/		
		/*********** Tree building strategy ************/
		/***********************************************/

		/******** Define how nodes are sampled from the above defined actions. *********/
		// Can be picked with command line argument --sampler or -sm
		ISampler currentSampler;
		switch (settings.sampler.toLowerCase()) {
		case "random":
			currentSampler = new Sampler_Random(); // Random sampler does not need a value function as it acts blindly anyway.
			System.out.println("SAMPLER: Using random node sampler.");
			break;
		case "distribution":
			currentSampler = new Sampler_Distribution();
			System.out.println("SAMPLER: Using distribution node sampler.");
			break;
		case "greedy":
			currentSampler = new Sampler_Greedy(currentEvaluator); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
			System.out.println("SAMPLER: Using greedy node sampler.");
			break;
		case "ucb":
			currentSampler = new Sampler_UCB(currentEvaluator); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
			System.out.println("SAMPLER: Using UCB node sampler.");
			break;
		case "deterministic":
			currentSampler = new Sampler_Deterministic();
			System.out.println("SAMPLER: Using deterministic DFS sampler.");
		default:
			currentSampler = new Sampler_UCB(currentEvaluator);
			System.out.println("SAMPLER: Unrecognized argument. Defaulting to UCB.");
		}

		/************************************************************/		
		/******* Decide how datasets are to be saved/loaded. ********/
		/************************************************************/

		// Can be picked with command line argument --sampler or -sm
		IDataSaver dataSaver;
		switch (settings.saver.get(0).toLowerCase()) { // Defaults to "" if no saver settings were passed.
		case "tfrecord":
			dataSaver = new DataSaver_DenseTFRecord(); // Saves full state/action info, in Tensorflow-compatible TFRecord format.
			System.out.println("SAVER: Using TFRecord data saver, densely storing state data. Frequency: " + settings.saver.get(1) + " games/save.");
			break;
		case "java_sparse":
			dataSaver = new DataSaver_Sparse(); // Saves just actions needed to recreate runs.
			System.out.println("SAVER: Using serialized Java class data saver, sparsely storing state data. Frequency: " + settings.saver.get(1) + " games/save.");
			break;
		case "java_dense":
			dataSaver = new DataSaver_DenseJava(); // Saves full state/action info, but in serialized java classes. // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
			System.out.println("SAVER: Using serialized Java class data saver, densely storing state data. Frequency: " + settings.saver.get(1) + " games/save.");
			break;
		case "none":
			dataSaver = new DataSaver_null(); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
			System.out.println("SAVER: Not saving data to file. Frequency: " + settings.saver.get(1) + " games/save.");
			break;
		default:
			System.out.println("SAVER: Unrecognized argument. Defaulting to no saving.");
			dataSaver = new DataSaver_null();

		}

		dataSaver.setSaveInterval(Integer.parseInt(settings.saver.get(1))); // set save frequency from parsed args if that argument has been input.
		//ArrayList<SaveableSingleGame> loaded = io_sparse.loadObjectsOrdered("test_2017-10-25_16-25-38.SaveableSingleGame");

		/************************************************************/		
		/**************** Pick user interface. **********************/
		/************************************************************/
		Node treeRoot = new Node();
		IUserInterface ui;
		if (settings.headless) {
			ui = new UI_Headless();
			System.out.println("GUI: Running in headless mode.");
		}else {
			ui = new UI_Full();
			System.out.println("GUI: Running in full graphics mode.");
		}

		// Root to visualize from.
		ui.addRootNode(treeRoot);

		/************************************************************/		
		/**************** Assign workers/threads ********************/
		/************************************************************/

		// Worker threads to run. Each worker independently explores the tree and has its own loaded copy of the Box2D libraries.
		int cores = Runtime.getRuntime().availableProcessors();
		int numWorkers = (int)(0.65f*cores); // Basing of number of cores including hyperthreading. May want to optimize this a tad.
		System.out.println("Detected " + cores + " physical cores. Making " + numWorkers + " workers.");

		Thread uiThread = new Thread(ui);
		uiThread.start();
		System.out.println("All initialized.");
		
		
		// MATT
		// MAKE A RANDOM SAMPLER WHICH ONLY GOES TO A FIXED DEPTH
		
		// Searches are divided into stages now, allowing for multiple objectives and searches in a single run.
		// Here is the searchForever -- the equivalent to the previous code.
		//TreeStage searchFixed = new TreeStage_FixedGames(currentSampler.clone(),1000l);
//		TreeStage searchForever = new TreeStage_SearchForever(new Sampler_Random());
		
		System.out.println("Starting stage 1.");
		TreeStage searchMax = new TreeStage_MaxDepth(currentSampler.clone(), 14); // Depth to get to sorta steady state.
		searchMax.initialize(treeRoot, numWorkers);
		List<Node> deepNode = searchMax.getResults(); // Should only return the one node way out there.
		Node expNode = deepNode.get(0).parent.parent; // Node at depth 12
		System.out.println("Stage 1 done.");
		
		System.out.println("Starting stage 2.");
		TreeStage searchMin = new TreeStage_MinDepth(new Sampler_Random(), 2); // Two actions to get weird.
		expNode.sweepAngle = (float)(Math.PI*1.);
		expNode.calcNodePos_below();
		searchMin.initialize(expNode, numWorkers); // Start from where the last stage stopped deep in the tree.
		List<Node> crazyNodes = searchMin.getResults(); // Should only return the one node way out there.
		System.out.println("Stage 2 done.");
		
		System.out.println("Starting stage 3. " + crazyNodes.size() + " nodes to expand.");
		int count = 0;
		for (Node n : crazyNodes) {
			System.out.println("Beginning node " + ++count + ".");
			n.overrideNodeColor = Color.RED;
			n.displayPoint = true;
			TreeStage searchRecovery = new TreeStage_MaxDepth(currentSampler.clone(), 4);
			
			//if (count < crazyNodes.size()) searchRecovery.blocking = false;
			searchRecovery.initialize(n, 4); // Start from where the last stage stopped deep in the tree.
		}

//		
//		System.out.println("Starting stage 2.");
//		searchFixed.initialize(treeRoot, numWorkers);
//		System.out.println("Starting stage 3.");
//		searchForever.initialize(treeRoot, numWorkers);



	}
}
