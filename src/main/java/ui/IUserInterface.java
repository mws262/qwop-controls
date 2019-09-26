package ui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.action.Command;
import game.state.IState;
import tree.TreeWorker;
import tree.node.NodeGameGraphicsBase;
import ui.histogram.PanelHistogram_LeafDepth;
import ui.pie.PanelPie_ViableFutures;
import ui.runner.PanelRunner;
import ui.scatterplot.PanelPlot;
import ui.timeseries.PanelTimeSeries;
import ui.timeseries.PanelTimeSeries_WorkerLoad;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UI_Headless.class, name = "headless"),
        @JsonSubTypes.Type(value = UI_Full.class, name = "full"),
})
public interface IUserInterface<C extends Command<?>, S extends IState> {

    void start();

    void kill();

    void addRootNode(NodeGameGraphicsBase<?, C, S> node);

    void clearRootNodes();

    void setActiveWorkers(List<TreeWorker<C, S>> workers);

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
            @JsonSubTypes.Type(value = PanelTimeSeries_WorkerLoad.class, name = "workerload"),
    })
    interface TabbedPaneActivator<C extends Command<?>, S extends IState> {
        void activateTab();

        void deactivateTab();

        @JsonIgnore
        boolean isActive();

        default void update(NodeGameGraphicsBase<?, C, S> node) {}

        String getName();
    }
}