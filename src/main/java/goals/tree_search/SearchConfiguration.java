package goals.tree_search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Preconditions;
import game.IGameInternal;
import game.action.Command;
import game.action.IActionGenerator;
import game.state.IState;
import org.apache.commons.io.input.XmlStreamReader;
import org.apache.commons.io.output.XmlStreamWriter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import savers.DataSaver_Null;
import savers.IDataSaver;
import tree.TreeWorker;
import tree.node.NodeGameExplorable;
import tree.node.NodeGameExplorableBase;
import tree.node.NodeGameGraphics;
import tree.sampler.ISampler;
import tree.stage.TreeStage;
import ui.IUserInterface;
import ui.UI_Headless;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SearchConfiguration<C extends Command<?>, S extends IState, G extends IGameInternal<C, S>> {

    public final Machine machine;

    public final Tree<C> tree;
    public final List<SearchOperation<C, S, G>> searchOperations;
    public final IUserInterface<C, S> ui;
    public final G game;

    SearchConfiguration(@JsonProperty("machine") Machine machine,
                        @JsonProperty("game") G game,
                        @JsonProperty("tree") Tree<C> tree,
                        @JsonProperty("searchOperations") List<SearchOperation<C, S, G>> searchOperations,
                        @JsonProperty("ui") IUserInterface<C, S> ui) {
        Preconditions.checkNotNull(machine);
        Preconditions.checkNotNull(game);
        Preconditions.checkNotNull(tree);
        Preconditions.checkNotNull(searchOperations);
        Preconditions.checkNotNull(ui);

        this.machine = machine;
        this.game = game;
        this.tree = tree;
        this.searchOperations = searchOperations;
        this.ui = ui;
    }

    /**
     * Defines run parameters having to do with threads, logging, etc.
     * Add -Dlog4j.configurationFile="./src/main/resources/log4j2.xml" to VM options if logging isn't working. Or run
     * with Maven.
     */
    @SuppressWarnings("unused")
    public static class Machine {

        /**
         * Fraction of machine cores to use for the tree search.
         */
        private final float coreFraction;

        /**
         * Minimum number of cores to be used for the tree search.
         */
        private final int coreMinimum;

        /**
         * Maximum number of cores to be used for the tree search.
         */
        private final int coreMaximum;

        /**
         * Log level to be used for log4j (e.g. debug, info, warn, error).
         */
        private final String logLevel;

        /**
         * Actual number of cores to be used for the search.
         */
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
        public int getCoreCount() { // Calculated from the other fields. Should not be saved.
            return coreCount;
        }

        @JsonIgnore
        public int getRequestedThreadCount() {
            return Math.min(Math.max((int)(coreFraction * coreCount), coreMinimum), coreMaximum);
        }
    }

    /**
     * Tree-building settings that apply to all stages.
     */
    public static class Tree<C extends Command<?>> {

        /**
         * Action generator to be used to assign the potential child actions throughout the entire tree. This remains
         * the same regardless of the tree stage being run.
         */
        public final IActionGenerator<C> actionGenerator;

        public Tree(
                @JsonProperty("actionGenerator") IActionGenerator<C> actionGenerator) {
            this.actionGenerator = actionGenerator;
        }
    }

    /**
     * Defines a single tree search operation.
     */
    public static class SearchOperation<C extends Command<?>, S extends IState, G extends IGameInternal<C, S>> {

        /**
         * Stage operation. Defines the goals and stopping points of this search.
         */
        @JacksonXmlProperty(isAttribute=true)
        private final TreeStage<C, S> stage;

        private final G game;
        /**
         * Defines how new nodes are added and scored.
         */
        private final ISampler<C, S> sampler;

        /**
         * Defines how and what data is saved at various points of the tree stage.
         */
        private final IDataSaver<C, S> saver;

        /**
         * Number of consecutive times that this operation is repeated. 0 means it only runs once. 1 means that it
         * runs one extra time.
         */
        private final int repetitionCount;


        @JsonCreator
        SearchOperation(@JsonProperty("stage") TreeStage<C, S> stage,
                        @JsonProperty("game") G game,
                        @JsonProperty("sampler") ISampler<C, S> sampler,
                        @JsonProperty("saver") IDataSaver<C, S> saver,
                        @JsonProperty("repetitionCount") int repetitionCount // Defaults to zero if not
                        // set in config file.
        ) {
            Preconditions.checkArgument(repetitionCount >= 0, "Stage repetitions may not be less than zero.",
                    repetitionCount);
            Preconditions.checkNotNull(stage);
            Preconditions.checkNotNull(sampler);

            this.stage = stage;
            this.game = game;
            this.sampler = sampler;
            this.saver = Objects.isNull(saver) ? new DataSaver_Null<>() : saver; // Default to no saving.
            this.repetitionCount = repetitionCount;
        }

        SearchOperation(TreeStage<C, S> stage,
                        G game,
                        ISampler<C, S> sampler,
                        IDataSaver<C, S> saver) {
            this(stage, game, sampler, saver, 0);
        }

        public TreeStage<C, S> getStage() {
            return stage;
        }

        public ISampler<C, S> getSampler() {
            return sampler;
        }

        public IDataSaver<C, S> getSaver() {
            return saver;
        }

        public G getGame() {
            return game;
        }

        int getRepetitionCount() {
            return repetitionCount;
        }

        /**
         * Begin this tree operation.
         * @param rootNode Node to build from.
         * @param machine Machine details, e.g. how many cores to use.
         */
        void startOperation(NodeGameExplorableBase<?, C, S> rootNode, Machine machine) {
            Preconditions.checkNotNull(rootNode);
            Preconditions.checkNotNull(machine);

            ArrayList<TreeWorker<C, S>> treeWorkers = new ArrayList<>();
            for (int i = 0; i < machine.getRequestedThreadCount(); i++) {
                treeWorkers.add(getTreeWorker());
            }

            stage.initialize(treeWorkers, rootNode);
        }

        @JsonIgnore
        TreeWorker<C, S> getTreeWorker() {
            return new TreeWorker<>(game.getCopy(), sampler.getCopy(), saver.getCopy());
        }
    }

    /**
     * Run all the operations defined in this SearchConfiguration. Tree stages will be run in the order they are listed.
     */
    public void execute() {
        NodeGameExplorableBase<?, C, S> rootNode;
        game.resetGame();
        if (ui instanceof UI_Headless) {
            rootNode = new NodeGameExplorable<>(game.getCurrentState(), tree.actionGenerator);
        } else {
            NodeGameGraphics<C, S> root = new NodeGameGraphics<>(game.getCurrentState(), tree.actionGenerator);
            ui.addRootNode(root);
            rootNode = root;
        }
        ui.start();

        for (SearchOperation<C, S, G> operation : searchOperations) {
            for (int i = 0; i <= operation.getRepetitionCount(); i++) {
                operation.startOperation(rootNode, machine);
            }
            operation.getSampler().close();
        }
    }

    static void serializeToJson(File jsonFileOutput, Object configuration) {
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.setAnnotationIntrospector(new IgnoreInheritedIntrospector());
        try {
            objectMapper.writeValue(jsonFileOutput, configuration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void serializeToXML(File xmlFileOutput, Object configuration) {
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

    /**
     * Attempt to serialize the provided object to a .yaml file. Can be applied to any object, but various
     * annotations may help make only the correct things save in the yaml.
     *
     * @param fileOutput File to save the .yaml configuration to.
     * @param object Particular object to serialize to yaml.
     */
    static void serializeToYaml(File fileOutput, Object object) {

        try {
            YAMLMapper objectMapper = new YAMLMapper();
            objectMapper.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
            objectMapper.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
            objectMapper.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
            objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
            objectMapper.disable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

            objectMapper.setAnnotationIntrospector(new IgnoreInheritedIntrospector());
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Output with line breaks.

            XmlStreamWriter xmlStreamWriter = new XmlStreamWriter(fileOutput);
            objectMapper.writeValue(xmlStreamWriter, object);
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

    /**
     * Take a .yaml definition of a class and attempt to recreate the Java object.
     * @param yamlFileOutput File containing the yaml definition.
     * @param clazz Class which the yaml is defining.
     * @param <T> Class type.
     * @return The recreated (deserialized) object.
     */
    public static <T> T deserializeYaml(File yamlFileOutput, Class<T> clazz) {
        try {
            YAMLMapper objectMapper = new YAMLMapper();
            objectMapper.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
            objectMapper.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
            objectMapper.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
            objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
            objectMapper.disable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

            objectMapper.setAnnotationIntrospector(new IgnoreInheritedIntrospector());
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper.readValue(yamlFileOutput, clazz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Prevent the serialization of certain library classes.
     */
    public static class IgnoreInheritedIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public boolean hasIgnoreMarker(final AnnotatedMember m) {
            // System.out.println(m.getDeclaringClass());
            return m.getDeclaringClass().getName().contains("sun.awt")
                    || m.getDeclaringClass().getName().contains("java.awt")
                    || m.getDeclaringClass().getName().contains("javax.swing")
                    || m.getDeclaringClass().getName().contains("org.jfree")
                    || m.getDeclaringClass().getName().contains("com.jogamp")
                    || super.hasIgnoreMarker(m);
        }
    }

    public static void main(String[] args) {
        SearchConfiguration config = deserializeYaml(new File("src/main/resources/config/default.yaml"),
                SearchConfiguration.class);
        Objects.requireNonNull(config).execute();
    }
}
