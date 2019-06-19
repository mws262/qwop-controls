package tree.stage;

import tree.sampler.ISampler;
import savers.IDataSaver;
import tree.node.NodeQWOPBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree search which continues sampling until the root node is fully-explored. If a broad set of game.action is provided,
 * this will probably never occur. This is mostly useful for testing.
 *
 * @author matt
 */
public class TreeStage_SearchForever extends TreeStage {

    /**
     * Tree stage which goes forever or until the root node is fully-explored.
     * @param sampler Sampling strategy used for creating the tree.
     * @param saver Data-saving policy used during and after the tree stage.
     */
    public TreeStage_SearchForever(ISampler sampler, IDataSaver saver) {
        this.sampler = sampler;
        this.saver = saver;
    }

    @Override
    public List<NodeQWOPBase<?>> getResults() {
        List<NodeQWOPBase<?>> resultList = new ArrayList<>();
        resultList.add(getRootNode()); // No particularly interesting results.
        return resultList;
    }

    @Override
    public boolean checkTerminationConditions() {
        return getRootNode().isFullyExplored(); // Only termination condition is a completely explored tree. Unlikely when the
        // selection pool is good.
    }
}
