package goals.tree_search;

import tree.Node;

import java.io.File;

public class MAIN_Search_CollectAllGameData extends MAIN_Search_Template {


    public MAIN_Search_CollectAllGameData() {
        super(new File("src/main/resources/config/" + "search.config_long"));
    }

    public static void main(String[] args) {
        MAIN_Search_CollectAllGameData manager = new MAIN_Search_CollectAllGameData();
        manager.doGames();
    }

    private void doGames() {
        assignAllowableActionsWider(-1);

        Node rootNode = new Node();
        ui.addRootNode(rootNode);

        doFixedGamesToFailureStage(rootNode, "good_and_bad", 1, 1000000);
    }
}
