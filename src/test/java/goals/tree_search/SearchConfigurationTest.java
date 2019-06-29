package goals.tree_search;

import controllers.Controller_Null;
import controllers.Controller_ValueFunction;
import distributions.Distribution_Equal;
import distributions.Distribution_Normal;
import game.GameUnified;
import game.action.Action;
import game.action.ActionGenerator_FixedActions;
import game.action.ActionGenerator_FixedSequence;
import game.action.ActionList;
import game.state.IState;
import game.state.State;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tree.node.NodeQWOP;
import tree.node.NodeQWOPExplorable;
import tree.node.evaluator.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SearchConfigurationTest {

    private final IState testState1 = GameUnified.getInitialState();
    private final NodeQWOPExplorable sampleNode1 = new NodeQWOPExplorable(testState1);

    private IState testState2;
    private NodeQWOPExplorable sampleNode2;

    @Before
    public void setup() {
        GameUnified game = new GameUnified();
        for (int i = 0; i < 10; i++) {
            game.step(false, true, true, false);
        }
        testState2 = game.getCurrentState();
        sampleNode2 = new NodeQWOPExplorable(testState2);
    }

    @Test
    public void yamlDistribution_Equal() throws IOException {
        File file = File.createTempFile("disteq", "yaml");
        file.deleteOnExit();
        Distribution_Equal dist = new Distribution_Equal();
        SearchConfiguration.serializeToYaml(file, dist);
        Assert.assertTrue(file.exists());

        Distribution_Equal distLoaded = SearchConfiguration.deserializeYaml(file, Distribution_Equal.class);
        Assert.assertNotNull(distLoaded);
        Assert.assertEquals(dist, distLoaded);
    }

    @Test
    public void yamlDistribution_Normal() throws IOException {
        File file = File.createTempFile("distnorm", "yaml");
        file.deleteOnExit();
        Distribution_Normal dist = new Distribution_Normal(12, 3);
        SearchConfiguration.serializeToYaml(file, dist);
        Assert.assertTrue(file.exists());

        Distribution_Normal distLoaded = SearchConfiguration.deserializeYaml(file, Distribution_Normal.class);
        Assert.assertNotNull(distLoaded);
        Assert.assertEquals(dist, distLoaded);
    }

    @Test
    public void yamlAction() throws IOException {
        File file = File.createTempFile("action", "yaml");
        file.deleteOnExit();
        Action action = new Action(52, false, true, true, false).getCopy();
        SearchConfiguration.serializeToYaml(file, action);
        Assert.assertTrue(file.exists());

        Action actionLoaded = SearchConfiguration.deserializeYaml(file, Action.class);
        Assert.assertNotNull(actionLoaded);
        Assert.assertArrayEquals(action.peek(), actionLoaded.peek());
        Assert.assertEquals(action.getTimestepsTotal(), actionLoaded.getTimestepsTotal());
        Assert.assertEquals(action.getTimestepsRemaining(), actionLoaded.getTimestepsRemaining());
        Assert.assertFalse(actionLoaded.isMutable());

        Assert.assertEquals(action.getKeys(), actionLoaded.getKeys());
        Assert.assertEquals(actionLoaded, actionLoaded.getCopy());
    }

    @Test
    public void yamlActionList() throws IOException {
        File file = File.createTempFile("actionlist", "yaml");
        file.deleteOnExit();
        ActionList actionList = ActionList.makeExhaustiveActionList(4, 7, new Distribution_Normal(6, 1));

        SearchConfiguration.serializeToYaml(file, actionList);
        Assert.assertTrue(file.exists());

        ActionList alistLoaded = SearchConfiguration.deserializeYaml(file, ActionList.class);
        Assert.assertNotNull(alistLoaded);
        Assert.assertEquals(actionList, alistLoaded);
    }

    @Test
    public void yamlActionGenerator_FixedActions() throws IOException{
        File file = File.createTempFile("actiongenfixed", "yaml");
        file.deleteOnExit();
        ActionGenerator_FixedActions agen = new ActionGenerator_FixedActions(ActionList.makeExhaustiveActionList(8,
                11, new Distribution_Equal()));
        SearchConfiguration.serializeToYaml(file, agen);
        Assert.assertTrue(file.exists());

        ActionGenerator_FixedActions agenLoaded = SearchConfiguration.deserializeYaml(file, ActionGenerator_FixedActions.class);
        Assert.assertNotNull(agenLoaded);
        Assert.assertEquals(agen, agenLoaded);
    }

    @Test
    public void yamlActionGenerator_FixedSequence() throws IOException {
        File file = File.createTempFile("actiongenfixedseq", "yaml");
        file.deleteOnExit();
        ActionGenerator_FixedSequence agen = ActionGenerator_FixedSequence.makeDefaultGenerator(5);
        SearchConfiguration.serializeToYaml(file, agen);
        Assert.assertTrue(file.exists());

        ActionGenerator_FixedSequence agenLoaded = SearchConfiguration.deserializeYaml(file, ActionGenerator_FixedSequence.class);
        Assert.assertNotNull(agenLoaded);
        Assert.assertEquals(agen, agenLoaded);
    }

    @Test
    public void yamlEvaluationFunction_Constant() throws IOException {
        File file = File.createTempFile("evalconst", "yaml");
        file.deleteOnExit();
        EvaluationFunction_Constant evaluationFunction = new EvaluationFunction_Constant(4.5f);
        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_Constant loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_Constant.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(evaluationFunction.getConstantValue(), loaded.getConstantValue(), 1e-12f);

        Assert.assertEquals(evaluationFunction.getValue(sampleNode1),
                loaded.getValue(sampleNode1), 1e-12f);
    }

    @Test
    public void yamlEvaluationFunction_Distance() throws IOException {
        File file = File.createTempFile("evaldist", "yaml");
        file.deleteOnExit();
        EvaluationFunction_Distance evaluationFunction = new EvaluationFunction_Distance();
        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_Distance loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_Distance.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(evaluationFunction.scalingFactor, loaded.scalingFactor, 1e-12f);

        Assert.assertEquals(evaluationFunction.getValue(sampleNode1), loaded.getValue(sampleNode1), 1e-12f);
    }

    @Test
    public void yamlEvaluationFunction_Random() throws IOException {
        File file = File.createTempFile("evalrand", "yaml");
        file.deleteOnExit();
        EvaluationFunction_Random evaluationFunction = new EvaluationFunction_Random();
        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_Random loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_Random.class);

        Assert.assertNotNull(loaded);
        // Ok, so technically we could get the same two random numbers....
        Assert.assertNotEquals(evaluationFunction.getValue(sampleNode1), loaded.getValue(sampleNode1), 1e-12f);
    }

    @Test
    public void yamlEvaluationFunction_HandTunedOnState() throws IOException {
        File file = File.createTempFile("evalhand", "yaml");
        file.deleteOnExit();
        EvaluationFunction_HandTunedOnState evaluationFunction = new EvaluationFunction_HandTunedOnState();
        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_HandTunedOnState loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_HandTunedOnState.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(evaluationFunction.getValue(sampleNode1), loaded.getValue(sampleNode1), 1e-12f);
    }

    @Test
    public void yamlEvaluationFunction_SqDistFromOther() throws IOException {
        File file = File.createTempFile("evalsqdist", "yaml");
        file.deleteOnExit();
        EvaluationFunction_SqDistFromOther evaluationFunction =
                new EvaluationFunction_SqDistFromOther(new State(new float[72], false));

        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_SqDistFromOther loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_SqDistFromOther.class);

        Assert.assertNotNull(loaded);
        Assert.assertEquals(evaluationFunction.getValue(sampleNode1), loaded.getValue(sampleNode1), 1e-12f);
        Assert.assertEquals(evaluationFunction.getComparisonState(), loaded.getComparisonState());
    }

    @Test
    public void yamlEvaluationFunction_Velocity() throws IOException {
        File file = File.createTempFile("evalvel", "yaml");
        file.deleteOnExit();
        EvaluationFunction_Velocity evaluationFunction = new EvaluationFunction_Velocity();

        SearchConfiguration.serializeToYaml(file, evaluationFunction);
        Assert.assertTrue(file.exists());

        EvaluationFunction_Velocity loaded = SearchConfiguration.deserializeYaml(file,
                EvaluationFunction_Velocity.class);
        Assert.assertEquals(evaluationFunction.getValue(sampleNode1), loaded.getValue(sampleNode1), 1e-12f);
    }

    @Test
    public void yamlController_Null() throws IOException {
        File file = File.createTempFile("controlnull", "yaml");
        file.deleteOnExit();
        Controller_Null controller = new Controller_Null();

        SearchConfiguration.serializeToYaml(file, controller);
        Assert.assertTrue(file.exists());

        Controller_Null loaded = SearchConfiguration.deserializeYaml(file, Controller_Null.class);
        Assert.assertNotNull(loaded);
        Assert.assertEquals(controller.policy(sampleNode1), loaded.policy(sampleNode1));
    }

    @Test
    public void yamlController_ValueFunction() throws IOException {
//        File file = File.createTempFile("controlvalue", "yaml");
//        file.deleteOnExit();
//        Controller_ValueFunction controller = new Controller_ValueFunction();
//
//        SearchConfiguration.serializeToYaml(file, controller);
//        Assert.assertTrue(file.exists());
//
//        Controller_Null loaded = SearchConfiguration.deserializeYaml(file, Controller_Null.class);
//        Assert.assertNotNull(loaded);
//        Assert.assertEquals(controller.policy(sampleNode1), loaded.policy(sampleNode1));
    }

    @Test
    public void yamlValueFunction_Constant() {

    }

    @Test
    public void yamlValueFunction_TensorFlow_StateOnly() {

    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private static Class[] getClasses(String packageName)
            throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

}