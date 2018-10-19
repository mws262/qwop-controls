package evaluators;

import actions.Action;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tree.Node;

import static org.mockito.Mockito.mock;

public class EvaluationFunction_RandomTest {
    // Some sample actions (mocked).
    private Action a1 = mock(Action.class);
    private Action a2 = mock(Action.class);
    private Action a3 = mock(Action.class);

    private Node node1;
    private Node node2;
    private Node node3;
    private Node node4;

    @Before
    public void setup() {
        node1 = new Node();
        node2 = node1.addChild(a1);
        node3 = node1.addChild(a2);
        node4 = node2.addChild(a3);
    }

    @Test
    public void getValue() {
        IEvaluationFunction efun = new EvaluationFunction_Random();
        Assert.assertFalse(efun.getValue(node1) == efun.getValue(node1));
        Assert.assertFalse(efun.getValue(node2) == efun.getValue(node2));
        Assert.assertFalse(efun.getValue(node3) == efun.getValue(node3));
        Assert.assertFalse(efun.getValue(node4) == efun.getValue(node4));
    }

    @Test
    public void getCopy() {
        IEvaluationFunction efun = new EvaluationFunction_Random();
        IEvaluationFunction efuncpy = efun.getCopy();

        Assert.assertFalse(efuncpy.getValue(node1) == efuncpy.getValue(node1));
        Assert.assertFalse(efuncpy.getValue(node2) == efuncpy.getValue(node2));
        Assert.assertFalse(efuncpy.getValue(node3) == efuncpy.getValue(node3));
        Assert.assertFalse(efuncpy.getValue(node4) == efuncpy.getValue(node4));
    }
}