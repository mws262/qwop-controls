package ui;

import game.action.Command;
import game.state.IState;
import tree.TreeWorker;
import tree.node.NodeGameGraphicsBase;

import java.util.List;

public class UI_Headless<C extends Command<?>, S extends IState> implements IUserInterface<C, S> {

    @Override
    public void start() {}

    @Override
    public void kill() {}

    @Override
    public void addRootNode(NodeGameGraphicsBase<?, C, S> node) {}

    @Override
    public void clearRootNodes() {}

    @Override
    public void setActiveWorkers(List<TreeWorker<C, S>> treeWorkers) {}
}
