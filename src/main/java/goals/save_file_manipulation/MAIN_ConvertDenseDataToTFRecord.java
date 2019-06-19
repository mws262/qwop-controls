package goals.save_file_manipulation;

import game.action.Action;
import com.google.protobuf.ByteString;
import data.SavableDenseData;
import data.SavableFileIO;
import data.TFRecordWriter;
import game.state.IState;
import game.state.State;
import game.state.StateVariable;
import org.tensorflow.example.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MAIN_ConvertDenseDataToTFRecord {

    @SuppressWarnings("WeakerAccess")
    static String sourceDir = "/media/matt/Storage/QWOP_SaveableDenseData/";
    @SuppressWarnings("WeakerAccess")
    static String outDir = "/media/matt/Storage/QWOP_Tfrecord_1_20/";
    @SuppressWarnings("WeakerAccess")
    static String inFileExt = "SavableDenseData";
    @SuppressWarnings("WeakerAccess")
    static String outFileExt = "tfrecord";

    public static void main(String[] args) {

        // Grab input files.
        System.out.println("Identifying input files...");
        File inDir = new File(sourceDir);
        if (!inDir.exists()) throw new RuntimeException("Input directory does not exist here: " + inDir.getName());

        double megabyteCount = 0;
        ArrayList<File> inFiles = new ArrayList<>();
        for (File file : Objects.requireNonNull(inDir.listFiles())) {
            if (!file.isDirectory()) {
                String extension = "";
                // Get only files with the correct file extension.
                int i = file.getName().lastIndexOf('.');
                if (i > 0) {
                    extension = file.getName().substring(i + 1);
                }
                if (extension.equals(inFileExt)) {
                    inFiles.add(file);
                    megabyteCount += file.length() / 1.0e6;
                } else {
                    System.out.println("Ignoring file in input directory: " + file.getName());
                }
            }
        }

        System.out.println("Found " + inFiles.size() + " input files with the extension " + inFileExt + ".");
        System.out.println("Total input size: " + Math.round(megabyteCount * 10) / 10. + " MB.");

        System.out.println("done");


        SavableFileIO<SavableDenseData> inFileLoader = new SavableFileIO<>();
        int count = 0;
        for (File file : inFiles) {
            List<SavableDenseData> denseDat = new ArrayList<>();
            inFileLoader.loadObjectsToCollection(file, denseDat);
            System.out.print("Beginning to package " + file.getName() + ". ");
            String fileOutName = file.getName().substring(0, file.getName().lastIndexOf('.')) + "." + outFileExt;
            try {
                convertToProtobuf(denseDat, fileOutName, outDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            count++;
            System.out.println("Done. " + count + "/" + inFiles.size());
        }
    }

    /**
     * Make a single feature representing the 6 state variables for a single body part at a single timestep. Append to
     * existing FeatureList for that body part.
     **/
    private static void makeFeature(IState.ObjectName bodyPart, IState state, FeatureList.Builder listToAppendTo) {
        Feature.Builder feat = Feature.newBuilder();
        FloatList.Builder featVals = FloatList.newBuilder();
        for (StateVariable.StateName stateName : StateVariable.StateName.values()) { // Iterate over all 6 state variables.
            featVals.addValue(state.getStateVariableFromName(bodyPart).getStateByName(stateName));
        }
        feat.setFloatList(featVals.build());
        listToAppendTo.addFeature(feat.build());
    }

    /**
     * Make a time series for a single run of a single state variable as a FeatureList. Add to the broader list of
     * FeatureList for this run.
     **/
    private static void makeStateFeatureList(SavableDenseData data, IState.ObjectName bodyPart,
                                             FeatureLists.Builder featLists) {
        FeatureList.Builder featList = FeatureList.newBuilder();
        for (IState st : data.getState()) { // Iterate through the timesteps in a single run.
            makeFeature(bodyPart, st, featList);
        }
        featLists.putFeatureList(bodyPart.toString(), featList.build()); // Add this feature to the broader list of
        // features.
    }

    private static void convertToProtobuf(List<SavableDenseData> denseData, String fileName, String destinationPath) throws IOException {
        File file = new File(destinationPath + fileName);

        file.getParentFile().mkdirs();
        FileOutputStream stream = new FileOutputStream(file);

        // Iterate through all runs in a single file.
        for (SavableDenseData dat : denseData) {
            int actionPad = dat.getState().length - dat.getAction().length; // Make the dimensions match for coding
            // convenience.
            if (actionPad != 1) {
                System.out.println("Dimensions of state is not 1 more than dimension of action as expected. Ignoring " +
                        "this one.");
                continue;
            }
            Example.Builder exB = Example.newBuilder();
            SequenceExample.Builder seqEx = SequenceExample.newBuilder();
            FeatureLists.Builder featLists = FeatureLists.newBuilder(); // All features (states & game.action) in a
            // single run.

            // Pack up states
            for (State.ObjectName bodyPart : State.ObjectName.values()) { // Make feature lists for all the body
                // parts and add to the overall list of feature lists.
                makeStateFeatureList(dat, bodyPart, featLists);
            }

            // Pack up game.action -- 3 different ways:
            // 1) Keys pressed at individual timestep.
            // 2) Timesteps until transition for each timestep.
            // 3) Just the action sequence (shorter than number of timesteps)

            // 1) Keys pressed at individual timestep. 0 or 1 in bytes for each key
            FeatureList.Builder keyFeatList = FeatureList.newBuilder();
            for (Action act : dat.getAction()) {
                Feature.Builder keyFeat = Feature.newBuilder();
                BytesList.Builder keyDat = BytesList.newBuilder();
                byte[] keys = new byte[]{
                        act.peek()[0] ? (byte) (1) : (byte) (0),
                        act.peek()[1] ? (byte) (1) : (byte) (0),
                        act.peek()[2] ? (byte) (1) : (byte) (0),
                        act.peek()[3] ? (byte) (1) : (byte) (0)};
                keyDat.addValue(ByteString.copyFrom(keys));
                keyFeat.setBytesList(keyDat.build());
                keyFeatList.addFeature(keyFeat.build());
            }
            // Pad it by one.
            Feature.Builder keyFeat = Feature.newBuilder();
            BytesList.Builder keyDat = BytesList.newBuilder();
            byte[] keys = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0};
            keyDat.addValue(ByteString.copyFrom(keys));
            keyFeat.setBytesList(keyDat.build());
            keyFeatList.addFeature(keyFeat.build());

            featLists.putFeatureList("PRESSED_KEYS", keyFeatList.build());


            // 2) Timesteps until transition for each timestep.
            FeatureList.Builder transitionTSList = FeatureList.newBuilder();
            for (int i = 0; i < dat.getAction().length; i++) {
                int action = dat.getAction()[i].getTimestepsTotal();

                while (action > 0 && i < dat.getAction().length) {
                    Feature.Builder transitionTSFeat = Feature.newBuilder();
                    BytesList.Builder transitionTS = BytesList.newBuilder();
                    transitionTS.addValue(ByteString.copyFrom(new byte[]{(byte) action}));
                    transitionTSFeat.setBytesList(transitionTS.build());
                    transitionTSList.addFeature(transitionTSFeat.build());

                    action--;
                    i++;
                }
            }
            // Pad by one.
            Feature.Builder transitionTSFeat = Feature.newBuilder();
            BytesList.Builder transitionTS = BytesList.newBuilder();
            transitionTS.addValue(ByteString.copyFrom(new byte[]{(byte) 0}));
            transitionTSFeat.setBytesList(transitionTS.build());
            transitionTSList.addFeature(transitionTSFeat.build());

            featLists.putFeatureList("TIME_TO_TRANSITION", transitionTSList.build());

            // 3) Just the action sequence (shorter than number of timesteps) -- bytestrings e.g. [15, 1, 0, 0, 1]
            FeatureList.Builder actionList = FeatureList.newBuilder();
            int prevAct = -1;
            for (Action act : dat.getAction()) {
                int action = act.getTimestepsTotal();
                if (action != prevAct) {
                    prevAct = action;
                    Feature.Builder sequenceFeat = Feature.newBuilder();
                    BytesList.Builder seqList = BytesList.newBuilder();

                    byte[] actionBytes = new byte[]{(byte) action,
                            act.peek()[0] ? (byte) (1) : (byte) (0),
                            act.peek()[1] ? (byte) (1) : (byte) (0),
                            act.peek()[2] ? (byte) (1) : (byte) (0),
                            act.peek()[3] ? (byte) (1) : (byte) (0)};

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
            tsList.addValue(dat.getState().length); // TOTAL number of timesteps in this run.
            timestepContext.setInt64List(tsList.build());
            contextFeats.putFeature("TIMESTEPS", timestepContext.build());

            seqEx.setContext(contextFeats.build());

            seqEx.setFeatureLists(featLists.build());
            TFRecordWriter.writeToStream(seqEx.build().toByteArray(), stream);
        }
        stream.close();
    }
}
