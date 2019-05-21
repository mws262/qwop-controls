package value.updaters;

import tree.NodeQWOPBase;

/**
 * Node value estimate update rule. The value is always estimated to be the average of all update value seen. This is
 * default in many implementations of upper confidence bound for trees.
 *
 * @author matt
 */
public class ValueUpdater_Average implements IValueUpdater {

    @Override
    public float update(float valueUpdate, NodeQWOPBase<?> node) {
        return (node.getValue() * node.getUpdateCount() + valueUpdate) / (node.getUpdateCount() + 1);
    }
}
