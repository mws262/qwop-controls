package main;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.beust.jcommander.*;

import TreeStages.TreeStage_FixedGames;
import TreeStages.TreeStage_MaxDepth;
import TreeStages.TreeStage_MinDepth;
import TreeStages.TreeStage_SearchForever;
import data.SaveableFileIO;
import data.SaveableSingleGame;
import distributions.Distribution_Normal;
import distributions.Distribution_Uniform;
import evaluators.Evaluator_Distance;
import evaluators.Evaluator_HandTunedOnState;
import evaluators.Evaluator_Random;
import samplers.Sampler_Deterministic;
import samplers.Sampler_Distribution;
import samplers.Sampler_FixedDepth;
import samplers.Sampler_Greedy;
import samplers.Sampler_Random;
import samplers.Sampler_UCB;
import savers.DataSaver_DenseJava;
import savers.DataSaver_DenseTFRecord;
import savers.DataSaver_Sparse;
import savers.DataSaver_StageSelected;
import savers.DataSaver_Null;
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

		Integer[] durations1 = IntStream.range(1, 25).boxed().toArray(Integer[] :: new);
		boolean[][] keySet1 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durations1.length);

		//Distribution<Action> dist1 = new Distribution_Uniform();
		Distribution<Action> dist1 = new Distribution_Normal(10f,2f);
		ActionSet actionSet1 = ActionSet.makeActionSet(durations1, keySet1, dist1);

		/**********  Repeated action 2 -- W-O pressed ***********/
		//		Integer[] durations2 = new Integer[]{36,37,38,39,40,41};
		Integer[] durations2 = IntStream.range(20, 60).boxed().toArray(Integer[] :: new);
		boolean[][] keySet2 = ActionSet.replicateKeyString(new boolean[]{false,true,true,false},durations2.length);

		//		Distribution<Action> dist2 = new Distribution_Uniform();
		Distribution<Action> dist2 = new Distribution_Normal(39f,3f);
		ActionSet actionSet2 = ActionSet.makeActionSet(durations2, keySet2, dist2);

		/**********  Repeated action 3 -- W-O pressed ***********/
		//		Integer[] durations3 = new Integer[]{5,6,7,8,9,10,11};
		Integer[] durations3 = IntStream.range(1, 25).boxed().toArray(Integer[] :: new);
		boolean[][] keySet3 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durations3.length);

		//		Distribution<Action> dist3 = new Distribution_Uniform();
		Distribution<Action> dist3 = new Distribution_Normal(10f,2f);
		ActionSet actionSet3 = ActionSet.makeActionSet(durations3, keySet3, dist3);

		/**********  Repeated action 4 -- Q-P pressed ***********/
		//		Integer[] durations4 = new Integer[]{35,36,37,38,39,40};
		Integer[] durations4 = IntStream.range(20, 60).boxed().toArray(Integer[] :: new);
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

		//////////////////////////////

		Integer[] durationsE15 = IntStream.range(1, 50).boxed().toArray(Integer[] :: new);
		boolean[][] keySetE15 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durationsE15.length);

		//Distribution<Action> dist1 = new Distribution_Uniform();
		Distribution<Action> distE15 = new Distribution_Normal(10f,2f);
		ActionSet actionSetE15 = ActionSet.makeActionSet(durationsE15, keySetE15, distE15);

		/**********  Repeated action 2 -- W-O pressed ***********/
		//		Integer[] durations2 = new Integer[]{36,37,38,39,40,41};
		Integer[] durationsE16 = IntStream.range(10, 70).boxed().toArray(Integer[] :: new);
		boolean[][] keySetE16 = ActionSet.replicateKeyString(new boolean[]{true,false,false,true},durationsE16.length);

		//		Distribution<Action> dist2 = new Distribution_Uniform();
		Distribution<Action> distE16 = new Distribution_Normal(39f,3f);
		ActionSet actionSetE16 = ActionSet.makeActionSet(durationsE16, keySetE16, distE16);

		/**********  Repeated action 3 -- W-O pressed ***********/
		//		Integer[] durations3 = new Integer[]{5,6,7,8,9,10,11};
		Integer[] durationsE17 = IntStream.range(1, 50).boxed().toArray(Integer[] :: new);
		boolean[][] keySetE17 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durationsE17.length);

		//		Distribution<Action> dist3 = new Distribution_Uniform();
		Distribution<Action> distE17 = new Distribution_Normal(10f,2f);
		ActionSet actionSetE17 = ActionSet.makeActionSet(durationsE17, keySetE17, distE17);

		/**********  Repeated action 4 -- Q-P pressed ***********/
		//		Integer[] durations4 = new Integer[]{35,36,37,38,39,40};
		Integer[] durationsE18 = IntStream.range(10, 70).boxed().toArray(Integer[] :: new);
		boolean[][] keySetE18 = ActionSet.replicateKeyString(new boolean[]{false,true,true,false},durationsE18.length);

		//		Distribution<Action> dist4 = new Distribution_Uniform();
		Distribution<Action> distE18 = new Distribution_Normal(39f,3f);
		ActionSet actionSetE18 = ActionSet.makeActionSet(durationsE18, keySetE18, distE18);


		Map<Integer,ActionSet> actionExceptions = new HashMap<Integer,ActionSet>();
		actionExceptions.put(0, actionSetE1);
		actionExceptions.put(1, actionSetE2);
		actionExceptions.put(2, actionSetE3);
		actionExceptions.put(3, actionSetE4);

		actionExceptions.put(14, actionSetE15); // Are these indices right?
		actionExceptions.put(15, actionSetE16);
		actionExceptions.put(16, actionSetE17);
		actionExceptions.put(17, actionSetE18);


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
			dataSaver = new DataSaver_Null(); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
			System.out.println("SAVER: Not saving data to file. Frequency: " + settings.saver.get(1) + " games/save.");
			break;
		default:
			System.out.println("SAVER: Unrecognized argument. Defaulting to no saving.");
			dataSaver = new DataSaver_Null();

		}

		dataSaver.setSaveInterval(Integer.parseInt(settings.saver.get(1))); // set save frequency from parsed args if that argument has been input.
		//ArrayList<SaveableSingleGame> loaded = io_sparse.loadObjectsOrdered("test_2017-10-25_16-25-38.SaveableSingleGame");

		/************************************************************/		
		/**************** Pick user interface. **********************/
		/************************************************************/
		Node treeRoot = new Node();
		IUserInterface ui;
		//settings.headless = false;
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


		// Make a new folder for this trial.
		File saveLoc = new File("./4_9_18");
		if (!saveLoc.exists()) {
			boolean success = saveLoc.mkdir();
			if (!success) throw new RuntimeException("Could not make save directory.");
		}


		boolean doStage1 = false;
		boolean doStage2 = false;
		boolean doStage3 = true;
		boolean doStage4 = false;

		// This stage generates the nominal gait. Roughly gets us to steady-state. Saves this 1 run to a file.
		if (doStage1) {
			System.out.println("Starting stage 1.");
			// Saver setup.
			DataSaver_StageSelected saver = new DataSaver_StageSelected();
			saver.overrideFilename = "steadyRunPrefix";
			saver.setSavePath(saveLoc.getPath() + "/");

			TreeStage searchMax = new TreeStage_MaxDepth(14, currentSampler.clone(), saver); // Depth to get to sorta steady state.
			searchMax.initialize(treeRoot, numWorkers);
			System.out.println("Stage 1 done.");
		}

		// This stage generates deviations from nominal. Load nominal gait. Do not allow expansion near the root. Expand to a fixed, small depth.
		if (doStage2) {
			System.out.println("Starting stage 2.");

			// Saver setup.
			DataSaver_StageSelected saver = new DataSaver_StageSelected();
			saver.overrideFilename = "deviations";
			saver.setSavePath(saveLoc.getPath() + "/");	

			Node rootNode = new Node();
			ui.clearRootNodes();
			ui.addRootNode(rootNode);

			TreeStage searchMin = new TreeStage_MinDepth(2, new Sampler_FixedDepth(2), saver); // Two actions to get weird.
			SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
			Node.makeNodesFromRunInfo(fileIO.loadObjectsOrdered(saveLoc.getPath() + "/steadyRunPrefix.SaveableSingleGame"), rootNode, 11);
			Node currNode = rootNode;
			while (currNode.treeDepth < 12) {
				currNode = currNode.children.get(0);
			}
			searchMin.initialize(currNode, 8);
			System.out.println("Stage 2 done.");
		}

		if (doStage3) {
			System.out.println("Starting stage 3.");

			// Saver setup.
			Node rootNode = new Node();
			ui.clearRootNodes();
			ui.addRootNode(rootNode);

			SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
			Node.makeNodesFromRunInfo(fileIO.loadObjectsOrdered(saveLoc.getPath() + "/deviations.SaveableSingleGame"), rootNode, 11);
			List<Node> leafList = new ArrayList<Node>();
			rootNode.getLeaves(leafList);

			int count = 0;
			int startAt = 150;
			for (Node leaf : leafList) {
				if (count >= startAt) {
					DataSaver_StageSelected saver = new DataSaver_StageSelected();
					saver.overrideFilename = "recoveries" + count;
					saver.setSavePath(saveLoc.getPath() + "/");

					TreeStage searchMax = new TreeStage_MaxDepth(16, new Sampler_UCB(new Evaluator_Distance()), saver); // Depth to get to sorta steady state.
					System.out.print("Started " + count + "...");
					searchMax.initialize(leaf, 30);
				}
				// Turn off drawing for this one.
				leaf.turnOffBranchDisplay();
				leaf.parent.children.remove(leaf);
				System.out.println(" finished.");
				count++;
			}

			System.out.println("Stage 3 done.");
		}

		//		if (doStage4) {
		//			File[] savedRecoveries = saveLoc.listFiles();
		//			for (File f : savedRecoveries) {
		//				if (f.getName().toLowerCase().contains("recoveries")){
		//					SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
		//					Node.makeNodesFromRunInfo(fileIO.loadObjectsOrdered(saveLoc.getPath() + "/deviations.SaveableSingleGame"), rootNode, 11);
		//					
		//					
		//					
		//					
		//				}
		//			}
		//		}
	}
}
