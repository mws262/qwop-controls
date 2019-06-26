package ui;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tree.node.NodeQWOPGraphicsBase;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UI_Headless.class, name = "headless"),
        @JsonSubTypes.Type(value = UI_Full.class, name = "full"),
})
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