package data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;

import main.State;
import main.State.StateVariable;
import main.Action;


public class DenseDataToTFRecord {

	static String sourceDir = ".";
	static String inFileExt = "SaveableDenseData";

	public static void main(String[] args) {

		/** Grab input files. **/
		System.out.println("Identifying input files...");
		File inDir = new File(sourceDir);
		if (!inDir.exists()) throw new RuntimeException("Input directory does not exist here: " + inDir.getName());

		double megabyteCount = 0;
		ArrayList<File> inFiles = new ArrayList<File>();
		for(File file: inDir.listFiles()) {	
			if (!file.isDirectory()) {
				String extension = "";
				// Get only files with the correct file extension.
				int i = file.getName().lastIndexOf('.');
				if (i > 0) {
					extension = file.getName().substring(i+1);
				}
				if (extension.equals(inFileExt)) {
					inFiles.add(file);	
					megabyteCount += file.length()/1.0e6;
				}else {
					System.out.println("Ignoring file in input directory: " + file.getName());
				}
			}
		}

		System.out.println("Found " + inFiles.size() + " input files with the extension " + inFileExt + ".");
		System.out.println("Total input size: " + Math.round(megabyteCount*10)/10. + " MB.");

		System.out.println("done");


		SaveableFileIO<SaveableDenseData> inFileLoader = new SaveableFileIO<SaveableDenseData>();
		int count = 0;
		for (File file : inFiles) {
			ArrayList<SaveableDenseData> denseDat = inFileLoader.loadObjectsOrdered(file.getAbsolutePath());
			System.out.print("Beginning to package " + file.getName() + ". ");
			String fileOutName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".NEWNEWNEW";
			try {
				convertToProtobuf(denseDat,fileOutName);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			count++;
			System.out.println("Done. " + count + "/" + inFiles.size());

			//			// Validate -- not needed during batch run.
			//			DataSet dataValidate = null;
			//			try {
			//				FileInputStream fIn = new FileInputStream(fileOutName);
			//				
			//				dataValidate = DataSet.parseFrom(new FileInputStream(fileOutName));
			//			} catch (IOException e) {
			//				// TODO Auto-generated catch block
			//				e.printStackTrace();
			//			}
			//			DenseData.State state1 = dataValidate.getDenseData(0).getState(10);
			//			float dx1 = state1.getBody().getDx();
			//			System.out.println("From new file: " + dx1 + ". From original data: " + denseDat.get(0).getState()[10].body.dx);
			//			
			//			
			//			DenseData.State state2 = dataValidate.getDenseData(10).getState(3);
			//			float th1 = state2.getHead().getTh();
			//			System.out.println("From new file: " + th1 + ". From original data: " + denseDat.get(10).getState()[3].head.th);
			//			break;
			//			
		}
	}

	/** Make a single feature representing the 6 state variables for a single body part at a single timestep. Append to existing FeatureList for that body part. **/
	private static void makeFeature(State.ObjectName bodyPart, State state, FeatureList.Builder listToAppendTo) {
		Feature.Builder feat = Feature.newBuilder();
		FloatList.Builder featVals = FloatList.newBuilder();
		for (State.StateName stateName : State.StateName.values()) { // Iterate over all 6 state variables.
			featVals.addValue(state.getStateVarFromName(bodyPart, stateName));
		}
		feat.setFloatList(featVals.build());
		listToAppendTo.addFeature(feat);
	}

	/** Make a time series for a single run of a single state variable as a FeatureList. Add to the broader list of FeatureList for this run. **/
	private static void makeStateFeatureList(SaveableDenseData data, State.ObjectName bodyPart, FeatureLists.Builder featLists) {
		FeatureList.Builder featList = FeatureList.newBuilder();
		for (State st : data.getState()) { // Iterate through the timesteps in a single run.
			makeFeature(bodyPart, st, featList);
		}
		featLists.putFeatureList(bodyPart.toString(), featList.build()); // Add this feature to the broader list of features.
	}

	public static void convertToProtobuf(List<SaveableDenseData> denseData, String fileName) throws IOException {
		FileOutputStream stream = new FileOutputStream(new File(fileName));
		
		// Iterate through all runs in a single file.
		for (SaveableDenseData dat : denseData) {
			SequenceExample.Builder seqEx = SequenceExample.newBuilder();
			FeatureLists.Builder featLists = FeatureLists.newBuilder(); // All features (states & actions) in a single run.

			// Pack up states
			for (State.ObjectName bodyPart : State.ObjectName.values()) { // Make feature lists for all the body parts and add to the overall list of feature lists.
				makeStateFeatureList(dat, bodyPart, featLists);
			}

			// Pack up actions -- 3 different ways:
			// 1) Keys pressed at individual timestep.
			// 2) Timesteps until transition for each timestep.
			// 3) Just the action sequence (shorter than number of timesteps)

			// 1) Keys pressed at individual timestep. 0 or 1 in bytes for each key
			FeatureList.Builder keyFeatList = FeatureList.newBuilder();
			for (Action act : dat.getAction()) {
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
			featLists.putFeatureList("PRESSED_KEYS", keyFeatList.build());

			// 2) Timesteps until transition for each timestep.
			FeatureList.Builder transitionTSList = FeatureList.newBuilder();
			for (int i = 0; i < dat.getAction().length; i++) {
				int action = dat.getAction()[i].getTimestepsTotal();

				while (action > 0 && i < dat.getAction().length) {
					Feature.Builder transitionTSFeat = Feature.newBuilder();
					BytesList.Builder transitionTS = BytesList.newBuilder();
					transitionTS.addValue(ByteString.copyFrom(new byte[] {(byte)action}));
					transitionTSFeat.setBytesList(transitionTS.build());
					transitionTSList.addFeature(transitionTSFeat.build());

					action--;
					i++;
				}
			}
			featLists.putFeatureList("TIME_TO_TRANSITION", transitionTSList.build());

			// 3) Just the action sequence (shorter than number of timesteps) -- bytestrings e.g. [15, 1, 0, 0, 1]
			FeatureList.Builder actionList = FeatureList.newBuilder();
			int prevAct = -1;
			for (Action act : dat.getAction()) {
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
			featLists.putFeatureList("ACTIONS", transitionTSList.build());
			
			seqEx.setFeatureLists(featLists.build());
			seqEx.build().writeTo(stream);
		}
		stream.close();
	}
}
