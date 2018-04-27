package main;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;

import TreeStages.TreeStage_MaxDepth;
import TreeStages.TreeStage_MinDepth;
import data.SaveableFileIO;
import data.SaveableSingleGame;
import data.SparseDataToDense;
import distributions.Distribution_Normal;
import distributions.Distribution_Uniform;
import evaluators.Evaluator_Distance;
import filters.NodeFilter_GoodDescendants;
import samplers.Sampler_FixedDepth;
import samplers.Sampler_UCB;
import savers.DataSaver_StageSelected;
import transformations.Transform_Autoencoder;
import transformations.Transform_PCA;
import ui.PanelPlot_Controls;
import ui.PanelPlot_SingleRun;
import ui.PanelPlot_States;
import ui.PanelPlot_Transformed;
import ui.PanelRunner_AnimatedTransformed;
import ui.PanelRunner_Comparison;
import ui.PanelRunner_Snapshot;
import ui.PanelTimeSeries_WorkerLoad;
import ui.UI_Full;
import ui.UI_Headless;

public class MAIN_Run {

	/** Location of the configuration file for this search. **/
	private File configFile = new File("./search.config");

	/** Settings loaded from the config file. **/
	private Properties properties;

	/** Whether or not to run without the UI. **/
	private boolean headless = false;

	/** Information put in a running log to be saved at shutdown. **/
	private String endLog = "";

	public MAIN_Run() {
		properties = Utility.loadConfigFile(configFile);
	}

	public static void main(String[] args) {

		MAIN_Run manager = new MAIN_Run();
		manager.doGames();
	}

	public void doGames() {
		// Options to consider:
		/*
		IEvaluationFunction evaluateRandom = new Evaluator_Random(); // Assigns a purely random score for diagnostics.
		IEvaluationFunction evaluateDistance = new Evaluator_Distance();
		IEvaluationFunction evaluateHandTuned = new Evaluator_HandTunedOnState();
		IEvaluationFunction currentEvaluator = evaluateDistance;

		ISampler currentSampler = new Sampler_Random();
		ISampler currentSampler = new Sampler_Distribution();
		ISampler currentSampler = new Sampler_Greedy(currentEvaluator); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
		ISampler currentSampler = new Sampler_UCB(currentEvaluator); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
		ISampler currentSampler = new Sampler_Deterministic();

		IDataSaver dataSaver = new DataSaver_DenseTFRecord(); // Saves full state/action info, in Tensorflow-compatible TFRecord format.
		IDataSaver dataSaver = new DataSaver_Sparse(); // Saves just actions needed to recreate runs.
		IDataSaver dataSaver = new DataSaver_DenseJava(); // Saves full state/action info, but in serialized java classes. // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
		IDataSaver dataSaver = new DataSaver_Null(); // Greedy sampler progresses down the tree only sampling things further back when its current expansion is exhausted.
		dataSaver.setSaveInterval(Integer.parseInt(settings.saver.get(1))); // set save frequency from parsed args if that argument has been input.
		 */

		// WORKER CORES:
		// Worker threads to run. Each worker independently explores the tree and has its own loaded copy of the Box2D libraries.
		float workersFractionOfCores = Float.parseFloat(properties.getProperty("workersFractionOfCores", "0.8"));
		int cores = Runtime.getRuntime().availableProcessors();
		int maxWorkers = (int)(workersFractionOfCores*cores); // Basing of number of cores including hyperthreading. May want to optimize this a tad.
		System.out.println("Detected " + cores + " physical cores. Making a max of " + maxWorkers + " workers.");


		// SAVE DIRECTORY:
		File saveLoc = new File(properties.getProperty("saveLocation", "./"));
		if (!saveLoc.exists()) {
			boolean success = saveLoc.mkdir();
			if (!success) throw new RuntimeException("Could not make save directory.");
		}

		// Copy the config file into the save directory.
		File configSave = new File(saveLoc.getAbsolutePath() + "/config_" + Utility.getTimestamp() + ".config");
		try {
			FileUtils.copyFile(configFile, configSave);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// Save a progress log before shutting down.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					Utility.stringToLogFile(endLog, saveLoc.toString() + "/" + "summary_" + Utility.getTimestamp() + ".log");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}}); 


		Sampler_UCB.explorationMultiplier = Float.valueOf(properties.getProperty("UCBExplorationMultiplier", "1"));

		headless = Boolean.valueOf(properties.getProperty("headless", "false"));
		boolean doStage1 = Boolean.valueOf(properties.getProperty("doStage1", "false"));
		boolean doStage2 = Boolean.valueOf(properties.getProperty("doStage2", "false"));;
		boolean doStage3 = Boolean.valueOf(properties.getProperty("doStage3", "false"));;
		boolean doStage4 = Boolean.valueOf(properties.getProperty("doStage4", "false"));;

		// Stage 1
		int getToSteadyDepth = Integer.valueOf(properties.getProperty("getToSteadyDepth", "18"));
		int stage1Workers = (int) Math.max(maxWorkers * Float.valueOf(properties.getProperty("fractionOfMaxWorkers1", "1")), 1);
		String fileSuffix1 = properties.getProperty("fileSuffix1", "");

		// Stage 2
		int trimSteadyBy = Integer.valueOf(properties.getProperty("trimSteadyBy", "7"));
		int deviationDepth = Integer.valueOf(properties.getProperty("deviationDepth", "2"));
		int stage2Workers = (int) Math.max(maxWorkers * Float.valueOf(properties.getProperty("fractionOfMaxWorkers2", "1")), 1);
		String fileSuffix2 = properties.getProperty("fileSuffix2", "");

		// Stage 3
		int stage3StartDepth = getToSteadyDepth - trimSteadyBy + deviationDepth;
		int recoveryResumePoint = Integer.valueOf(properties.getProperty("resumePoint", "0")); // Return here if we're restarting.
		boolean autoResume = Boolean.valueOf(properties.getProperty("autoResume","true")); // If true, overrides the recoveryResumePoint and looks at which files have been completed.
		int getBackToSteadyDepth = Integer.valueOf(properties.getProperty("recoveryActions", "14")); // This many moves to recover.
		int stage3Workers = (int) Math.max(maxWorkers * Float.valueOf(properties.getProperty("fractionOfMaxWorkers3", "1")), 1);
		String fileSuffix3 = properties.getProperty("fileSuffix3", "");

		// Stage 4
		int trimStartBy = stage3StartDepth; 
		int trimEndBy = Integer.valueOf(properties.getProperty("trimEndBy", "4"));

		assignAllowableActions(stage3StartDepth);


		/************************************************************/		
		/**************** Pick user interface. **********************/
		/************************************************************/
		Node treeRoot = new Node();

		IUserInterface ui;
		PanelTimeSeries_WorkerLoad workerMonitorPanel = null;

		if (!headless) {
			UI_Full fullUI = new UI_Full();

			/* Make each UI component */
			PanelRunner_AnimatedTransformed runnerPanel = new PanelRunner_AnimatedTransformed();
			PanelRunner_Snapshot snapshotPane = new PanelRunner_Snapshot();
			PanelRunner_Comparison comparisonPane = new PanelRunner_Comparison();
			PanelPlot_States statePlotPane = new PanelPlot_States(6); // 6 plots per view at the bottom.

			PanelPlot_Transformed pcaPlotPane = new PanelPlot_Transformed(new Transform_PCA(IntStream.range(0, 72).toArray()), 6);
			PanelPlot_Controls controlsPlotPane = new PanelPlot_Controls(6); // 6 plots per view at the bottom.
			PanelPlot_Transformed autoencPlotPane = new PanelPlot_Transformed(new Transform_Autoencoder("AutoEnc_72to12_6layer.pb", 12), 6);
			autoencPlotPane.addFilter(new NodeFilter_GoodDescendants(1));
			PanelPlot_SingleRun singleRunPlotPane = new PanelPlot_SingleRun(6);
			workerMonitorPanel = new PanelTimeSeries_WorkerLoad(maxWorkers);

			fullUI.addTab(runnerPanel, "Run Animation");
			fullUI.addTab(snapshotPane, "State Viewer");
			fullUI.addTab(comparisonPane, "State Compare");
			fullUI.addTab(statePlotPane, "State Plots");
			fullUI.addTab(controlsPlotPane, "Controls Plots");
			fullUI.addTab(singleRunPlotPane, "Single Run Plots");
			fullUI.addTab(pcaPlotPane, "PCA Plots");
			fullUI.addTab(autoencPlotPane, "Autoenc Plots");
			fullUI.addTab(workerMonitorPanel, "Worker status");

			Thread runnerPanelThread = new Thread(runnerPanel); // All components with a copy of the GameLoader should have their own threads.
			runnerPanelThread.start();

			Thread monitorThread = new Thread(workerMonitorPanel);
			monitorThread.start();

			ui = fullUI;
			System.out.println("GUI: Running in full graphics mode.");
		}else {
			ui = new UI_Headless();
			System.out.println("GUI: Running in headless mode.");
		}

		// Root to visualize from.
		ui.addRootNode(treeRoot);

		Thread uiThread = new Thread(ui);
		uiThread.start();

		///////////////////////////////////////////////////////////

		// This stage generates the nominal gait. Roughly gets us to steady-state. Saves this 1 run to a file.

		// Pool of workers recycled between stages.
		GenericObjectPool<TreeWorker> workerPool = new GenericObjectPool<TreeWorker>(new WorkerFactory());
		workerPool.setMaxTotal(maxWorkers);
		workerPool.setMaxIdle(-1); // No limit to idle. Would have defaulted to 8, meaning all others would get culled between stages.

		
		// Check if we actually need to do stage 1.
		if (doStage1 && autoResume) {
			File[] existingFiles = saveLoc.listFiles();
			for (File f : existingFiles) {
				if (f.getName().contains("steadyRunPrefix")) {
					System.out.println("Found a completed stage 1 file. Skipping.");
					doStage1 = false;
					break;
				}
			}
		}
		
		if (doStage1) {
			System.out.println("Starting stage 1.");
			// Saver setup.
			DataSaver_StageSelected saver = new DataSaver_StageSelected();
			saver.overrideFilename = "steadyRunPrefix" + fileSuffix1;
			saver.setSavePath(saveLoc.getPath() + "/");

			TreeStage_MaxDepth searchMax = new TreeStage_MaxDepth(getToSteadyDepth, new Sampler_UCB(new Evaluator_Distance()), saver); // Depth to get to sorta steady state. was 
			searchMax.terminateAfterXGames = Integer.valueOf(properties.getProperty("bailAfterXGames1", "1000000")); // Will terminate after this many games played regardless of whether goals have been met.

			// Grab some workers from the pool.
			List<TreeWorker> tws1 = new ArrayList<TreeWorker>();
			for (int i = 0; i < stage1Workers; i++) {
				try {
					tws1.add(workerPool.borrowObject());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (!headless) workerMonitorPanel.setWorkers(tws1);
			// Do stage search
			searchMax.initialize(tws1, treeRoot);

			// Return the checked out workers.
			for (TreeWorker w : tws1) {
				workerPool.returnObject(w);
			}
			System.out.println("Stage 1 done.");
			endLog += "Stage 1 done.\n";
		}

		// This stage generates deviations from nominal. Load nominal gait. Do not allow expansion near the root. Expand to a fixed, small depth.
		// Check if we actually need to do stage 2.
		if (doStage2 && autoResume) {
			File[] existingFiles = saveLoc.listFiles();
			for (File f : existingFiles) {
				if (f.getName().contains("deviations")) {
					System.out.println("Found a completed stage 2 file. Skipping.");
					doStage2 = false;
					break;
				}
			}
		}
		
		if (doStage2) {
			System.out.println("Starting stage 2.");

			// Saver setup.
			DataSaver_StageSelected saver = new DataSaver_StageSelected();
			saver.overrideFilename = "deviations" + fileSuffix2;
			saver.setSavePath(saveLoc.getPath() + "/");	

			Node rootNode = new Node();
			ui.clearRootNodes();
			ui.addRootNode(rootNode);

			TreeStage searchMin = new TreeStage_MinDepth(deviationDepth, new Sampler_FixedDepth(deviationDepth), saver); // Two actions to get weird. new Sampler_FixedDepth(deviationDepth)
			SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
			Node.makeNodesFromRunInfo(fileIO.loadObjectsOrdered(saveLoc.getPath() + "/steadyRunPrefix" + fileSuffix1 + ".SaveableSingleGame"), rootNode, getToSteadyDepth - trimSteadyBy - 1);
			Node currNode = rootNode;
			while (currNode.treeDepth < getToSteadyDepth - trimSteadyBy) {
				currNode = currNode.children.get(0);
			}

			// Grab some workers from the pool.
			List<TreeWorker> tws2 = new ArrayList<TreeWorker>();
			for (int i = 0; i < stage2Workers; i++) {
				try {
					tws2.add(workerPool.borrowObject());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (!headless) workerMonitorPanel.setWorkers(tws2);
			searchMin.initialize(tws2, currNode);

			// Return the checked out workers.
			for (TreeWorker w : tws2) {
				workerPool.returnObject(w);
			}
			System.out.println("Stage 2 done.");
			endLog += "Did " + searchMin.getResults().size() + " deviations.\n";
			endLog += "Stage 2 done.\n";
		}

		// Expand the deviated spots and find recoveries.
		if (doStage3) {
			System.out.println("Starting stage 3.");

			// Auto-detect where we left off if this setting is selected.
			if (autoResume) {
				File[] existingFiles = saveLoc.listFiles();
				int startIdx = 0;
				boolean foundFile = false;
				while (startIdx < existingFiles.length){
					for (File f : existingFiles) {
						if (f.getName().contains("recoveries" + startIdx)) {
							System.out.println("Found file for recovery " + startIdx);
							foundFile = true;
							break;
						}
					}

					if (!foundFile) {
						break;
					}
					startIdx++;
					foundFile = false;
				}
				System.out.println("Resuming at recovery #: " + startIdx);
				recoveryResumePoint = startIdx;
			}

			// Saver setup.
			Node rootNode = new Node();
			ui.clearRootNodes();
			ui.addRootNode(rootNode);

			SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
			Node.makeNodesFromRunInfo(fileIO.loadObjectsOrdered(saveLoc.getPath() + "/deviations" + fileSuffix2 + ".SaveableSingleGame"), rootNode, stage3StartDepth);
			List<Node> leafList = new ArrayList<Node>();
			rootNode.getLeaves(leafList);

			int count = 0;
			int startAt = recoveryResumePoint;
			Node previousLeaf = null;

			for (Node leaf : leafList) {
				if (count >= startAt) {
					Utility.tic();
					DataSaver_StageSelected saver = new DataSaver_StageSelected();
					saver.overrideFilename = "recoveries" + count + fileSuffix3;
					saver.setSavePath(saveLoc.getPath() + "/");

					TreeStage_MaxDepth searchMax = new TreeStage_MaxDepth(getBackToSteadyDepth, new Sampler_UCB(new Evaluator_Distance()), saver); // Depth to get to sorta steady state.
					searchMax.terminateAfterXGames = Integer.valueOf(properties.getProperty("bailAfterXGames3", "100000"));;

					System.out.print("Started " + count + "...");

					// Grab some workers from the pool.
					List<TreeWorker> tws3 = new ArrayList<TreeWorker>();
					for (int i = 0; i < stage3Workers; i++) {
						try {
							tws3.add(workerPool.borrowObject());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (!headless) workerMonitorPanel.setWorkers(tws3);
					searchMax.initialize(tws3, leaf);

					// Return the checked out workers.
					for (TreeWorker w : tws3) {
						workerPool.returnObject(w);
					}
					Utility.toc();
				}
				// Turn off drawing for this one.
				leaf.turnOffBranchDisplay();
				leaf.parent.children.remove(leaf);
				if (previousLeaf != null) {
					previousLeaf.destroyAllNodesBelowAndCheckExplored();
				}
				previousLeaf = leaf;

				count++;
				endLog += "Expanded leaf: " + count + ".\n";
			}

			System.out.println("Stage 3 done.");
			endLog += "Stage 3 done.\n";
		}

		// Convert the sections of the runs we care about to dense data by resimulating.
		if (doStage4) {
			List<File> filesToConvert = new ArrayList<File>();
			File[] files = saveLoc.listFiles();
			for (File f : files) {
				if (f.toString().toLowerCase().contains("recoveries") && f.toString().toLowerCase().contains("saveablesinglegame") && !f.toString().toLowerCase().contains("unsuccessful")) {
					filesToConvert.add(f);
				}
			}

			SparseDataToDense converter = new SparseDataToDense(saveLoc.getAbsolutePath() + "/");
			converter.trimFirst = trimStartBy;
			converter.trimLast = trimEndBy;
			converter.convert(filesToConvert, true);	

			System.out.println("Stage 4 done.");
			endLog += "Stage 4 done.\n";
		}

		workerPool.close();
	}

	/** Assign the correct generator of actions based on the baseline options and exceptions. **/
	private void assignAllowableActions(int recoveryExceptionStart) {
		/********************************************/		
		/******* Space of allowable actions. ********/
		/********************************************/

		/***** Space of allowed actions to sample ******/
		//Distribution<Action> uniform_dist = new Distribution_Uniform();

		/********** Repeated action 1 -- no keys pressed. ***********/
		Integer[] durations1 = IntStream.range(1, 25).boxed().toArray(Integer[] :: new);
		boolean[][] keySet1 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durations1.length);

		//Distribution<Action> dist1 = new Distribution_Uniform();
		Distribution<Action> dist1 = new Distribution_Normal(10f,2f);
		ActionSet actionSet1 = ActionSet.makeActionSet(durations1, keySet1, dist1);

		/**********  Repeated action 2 -- W-O pressed ***********/
		Integer[] durations2 = IntStream.range(20, 60).boxed().toArray(Integer[] :: new);
		boolean[][] keySet2 = ActionSet.replicateKeyString(new boolean[]{false,true,true,false},durations2.length);

		//		Distribution<Action> dist2 = new Distribution_Uniform();
		Distribution<Action> dist2 = new Distribution_Normal(39f,3f);
		ActionSet actionSet2 = ActionSet.makeActionSet(durations2, keySet2, dist2);

		/**********  Repeated action 3 -- W-O pressed ***********/
		Integer[] durations3 = IntStream.range(1, 25).boxed().toArray(Integer[] :: new);
		boolean[][] keySet3 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durations3.length);

		//		Distribution<Action> dist3 = new Distribution_Uniform();
		Distribution<Action> dist3 = new Distribution_Normal(10f,2f);
		ActionSet actionSet3 = ActionSet.makeActionSet(durations3, keySet3, dist3);

		/**********  Repeated action 4 -- Q-P pressed ***********/
		Integer[] durations4 = IntStream.range(20, 60).boxed().toArray(Integer[] :: new);
		boolean[][] keySet4 = ActionSet.replicateKeyString(new boolean[]{true,false,false,true},durations4.length);

		Distribution<Action> dist4 = new Distribution_Normal(39f,3f);
		ActionSet actionSet4 = ActionSet.makeActionSet(durations4, keySet4, dist4);
		ActionSet[] repeatedActions = new ActionSet[] {actionSet1,actionSet2,actionSet3,actionSet4};

		/////// Action Exceptions for starting up. ////////
		/********** Repeated action exceptions 1 -- no keys pressed. ***********/
		Integer[] durationsE1 = IntStream.range(1, 25).boxed().toArray(Integer[] :: new);
		boolean[][] keySetE1 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durationsE1.length);

		Distribution<Action> distE1 = new Distribution_Normal(5f,1f);
		ActionSet actionSetE1 = ActionSet.makeActionSet(durationsE1, keySetE1, distE1);

		/**********  Repeated action exceptions 2 -- W-O pressed ***********/
		Integer[] durationsE2 = IntStream.range(20, 50).boxed().toArray(Integer[] :: new);
		boolean[][] keySetE2 = ActionSet.replicateKeyString(new boolean[]{false,true,true,false},durationsE2.length);

		Distribution<Action> distE2 = new Distribution_Normal(34f,2f);
		ActionSet actionSetE2 = ActionSet.makeActionSet(durationsE2, keySetE2, distE2);

		/**********  Repeated action exceptions 3 -- no keys pressed. ***********/
		Integer[] durationsE3 = IntStream.range(10, 45).boxed().toArray(Integer[] :: new);
		boolean[][] keySetE3 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durationsE3.length);

		Distribution<Action> distE3 = new Distribution_Normal(24f,2f);
		ActionSet actionSetE3 = ActionSet.makeActionSet(durationsE3, keySetE3, distE3);

		/**********  Repeated action exceptions 4 -- Q-P pressed ***********/
		Integer[] durationsE4 = IntStream.range(25, 65).boxed().toArray(Integer[] :: new);
		boolean[][] keySetE4 = ActionSet.replicateKeyString(new boolean[]{true,false,false,true},durationsE4.length);

		Distribution<Action> distE4 = new Distribution_Normal(49f,2f);
		ActionSet actionSetE4 = ActionSet.makeActionSet(durationsE4, keySetE4, distE4);


		/////// Action Exceptions for recovery. ////////
		/**********  Repeated action 1 and 3 -- Nothing pressed ***********/
		Integer[] durationsFalseFalse = IntStream.range(1, 50).boxed().toArray(Integer[] :: new);
		boolean[][] keySetFalseFalse = ActionSet.replicateKeyString(new boolean[]{false,false,false,false}, durationsFalseFalse.length);

		Distribution<Action> distFalseFalse = new Distribution_Normal(10f,2f);
		ActionSet actionSetFalseFalse = ActionSet.makeActionSet(durationsFalseFalse, keySetFalseFalse, distFalseFalse);

		/**********  Repeated action 2 -- W-O pressed ***********/
		Integer[] durationsWO = IntStream.range(10, 70).boxed().toArray(Integer[] :: new);
		boolean[][] keySetWO = ActionSet.replicateKeyString(new boolean[]{false,true,true,false}, durationsWO.length);

		Distribution<Action> distWO = new Distribution_Normal(39f,3f);
		ActionSet actionSetWO = ActionSet.makeActionSet(durationsWO, keySetWO, distWO);

		/**********  Repeated action 4 -- Q-P pressed ***********/
		Integer[] durationsQP = IntStream.range(10, 70).boxed().toArray(Integer[] :: new);
		boolean[][] keySetQP = ActionSet.replicateKeyString(new boolean[]{true,false,false,true}, durationsQP.length);

		Distribution<Action> distQP = new Distribution_Normal(39f,3f);
		ActionSet actionSetQP = ActionSet.makeActionSet(durationsQP, keySetQP, distQP);


		Map<Integer,ActionSet> actionExceptions = new HashMap<Integer,ActionSet>();
		actionExceptions.put(0, actionSetE1);
		actionExceptions.put(1, actionSetE2);
		actionExceptions.put(2, actionSetE3);
		actionExceptions.put(3, actionSetE4);

		// Put the recovery exceptions in the right spot.
		for (int i = 0; i < 4; i++) {
			int sequencePos = (recoveryExceptionStart + i) % 4;

			switch (sequencePos) {
			case 0:
				actionExceptions.put(recoveryExceptionStart + i, actionSetFalseFalse);
				break;
			case 1:
				actionExceptions.put(recoveryExceptionStart + i, actionSetWO);
				break;
			case 2:
				actionExceptions.put(recoveryExceptionStart + i, actionSetFalseFalse);
				break;
			case 3:
				actionExceptions.put(recoveryExceptionStart + i, actionSetQP);
				break;
			}	
		}

		// Define the specific way that these allowed actions are assigned as potential options for nodes.
		IActionGenerator actionGenerator = new ActionGenerator_FixedSequence(repeatedActions, actionExceptions);
		Node.potentialActionGenerator = actionGenerator;
	}
}
