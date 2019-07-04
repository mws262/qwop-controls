package value.updaters;

import com.fasterxml.jackson.annotation.JsonProperty;
import tree.node.NodeQWOPBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Node value estimator that averages the top N child values. If there are no children, it gets the provided value
 * update. If there are less than N children, the lowest value gets added enough times to make up the difference.
 * // TODO this needs testing.
 *
 * @author matt
 */
public class ValueUpdater_TopNChildren implements IValueUpdater {

    /**
     * Number of children to average when updating the value estimate for a node.
     */
    public final int numChildrenToAvg;

    /**
     * List of children of the node being updated.
     */
    private List<NodeQWOPBase<?>> children = new ArrayList<>();

    public ValueUpdater_TopNChildren(@JsonProperty("numChildrenToAvg") int numChildrenToAvg) {
        this.numChildrenToAvg = numChildrenToAvg;
    }

    @Override
    public float update(float valueUpdate, NodeQWOPBase<?> node) {
        if (node.getChildCount() == 0) {
            return valueUpdate;
        } else {
            children.clear();
            node.applyToThis(n -> children.addAll(n.getChildren())); // Trick to get around type erasure.
            children.sort(Comparator.comparing(NodeQWOPBase::getValue));
            Collections.reverse(children);

            float value = 0f;
            for (int i = 0; i < numChildrenToAvg; i++) {
                int idx = Math.min(i, children.size() - 1); // Fills any remaining "slots" with the lowest child if
                // less children than number of children to average.
                value += children.get(idx).getValue();
            }
            return value /(float) numChildrenToAvg;
        }
    }
}
