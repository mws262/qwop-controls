package value.updaters;

import tree.NodeQWOPBase;

public class ValueUpdater_Average implements IValueUpdater {
    public float update(float originalValue, float valueUpdate, int updateCountPrior, NodeQWOPBase<?> node) {
        return (originalValue * updateCountPrior + valueUpdate) / (updateCountPrior + 1);
    }
}
