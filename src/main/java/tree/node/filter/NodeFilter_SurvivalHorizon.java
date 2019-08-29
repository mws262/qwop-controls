package tree.node.filter;

import game.action.Command;
import game.state.IState;
import tree.node.NodeGameExplorableBase;

/**
 * Filters nodes based on survival past them to a specified horizon. For example, with a specified horizon of 5, only
 * nodes with descendants at least 5 layers deeper in the tree would be accepted. All others would be filtered.
 *
 * @author matt
 */
public class NodeFilter_SurvivalHorizon<C extends Command<?>, S extends IState> implements INodeFilter<C, S> {

    /**
     * How many tree layers beyond this node is required for it to be included by the tree.node.filter.
     */
    private final int requiredSurvivalHorizon;

    /**
     * Creates a new node tree.node.filter which keeps only nodes who have descendants deep enough beyond them.
     *
     * @param requiredSurvivalHorizon Survival horizon beyond the query node required to keep the node and not tree.node.filter
     *                                it out.
     */
    public NodeFilter_SurvivalHorizon(int requiredSurvivalHorizon) {
        if (requiredSurvivalHorizon < 1)
            throw new IllegalArgumentException("The desired survival horizon should at least be non-negative.");

        this.requiredSurvivalHorizon = requiredSurvivalHorizon;
    }

    @Override
    public boolean filter(NodeGameExplorableBase<?, C, S> node) {
        return node.getMaxBranchDepth() - node.getTreeDepth() >= requiredSurvivalHorizon;
    }

}
