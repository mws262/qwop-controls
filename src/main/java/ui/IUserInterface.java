package ui;

import tree.NodeQWOPExplorable;
import tree.NodeQWOPExplorableBase;
import tree.NodeQWOPGraphicsBase;

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

    void addRootNode(NodeQWOPExplorableBase<?> node);

    void clearRootNodes();

    interface TabbedPaneActivator {
        void activateTab();

        void deactivateTab();

        boolean isActive();

        void update(NodeQWOPExplorableBase<?> node);
    }
}