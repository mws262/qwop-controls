package value.updaters;

import game.action.Action;
import game.action.CommandQWOP;
import game.state.IState;
import game.state.State;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeQWOPExplorable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValueUpdater_TopNChildrenTest {

    private IState unfailedState = mock(State.class);
    private Action a1 = new Action(1, CommandQWOP.WO);
    private Action a2 = new Action(2, CommandQWOP.O);
    private Action a3 = new Action(3, CommandQWOP.W);
    private Action a4 = new Action(4, CommandQWOP.Q);
    private Action a5 = new Action(5, CommandQWOP.WP);

    @Test
    public void update() {
        when(unfailedState.isFailed()).thenReturn(false);

        NodeQWOPExplorable root = new NodeQWOPExplorable(unfailedState);
        NodeQWOPExplorable n1 = root.addDoublyLinkedChild(a1, unfailedState);
        NodeQWOPExplorable n2 = root.addDoublyLinkedChild(a2, unfailedState);
        NodeQWOPExplorable n3 = root.addDoublyLinkedChild(a3, unfailedState);
        NodeQWOPExplorable n4 = root.addDoublyLinkedChild(a4, unfailedState);
        NodeQWOPExplorable n5 = root.addDoublyLinkedChild(a5, unfailedState);

        NodeQWOPExplorable n1_1 = n1.addDoublyLinkedChild(a1, unfailedState);
        NodeQWOPExplorable n1_2 = n1.addDoublyLinkedChild(a2, unfailedState);

        // Size 1.
        ValueUpdater_TopNChildren updater = new ValueUpdater_TopNChildren(1);

        n1_1.updateValue(2f, updater);
        n1_2.updateValue(4f, updater);
        Assert.assertEquals(2f, n1_1.getValue(), 1e-10f);
        Assert.assertEquals(4f, n1_2.getValue(), 1e-10f);

        n2.updateValue(6f, updater);
        n3.updateValue(1f, updater);
        n4.updateValue(7f, updater);
        n5.updateValue(9f, updater);

        Assert.assertEquals(6f, n2.getValue(), 1e-10f);
        Assert.assertEquals(1f, n3.getValue(), 1e-10f);
        Assert.assertEquals(7f, n4.getValue(), 1e-10f);
        Assert.assertEquals(9f, n5.getValue(), 1e-10f);

        n1.updateValue(1010101, updater);
        Assert.assertEquals(4f, n1.getValue(), 1e-10f);

        root.updateValue(22f, updater);
        Assert.assertEquals(9f, root.getValue(), 1e-10f);

        // Size 2.
        updater = new ValueUpdater_TopNChildren(2);

        n1_1.updateValue(2f, updater);
        n1_2.updateValue(4f, updater);
        Assert.assertEquals(2f, n1_1.getValue(), 1e-10f);
        Assert.assertEquals(4f, n1_2.getValue(), 1e-10f);

        n2.updateValue(6f, updater);
        n3.updateValue(1f, updater);
        n4.updateValue(7f, updater);
        n5.updateValue(9f, updater);

        Assert.assertEquals(6f, n2.getValue(), 1e-10f);
        Assert.assertEquals(1f, n3.getValue(), 1e-10f);
        Assert.assertEquals(7f, n4.getValue(), 1e-10f);
        Assert.assertEquals(9f, n5.getValue(), 1e-10f);

        n1.updateValue(1010101, updater);
        Assert.assertEquals(3f, n1.getValue(), 1e-10f);

        root.updateValue(22f, updater);
        Assert.assertEquals(8f, root.getValue(), 1e-10f);

        // Size 3.
        updater = new ValueUpdater_TopNChildren(3);

        n1_1.updateValue(2f, updater);
        n1_2.updateValue(4f, updater);
        Assert.assertEquals(2f, n1_1.getValue(), 1e-10f);
        Assert.assertEquals(4f, n1_2.getValue(), 1e-10f);

        n2.updateValue(6f, updater);
        n3.updateValue(1f, updater);
        n4.updateValue(7f, updater);
        n5.updateValue(9f, updater);

        Assert.assertEquals(6f, n2.getValue(), 1e-10f);
        Assert.assertEquals(1f, n3.getValue(), 1e-10f);
        Assert.assertEquals(7f, n4.getValue(), 1e-10f);
        Assert.assertEquals(9f, n5.getValue(), 1e-10f);

        n1.updateValue(1010101, updater);
        Assert.assertEquals(8f/3f, n1.getValue(), 1e-10f);

        root.updateValue(22f, updater);
        Assert.assertEquals(22f/3f, root.getValue(), 1e-10f);

    }
}