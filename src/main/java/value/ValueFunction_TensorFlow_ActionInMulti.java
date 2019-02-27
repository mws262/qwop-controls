package value;

import actions.Action;
import tree.Node;

import java.util.*;

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
        KeyCombination nodeActionKeys = getKeyCombination(currentNode.getAction().peek());
        ValueFunction_TensorFlow_ActionIn valFun = allValueFunctions.get(nodeActionKeys);
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
        for (KeyCombination keys : allValueFunctions.keySet()) {
            List<Node> thisKeyNodes = new ArrayList<>();
            for (Node n : nodes) {
                if (getKeyCombination(n.getAction().peek()) == keys) {
                    thisKeyNodes.add(n);
                }
            }
            allValueFunctions.get(keys).update(thisKeyNodes);
        }
    }

    /**
     * Load checkpoints of all the nets involved.
     * @param checkpointPrefix Prefix of the filename. The suffix will be the key combinations involved.
     */
    public void loadCheckpoints(String checkpointPrefix) {
        assert !checkpointPrefix.isEmpty();
        for (KeyCombination keys : allValueFunctions.keySet()) {
            allValueFunctions.get(keys).loadCheckpoint(checkpointPrefix + keys.name());
        }
    }

    /**
     * Save checkpoints of all the nets involved.
     * @param checkpointPrefix Prefix of the filename. The suffix will be the key combinations involved.
     */
    public void saveCheckpoints(String checkpointPrefix) {
        assert !checkpointPrefix.isEmpty();
        for (KeyCombination keys : allValueFunctions.keySet()) {
            allValueFunctions.get(keys).saveCheckpoint(checkpointPrefix + keys.name());
        }
    }

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

    //TODO MAKE FROM EXISTING MODELS
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
