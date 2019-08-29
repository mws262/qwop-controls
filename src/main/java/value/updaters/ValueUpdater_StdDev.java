package value.updaters;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Node value estimator which calculates the value to be a certain number of standard deviations above or below that
 * of its children (by default, one above). // TODO this needs testing. It might be a terrible idea.
 *
 * @author matt
 */
public class ValueUpdater_StdDev<C extends Command<?>, S extends IState> implements IValueUpdater<C, S> {

    /**
     * How many standard deviations above the child values should this node be updated to?
     */
    public final float stdevAbove;

    private List<NodeGameBase<?, C, S>> children = new ArrayList<>();

    public ValueUpdater_StdDev(@JsonProperty("stdevAbove") float stdevAbove) {
        this.stdevAbove = stdevAbove;
    }

    @Override
    public float update(float valueUpdate, NodeGameBase<?, C, S> node) {
        if (node.getChildCount() > 0) {
            children.clear();
            node.applyToThis(n -> children.addAll(n.getChildren()));

            // Calculate the mean.
            float mean = 0f;
            for (NodeGameBase<?, C, S> child : children) {
                mean += child.getValue();
            }
            mean /= (float) children.size();

            // Calculate the standard deviation.
            float stdev = 0f;
            for (NodeGameBase<?, C, S> child : children) {
                stdev += (child.getValue() - mean) * (child.getValue() - mean);
            }
            stdev = (float) Math.sqrt(stdev / (float) children.size());

            return mean + stdev * stdevAbove;

        } else {
            return valueUpdate;
        }
    }

    @Override
    public IValueUpdater<C, S> getCopy() {
        return new ValueUpdater_StdDev<>(stdevAbove);
    }
}
