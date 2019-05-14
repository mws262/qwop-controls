package value.updaters;

import tree.NodeQWOPBase;

public interface IValueUpdater {
    float update(float originalValue, float valueUpdate, int updateCountPrior, NodeQWOPBase<?> node);
}
