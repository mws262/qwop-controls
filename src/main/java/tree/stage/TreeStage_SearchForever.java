package tree.stage;

import game.action.Command;
import tree.node.NodeQWOPBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree search which continues sampling until the root node is fully-explored. If a broad set of game.command is provided,
 * this will probably never occur. This is mostly useful for testing.
 *
 * @author matt
 */
public class TreeStage_SearchForever<C extends Command<?>> extends TreeStage<C> {

    /**
     * Tree stage which goes forever or until the root node is fully-explored.
     */
    public TreeStage_SearchForever() {}

    @Override
    public List<NodeQWOPBase<?, C>> getResults() {
        List<NodeQWOPBase<?, C>> resultList = new ArrayList<>();
        resultList.add(getRootNode()); // No particularly interesting results.
        return resultList;
    }

    @Override
    public boolean checkTerminationConditions() {
        return getRootNode().isFullyExplored(); // Only termination condition is a completely explored tree. Unlikely when the
        // selection pool is good.
    }
}
