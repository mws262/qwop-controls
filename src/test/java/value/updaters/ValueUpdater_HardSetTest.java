package value.updaters;

import game.qwop.GameQWOP;
import game.action.Action;
import game.qwop.CommandQWOP;
import game.state.IState;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeQWOP;

public class ValueUpdater_HardSetTest {

    @Test
    public void update() {

        IState s = GameQWOP.getInitialState();
        NodeQWOP<CommandQWOP> root = new NodeQWOP<>(s);

        Action<CommandQWOP> a1 = new Action<>(4, CommandQWOP.Q);
        Action<CommandQWOP> a2 = new Action<>(10, CommandQWOP.QO);
        Action<CommandQWOP> a3 = new Action<>(12, CommandQWOP.NONE);

        NodeQWOP<CommandQWOP> n1 = root.addBackwardsLinkedChild(a1, s);
        NodeQWOP<CommandQWOP> n2 = root.addBackwardsLinkedChild(a2, s);
        NodeQWOP<CommandQWOP> n3 = root.addBackwardsLinkedChild(a3, s);

        NodeQWOP<CommandQWOP> n1_1 = n1.addDoublyLinkedChild(a1, s);
        NodeQWOP<CommandQWOP> n1_2 = n1.addDoublyLinkedChild(a2, s);
        NodeQWOP<CommandQWOP> n1_1_1 = n1_1.addDoublyLinkedChild(a1, s);

        ValueUpdater_HardSet<CommandQWOP> updater = new ValueUpdater_HardSet<>();

        n1.updateValue(3, updater);
        n2.updateValue(4, updater);
        n3.updateValue(5, updater);
        n1_1.updateValue(6, updater);
        n1_2.updateValue(7, updater);
        n1_1_1.updateValue(8, updater);

        Assert.assertEquals(3, n1.getValue(), 1e-10f);
        Assert.assertEquals(4, n2.getValue(), 1e-10f);
        Assert.assertEquals(5, n3.getValue(), 1e-10f);
        Assert.assertEquals(6, n1_1.getValue(), 1e-10f);
        Assert.assertEquals(7, n1_2.getValue(), 1e-10f);
        Assert.assertEquals(8, n1_1_1.getValue(), 1e-10f);

        n1.updateValue(9, updater);
        n2.updateValue(10, updater);
        n3.updateValue(11, updater);
        n1_1.updateValue(12, updater);
        n1_2.updateValue(13, updater);
        n1_1_1.updateValue(14, updater);

        Assert.assertEquals(9, n1.getValue(), 1e-10f);
        Assert.assertEquals(10, n2.getValue(), 1e-10f);
        Assert.assertEquals(11, n3.getValue(), 1e-10f);
        Assert.assertEquals(12, n1_1.getValue(), 1e-10f);
        Assert.assertEquals(13, n1_2.getValue(), 1e-10f);
        Assert.assertEquals(14, n1_1_1.getValue(), 1e-10f);
    }
}