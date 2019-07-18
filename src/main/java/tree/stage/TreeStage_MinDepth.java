package tree.stage;

import tree.TreeWorker;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorableBase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Searches until we meet a minimum depth requirement in all branches
 * sort of like a BFS. This might never terminate with some greedy searches
 * so be careful.
 *
 * @author matt
 */
public class TreeStage_MinDepth extends TreeStage {

    private List<NodeQWOPExplorableBase<?>> leafList = new LinkedList<>();

    /**
     * Minimum relative depth (relative to given root) that we want to achieve.
     */
    private int minDepth;

    /**
     * Minimum ABSOLUTE depth (relative to absolute root) that we want to achieve.
     */
    private int minEffectiveDepth;

    /**
     * Tree stage which searches until all branches reach a specified depth, or all leaves less than this depth are
     * failed.
     * @param minDepth Search until all branches are this depth or fully-explored/failed.
     */
    public TreeStage_MinDepth(int minDepth) {
        this.minDepth = minDepth;
    }

    @Override
    public void initialize(List<TreeWorker> workers, NodeQWOPExplorableBase<?> stageRoot) {
        minEffectiveDepth = minDepth + stageRoot.getTreeDepth();
        super.initialize(workers, stageRoot);
    }

    @Override
    public List<NodeQWOPBase<?>> getResults() {
        leafList.clear();
        getRootNode().recurseDownTreeInclusive(n -> {
            if (n.getChildCount() == 0) {
                leafList.add(n);
            }
        });

        List<NodeQWOPBase<?>> resultList = new ArrayList<>();

        for (NodeQWOPBase<?> n : leafList) {
            if (n.getTreeDepth() == minEffectiveDepth) {
                resultList.add(n);
            } else if (n.getTreeDepth() > minEffectiveDepth) {
                NodeQWOPBase<?> atDepth = n;
                while (atDepth.getTreeDepth() > minEffectiveDepth) {
                    atDepth = atDepth.getParent();
                }
                resultList.add(atDepth);
            }
        }
        return resultList;
    }

    @Override
    public boolean checkTerminationConditions() {
        NodeQWOPExplorableBase<?> rootNode = getRootNode();
        if (rootNode.isFullyExplored()) return true;
        if (!areWorkersRunning()) return true;

        leafList.clear();
        getRootNode().recurseDownTreeInclusive(n -> {
            if (n.getChildCount() == 0) {
                leafList.add(n);
            }
        });

        // If no leaves, then we haven't gotten far enough for sure.
        if (leafList.isEmpty()) return false;

        for (NodeQWOPExplorableBase<?> n : leafList) {
            // We find a leaf which is not deep enough and not failed.
            if (n.getTreeDepth() < minEffectiveDepth && !n.getState().isFailed()) {
                return false;
            } else {
                NodeQWOPExplorableBase<?> currNode = n;
                // Get back from the leaf to the horizon we wish to achieve.
                while (currNode.getTreeDepth() > minEffectiveDepth) {
                    currNode = currNode.getParent();
                }

                // Make sure everything under that horizon has been tried.
                while (currNode.getTreeDepth() > rootNode.getTreeDepth()) {
                    currNode = currNode.getParent();
                    if (currNode.getUntriedActionCount() > 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
