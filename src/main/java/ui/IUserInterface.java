package ui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.action.Command;
import tree.node.NodeGameGraphicsBase;
import ui.histogram.PanelHistogram_LeafDepth;
import ui.pie.PanelPie_ViableFutures;
import ui.runner.PanelRunner;
import ui.scatterplot.PanelPlot;
import ui.timeseries.PanelTimeSeries;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UI_Headless.class, name = "headless"),
        @JsonSubTypes.Type(value = UI_Full.class, name = "full"),
})
public interface IUserInterface<C extends Command<?>> {

    void start();

    void kill();

    void addRootNode(NodeGameGraphicsBase<?, C> node);

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
    interface TabbedPaneActivator<C extends Command<?>> {
        void activateTab();

        void deactivateTab();

        @JsonIgnore
        boolean isActive();

        default void update(NodeGameGraphicsBase<?, C> node) {}

        String getName();
    }
}