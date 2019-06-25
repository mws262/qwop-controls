package value.updaters;

import tree.node.NodeQWOPBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Node value estimator which calculates the value to be a certain number of standard deviations above or below that
 * of its children (by default, one above). // TODO this needs testing. It might be a terrible idea.
 *
 * @author matt
 */
public class ValueUpdater_StdDev implements IValueUpdater {

    /**
     * How many standard deviations above the child values should this node be updated to?
     */
    public float stdevAbove = 1.0f;

    private List<NodeQWOPBase<?>> children = new ArrayList<>();

    @Override
    public float update(float valueUpdate, NodeQWOPBase<?> node) {
        if (node.getChildCount() > 0) {
            children.clear();
            node.applyToThis(n -> children.addAll(n.getChildren()));

            // Calculate the mean.
            float mean = 0f;
            for (NodeQWOPBase<?> child : children) {
                mean += child.getValue();
            }
            mean /= (float) children.size();

            // Calculate the standard deviation.
            float stdev = 0f;
            for (NodeQWOPBase<?> child : children) {
                stdev += (child.getValue() - mean) * (child.getValue() - mean);
            }
            stdev = (float) Math.sqrt(stdev / (float) children.size());

            return mean + stdev * stdevAbove;

        } else {
            return valueUpdate;
        }
    }
}
