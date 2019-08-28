package ui;

import game.action.Command;
import tree.node.NodeGameGraphicsBase;

public class UI_Headless<C extends Command<?>> implements IUserInterface<C> {

    @Override
    public void start() {}

    @Override
    public void kill() {}

    @Override
    public void addRootNode(NodeGameGraphicsBase<?, C> node) {}

    @Override
    public void clearRootNodes() {}

}
