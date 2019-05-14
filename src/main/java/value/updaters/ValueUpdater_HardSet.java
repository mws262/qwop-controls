package value.updaters;

import tree.NodeQWOPBase;

public class ValueUpdater_HardSet implements IValueUpdater {
    @Override
    public float update(float originalValue, float valueUpdate, int updateCountPrior, NodeQWOPBase<?> node) {
        return valueUpdate;
    }
}
