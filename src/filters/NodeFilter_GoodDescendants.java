package filters;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;

import main.INodeFilter;
import main.Node;

public class NodeFilter_GoodDescendants implements INodeFilter {

    /**
     * How many tree layers beyond this node is required for it to be included by the filter.
     **/
    public int requiredSurvivalHorizon;


    public NodeFilter_GoodDescendants(int requiredSurvivalHorizon) {
        this.requiredSurvivalHorizon = requiredSurvivalHorizon;
    }

    @Override
    public boolean filter(Node node) {

        int below = countBelow(node, 0);
        return below > requiredSurvivalHorizon;
    }

    @Override
    public void filter(List<Node> nodes) {
        Iterator<Node> iter = nodes.iterator();
        while (iter.hasNext()) {
            Node n = iter.next();
            if (!filter(n)) {
                iter.remove();
            } else {
                n.overrideNodeColor = Color.PINK;
                n.displayPoint = true;
                n.setBranchZOffset(0.1f);
                n.overrideLineColor = Color.PINK;
            }
        }
    }

    /**
     * See if the required depth below is possible through any set of children. Breaks once the survival horizon is
     * met.
     **/
    private int countBelow(Node node, int currentCount) {
        currentCount++;
        if (currentCount > requiredSurvivalHorizon) return currentCount;
        int currentBest = 0;
        for (Node child : node.children) {
            int newCount = countBelow(child, currentCount);
            if (newCount > currentBest) {
                currentBest = newCount;
                if (currentBest > requiredSurvivalHorizon) break;
            }
        }
        return currentBest;
    }
}
