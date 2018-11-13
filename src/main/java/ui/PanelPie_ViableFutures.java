package ui;

import org.jfree.data.general.DefaultPieDataset;
import tree.Node;

public class PanelPie_ViableFutures extends PanelPie implements IUserInterface.TabbedPaneActivator {

    private boolean active = false;

    private Node currentNode;


    public PanelPie_ViableFutures() {
    }

    @Override
    public void activateTab() {
        active = true;
    }

    @Override
    public void deactivateTab() {
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void update(Node node) {
        currentNode = node;

        int failCount = 0;
        int cat1 = 0;
        int cat2 = 0;
        int cat3 = 0;
        int cat4 = 0;
        for (Node child : currentNode.getChildren()) {
            int diffBranchDepth = child.maxBranchDepth.get() - child.getTreeDepth();

            if (child.isFailed()) {
                failCount++;
            } else if (diffBranchDepth < 1) {
                cat1++;
            } else if (diffBranchDepth < 3) {
                cat2++;
            } else if (diffBranchDepth < 6) {
                cat3++;
            } else {
                cat4++;
            }
        }

        DefaultPieDataset data = getDataset();
        data.clear();
        data.insertValue(0, "Untried", currentNode.uncheckedActions.size());
        data.insertValue(1, "Failed", failCount);
        data.insertValue(2, "0 depth", cat1);
        data.insertValue(3, "<3 depth", cat2);
        data.insertValue(4, "<6 depth", cat3);
        data.insertValue(5, ">6 depth", cat4);
    }
}
