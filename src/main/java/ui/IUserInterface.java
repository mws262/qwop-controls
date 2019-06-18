package ui;

import tree.node.NodeQWOPGraphicsBase;

public interface IUserInterface extends Runnable {

    /**
     * Main graphics loop.
     */
    @Override
    void run();

    /**
     * Stop the FSM.
     */
    void kill();

    void addRootNode(NodeQWOPGraphicsBase<?> node);

    void clearRootNodes();

    interface TabbedPaneActivator {
        void activateTab();

        void deactivateTab();

        boolean isActive();

        default void update(NodeQWOPGraphicsBase<?> node) {}
    }
}