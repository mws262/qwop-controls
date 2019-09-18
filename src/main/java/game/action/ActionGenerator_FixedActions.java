package game.action;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import tree.node.NodeGameExplorableBase;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * ActionGenerator which is defined for a single ActionList which is always returned when queried.
 *
 * @author matt
 */
public class ActionGenerator_FixedActions<C extends Command<?>> implements IActionGenerator<C> {

    private final ActionList<C> actionList;

    @JsonCreator
    public ActionGenerator_FixedActions(@JsonProperty("actionList") ActionList<C> actionList) {
        this.actionList = actionList;
    }

    @JsonIgnore
    @Override
    public ActionList<C> getPotentialChildActionSet(NodeGameExplorableBase<?, C, ?> parentNode) {
        return actionList.getCopy();
    }

    @JsonIgnore
    @Override
    public Set<Action<C>> getAllPossibleActions() {
        return new HashSet<>(actionList);
    }

    @JsonGetter("actionList")
    public ActionList<C> getActionList() {
        return actionList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionGenerator_FixedActions that = (ActionGenerator_FixedActions) o;
        return actionList.equals(that.actionList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionList);
    }
}
