package main;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;

import TreeStages.TreeStage_MaxDepth;
import TreeStages.TreeStage_MinDepth;
import data.SaveableActionSequence;
import data.SaveableFileIO;
import data.SaveableSingleGame;
import data.SparseDataToDense;
import evaluators.Evaluator_Distance;
import filters.NodeFilter_GoodDescendants;
import game.GameLoader;
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

public class MAIN_Search_Full extends MAIN_Search_Template {

	public MAIN_Search_Full() {

	}

	public void doGames() {

		// Load all parameters specific to this search.
		Sampler_UCB.explorationMultiplier = Float.valueOf(properties.getProperty("UCBExplorationMultiplier", "1"));
		boolean doStage1 = Boolean.valueOf(properties.getProperty("doStage1", "false"));
		boolean doStage2 = Boolean.valueOf(properties.getProperty("doStage2", "false"));;
		boolean doStage3 = Boolean.valueOf(properties.getProperty("doStage3", "false"));;
		boolean doStage4 = Boolean.valueOf(properties.getProperty("doStage4", "false"));;
		
		boolean autoResume = Boolean.valueOf(properties.getProperty("autoResume","true")); // If true, overrides the recoveryResumePoint and looks at which files have been completed.

		// Stage 1
		int getToSteadyDepth = Integer.valueOf(properties.getProperty("getToSteadyDepth", "18")); // Stage terminates after any part of the tree reaches this depth.
		float maxWorkerFraction1 = Float.valueOf(properties.getProperty("fractionOfMaxWorkers1", "1")); // Fraction of the maximum workers used for this stage.
		int bailAfterXGames1 = Integer.valueOf(properties.getProperty("bailAfterXGames1", "1000000")); // Stop stage 1 after this many games even if we don't reach the goal depth.
		String fileSuffix1 = properties.getProperty("fileSuffix1", "");
		
		String filename1 = "steadyRunPrefix" + fileSuffix1;

		// Stage 2
		int trimSteadyBy = Integer.valueOf(properties.getProperty("trimSteadyBy", "7"));
		int deviationDepth = Integer.valueOf(properties.getProperty("deviationDepth", "2"));
		float maxWorkerFraction2 = Float.valueOf(properties.getProperty("fractionOfMaxWorkers2", "1"));
		int bailAfterXGames2 = Integer.valueOf(properties.getProperty("bailAfterXGames2", "1000000")); // Stop stage 1 after this many games even if we don't reach the goal depth.
		String fileSuffix2 = properties.getProperty("fileSuffix2", "");

		String filename2 = "deviations" + fileSuffix2;
		
		// Stage 3
		int stage3StartDepth = getToSteadyDepth - trimSteadyBy + deviationDepth;
		int recoveryResumePoint = Integer.valueOf(properties.getProperty("resumePoint", "0")); // Return here if we're restarting.
		int getBackToSteadyDepth = Integer.valueOf(properties.getProperty("recoveryActions", "14")); // This many moves to recover.
		float maxWorkerFraction3 = Float.valueOf(properties.getProperty("fractionOfMaxWorkers3", "1"));
		int bailAfterXGames3 = Integer.valueOf(properties.getProperty("bailAfterXGames3", "100000")); // Stop stage 1 after this many games even if we don't reach the goal depth.

		String fileSuffix3 = properties.getProperty("fileSuffix3", "");

		// Stage 4
		int trimStartBy = stage3StartDepth; 
		int trimEndBy = Integer.valueOf(properties.getProperty("trimEndBy", "4"));
		///////////////////////////////////////////////////////////

		assignAllowableActions(stage3StartDepth);

		// This stage generates the nominal gait. Roughly gets us to steady-state. Saves this 1 run to a file.
		// Check if we actually need to do stage 1.
		if (doStage1 && autoResume) {
			File[] existingFiles = getSaveLocation().listFiles();
			for (File f : existingFiles) {
				if (f.getName().contains("steadyRunPrefix")) {
					appendSummaryLog("Found a completed stage 1 file. Skipping.");
					doStage1 = false;
					break;
				}
			}
		}

		if (doStage1) {
			Node rootNode = new Node();
			ui.clearRootNodes();
			ui.addRootNode(rootNode);
			
			appendSummaryLog("Starting stage 1.");
			doBasicMaxDepthStage(rootNode, filename1, getToSteadyDepth, maxWorkerFraction1, bailAfterXGames1);
			appendSummaryLog("Stage 1 done.");
		}

		// This stage generates deviations from nominal. Load nominal gait. Do not allow expansion near the root. Expand to a fixed, small depth.
		// Check if we actually need to do stage 2.
		if (doStage2 && autoResume) {
			File[] existingFiles = getSaveLocation().listFiles();
			for (File f : existingFiles) {
				if (f.getName().contains("deviations")) {
					appendSummaryLog("Found a completed stage 2 file. Skipping.");
					doStage2 = false;
					break;
				}
			}
		}
		if (doStage2) {
			appendSummaryLog("Starting stage 2.");
			Node rootNode = new Node();
			ui.clearRootNodes();
			ui.addRootNode(rootNode);
			
			SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
			Node.makeNodesFromRunInfo(fileIO.loadObjectsOrdered(getSaveLocation().getPath() + "/" + filename1 + ".SaveableSingleGame"), rootNode, getToSteadyDepth - trimSteadyBy - 1);
			Node currNode = rootNode;
			while (currNode.treeDepth < getToSteadyDepth - trimSteadyBy) {
				currNode = currNode.children.get(0);
			}

			doBasicMinDepthStage(currNode, filename2, deviationDepth, maxWorkerFraction2, bailAfterXGames2);
			
			appendSummaryLog("Stage 2 done.");
		}

		// Expand the deviated spots and find recoveries.
		if (doStage3) {
			appendSummaryLog("Starting stage 3.");

			// Auto-detect where we left off if this setting is selected.
			if (autoResume) {
				File[] existingFiles = getSaveLocation().listFiles();
				int startIdx = 0;
				boolean foundFile = false;
				while (startIdx < existingFiles.length){
					for (File f : existingFiles) {
						if (f.getName().contains("recoveries" + startIdx)) {
							appendSummaryLog("Found file for recovery " + startIdx);
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
				appendSummaryLog("Resuming at recovery #: " + startIdx);
				recoveryResumePoint = startIdx;
			}

			// Saver setup.
			Node rootNode = new Node();
			ui.clearRootNodes();
			ui.addRootNode(rootNode);

			SaveableFileIO<SaveableSingleGame> fileIO = new SaveableFileIO<SaveableSingleGame>();
			Node.makeNodesFromRunInfo(fileIO.loadObjectsOrdered(getSaveLocation().getPath() + "/" + filename2 + ".SaveableSingleGame"), rootNode, stage3StartDepth);

			List<Node> leafList = new ArrayList<Node>();
			rootNode.getLeaves(leafList);

			int count = 0;
			int startAt = recoveryResumePoint;
			Node previousLeaf = null;

			for (Node leaf : leafList) {
				if (count >= startAt) {
					doBasicMaxDepthStage(leaf, "recoveries" + count + fileSuffix3, getBackToSteadyDepth, maxWorkerFraction3, bailAfterXGames3);
				}
				// Turn off drawing for this one.
				leaf.turnOffBranchDisplay();
				leaf.parent.children.remove(leaf);
				if (previousLeaf != null) {
					previousLeaf.destroyAllNodesBelowAndCheckExplored();
				}
				previousLeaf = leaf;

				count++;
				appendSummaryLog("Expanded leaf: " + count + ".");
			}
			appendSummaryLog("Stage 3 done.");
		}

		// Convert the sections of the runs we care about to dense data by resimulating.
		if (doStage4) {
			List<File> filesToConvert = new ArrayList<File>();
			File[] files = getSaveLocation().listFiles();
			for (File f : files) {
				if (f.toString().toLowerCase().contains("recoveries") && f.toString().toLowerCase().contains("saveablesinglegame") && !f.toString().toLowerCase().contains("unsuccessful")) {
					filesToConvert.add(f);
				}
			}
			SparseDataToDense converter = new SparseDataToDense(getSaveLocation().getAbsolutePath() + "/");
			converter.trimFirst = 0;//trimStartBy;
			converter.trimLast = trimEndBy;
			converter.convert(filesToConvert, false);	

			appendSummaryLog("Stage 4 done.");
		}
	}
}


//{	// For extending and fixing saved games from MAIN_Controlled
//	SaveableFileIO<SaveableActionSequence> actionSequenceLoader = new SaveableFileIO<SaveableActionSequence>();
//
//	File actionSequenceLoadPath = new File(Utility.getExcutionPath() + "saved_data/individual_expansions_todo");
//	File[] actionFiles = actionSequenceLoadPath.listFiles();
//
//	GameLoader game = new GameLoader();
//	Node rt = new Node();
//
//	List<SaveableActionSequence> actionSequences = new ArrayList<SaveableActionSequence>();
//	for (File f : actionFiles) {
//		if (f.getName().contains("SaveableActionSequence")) {
//			actionSequences.addAll(actionSequenceLoader.loadObjectsOrdered(f));
//		}
//	}
//	List<Action[]> acts = actionSequences.stream().map(seq -> seq.getActions()).collect(Collectors.toList());	
//	Node.makeNodesFromActionSequences(acts, rt, game);
//
//	ui.clearRootNodes();
//	ui.addRootNode(rt);
//	
//
//	List<Node> leafList = new ArrayList<Node>();
//	rt.getLeaves(leafList);
//
//	int count = 0;
//	int startAt = 0;
//	Node previousLeaf = null;
//
//	for (Node leaf : leafList) {
//		if (count >= startAt) {
//			Utility.tic();
//			DataSaver_StageSelected saver = new DataSaver_StageSelected();
//			saver.overrideFilename = "recoveries" + count + fileSuffix3;
//			saver.setSavePath(saveLoc.getPath() + "/");
//
//			TreeStage_MaxDepth searchMax = new TreeStage_MaxDepth(getBackToSteadyDepth, new Sampler_UCB(new Evaluator_Distance()), saver); // Depth to get to sorta steady state.
//			searchMax.terminateAfterXGames = Integer.valueOf(properties.getProperty("bailAfterXGames3", "100000"));;
//
//			System.out.print("Started " + count + "...");
//
//			// Grab some workers from the pool.
//			List<TreeWorker> tws3 = new ArrayList<TreeWorker>();
//			for (int i = 0; i < stage3Workers; i++) {
//				try {
//					tws3.add(workerPool.borrowObject());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			if (!headless) workerMonitorPanel.setWorkers(tws3);
//			searchMax.initialize(tws3, leaf);
//
//			// Return the checked out workers.
//			for (TreeWorker w : tws3) {
//				workerPool.returnObject(w);
//			}
//			Utility.toc();
//		}
//		// Turn off drawing for this one.
//		leaf.turnOffBranchDisplay();
//		leaf.parent.children.remove(leaf);
//		if (previousLeaf != null) {
//			previousLeaf.destroyAllNodesBelowAndCheckExplored();
//		}
//		previousLeaf = leaf;
//
//		count++;
//		endLog += "Expanded leaf: " + count + ".\n";
//	}
//
//	System.out.println("Stage 3 done.");
//	endLog += "Stage 3 done.\n";
//
//}
