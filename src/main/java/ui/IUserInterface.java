package ui;

import tree.Node;

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

    void addRootNode(Node node);

    void clearRootNodes();

    interface TabbedPaneActivator {
        void activateTab();

        void deactivateTab();

        boolean isActive();

        void update(Node node);
    }
}