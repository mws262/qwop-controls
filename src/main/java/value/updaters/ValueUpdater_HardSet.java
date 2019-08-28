package value.updaters;

import game.action.Command;
import tree.node.NodeGameBase;

/**
 * Node value estimator which just replaces the existing estimate with the newly-provided value. Mostly useful as a
 * test.
 *
 * @author matt
 */
public class ValueUpdater_HardSet<C extends Command<?>> implements IValueUpdater<C> {

    @Override
    public float update(float valueUpdate, NodeGameBase<?, C> node) {
        return valueUpdate;
    }

    @Override
    public IValueUpdater<C> getCopy() {
        return new ValueUpdater_HardSet<>();
    }
}
