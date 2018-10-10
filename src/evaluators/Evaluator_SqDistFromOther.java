package evaluators;

import java.util.List;

import game.StateVariable;
import main.IEvaluationFunction;
import main.Node;

public class Evaluator_SqDistFromOther implements IEvaluationFunction {

    /**
     * All nodes will be compared to this one by square distance in state space.
     **/
    private final Node nodeToCompareAllOthersTo;

    private final List<StateVariable> baseStateVarList;

    public Evaluator_SqDistFromOther(Node nodeToCompareAllOthersTo) {
        this.nodeToCompareAllOthersTo = nodeToCompareAllOthersTo;
        baseStateVarList = nodeToCompareAllOthersTo.state.getStateList();
    }

    @Override
    public float getValue(Node nodeToEvaluate) {
        List<StateVariable> otherStateVarList = nodeToEvaluate.state.getStateList();
        if (otherStateVarList.get(0) == null)
            return Float.MAX_VALUE; // This node hasn't quite finished doing stuff. So, just don't seriously consider
        // it.
        float sqError = 0;

        for (int i = 0; i < baseStateVarList.size(); i++) {
            float diff =
                    (baseStateVarList.get(i).x - baseStateVarList.get(0).x) - (otherStateVarList.get(i).x - otherStateVarList.get(0).x); // Subtract out the absolute x of body.
            sqError += diff * diff;
            diff = baseStateVarList.get(i).y - otherStateVarList.get(i).y;
            sqError += diff * diff;
            diff = baseStateVarList.get(i).th - otherStateVarList.get(i).th;
            sqError += diff * diff;
            diff = baseStateVarList.get(i).dx - otherStateVarList.get(i).dx;
            sqError += diff * diff;
            diff = baseStateVarList.get(i).dy - otherStateVarList.get(i).dy;
            sqError += diff * diff;
            diff = baseStateVarList.get(i).dth - otherStateVarList.get(i).dth;
        }
        return sqError;
    }

    @Override
    public String getValueString(Node nodeToEvaluate) {
        List<StateVariable> otherStateVarList = nodeToEvaluate.state.getStateList();
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < baseStateVarList.size(); i++) {
            float diff =
                    (baseStateVarList.get(i).x - baseStateVarList.get(0).x) - (otherStateVarList.get(i).x - otherStateVarList.get(0).x);
            value.append("x: ").append(diff * diff).append(", ");
            diff = baseStateVarList.get(i).y - otherStateVarList.get(i).y;
            value.append("y: ").append(diff * diff).append(", ");
            diff = baseStateVarList.get(i).th - otherStateVarList.get(i).th;
            value.append("th: ").append(diff * diff).append(", ");
            diff = baseStateVarList.get(i).dx - otherStateVarList.get(i).dx;
            value.append("dx: ").append(diff * diff).append(", ");
            diff = baseStateVarList.get(i).dy - otherStateVarList.get(i).dy;
            value.append("dy: ").append(diff * diff).append(", ");
            diff = baseStateVarList.get(i).dth - otherStateVarList.get(i).dth;
            value.append("dth: ").append(diff * diff);
        }
        return value.toString();
    }

    @Override
    public Evaluator_SqDistFromOther clone() {
        return new Evaluator_SqDistFromOther(nodeToCompareAllOthersTo);
    }

}
