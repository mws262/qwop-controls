package filters;

import main.Node;

/**
 * Filters nodes based on survival past them to a specified horizon. For example, with a specified horizon of 5, only
 * nodes with descendants at least 5 layers deeper in the tree would be accepted. All others would be filtered.
 *
 * @author matt
 */
public class NodeFilter_SurvivalHorizon implements INodeFilter {

    /**
     * How many tree layers beyond this node is required for it to be included by the filter.
     **/
    private final int requiredSurvivalHorizon;


    /**
     * Creates a new node filter which keeps only nodes who have descendants deep enough beyond them.
     *
     * @param requiredSurvivalHorizon Survival horizon beyond the query node required to keep the node and not filter
     *                                it out.
     */
    public NodeFilter_SurvivalHorizon(int requiredSurvivalHorizon) {
        if (requiredSurvivalHorizon < 1)
            throw new IllegalArgumentException("The desired survival horizon should at least be non-negative.");

        this.requiredSurvivalHorizon = requiredSurvivalHorizon;
    }

    @Override
    public boolean filter(Node node) {
        return node.maxBranchDepth.get() - node.getTreeDepth() >= requiredSurvivalHorizon;
    }

}
