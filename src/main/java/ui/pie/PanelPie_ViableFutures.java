package ui.pie;

import com.fasterxml.jackson.annotation.JsonProperty;
import game.action.Command;
import game.state.IState;
import org.jfree.data.general.DefaultPieDataset;
import tree.node.NodeGameExplorableBase;
import tree.node.NodeGameGraphicsBase;
import ui.IUserInterface;

public class PanelPie_ViableFutures<C extends Command<?>, S extends IState> extends PanelPie implements IUserInterface.TabbedPaneActivator<C, S> {

    private boolean active = false;

    private final String name;

    public PanelPie_ViableFutures(@JsonProperty("name") String name) {
        this.name = name;
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
    public void update(NodeGameGraphicsBase<?, C, S> node) {
        int failCount = 0;
        int cat1 = 0;
        int cat2 = 0;
        int cat3 = 0;
        int cat4 = 0;
        for (NodeGameExplorableBase<?, C, S> child : node.getChildren()) {
            int diffBranchDepth = child.getMaxBranchDepth() - child.getTreeDepth();

            if (child.getState().isFailed()) {
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
        data.insertValue(0, "Untried", node.getUntriedActionCount());
        data.insertValue(1, "Failed", failCount);
        data.insertValue(2, "0 depth", cat1);
        data.insertValue(3, "<3 depth", cat2);
        data.insertValue(4, "<6 depth", cat3);
        data.insertValue(5, ">6 depth", cat4);
    }

    @Override
    public String getName() {
        return name;
    }
}
