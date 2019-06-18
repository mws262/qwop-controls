package evaluators;

import game.actions.Action;
import game.state.State;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tree.node.NodeQWOP;

import static org.mockito.Mockito.mock;

public class EvaluationFunction_RandomTest {
    // Some sample game.actions (mocked).
    private Action a1 = mock(Action.class);
    private Action a2 = mock(Action.class);
    private Action a3 = mock(Action.class);

    private NodeQWOP node1;
    private NodeQWOP node2;
    private NodeQWOP node3;
    private NodeQWOP node4;

    @Before
    public void setup() {
        State st = mock(State.class);
        node1 = new NodeQWOP(st);
        node2 = node1.addDoublyLinkedChild(a1, st);
        node3 = node1.addDoublyLinkedChild(a2, st);
        node4 = node2.addDoublyLinkedChild(a3, st);
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