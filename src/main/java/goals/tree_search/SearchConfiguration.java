package goals.tree_search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.base.Preconditions;
import controllers.Controller_Null;
import game.state.transform.Transform_Autoencoder;
import game.state.transform.Transform_PCA;
import org.apache.commons.io.input.XmlStreamReader;
import org.apache.commons.io.output.XmlStreamWriter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import savers.DataSaver_Null;
import savers.IDataSaver;
import tree.TreeWorker;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.filter.NodeFilter_SurvivalHorizon;
import tree.sampler.ISampler;
import tree.sampler.Sampler_UCB;
import tree.sampler.rollout.RolloutPolicy_EndScore;
import tree.stage.TreeStage;
import tree.stage.TreeStage_MaxDepth;
import ui.IUserInterface;
import ui.UI_Full;
import ui.histogram.PanelHistogram_LeafDepth;
import ui.pie.PanelPie_ViableFutures;
import ui.runner.PanelRunner_Animated;
import ui.runner.PanelRunner_AnimatedTransformed;
import ui.runner.PanelRunner_Comparison;
import ui.runner.PanelRunner_Snapshot;
import ui.scatterplot.*;
import ui.timeseries.PanelTimeSeries_WorkerLoad;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class SearchConfiguration implements Serializable {

    public Machine machine = new Machine(0.5f, 1, 20, "DEBUG");
    public List<SearchOperation> searchOperations = new ArrayList<>();
    public UI ui;

    public SearchConfiguration() {}

    /**
     * Defines run parameters having to do with threads, logging, etc.
     */
    public static class Machine {
        private final float coreFraction;
        private final int coreMinimum;
        private final int coreMaximum;
        private final String logLevel;
        private final int coreCount;

        Machine(@JsonProperty(value = "coreFraction", required = true) float coreFraction,
                @JsonProperty(value = "coreMinimum", required = true) int coreMinimum,
                @JsonProperty(value = "coreMaximum", required = true) int coreMaximum,
                @JsonProperty(value = "logLevel") String logLevel) {
            Preconditions.checkArgument(coreFraction > 0 && coreFraction <= 1, "Core fraction should be between zero " +
                    "and one.", coreFraction);
            Preconditions.checkArgument(coreMinimum > 0, "Minimum allotted core should be at least one.", coreMinimum);
            Preconditions.checkArgument(coreMaximum >= coreMinimum, "Core maximum must be at least as big as the core" +
                    " minimum.", coreMaximum);

            this.coreFraction = coreFraction;
            this.coreMinimum = coreMinimum;
            this.coreMaximum = coreMaximum;
            this.logLevel = logLevel;
            coreCount = Runtime.getRuntime().availableProcessors();
            assert coreCount > 0 && coreCount < 100;

            loadLoggerConfiguration();
            Configurator.setRootLevel(Level.valueOf(logLevel));
        }

        public float getCoreFraction() {
            return coreFraction;
        }
        public int getCoreMinimum() {
            return coreMinimum;
        }
        public int getCoreMaximum() {
            return coreMaximum;
        }
        public String getLogLevel() {
            return logLevel;
        }

        @JsonIgnore
        public int getCoreCount() {
            return coreCount;
        }

        @JsonIgnore
        public int getRequestedThreadCount() {
            return Math.min(Math.max((int)(coreFraction * coreCount), coreMinimum), coreMinimum);
        }
    }

    /**
     * Defines user interface settings.
     */
    public static class UI {
        @JacksonXmlProperty(isAttribute=true)
        private final IUserInterface UI;

        public UI(@JsonProperty("UI") IUserInterface UI) {
            this.UI = UI;
        }
        public IUserInterface getUI() {
            return UI;
        }
    }

    /**
     * Defines a tree search operation.
     */
    public static class SearchOperation {

        @JacksonXmlProperty(isAttribute=true)
        private final TreeStage stage;

        private final ISampler sampler;

        private final IDataSaver saver;

        private final int repetitionCount;

        @JsonCreator
        SearchOperation(@JsonProperty("stage") TreeStage stage,
                        @JsonProperty("sampler") ISampler sampler,
                        @JsonProperty("saver") IDataSaver saver,
                        @JsonProperty("repetitionCount") int repetitionCount // Defaults to zero if not
                        // set in config file.
        ) {
            Preconditions.checkArgument(repetitionCount >= 0, "Stage repetitions may not be less than zero.",
                    repetitionCount);
            Preconditions.checkNotNull(stage);
            Preconditions.checkNotNull(sampler);

            this.stage = stage;
            this.sampler = sampler;
            this.saver = Objects.isNull(saver) ? new DataSaver_Null() : saver; // Default to no saving.
            this.repetitionCount = repetitionCount;
        }

        SearchOperation(TreeStage stage, ISampler sampler, IDataSaver saver) {
            this.stage = stage;
            this.sampler = sampler;
            this.saver = saver;
            this.repetitionCount = 0;
        }

        public TreeStage getStage() {
            return stage;
        }

        public ISampler getSampler() {
            return sampler;
        }

        public IDataSaver getSaver() {
            return saver;
        }

        public int getRepetitionCount() {
            return repetitionCount;
        }

        public void startOperation(NodeQWOPExplorableBase<?> rootNode, Machine machine) {
            Preconditions.checkNotNull(rootNode);
            Preconditions.checkNotNull(machine);

            ArrayList<TreeWorker> treeWorkers = new ArrayList<>();
            for (int i = 0; i < machine.getRequestedThreadCount(); i++) {
                treeWorkers.add(getTreeWorker());
            }
            stage.initialize(treeWorkers, rootNode);
        }

        @JsonIgnore
        public TreeWorker getTreeWorker() {
            return TreeWorker.makeStandardTreeWorker(sampler.getCopy(), saver.getCopy()); // TODO handle other kinds of
            // treeworkers.
        }
    }

    public static void main(String[] args) {
        // configuration.searchOperations.add(configuration.searchOperations.get(0));
//        serializeToXML(new File("./src/main/resources/config/config.xml"), configuration);
//        serializeToJson(new File("./src/main/resources/config/config.json"), configuration);
//        serializeToYaml(new File("./src/main/resources/config/config.yaml"), configuration);

//        configuration = deserializeXML(new File("./src/main/resources/config/config.xml"));
    }

    public static void serializeToJson(File jsonFileOutput, SearchConfiguration configuration) {
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setAnnotationIntrospector(new IgnoreInheritedIntrospector());
        try {
            objectMapper.writeValue(jsonFileOutput, configuration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void serializeToXML(File xmlFileOutput, SearchConfiguration configuration) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.setAnnotationIntrospector(new IgnoreInheritedIntrospector());
            xmlMapper.enable(SerializationFeature.INDENT_OUTPUT); // Output with line breaks.
            XmlStreamWriter xmlStreamWriter = new XmlStreamWriter(xmlFileOutput);
            xmlMapper.writeValue(xmlStreamWriter, configuration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void serializeToYaml(File xmlFileOutput, SearchConfiguration configuration) {
        try {
            ObjectMapper objectMapper =
                    new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
            objectMapper.setAnnotationIntrospector(new IgnoreInheritedIntrospector());
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Output with line breaks.
            XmlStreamWriter xmlStreamWriter = new XmlStreamWriter(xmlFileOutput);
            objectMapper.writeValue(xmlStreamWriter, configuration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SearchConfiguration deserializeXML(File xmlFileInput) {
        try {
            XmlStreamReader xmlStreamReader = new XmlStreamReader(xmlFileInput);
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.setAnnotationIntrospector(new IgnoreInheritedIntrospector());
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return xmlMapper.readValue(xmlStreamReader, SearchConfiguration.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void loadLoggerConfiguration() {
        try {
            File file = new File(".", File.separatorChar + "log4j.xml");
            if (!file.exists()) {
                file = new File("./src/main/resources/log4j.xml");
            }
            System.setProperty("log4j.configurationFile", file.toURI().toURL().toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    // Prevent serialization of lots of graphics things.
    private static class IgnoreInheritedIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public boolean hasIgnoreMarker(final AnnotatedMember m) {
            // System.out.println(m.getDeclaringClass());
            return m.getDeclaringClass().getName().contains("sun.awt")
                    || m.getDeclaringClass().getName().contains("java.awt")
                    || m.getDeclaringClass().getName().contains("javax.swing")
                    || m.getDeclaringClass().getName().contains("org.jfree")
                    || super.hasIgnoreMarker(m);
        }
    }
}
