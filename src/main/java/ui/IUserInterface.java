package ui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tree.node.NodeQWOPGraphicsBase;
import tree.sampler.*;
import ui.histogram.PanelHistogram;
import ui.histogram.PanelHistogram_LeafDepth;
import ui.pie.PanelPie_ViableFutures;
import ui.runner.PanelRunner;
import ui.scatterplot.PanelPlot;
import ui.scatterplot.PanelPlot_Simple;
import ui.timeseries.PanelTimeSeries;

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

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = PanelTree.class, name = "tree"),
            @JsonSubTypes.Type(value = PanelHistogram_LeafDepth.class, name = "depth_histogram"),
            @JsonSubTypes.Type(value = PanelPie_ViableFutures.class, name = "futures_pie"),
            @JsonSubTypes.Type(value = PanelRunner.class, name = "runner"),
            @JsonSubTypes.Type(value = PanelPlot.class, name = "scatterplot"),
            @JsonSubTypes.Type(value = PanelTimeSeries.class, name = "timeseries"),
    })
    interface TabbedPaneActivator {
        void activateTab();

        void deactivateTab();

        @JsonIgnore
        boolean isActive();

        default void update(NodeQWOPGraphicsBase<?> node) {}

        String getName();
    }
}