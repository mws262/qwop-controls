package value;

import actions.Action;

import java.util.*;

public class ValueFunction_TensorFlow_ActionInMulti {

    enum KeyCombination {
        NN, QN, QO, QP, WN, WO, WP, NO, NP
    }

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


    Map<KeyCombination, Set<Integer>> availableActions = new HashMap<>();


    public ValueFunction_TensorFlow_ActionInMulti(List<Action> allPossibleActions) {


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


    private static KeyCombination getKeyCombination(boolean[] keys) {
        KeyCombination combination = null;
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

}
