package TreeStages;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import savers.IDataSaver;
import samplers.ISampler;
import main.Node;
import main.TreeStage;
import main.TreeWorker;

/**
 * Searches until we meet a minimum depth requirement in all branches
 * sort of like a BFS. This might never terminate with some greedy searches
 * so be careful.
 *
 * @author matt
 */
public class TreeStage_MinDepth extends TreeStage {

    private List<Node> leafList = new LinkedList<>();

    /**
     * Minimum relative depth (relative to given root) that we want to achieve.
     **/
    private int minDepth;

    /**
     * Minimum ABSOLUTE depth (relative to absolute root) that we want to achieve.
     **/
    private int minEffectiveDepth;

    public TreeStage_MinDepth(int minDepth, ISampler sampler, IDataSaver saver) {
        this.minDepth = minDepth;
        this.sampler = sampler;
        this.saver = saver;
    }

    @Override
    public void initialize(List<TreeWorker> workers, Node stageRoot) {
        minEffectiveDepth = minDepth + stageRoot.treeDepth;
        super.initialize(workers, stageRoot);
    }

    @Override
    public List<Node> getResults() {
        leafList.clear();
        getRootNode().getLeaves(leafList);
        System.out.println(leafList.size());

        List<Node> resultList = new ArrayList<>();

        for (Node n : leafList) {
            if (n.getState() == null) continue;
            if (n.treeDepth == minEffectiveDepth) {
                resultList.add(n);
            } else if (n.treeDepth > minEffectiveDepth) {
                Node atDepth = n;
                while (atDepth.treeDepth > minEffectiveDepth) {
                    atDepth = atDepth.getParent();
                }
                resultList.add(atDepth);
            }
        }
        return resultList;
    }

    @Override
    public boolean checkTerminationConditions() {
        Node rootNode = getRootNode();
        if (rootNode.fullyExplored.get()) return true;
        if (!areWorkersRunning()) return true;

        leafList.clear();
        rootNode.getLeaves(leafList);

        // If no leaves, then we haven't gotten far enough for sure.
        if (leafList.isEmpty()) return false;

        for (Node n : leafList) {
            // We find a leaf which is not deep enough AND not terminal
            if (n.treeDepth < minEffectiveDepth && !n.isTerminal) {
                return false;
            } else {
                Node currNode = n;
                // Get back from the leaf to the horizon we wish to achieve.
                while (currNode.treeDepth > minEffectiveDepth) {
                    currNode = currNode.getParent();
                }

                // Make sure everything under that horizon has been tried.
                while (currNode.treeDepth > rootNode.treeDepth) {
                    currNode = currNode.getParent();
                    if (currNode.uncheckedActions.size() > 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
