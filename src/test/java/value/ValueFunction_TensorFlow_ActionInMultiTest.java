package value;

import actions.Action;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tree.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ValueFunction_TensorFlow_ActionInMultiTest {

    private static Node rootNode;

    private static ValueFunction_TensorFlow_ActionInMulti valFun;

    @BeforeClass
    public static void setUp() {

        // We are setting up actions:
        rootNode = ValueFunction_TensorFlow_ActionInTest.makeDemoTree();

        // Test makeNew here because it must happen first.
        List<Integer> hiddenLayerSizes = new ArrayList<>();
        hiddenLayerSizes.add(128);
        hiddenLayerSizes.add(64);

        List<Action> allActions = new ArrayList<Action>();
        allActions.addAll(ValueFunction_TensorFlow_ActionInTest.actionsLayer1);
        allActions.addAll(ValueFunction_TensorFlow_ActionInTest.actionsLayer2);
        allActions.addAll(ValueFunction_TensorFlow_ActionInTest.actionsLayer3);
        try {
            valFun = ValueFunction_TensorFlow_ActionInMulti.makeNew(allActions, "valfun_network_unit_test_tmp",
                    hiddenLayerSizes, new ArrayList<>());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Make sure that the graph files were created and that they will be cleaned up on exit.
        File[] graphDefinitionFiles = valFun.getGraphDefinitionFiles();
        for (File file : graphDefinitionFiles) {
            Assert.assertTrue(file.exists());
            file.deleteOnExit();
        }
    }

    @Test
    public void getMaximizingAction() {
    }

    @Test
    public void evaluate() {
    }

    @Test
    public void update() {
    }

    @Test
    public void loadCheckpoints() {
    }

    @Test
    public void saveCheckpoints() {
    }

    @Test
    public void loadExisting() {
    }

    @Test
    public void makeNew() {
    }
}