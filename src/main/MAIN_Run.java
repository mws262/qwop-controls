package main;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import data.SaveableDenseData;
import data.SaveableFileIO;
import data.SaveableSingleGame;


public class MAIN_Run {

	private static long ticTime;
	private static long tocTime;
	
	private static final boolean useTreePhysics = false;
	private static final boolean saveGamesToFile = true;
	
	public MAIN_Run() {}

	public static void main(String[] args) {
	
		/********************************************************************/		
		/******* Decide the rules for picking child actions to test. ********/
		/********************************************************************/
		
		/***** Space of allowed actions to sample ******/
		
		
		Distribution<Action> uniform_dist = new Distribution_Uniform();
		
		/********** Repeated action 1 -- no keys pressed. ***********/
		boolean[] keys1 = new boolean[]{false,false,false,false};
		Integer[] durations1 = new Integer[]{5,6,7,8,9,10,11,12,13,14,15};
		boolean[][] keySet1 = new boolean[durations1.length][];
		
		for (int i = 0; i < durations1.length; i++) {
			keySet1[i] = keys1; // Other keys are allowable, but not used yet.
		}
		
		//Distribution<Action> dist1 = new Distribution_Uniform();
		Distribution<Action> dist1 = new Distribution_Normal(8f,2f);
		ActionSet actionSet1 = ActionSet.makeActionSet(durations1, keySet1, dist1);
		
		/**********  Repeated action 2 -- W-O pressed ***********/
		boolean[] keys2 = new boolean[]{false,true,true,false};
		Integer[] durations2 = new Integer[]{30,31,32,33,34,35,36,37,38,39,40};
		boolean[][] keySet2 = new boolean[durations2.length][];
		
		for (int i = 0; i < durations2.length; i++) {
			keySet2[i] = keys2; // Other keys are allowable, but not used yet.
		}
		
//		Distribution<Action> dist2 = new Distribution_Uniform();
		Distribution<Action> dist2 = new Distribution_Normal(35f,2f);
		ActionSet actionSet2 = ActionSet.makeActionSet(durations2, keySet2, dist2);
		
		/**********  Repeated action 3 -- W-O pressed ***********/
		boolean[] keys3 = new boolean[]{false,false,false,false};
		Integer[] durations3 = new Integer[]{5,6,7,8,9,10,11,12,13,14,15};
		boolean[][] keySet3 = new boolean[durations3.length][];
		
		for (int i = 0; i < durations3.length; i++) {
			keySet3[i] = keys3; // Other keys are allowable, but not used yet.
		}
		
//		Distribution<Action> dist3 = new Distribution_Uniform();
		Distribution<Action> dist3 = new Distribution_Normal(8f,2f);
		ActionSet actionSet3 = ActionSet.makeActionSet(durations3, keySet3, dist3);
		
		/**********  Repeated action 4 -- Q-P pressed ***********/
		boolean[] keys4 = new boolean[]{true,false,false,true};
		Integer[] durations4 = new Integer[]{30,31,32,33,34,35,36,37,38,39,40};
		boolean[][] keySet4 = new boolean[durations4.length][];
		
		for (int i = 0; i < durations4.length; i++) {
			keySet4[i] = keys4; // Other keys are allowable, but not used yet.
		}
		
//		Distribution<Action> dist4 = new Distribution_Uniform();
		Distribution<Action> dist4 = new Distribution_Normal(35f,2f);
		ActionSet actionSet4 = ActionSet.makeActionSet(durations4, keySet4, dist4);
								
		ActionSet[] repeatedActions = new ActionSet[] {actionSet1,actionSet2,actionSet3,actionSet4};
		
		
		
		/////// Action Exceptions ////////
		/********** Repeated action exceptions 1 -- no keys pressed. ***********/
		boolean[] keysE1 = new boolean[]{false,false,false,false};
		Integer[] durationsE1 = new Integer[]{4,5,6};
		boolean[][] keySetE1 = new boolean[durationsE1.length][];
		
		for (int i = 0; i < durationsE1.length; i++) {
			keySetE1[i] = keysE1; // Other keys are allowable, but not used yet.
		}
		
		//Distribution<Action> distE1 = new Distribution_Uniform();
		Distribution<Action> distE1 = new Distribution_Normal(5f,0.4f);
		ActionSet actionSetE1 = ActionSet.makeActionSet(durationsE1, keySetE1, distE1);
		
		/**********  Repeated action exceptions 2 -- W-O pressed ***********/
		boolean[] keysE2 = new boolean[]{false,true,true,false};
		Integer[] durationsE2 = new Integer[]{31,32,33,34,35,36};
		boolean[][] keySetE2 = new boolean[durationsE2.length][];
		
		for (int i = 0; i < durationsE2.length; i++) {
			keySetE2[i] = keysE2; // Other keys are allowable, but not used yet.
		}
		
		Distribution<Action> distE2 = new Distribution_Uniform();
		ActionSet actionSetE2 = ActionSet.makeActionSet(durationsE2, keySetE2, distE2);
		
		/**********  Repeated action exceptions 3 -- W-O pressed ***********/
		boolean[] keysE3 = new boolean[]{false,false,false,false};
		Integer[] durationsE3 = new Integer[]{21,22,23,24,25};
		boolean[][] keySetE3 = new boolean[durationsE3.length][];
		
		for (int i = 0; i < durationsE3.length; i++) {
			keySetE3[i] = keysE3; // Other keys are allowable, but not used yet.
		}
		
		Distribution<Action> distE3 = new Distribution_Uniform();
		ActionSet actionSetE3 = ActionSet.makeActionSet(durationsE3, keySetE3, distE3);
		
		/**********  Repeated action exceptions 4 -- Q-P pressed ***********/
		boolean[] keysE4 = new boolean[]{true,false,false,true};
		Integer[] durationsE4 = new Integer[]{45,46,47,48,49,50};
		boolean[][] keySetE4 = new boolean[durationsE4.length][];
		
		for (int i = 0; i < durationsE4.length; i++) {
			keySetE4[i] = keysE4; // Other keys are allowable, but not used yet.
		}
		
		Distribution<Action> distE4 = new Distribution_Uniform();
		ActionSet actionSetE4 = ActionSet.makeActionSet(durationsE4, keySetE4, distE4);
								
		Map<Integer,ActionSet> actionExceptions = new HashMap<Integer,ActionSet>();
		actionExceptions.put(0, actionSetE1);
		actionExceptions.put(1, actionSetE2);
		actionExceptions.put(2, actionSetE3);
		actionExceptions.put(3, actionSetE4);
		

		// Define the specific way that these allowed actions are assigned as potential options for nodes.
		IActionGenerator actionGenerator = new ActionGenerator_FixedSequence(repeatedActions, actionExceptions);
		Node.potentialActionGenerator = actionGenerator;
		
		/******** Define how nodes are evaluated. *********/

		IEvaluationFunction evaluateRandom = new Evaluator_Random(); // Assigns a purely random score for diagnostics.
		IEvaluationFunction evaluateDistance = new Evaluator_Distance();
		IEvaluationFunction evaluateHandTuned = new Evaluator_HandTunedOnState();
		
		IEvaluationFunction currentEvaluator = evaluateHandTuned;
		
		/******** Define how nodes are sampled from the above defined actions. *********/
		ISampler samplerRandom = new Sampler_Random(); // Random sampler does not need a value function as it acts blindly anyway.
		ISampler samplerGreedy = new Sampler_Greedy(currentEvaluator); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
		ISampler samplerUCB = new Sampler_UCB(currentEvaluator); // Upper confidence bound for trees sampler. More principled way of assigning weight for exploration/exploitation.
		
		ISampler currentSampler = samplerRandom;
		
		/************************************************************/		
		/******* Decide how datasets are to be saved/loaded. ********/
		/************************************************************/

//		System.out.println("Reading saved games file."); tic();
		SaveableFileIO<SaveableSingleGame> io_sparse = new SaveableFileIO<SaveableSingleGame>();
		SaveableFileIO<SaveableDenseData> io_dense = new SaveableFileIO<SaveableDenseData>();
		
		String sparseFileName = generateFileName("test", "SaveableSingleGame");
		String denseFileName =  generateFileName("test", "SaveableDenseData");

		//ArrayList<SaveableSingleGame> loaded = io_sparse.loadObjectsOrdered("test_2017-10-25_16-25-38.SaveableSingleGame");
		
		//Node treeRoot = Node.makeNodesFromRunInfo(loaded, false);
		// If we don't want to append to an existing file, we should clear out anything existing with this name.
//		clearExistingFile(sparseFileName);
//		clearExistingFile(denseFileName);
		
		Node treeRoot = new Node(useTreePhysics);
		
		

		FSM_UI ui = new FSM_UI();
		FSM_Tree tree = new FSM_Tree(currentSampler);
		FSM_Game game = new FSM_Game();
		
		/* Manage the tree, UI, and game. Start some threads. */
		Negotiator negotiator = new Negotiator(tree, ui, game, 
												io_sparse, sparseFileName);//, io_dense, denseFileName);
		
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
