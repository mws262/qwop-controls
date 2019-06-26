package goals.tree_search;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import controllers.Controller_Null;
import game.GameUnified;
import org.apache.commons.io.input.XmlStreamReader;
import org.apache.commons.io.output.XmlStreamWriter;
import savers.DataSaver_Null;
import savers.IDataSaver;
import tree.node.NodeQWOP;
import tree.node.NodeQWOPBase;
import tree.node.evaluator.*;
import tree.sampler.ISampler;
import tree.sampler.Sampler_Greedy;
import tree.sampler.Sampler_Random;
import tree.sampler.rollout.IRolloutPolicy;
import tree.sampler.rollout.RolloutPolicy_JustEvaluate;
import tree.stage.TreeStage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SearchConfiguration implements Serializable {

    public Machine machine = new Machine();
    public List<Stage> stages = new ArrayList<>();

    public SearchConfiguration() {
        Stage st = new Stage();
        stages.add(st);
    }

    /**
     * Defines run parameters having to do with threads, logging, etc.
     */
    public static class Machine {
        public float coreFraction = 0.5f;
        public int coreMinimum = 1;
        public int coreMaximum = 32;
    }

    /**
     * Defines user interface settings.
     */
    public static class UI {
        public enum UIType {
            CONSOLE, GUI
        }
        @JacksonXmlProperty(isAttribute=true)
        public String type;
    }

    /**
     * Defines a tree search operation.
     */
    public static class Stage {

        public enum StageType { // Fully-serializing the stage catches too much unnecessary information.
            FIXED_GAMES, MAX_DEPTH, MIN_DEPTH, SEARCH_FOREVER
        }

        @JacksonXmlProperty(isAttribute=true)
        public StageType type = StageType.FIXED_GAMES;

        public ISampler sampler =
                new Sampler_Greedy(new EvaluationFunction_SqDistFromOther(GameUnified.getInitialState()));

        public IDataSaver saver = new DataSaver_Null();

    }

    public static void main(String[] args) {
        SearchConfiguration configuration = new SearchConfiguration();
        serializeToXML(new File("./src/main/resources/config/config.xml"), configuration);

        configuration = deserializeXML(new File("./src/main/resources/config/config.xml"));
    }

    public static void serializeToXML(File xmlFileOutput, SearchConfiguration configuration) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            XmlStreamWriter xmlStreamWriter = new XmlStreamWriter(xmlFileOutput);
            xmlMapper.writeValue(xmlStreamWriter, configuration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SearchConfiguration deserializeXML(File xmlFileInput) {
        try {
            XmlStreamReader xmlStreamReader = new XmlStreamReader(xmlFileInput);
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            return xmlMapper.readValue(xmlStreamReader, SearchConfiguration.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
