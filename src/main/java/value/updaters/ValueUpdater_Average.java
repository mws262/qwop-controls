package value.updaters;

import game.action.Command;
import game.state.IState;
import tree.node.NodeGameBase;

/**
 * Node value estimate update rule. The value is always estimated to be the average of all update value seen. This is
 * default in many implementations of upper confidence bound for trees.
 *
 * @author matt
 */
public class ValueUpdater_Average<C extends Command<?>, S extends IState> implements IValueUpdater<C, S> {

    @Override
    public float update(float valueUpdate, NodeGameBase<?, C, S> node) {
        return (node.getValue() * node.getUpdateCount() + valueUpdate) / (node.getUpdateCount() + 1);
    }

    @Override
    public IValueUpdater<C, S> getCopy() {
        return new ValueUpdater_Average<>();
    }
}
