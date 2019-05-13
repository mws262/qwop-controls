package ui;

import tree.NodeQWOPExplorableBase;

public class UI_Headless implements IUserInterface {

    @Override
    public void run() {
        System.out.println("Headless mode: Running without user interface.");
    }

    @Override
    public void kill() {}

    @Override
    public void addRootNode(NodeQWOPExplorableBase<?> node) {}

    @Override
    public void clearRootNodes() {}

}
