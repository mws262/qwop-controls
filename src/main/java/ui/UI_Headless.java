package ui;

import tree.NodeQWOPGraphicsBase;

public class UI_Headless implements IUserInterface {

    @Override
    public void run() {
        System.out.println("Headless mode: Running without user interface.");
    }

    @Override
    public void kill() {}

    @Override
    public void addRootNode(NodeQWOPGraphicsBase<?> node) {}

    @Override
    public void clearRootNodes() {}

}
