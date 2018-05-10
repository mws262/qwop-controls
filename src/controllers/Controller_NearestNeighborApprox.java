package controllers;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.tensorflow.example.FeatureList;
import org.tensorflow.example.SequenceExample;

import data.TFRecordReader;
import game.GameLoader;
import game.State;
import game.StateVariable;
import main.Action;
import main.IController;
import main.PanelRunner;
import main.State_Weights;


public class Controller_NearestNeighborApprox implements IController {
	// TODO: "Snapping" to certain sequences.
	// TODO: Preferential treatment of sequences with more than a few timesteps remaining.
	// TODO: Recent history matching.


	/** How many nearby (in terms of body theta) states are compared when determining the "closest" one. **/
	public int stateCompareNeighborhoodSize = 5000;

	/** Total number of states loaded from TFRecord file. **/
	private int numStatesLoaded = 0;

	/** All states loaded, regardless of run, in sorted order, by body theta. **/
	private NavigableMap<Float, StateHolder> allStates = new TreeMap<Float, StateHolder>();

	/** All runs loaded. **/
	public Set<RunHolder> runs = new HashSet<RunHolder>(); 

	/** Keep track of which total run that the currently selected action comes from. **/
	private RunHolder currentTrajectory;
	private StateHolder currentState;


	public Controller_NearestNeighborApprox(List<File> files) {
		loadAll(files);
		System.out.println("Wow! " + numStatesLoaded + " states were loaded!");
	}

	@Override
	public Action policy(State state) {
		//Utility.tic();
		// Get nearest states (determined ONLY by body theta).
		NavigableMap<Float, StateHolder> lowerSet = allStates.headMap(state.body.th, true).descendingMap();
		NavigableMap<Float, StateHolder> upperSet = allStates.tailMap(state.body.th, true);

		Iterator<StateHolder> iterLower = lowerSet.values().iterator();
		Iterator<StateHolder> iterUpper = upperSet.values().iterator();

		StateHolder bestSoFar = null;
		float bestErrorSoFar = Float.MAX_VALUE;

		// Check full sq error with nearest bunch of states with lower body theta
		int maxHalfExtent = 5000;
		int counter = 0;
		while (iterLower.hasNext() && counter++ < maxHalfExtent) {
			StateHolder st = iterLower.next();
			float error = sqError(st.state, state);

			int parentRunLength = st.parentRun.states.size();
			int stateLocInSequence = st.parentRun.states.indexOf(st);

			error += 100f*(float)stateLocInSequence/(float)parentRunLength;

			if (error < bestErrorSoFar) {
				bestSoFar = st;
				bestErrorSoFar = error;
			}
		}
		//System.out.println(counter + " compared from below.");

		// Check full sq error with nearest bunch of states with higher body theta
		counter = 0;
		while (iterUpper.hasNext() && counter++ < maxHalfExtent) {
			StateHolder st = iterUpper.next();
			float error = sqError(st.state, state);

			int parentRunLength = st.parentRun.states.size();
			int stateLocInSequence = st.parentRun.states.indexOf(st);

			error += 25f*(float)stateLocInSequence/(float)parentRunLength;
			if (error < bestErrorSoFar) {
				bestSoFar = st;
				bestErrorSoFar = error;
			}
		}
		System.out.println(bestErrorSoFar);
		//System.out.println(counter + " compared8 from above.");

		//Utility.toc();

		// Choose whether to stay on the previous trajectory or not.
		if (currentTrajectory != null) {
			int lastStIdx = currentTrajectory.states.indexOf(currentState);

			if (lastStIdx < currentTrajectory.states.size() - 2) {

				StateHolder nextStateOnOldTraj = currentTrajectory.states.get(lastStIdx + 1);
				float oldTrajError = sqError(nextStateOnOldTraj.state, state);

				if (bestErrorSoFar + 5 < oldTrajError) {
					currentTrajectory = bestSoFar.parentRun;
					currentState = bestSoFar;
				}else {
					System.out.print("SNAP");
					currentState = nextStateOnOldTraj;
				}
			}else {
				currentTrajectory = bestSoFar.parentRun;
				currentState = bestSoFar;
			}
		}else {

			currentTrajectory = bestSoFar.parentRun;
			currentState = bestSoFar;
		}


		return new Action(1, currentState.keys);
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
					allStates.put(st.body.th, newState);

					numStatesLoaded++;
				}

				runs.add(rh);

			}
		}	
	}

	private class StateHolder{
		/** Actual physical state variables. **/
		State state;

		/** QWOP keys pressed. **/
		boolean[] keys;

		/** What run is this state a part of? **/
		RunHolder parentRun;

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
			for (int i = startIdx + 1; i < currentTrajectory.states.size(); i++) {
				if (i % 20 == 0) {
					game.drawExtraRunner((Graphics2D)g, currentTrajectory.states.get(i).state, "", runnerScaling, xOffsetPixels - (int)(runnerScaling * bodyX), yOffsetPixels, PanelRunner.ghostGray, PanelRunner.normalStroke);
				}
			}
		}
	}
}
