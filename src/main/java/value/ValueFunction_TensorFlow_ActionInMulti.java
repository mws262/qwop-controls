package value;

import actions.Action;
import tree.Node;

import java.io.File;
import java.util.*;

/**
 * Another {@link IValueFunction} which essentially wraps multiple {@link ValueFunction_TensorFlow_ActionIn}. It uses
 * one for each key combination instead of only one for all key combinations.
 *
 * @author matt
 */
public class ValueFunction_TensorFlow_ActionInMulti implements IValueFunction {

    /**
     * All the valid key combinations.
     */
    private enum KeyCombination {
        NN, QN, QO, QP, WN, WO, WP, NO, NP
    }

    /**
     * The key combination definitions.
     */
    private static final boolean[]
            nn = new boolean[]{false, false, false, false},
            qn = new boolean[]{true, false, false, false},
            qo = new boolean[]{true, false, true, false},
            qp = new boolean[]{true, false, false, true},
            wn = new boolean[]{false, true, false, false},
            wo = new boolean[]{false, true, true, false},
            wp = new boolean[]{false, true, false, true},
            no = new boolean[]{false, false, true, false},
            np = new boolean[]{false, false, false, true};

    /**
     * All the {@link Action} durations which could be selected by this {@link IValueFunction}.
     */
    private Map<KeyCombination, Set<Integer>> availableActions = new HashMap<>();

    /**
     * The value functions associated with all the key combinations.
     */
    private Map<KeyCombination, ValueFunction_TensorFlow_ActionIn> allValueFunctions = new HashMap<>();

    /**
     * Creates a new value function and figures out which actions need to be possibly classified by the value
     * function. This does not load models directly, and so is not public. Call
     * {@link ValueFunction_TensorFlow_ActionInMulti#makeNew(Collection, String, List, List)} or
     * {@link ValueFunction_TensorFlow_ActionInMulti#loadExisting(Collection, String, String)}.
     * @param allPossibleActions ALl actions which this value function will be aware of.
     */
    private ValueFunction_TensorFlow_ActionInMulti(Collection<Action> allPossibleActions) {

        // Sort out all the actions we'll potentially have to differentiate between.
        for (Action a : allPossibleActions) {
            KeyCombination actionKeys = getKeyCombination(a.peek());

            // Only add key combinations to the available options if they actually appear.
            if (!availableActions.containsKey(actionKeys)) {
                availableActions.put(actionKeys, new HashSet<>());
            }

            Set<Integer> keyDurations = availableActions.get(actionKeys);
            keyDurations.add(a.getTimestepsTotal());
        }
    }

    @Override
    public Action getMaximizingAction(Node currentNode) {
        KeyCombination nodeActionKeys =
                getKeyCombination(Node.potentialActionGenerator.getPotentialChildActionSet(currentNode).get(0).peek());
        ValueFunction_TensorFlow_ActionIn valFun = allValueFunctions.get(nodeActionKeys);
//        for (ValueFunction_TensorFlow_ActionIn valFunny : allValueFunctions.values()) {
//            System.out.println(valFunny.getMaximizingAction(currentNode));
//        }
        return valFun.getMaximizingAction(currentNode);
    }

    @Override
    public float evaluate(Node currentNode) {
        // Get the key combination associated with the action taking the game to this node.
        KeyCombination nodeKeyCombo = getKeyCombination(currentNode.getAction().peek());
        // Evaluated the value function associated with that key combination.
        return allValueFunctions.get(nodeKeyCombo).evaluate(currentNode);
    }

    @Override
    public void update(List<Node> nodes) {
        // Update the value function for each of the different keys.

        for (Map.Entry<KeyCombination, ValueFunction_TensorFlow_ActionIn> entry : allValueFunctions.entrySet()) {
            KeyCombination keys = entry.getKey();
            ValueFunction_TensorFlow_ActionIn valFun = entry.getValue();

            List<Node> thisKeyNodes = new ArrayList<>();
            for (Node n : nodes) {
                if (getKeyCombination(n.getAction().peek()) == keys) {
                    thisKeyNodes.add(n);
                }
            }
            valFun.update(thisKeyNodes);
        }
    }

    /**
     * Load checkpoints of all the nets involved.
     * @param checkpointPrefix Prefix of the filename. The suffix will be the key combinations involved.
     */
    public void loadCheckpoints(String checkpointPrefix) {
        assert !checkpointPrefix.isEmpty();
        for (Map.Entry<KeyCombination, ValueFunction_TensorFlow_ActionIn> entry : allValueFunctions.entrySet()) {
            entry.getValue().loadCheckpoint(checkpointPrefix + entry.getKey().name());
        }
    }

    /**
     * Save checkpoints of all the nets involved.
     * @param checkpointPrefix Prefix of the filename. The suffix will be the key combinations involved.
     */
    public void saveCheckpoints(String checkpointPrefix) {
        assert !checkpointPrefix.isEmpty();
        for (Map.Entry<KeyCombination, ValueFunction_TensorFlow_ActionIn> entry : allValueFunctions.entrySet()) {
            entry.getValue().saveCheckpoint(checkpointPrefix + entry.getKey().name());
        }
    }

    /**
     * Get the enum KeyCombination from the array of booleans representing the keypresses.
     * @param keys 4-element array of booleans representing keys pressed.
     * @return The enum representation of that key combination.
     */
    private static KeyCombination getKeyCombination(boolean[] keys) {
        KeyCombination combination;
        if (Arrays.equals(keys, nn)) {
            combination = KeyCombination.NN;
        } else if (Arrays.equals(keys, qn)) {
            combination = KeyCombination.QN;
        } else if (Arrays.equals(keys, qo)) {
            combination = KeyCombination.QO;
        } else if (Arrays.equals(keys, qp)) {
            combination = KeyCombination.QP;
        } else if (Arrays.equals(keys, wn)) {
            combination = KeyCombination.WN;
        } else if (Arrays.equals(keys, wo)) {
            combination = KeyCombination.WO;
        } else if (Arrays.equals(keys, wp)) {
            combination = KeyCombination.WP;
        } else if (Arrays.equals(keys, no)) {
            combination = KeyCombination.NO;
        } else if (Arrays.equals(keys, np)) {
            combination = KeyCombination.NP;
        } else {
            throw new IllegalArgumentException("Invalid key combination.");
        }
        return combination;
    }

    /**
     * Load an existing set of value functions using existing TensorFlow files. Note that the dimensionality of
     * inputs/outputs is NOT checked and will cause issues later on if incorrect. Also, if extra actions have been
     * added to allPossibleActions since when the models were created, then some may be missing. NOTE: If checkpoint
     * files exist, these need to be loaded separately with
     * {@link ValueFunction_TensorFlow_ActionInMulti#loadCheckpoints(String)}.
     * @param allPossibleActions All possible actions that this value function should be able to differentiate between.
     * @param fileNamePrefix Saved model file prefix (e.g. if files are testQP.pb..., then the prefix would be "test").
     * @param modelPath Location of the model files relative to the base project directory.
     * @return ValueFunction created using existing model files.
     */
    public static ValueFunction_TensorFlow_ActionInMulti loadExisting(Collection<Action> allPossibleActions,
                                                                      String fileNamePrefix, String modelPath) {
        ValueFunction_TensorFlow_ActionInMulti valFunMulti =
                new ValueFunction_TensorFlow_ActionInMulti(allPossibleActions);

        for (KeyCombination keys : valFunMulti.availableActions.keySet()) {
            File existingModel = new File(modelPath + fileNamePrefix + keys.name() + ".pb");
            assert existingModel.isFile(); // TODO make it fail gracefully by creating new models where needed.

            ValueFunction_TensorFlow_ActionIn valFun =
                    new ValueFunction_TensorFlow_ActionIn(existingModel);
            valFunMulti.allValueFunctions.put(keys, valFun);
        }
        return valFunMulti;
    }

    /**
     * Make a new value function and create new TensorFlow models to go with it.
     * @param allPossibleActions All possible actions that this value function should be able to differentiate between.
     * @param fileNamePrefix Saved model file prefix (e.g. if files are testQP.pb..., then the prefix would be "test").
     * @param hiddenLayerSizes Hidden layer sizes for the value function TensorFlow networks. Don't include
     *                         input/output sizes.
     * @param additionalArgs Additional arguments for creating the TensorFlow networks. See
     * {@link tflowtools.TrainableNetwork} for more info.
     * @return New value function created with new model files.
     */
    public static ValueFunction_TensorFlow_ActionInMulti makeNew(Collection<Action> allPossibleActions,
                                                                 String fileNamePrefix,
                                                                 List<Integer> hiddenLayerSizes,
                                                                 List<String> additionalArgs) {
        ValueFunction_TensorFlow_ActionInMulti valFunMulti =
                new ValueFunction_TensorFlow_ActionInMulti(allPossibleActions);

        // We need one network per key combination which actually appears. Don't make one for the key combinations
        // which are possible but unused.
        for (KeyCombination keys : valFunMulti.availableActions.keySet()) {
            ValueFunction_TensorFlow_ActionIn valFun =
                    ValueFunction_TensorFlow_ActionIn.makeNew(fileNamePrefix + keys.name(), hiddenLayerSizes, additionalArgs);
            valFunMulti.allValueFunctions.put(keys, valFun);
        }
        return valFunMulti;
    }
}
