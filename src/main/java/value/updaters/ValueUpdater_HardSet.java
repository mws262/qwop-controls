package value.updaters;

import tree.node.NodeQWOPBase;

/**
 * Node value estimator which just replaces the existing estimate with the newly-provided value. Mostly useful as a
 * test.
 *
 * @author matt
 */
public class ValueUpdater_HardSet implements IValueUpdater {

    @Override
    public float update(float valueUpdate, NodeQWOPBase<?> node) {
        return valueUpdate;
    }

    @Override
    public IValueUpdater getCopy() {
        return new ValueUpdater_HardSet();
    }
}
