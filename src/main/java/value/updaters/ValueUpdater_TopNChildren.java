package value.updaters;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import game.state.IState;
import org.jcodec.common.Preconditions;
import tree.node.NodeGameBase;

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
public class ValueUpdater_TopNChildren<C extends Command<?>, S extends IState> implements IValueUpdater<C, S> {

    /**
     * Number of children to average when updating the value estimate for a node.
     */
    public final int numChildrenToAvg;

    public ValueUpdater_TopNChildren(@JsonProperty("numChildrenToAvg") int numChildrenToAvg) {
        Preconditions.checkArgument(numChildrenToAvg > 0, "Number of children to average in the updater must be at " +
                "least 1.", numChildrenToAvg);
        this.numChildrenToAvg = numChildrenToAvg;
    }

    @Override
    public float update(float valueUpdate, NodeGameBase<?, C, S> node) {
        if (node.getChildCount() == 0) {
            return valueUpdate;
        } else {
            List<NodeGameBase<?, C, S>> children = new ArrayList<>();
            node.applyToThis(n -> children.addAll(n.getChildren())); // Trick to get around type erasure.
            children.sort(Comparator.comparing(NodeGameBase::getValue));
            Collections.reverse(children);

            float value = 0f;
            for (int i = 0; i < numChildrenToAvg; i++) {
                int idx = Math.min(i, children.size() - 1); // Fills any remaining "slots" with the lowest child if
                // less children than number of children to average.
//                int idx = i >= children.size() ? 0 : i;
                value += children.get(idx).getValue();
            }
            return value /(float) numChildrenToAvg;
        }
    }

    @Override
    public IValueUpdater<C, S> getCopy() {
        return new ValueUpdater_TopNChildren<>(numChildrenToAvg);
    }
}
