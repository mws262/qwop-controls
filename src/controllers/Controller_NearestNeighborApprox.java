package controllers;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.tensorflow.example.FeatureList;
import org.tensorflow.example.SequenceExample;

import data.EvictingTreeMap;
import data.LIFOFixedSize;
import data.TFRecordReader;
import game.GameLoader;
import game.State;
import game.StateVariable;
import main.Action;
import main.IController;
import main.PanelRunner;
import main.State_Weights;
import main.Utility;


public class Controller_NearestNeighborApprox implements IController {
	// TODO: "Snapping" to certain sequences.
	// TODO: Preferential treatment of sequences with more than a few timesteps remaining.
	// TODO: Recent history matching.


	/** How many nearby (in terms of body theta) states are compared when determining the "closest" one. **/
	public int upperSetLimit = 10000;
	public int lowerSetLimit = 10000;
	
	/** Total number of states loaded from TFRecord file. **/
	private int numStatesLoaded = 0;

	/** All states loaded, regardless of run, in sorted order, by body theta. **/
	private NavigableMap<Float, StateHolder> allStates = new TreeMap<Float, StateHolder>();

	/** All runs loaded. **/
	public Set<RunHolder> runs = new HashSet<RunHolder>(); 

	/** Keep track of which total run that the currently selected action comes from. **/
	private RunHolder currentTrajectory;
	private StateHolder currentState;
	private Deque<State> previousStatesLIFO = new LIFOFixedSize<State>(5);

	State.ObjectName sortByPart = State.ObjectName.BODY;
	State.StateName sortBySt = State.StateName.TH;
	
	EvictingTreeMap<Float, StateHolder> topMatches = new EvictingTreeMap<Float, StateHolder>(10);

	public Controller_NearestNeighborApprox(List<File> files) {
		loadAll(files);
		System.out.println("Wow! " + numStatesLoaded + " states were loaded!");
	}

	@Override
	public Action policy(State state) {
		//Utility.tic();
		// Get nearest states (determined ONLY by body theta).
		float sortBy = state.getStateVarFromName(sortByPart, sortBySt);
		
		NavigableMap<Float, StateHolder> lowerSet = allStates.headMap(sortBy, true);
		NavigableMap<Float, StateHolder> upperSet = allStates.tailMap(sortBy, false);	
		
//		Iterator<StateHolder> iterLower = lowerSet.values().iterator();
//		Iterator<StateHolder> iterUpper = upperSet.values().iterator();

		StateHolder bestSoFar = null;
		float bestErrorSoFar = Float.MAX_VALUE;

//		//		// Check full sq error with nearest bunch of states with lower body theta
//				Utility.tic();
//				int counter = 0;
//				while (iterLower.hasNext() && counter++ < maxHalfExtent) {
//					StateHolder st = iterLower.next();
//					float error = totalEvalFunction(st, state);
//					if (error < bestErrorSoFar) {
//						bestSoFar = st;
//						bestErrorSoFar = error;
//					}
//				}
//				//System.out.print(bestErrorSoFar + ", ");
//				//System.out.println(counter + " compared from below.");
//		
//				// Check full sq error with nearest bunch of states with higher body theta
//				
//				counter = 0;
//				while (iterUpper.hasNext() && counter++ < maxHalfExtent) {
//					StateHolder st = iterUpper.next();
//					float error = totalEvalFunction(st, state);
//					if (error < bestErrorSoFar) {
//						bestSoFar = st;
//						bestErrorSoFar = error;
//					}
//				}
//				System.out.print("old ");
//
//				Utility.toc();
		
		
//		Utility.tic();
//		StateHolder sh1 = lowerSet.values().parallelStream().limit(lowerSetLimit).min(Comparator.comparingDouble( v -> totalEvalFunction(v, state))).orElse(null);
//		StateHolder sh2 = upperSet.values().parallelStream().limit(upperSetLimit).min(Comparator.comparingDouble( v -> totalEvalFunction(v, state))).orElse(null);
//		Utility.toc();
		
		EvictingTreeMap<Float, StateHolder> topMatches = new EvictingTreeMap<Float, StateHolder>(10);
		
		Utility.tic();
		lowerSet.values().parallelStream().limit(lowerSetLimit).forEach(v -> topMatches.put(totalEvalFunction(v, state),v));
		upperSet.values().parallelStream().limit(upperSetLimit).forEach(v -> topMatches.put(totalEvalFunction(v, state),v));
		Utility.toc();
		
		Iterator<Float> iter = topMatches.keySet().iterator();
		
		float keySum = 0;
		float[] keysWeighted = new float[4]; 
		while (iter.hasNext()) {
			Float k = iter.next();
			StateHolder v = topMatches.get(k);
			
			float multiplier = 1f/Float.max(k, Float.MIN_VALUE);
			keySum += multiplier;
			keysWeighted[0] += multiplier * (v.keys[0] ? 1 : 0);
			keysWeighted[1] += multiplier * (v.keys[1] ? 1 : 0);
			keysWeighted[2] += multiplier * (v.keys[2] ? 1 : 0);
			keysWeighted[3] += multiplier * (v.keys[3] ? 1 : 0);
			
			
			
			System.out.println(
			k + ", \t"
			+ v.keys[0] + "," + v.keys[1] + "," + v.keys[2] + "," + v.keys[3]);
		}
		
		keysWeighted[0] /= keySum;
		keysWeighted[1] /= keySum;
		keysWeighted[2] /= keySum;
		keysWeighted[3] /= keySum;
		
//		System.out.println(keysWeighted[0] + "," + keysWeighted[1] + "," + keysWeighted[2] + "," + keysWeighted[3]);
		Entry<Float, StateHolder> bestEntry = topMatches.pollFirstEntry();
		bestSoFar = bestEntry.getValue();
		bestErrorSoFar = bestEntry.getKey();
		System.out.println(state.body.x);
		
		boolean[] chosenKeys = new boolean[4];
		chosenKeys[0] = keysWeighted[0] >= 0.5f;
		chosenKeys[1] = keysWeighted[1] >= 0.5f;
		chosenKeys[2] = keysWeighted[2] >= 0.5f;
		chosenKeys[3] = keysWeighted[3] >= 0.5f;
		
		if (!Arrays.equals(chosenKeys, bestEntry.getValue().keys)) {
			System.out.println("WOW, OVERRIDING THE WINNER");
		}
//		Utility.tic();
//		List<StateHolder> sh3 = lowerSet.values().parallelStream()
//				.limit(upperSetLimit)
//				.sorted(Comparator.comparingDouble( v -> totalEvalFunction(v, state)))
//				.limit(5)
//				.collect(Collectors.toList());
//		List<StateHolder> sh4 = upperSet.values().parallelStream()
//				.limit(upperSetLimit)
//				.sorted(Comparator.comparingDouble( v -> totalEvalFunction(v, state)))
//				.limit(5)
//				.collect(Collectors.toList());
//		Utility.toc();
//		
//		System.out.println("");
//		for (StateHolder sh : sh3) {
//			System.out.println(
//					totalEvalFunction(sh, state) + ", \t"
//					+ sh.keys[0] + "," + sh.keys[1] + "," + sh.keys[2] + "," + sh.keys[3]);
//		}
//		for (StateHolder sh : sh4) {
//			System.out.println(
//					totalEvalFunction(sh, state) + ", \t"
//					+ sh.keys[0] + "," + sh.keys[1] + "," + sh.keys[2] + "," + sh.keys[3]);
//		}
		
//		float lowSetErr = (sh1 == null) ? Float.MAX_VALUE : totalEvalFunction(sh1, state);
//		float upperSetErr = (sh2 == null) ? Float.MAX_VALUE : totalEvalFunction(sh2, state);
//		
//		if (upperSetErr > lowSetErr) {
//			bestSoFar = sh1;
//			bestErrorSoFar = lowSetErr;
//		}else {
//			bestSoFar = sh2;
//			bestErrorSoFar = upperSetErr;
//		}
		//System.out.print(state.body.x + ", \t");


		//Utility.toc();

		// Choose whether to stay on the previous trajectory or not.
//		if (currentTrajectory != null) {
//			int lastStIdx = currentTrajectory.states.indexOf(currentState);
//
//			if (lastStIdx < currentTrajectory.states.size() - 2) {
//
//				StateHolder nextStateOnOldTraj = currentTrajectory.states.get(lastStIdx + 1);
//				float oldTrajError = sqError(nextStateOnOldTraj.state, state);
//
//				if (bestErrorSoFar + 5 < oldTrajError) {
//					currentTrajectory = bestSoFar.parentRun;
//					currentState = bestSoFar;
//				}else {
//					//System.out.print("SNAP");
//					currentState = nextStateOnOldTraj;
//				}
//			}else {
//				currentTrajectory = bestSoFar.parentRun;
//				currentState = bestSoFar;
//			}
//		}else {
//
			currentTrajectory = bestSoFar.parentRun;
			currentState = bestSoFar;
//		}

		previousStatesLIFO.push(state); // Keep previous states too.

		return new Action(1, bestSoFar.keys);
	}


	public float totalEvalFunction(StateHolder sh, State actualState) {
		float cost = 0f;

		// Error relative to current state.

		float currentStateError = sqError(sh.state, actualState);
		cost += currentStateError;

		// Sequences near their ends get a penalty.
		int parentRunLength = sh.parentRun.states.size();
		int stateLocInSequence = sh.parentRun.states.indexOf(sh);

		float futureSequenceSizeError = 15f/(float)(parentRunLength - stateLocInSequence + 1);
		cost += futureSequenceSizeError;

		// 
		int count = 1;
		Iterator<State> iter = previousStatesLIFO.iterator();
		float oldStateError = 0;
		while (iter.hasNext()) {
			State oldState = iter.next();
			int idx = stateLocInSequence - count;
			if (idx >= 0) {
				State stateFromLibrary = sh.parentRun.states.get(idx).state;
				oldStateError += (0.1f/(float)count)*sqError(oldState, stateFromLibrary);
			}else {
				oldStateError += 0.1f * currentStateError;
			}
			count++;
		}
		//System.out.println("currentStateError: " + currentStateError + ", oldStateError: " + oldStateError + ", futureError: " + futureSequenceSizeError);
		cost += oldStateError;
		return cost;
	}

	/** Sum of squared distance of all values in two states. **/
	private float sqError(State s1, State s2) {
		float errorAccumulator = 0;
		float xOffset1 = s1.getStateVarFromName(State.ObjectName.BODY, State.StateName.X);
		float xOffset2 = s2.getStateVarFromName(State.ObjectName.BODY, State.StateName.X);

		for (State.ObjectName bodyPart : State.ObjectName.values()) {
			for (State.StateName stateVar : State.StateName.values()) {

				float thisVal = s1.getStateVarFromName(bodyPart, stateVar) - ((stateVar == State.StateName.X) ? xOffset1 : 0);
				float otherVal = s2.getStateVarFromName(bodyPart, stateVar) - ((stateVar == State.StateName.X) ? xOffset2 : 0);
				float diff = thisVal - otherVal;
				errorAccumulator += State_Weights.getWeight(bodyPart, stateVar)*diff*diff;
			}
		}
		return errorAccumulator;
	}

	/** Handle loading the TFRecords and making the appropriate data structures. **/
	public void loadAll(List<File> files) {
		Utility.tic();
		List<SequenceExample> dataSeries = new ArrayList<SequenceExample>();
		FileInputStream fIn = null;
		for (File f : files) {
			dataSeries.clear();
			// Read TFRecord files.
			try {
				fIn = new FileInputStream(f);
				DataInputStream dIn = new DataInputStream(fIn);
				TFRecordReader tfReader = new TFRecordReader(dIn, true);

				while (fIn.available() > 0) {
					dataSeries.add(SequenceExample.parser().parseFrom(tfReader.read()));
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					fIn.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.out.println("Read " + dataSeries.size() + " runs from file " + f.getName());

			// Process into appropriate objects.
			for (SequenceExample seq : dataSeries) {
				int totalTimestepsInRun = seq.getFeatureLists().getFeatureListMap().get("BODY").getFeatureCount();

				RunHolder rh = new RunHolder();
				//System.out.println(totalTimestepsInRun);
				for (int i = 0; i < totalTimestepsInRun; i++) {
					// Unpack each x y th... value in a given timestep. Turn them into StateVariables.
					Map<String, FeatureList> featureListMap = seq.getFeatureLists().getFeatureListMap();
					StateVariable[] sVarBuffer = new StateVariable[State.ObjectName.values().length];
					int idx = 0;
					for (State.ObjectName bodyPart : State.ObjectName.values()) {
						List<Float> sValList = featureListMap.get(bodyPart.toString()).getFeature(i).getFloatList().getValueList();

						sVarBuffer[idx] = new StateVariable(sValList);
						idx++;
					}

					// Turn the StateVariables into a single State for this timestep.
					State st = new State(sVarBuffer[0], sVarBuffer[1], sVarBuffer[2], sVarBuffer[3], sVarBuffer[4], 
							sVarBuffer[5], sVarBuffer[6], sVarBuffer[7], sVarBuffer[8], sVarBuffer[9], sVarBuffer[10], sVarBuffer[11], false);

					byte[] keyPressBytes = seq.getFeatureLists().getFeatureListMap().get("PRESSED_KEYS").getFeature(i).getBytesList().getValue(0).toByteArray();
					boolean[] keyPress = new boolean[4];
					keyPress[0] = (keyPressBytes[0] == (byte)1) ? true : false;
					keyPress[1] = (keyPressBytes[1] == (byte)1) ? true : false;
					keyPress[2] = (keyPressBytes[2] == (byte)1) ? true : false;
					keyPress[3] = (keyPressBytes[3] == (byte)1) ? true : false;

					StateHolder newState = new StateHolder(st, keyPress, rh);
					allStates.put(st.getStateVarFromName(sortByPart, sortBySt), newState);
					numStatesLoaded++;
				}

				runs.add(rh);
			}
		}
		Utility.toc();
	}

	private class StateHolder{
		/** Actual physical state variables. **/
		final State state;

		/** QWOP keys pressed. **/
		final boolean[] keys;

		/** What run is this state a part of? **/
		final RunHolder parentRun;

		private StateHolder(State state, boolean[] keys, RunHolder parentRun) {
			this.state = state;
			this.keys = keys;
			this.parentRun = parentRun;
			parentRun.addState(this); // Each StateHolder adds itself to the run it's part of. Probably terrible programming, but I'm keeping it pretty contained here.
		}
	}
	private class RunHolder {
		/** All the states seen in this single run. **/
		List<StateHolder> states = new ArrayList<StateHolder>();

		/** Adds a state, in the order it's seen, to this run. Should only be automatically called. **/
		private void addState(StateHolder sh) { states.add(sh); }
	}

	@Override
	public void draw(Graphics g, GameLoader game, float runnerScaling, int xOffsetPixels, int yOffsetPixels) {
		if (currentTrajectory != null && currentState != null) {
			int startIdx = currentTrajectory.states.indexOf(currentState);
			float bodyX = currentState.state.body.x;
			for (int i = 0; i < currentTrajectory.states.size(); i++) {
				if (i % 20 == 0) {
					game.drawExtraRunner((Graphics2D)g, currentTrajectory.states.get(i).state, "", runnerScaling, xOffsetPixels - (int)(runnerScaling * bodyX), yOffsetPixels, PanelRunner.ghostGray, PanelRunner.normalStroke);
				}
			}
		}
	}
}
