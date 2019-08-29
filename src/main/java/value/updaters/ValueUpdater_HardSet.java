package value.updaters;

import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

/**
 * Node value estimator which just replaces the existing estimate with the newly-provided value. Mostly useful as a
 * test.
 *
 * @author matt
 */
public class ValueUpdater_HardSet<C extends Command<?>, S extends IState> implements IValueUpdater<C, S> {

    @Override
    public float update(float valueUpdate, NodeGameBase<?, C, S> node) {
        return valueUpdate;
    }

    @Override
    public IValueUpdater<C, S> getCopy() {
        return new ValueUpdater_HardSet<>();
    }
}
