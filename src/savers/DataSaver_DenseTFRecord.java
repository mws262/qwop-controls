package savers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.tensorflow.example.BytesList;
import org.tensorflow.example.Feature;
import org.tensorflow.example.FeatureList;
import org.tensorflow.example.FeatureLists;
import org.tensorflow.example.Features;
import org.tensorflow.example.FloatList;
import org.tensorflow.example.Int64List;
import org.tensorflow.example.SequenceExample;

import com.google.protobuf.ByteString;

import data.TFRecordWriter;
import game.State;
import main.Action;
import main.IDataSaver;
import main.Node;

public class DataSaver_DenseTFRecord extends DataSaver_Dense{
	
	/** File prefix. Goes in front of date. **/
	public String filePrefix = "denseTF";
	
	/** Do not include dot before. **/
	public String fileExtension = "TFRecord";
	
	/** Games since last save. **/
	private int saveCounter = 0;
	
	/** List of sets of states and actions for individual games awaiting file write. **/
	ArrayList<GameContainer> gameData = new ArrayList<GameContainer>();
	
	@Override
	public void reportGameEnding(Node endNode) {
		saveCounter++;
		gameData.add(new GameContainer(actionBuffer, stateBuffer));
		
		if (saveInterval == saveCounter) {
			try {
				convertToProtobuf();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Reset until next file write interval is up.
			gameData.clear();
			saveCounter = 0;
		}
		
		// Clear out for the next run to begin.
		stateBuffer.clear();
		actionBuffer.clear();
		
	}

	// NOTE: The following methods were borrowed and altered from my DenseDataToTFRecord class.
	
	/** Make a single feature representing the 6 state variables for a single 
	 * body part at a single timestep. Append to existing FeatureList for that body part. 
	 **/
	private static void makeFeature(State.ObjectName bodyPart, State state, FeatureList.Builder listToAppendTo) {
		Feature.Builder feat = Feature.newBuilder();
		FloatList.Builder featVals = FloatList.newBuilder();
		for (State.StateName stateName : State.StateName.values()) { // Iterate over all 6 state variables.
			featVals.addValue(state.getStateVarFromName(bodyPart, stateName));
		}
		feat.setFloatList(featVals.build());
		listToAppendTo.addFeature(feat.build());
	}

	/** Make a time series for a single run of a single state variable as a 
	 * FeatureList. Add to the broader list of FeatureList for this run. 
	 * **/
	private static void makeStateFeatureList(ArrayList<State> states, State.ObjectName bodyPart, FeatureLists.Builder featLists) {
		FeatureList.Builder featList = FeatureList.newBuilder();
		for (State st : states) { // Iterate through the timesteps in a single run.
			makeFeature(bodyPart, st, featList);
		}
		featLists.putFeatureList(bodyPart.toString(), featList.build()); // Add this feature to the broader list of features.
	}

	private void convertToProtobuf() throws IOException {
		File file = new File(fileLocation + IDataSaver.generateFileName(filePrefix, fileExtension));
		
		file.getParentFile().mkdirs();
		FileOutputStream stream = new FileOutputStream(file);
		
		// Iterate through all runs in a single file.
		for (GameContainer dat : gameData) {
			int actionPad = dat.states.size() - dat.actions.size(); // Make the dimensions match for coding convenience.
			if (actionPad != 1) {
				System.out.println("Dimensions of state is not 1 more than dimension of action as expected. Ignoring this one.");
				continue;
			}
			SequenceExample.Builder seqEx = SequenceExample.newBuilder();
			FeatureLists.Builder featLists = FeatureLists.newBuilder(); // All features (states & actions) in a single run.

			// Pack up states
			for (State.ObjectName bodyPart : State.ObjectName.values()) { // Make feature lists for all the body parts and add to the overall list of feature lists.
				makeStateFeatureList(dat.states, bodyPart, featLists);
			}

			// Pack up actions -- 3 different ways:
			// 1) Keys pressed at individual timestep.
			// 2) Timesteps until transition for each timestep.
			// 3) Just the action sequence (shorter than number of timesteps)

			// 1) Keys pressed at individual timestep. 0 or 1 in bytes for each key
			FeatureList.Builder keyFeatList = FeatureList.newBuilder();
			for (Action act : actionBuffer) {
				Feature.Builder keyFeat = Feature.newBuilder();
				BytesList.Builder keyDat = BytesList.newBuilder();
				byte[] keys = new byte[] {
						act.peek()[0] ? (byte)(1) : (byte)(0),
						act.peek()[1] ? (byte)(1) : (byte)(0),
						act.peek()[2] ? (byte)(1) : (byte)(0),
						act.peek()[3] ? (byte)(1) : (byte)(0)};
				keyDat.addValue(ByteString.copyFrom(keys));
				keyFeat.setBytesList(keyDat.build());
				keyFeatList.addFeature(keyFeat.build());
			}
			// Pad it by one.
			Feature.Builder keyFeat = Feature.newBuilder();
			BytesList.Builder keyDat = BytesList.newBuilder();
			byte[] keys = new byte[] {(byte)0, (byte)0, (byte)0, (byte)0};
			keyDat.addValue(ByteString.copyFrom(keys));
			keyFeat.setBytesList(keyDat.build());
			keyFeatList.addFeature(keyFeat.build());
			
			featLists.putFeatureList("PRESSED_KEYS", keyFeatList.build());

			
			// 2) Timesteps until transition for each timestep.
			FeatureList.Builder transitionTSList = FeatureList.newBuilder();
			for (int i = 0; i < dat.actions.size(); i++) {
				int action = dat.actions.get(i).getTimestepsTotal();

				while (action > 0 && i < dat.actions.size()) {
					Feature.Builder transitionTSFeat = Feature.newBuilder();
					BytesList.Builder transitionTS = BytesList.newBuilder();
					transitionTS.addValue(ByteString.copyFrom(new byte[] {(byte)action}));
					transitionTSFeat.setBytesList(transitionTS.build());
					transitionTSList.addFeature(transitionTSFeat.build());

					action--;
					i++;
				}
			}
			// Pad by one.
			Feature.Builder transitionTSFeat = Feature.newBuilder();
			BytesList.Builder transitionTS = BytesList.newBuilder();
			transitionTS.addValue(ByteString.copyFrom(new byte[] {(byte)0}));
			transitionTSFeat.setBytesList(transitionTS.build());
			transitionTSList.addFeature(transitionTSFeat.build());
			
			featLists.putFeatureList("TIME_TO_TRANSITION", transitionTSList.build());

			// 3) Just the action sequence (shorter than number of timesteps) -- bytestrings e.g. [15, 1, 0, 0, 1]
			FeatureList.Builder actionList = FeatureList.newBuilder();
			int prevAct = -1;
			for (Action act : dat.actions) {
				int action = act.getTimestepsTotal();
				if (action == prevAct) {
					continue;
				}else {
					prevAct = action;
					Feature.Builder sequenceFeat = Feature.newBuilder();
					BytesList.Builder seqList = BytesList.newBuilder();
					
					byte[] actionBytes = new byte[] {(byte)action,
							act.peek()[0] ? (byte)(1) : (byte)(0),
							act.peek()[1] ? (byte)(1) : (byte)(0),
							act.peek()[2] ? (byte)(1) : (byte)(0),
							act.peek()[3] ? (byte)(1) : (byte)(0)};
							
					seqList.addValue(ByteString.copyFrom(actionBytes));
					sequenceFeat.setBytesList(seqList.build());
					actionList.addFeature(sequenceFeat.build());
				}
			}
			featLists.putFeatureList("ACTIONS", actionList.build());
			
			// Adding the timesteps as the context for each sequence.
			Features.Builder contextFeats = Features.newBuilder();
			Feature.Builder timestepContext = Feature.newBuilder();			
			Int64List.Builder tsList = Int64List.newBuilder();
			tsList.addValue(dat.states.size()); // TOTAL number of timesteps in this run.
			timestepContext.setInt64List(tsList.build());
			contextFeats.putFeature("TIMESTEPS", timestepContext.build());
			
			seqEx.setContext(contextFeats.build());

			seqEx.setFeatureLists(featLists.build());
			TFRecordWriter.writeToStream(seqEx.build().toByteArray(), stream);
		}
		stream.close();
	}
	
	/**
	 * Just a holder for the states and actions of individual games.
	 * Just so we can add these combinations to another list.
	 * @author matt
	 *
	 */
	private class GameContainer{
		
		ArrayList<Action> actions = new ArrayList<Action>();
		ArrayList<State> states = new ArrayList<State>();
		
		private GameContainer(ArrayList<Action> actions, ArrayList<State> states) {
			this.actions.addAll(actions);
			this.states.addAll(states);
		}
	}
}
