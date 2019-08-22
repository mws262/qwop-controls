package data;

import game.action.Action;
import com.google.protobuf.ByteString;
import game.action.CommandQWOP;
import game.state.State;
import game.state.StateVariable;
import org.tensorflow.example.FeatureList;
import org.tensorflow.example.SequenceExample;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for parsing data loaded from TFRecords filled with QWOP run data. TFRecord data can by loaded into
 * a byte array by {@link TFRecordReader}. That step is generic to all types of TFRecord information. For our case,
 * all TFRecord data is a {@link SequenceExample} of a specific form. This class has methods for loading the byte
 * arrays into {@link SequenceExample}, and methods for parsing the {@link SequenceExample} into {@link State},
 * {@link game.action.Action}, etc.
 *
 * @author matt
 */
public class TFRecordDataParsers {

    /**
     * Take a TFRecord file containing {@link SequenceExample SequenceExamples} in binary form and parse them into a
     * list of the Java class. Each TFRecord may contain may SequenceExamples, representing many runs.
     *
     * @param tfrecordFile TFRecord binary file containing one or more SequenceExamples.
     * @return A list of loaded sequence examples.
     */
    public static List<SequenceExample> loadSequencesFromTFRecord(File tfrecordFile) throws IOException {
        List<SequenceExample> dataSeries = new ArrayList<>();
        try (FileInputStream fIn = new FileInputStream(tfrecordFile); DataInputStream dIn = new DataInputStream(fIn)) {
            TFRecordReader tfReader = new TFRecordReader(dIn, true);
            while (fIn.available() > 0) {
                dataSeries.add(SequenceExample.parser().parseFrom(tfReader.read()));
            }
        }
        return dataSeries;
    }

    /**
     * Takes a {@link SequenceExample} loaded from a TFRecord file and parses the sequence of states stored in it.
     * This array of states contains one {@link State} per timestep for one run.
     *
     * @param sequenceFromTFRecord One sequence loaded from a TFRecord that we wish to parse into a series of states
     * @return An array of states, one for each timestep in a run, from beginning to end.
     */
    public static State[] getStatesFromLoadedSequence(SequenceExample sequenceFromTFRecord) {
        int totalTimestepsInRun = sequenceFromTFRecord.getFeatureLists().getFeatureListMap().get("BODY").getFeatureCount();
        State[] stateVars = new State[totalTimestepsInRun];

        for (int i = 0; i < totalTimestepsInRun; i++) {
            // Unpack each x y th... value in a given timestep. Turn them into StateVariables.
            Map<String, FeatureList> featureListMap = sequenceFromTFRecord.getFeatureLists().getFeatureListMap();
            StateVariable[] sVarBuffer = new StateVariable[State.ObjectName.values().length];
            int idx = 0;
            for (State.ObjectName bodyPart : State.ObjectName.values()) {
                List<Float> sValList =
                        featureListMap.get(bodyPart.toString()).getFeature(i).getFloatList().getValueList();

                sVarBuffer[idx] = new StateVariable(sValList);
                idx++;
            }

            // Turn the StateVariables into a single State for this timestep.
            stateVars[i] = new State(sVarBuffer[0], sVarBuffer[1], sVarBuffer[2], sVarBuffer[3], sVarBuffer[4],
                    sVarBuffer[5], sVarBuffer[6], sVarBuffer[7], sVarBuffer[8], sVarBuffer[9], sVarBuffer[10], sVarBuffer[11], false);
        }
        return stateVars;
    }

    /**
     * Takes a {@link SequenceExample} loaded from a TFRecord file and parses the sequence of {@link Action game.action}
     * stored in it.
     *
     * @param sequenceFromTFRecord One sequence loaded from a TFRecord that we wish to parse into a series of Actions.
     * @return A list of game.action for the loaded run.
     */
    public static List<Action<CommandQWOP>> getActionsFromLoadedSequence(SequenceExample sequenceFromTFRecord) {
        List<Action<CommandQWOP>> actionList = new ArrayList<>();
        FeatureList actionFeatures = sequenceFromTFRecord.getFeatureLists().getFeatureListMap().get("ACTIONS");

        for (int i = 0; i < actionFeatures.getFeatureCount(); i++) {
            ByteString byteStringOfAction = actionFeatures.getFeature(i).getBytesList().getValue(0);
            int actionLength = Byte.toUnsignedInt(byteStringOfAction.byteAt(0)); // this is the [duration, q,w,o,p]

            assert !(Byte.toUnsignedInt(byteStringOfAction.byteAt(1)) > 1 || Byte.toUnsignedInt(byteStringOfAction.byteAt(2)) > 1 || Byte.toUnsignedInt(byteStringOfAction.byteAt(3)) > 1 || Byte.toUnsignedInt(byteStringOfAction.byteAt(4)) > 1);

            boolean Q = (byte)1 == byteStringOfAction.byteAt(1);
            boolean W = (byte)1 == byteStringOfAction.byteAt(2);
            boolean O = (byte)1 == byteStringOfAction.byteAt(3);
            boolean P = (byte)1 == byteStringOfAction.byteAt(4);

            actionList.add(new Action<>(actionLength, CommandQWOP.booleansToCommand(Q, W, O, P)));
        }
        return actionList;
    }

    /**
     * Takes a {@link SequenceExample} loaded from a TFRecord file and parses the sequence of commands, i.e. a
     * true/false for each of the Q, W, O, and P keys for each timestep in the sequence.
     *
     * @param sequenceFromTFRecord One sequence loaded from a TFRecord that we wish to parse into a commands on a
     *                             per-timestep basis.
     * @return 2D boolean array containing which keys should be pressed. 1st dimension is timesteps, from start to
     * end. 2nd dimension is QWOP keypress boolean flags.
     */
    public static CommandQWOP[] getCommandSequenceFromLoadedSequence(SequenceExample sequenceFromTFRecord) {
        FeatureList pressedKeysFeatures = sequenceFromTFRecord.getFeatureLists().getFeatureListMap().get("PRESSED_KEYS");
        CommandQWOP[] commandSequence = new CommandQWOP[pressedKeysFeatures.getFeatureCount()];

        for (int i = 0; i < pressedKeysFeatures.getFeatureCount(); i++) {
            byte[] keyPressBytes =
                    sequenceFromTFRecord.getFeatureLists().getFeatureListMap().get("PRESSED_KEYS").getFeature(i).getBytesList().getValue(0).toByteArray();
            commandSequence[i] = CommandQWOP.booleansToCommand(keyPressBytes[0] == (byte) 1,
                    keyPressBytes[1] == (byte) 1,
                    keyPressBytes[2] == (byte) 1,
                    keyPressBytes[3] == (byte) 1);
        }
        return commandSequence;
    }
}
