package ui;

import main.Node;

public interface IUserInterface extends Runnable {

    /**
     * Main graphics loop.
     **/
    @Override
    void run();

    /**
     * Stop the FSM.
     **/
    void kill();

    /**
     * Pick a node for the UI to highlight and potentially display.
     **/
    void selectNode(Node selected);

    void addRootNode(Node node);

    void clearRootNodes();

    interface TabbedPaneActivator {
        void activateTab();

        void deactivateTab();

        boolean isActive();

        void update(Node node);
    }
}