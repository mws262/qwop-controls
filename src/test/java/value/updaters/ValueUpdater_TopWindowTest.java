package value.updaters;

import game.qwop.GameQWOP;
import game.action.Action;
import game.qwop.CommandQWOP;
import game.state.IState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tree.node.NodeQWOP;
import tree.node.NodeQWOPBase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ValueUpdater_TopWindowTest {

    private List<NodeQWOPBase<?, CommandQWOP>> nlist = new ArrayList<>();
    private Action<CommandQWOP> a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13;
    private NodeQWOP<CommandQWOP> root, n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, n13;

    @Before
    public void setup() {
        IState s = GameQWOP.getInitialState();
        a3 = new Action<>(1, CommandQWOP.NONE);
        a1 = new Action<>(5, CommandQWOP.NONE);
        a2 = new Action<>(6, CommandQWOP.NONE);
        a11 = new Action<>(8, CommandQWOP.NONE);
        a12 = new Action<>(9, CommandQWOP.NONE);
        a13 = new Action<>(10, CommandQWOP.NONE);
        a9 = new Action<>(3, CommandQWOP.WO);
        a7 = new Action<>(4, CommandQWOP.WO);
        a6 = new Action<>(5, CommandQWOP.WO);
        a5 = new Action<>(6, CommandQWOP.WO);
        a8 = new Action<>(7, CommandQWOP.WO);
        a10 = new Action<>(8, CommandQWOP.WO);
        a4 = new Action<>(3, CommandQWOP.QP);

         root = new NodeQWOP<>(s);
         n1 = root.addDoublyLinkedChild(a1, s);
         n2 = root.addDoublyLinkedChild(a2, s);
         n3 = root.addDoublyLinkedChild(a3, s);
         n4 = root.addDoublyLinkedChild(a4, s);
         n5 = root.addDoublyLinkedChild(a5, s);
         n6 = root.addDoublyLinkedChild(a6, s);
         n7 = root.addDoublyLinkedChild(a7, s);
         n8 = root.addDoublyLinkedChild(a8, s);
         n9 = root.addDoublyLinkedChild(a9, s);
         n10 = root.addDoublyLinkedChild(a10, s);
         n11 = root.addDoublyLinkedChild(a11, s);
         n12 = root.addDoublyLinkedChild(a12, s);
         n13 = root.addDoublyLinkedChild(a13, s);

        nlist.add(n1);
        nlist.add(n2);
        nlist.add(n3);
        nlist.add(n4);
        nlist.add(n5);
        nlist.add(n6);
        nlist.add(n7);
        nlist.add(n8);
        nlist.add(n9);
        nlist.add(n10);
        nlist.add(n11);
        nlist.add(n12);
        nlist.add(n13);
    }

    @Test
    public void ClusterSortedNodes() {
        nlist.sort(Comparator.comparing(NodeQWOPBase::getAction));

        List<List<NodeQWOPBase<?, CommandQWOP>>> lists = ValueUpdater_TopWindow.separateClustersInSortedList(nlist);

        Assert.assertEquals(1, lists.get(0).size());
        Assert.assertEquals(a3, lists.get(0).get(0).getAction());

        Assert.assertEquals(2, lists.get(1).size());
        Assert.assertEquals(a1, lists.get(1).get(0).getAction());
        Assert.assertEquals(a2, lists.get(1).get(1).getAction());

        Assert.assertEquals(3, lists.get(2).size());
        Assert.assertEquals(a11, lists.get(2).get(0).getAction());
        Assert.assertEquals(a12, lists.get(2).get(1).getAction());
        Assert.assertEquals(a13, lists.get(2).get(2).getAction());

        Assert.assertEquals(5, lists.size());
        Assert.assertEquals(1, lists.get(3).size());
        Assert.assertEquals(a4, lists.get(3).get(0).getAction());

        Assert.assertEquals(6, lists.get(4).size());
        Assert.assertEquals(a9, lists.get(4).get(0).getAction());
        Assert.assertEquals(a7, lists.get(4).get(1).getAction());
        Assert.assertEquals(a6, lists.get(4).get(2).getAction());
        Assert.assertEquals(a5, lists.get(4).get(3).getAction());
        Assert.assertEquals(a8, lists.get(4).get(4).getAction());
        Assert.assertEquals(a10, lists.get(4).get(5).getAction());
    }

    @Test
    public void WorstCaseWindow() {
        // Try for various window sizes.
        ValueUpdater_TopWindow<CommandQWOP> updater = new ValueUpdater_TopWindow<>(1);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.WORST;

        n1.updateValue(8, updater);
        Assert.assertEquals(8, n1.getValue(), 1e-10f);
        n2.updateValue(10, updater);
        Assert.assertEquals(10, n2.getValue(), 1e-10f);
        n3.updateValue(4, updater);
        Assert.assertEquals(4, n3.getValue(), 1e-10f);
        n4.updateValue(-1, updater);
        Assert.assertEquals(-1, n4.getValue(), 1e-10f);
        n5.updateValue(0, updater);
        Assert.assertEquals(0, n5.getValue(), 1e-10f);
        n6.updateValue(4, updater);
        Assert.assertEquals(4, n6.getValue(), 1e-10f);
        n7.updateValue(11, updater);
        Assert.assertEquals(11, n7.getValue(), 1e-10f);
        n8.updateValue(-2, updater);
        Assert.assertEquals(-2, n8.getValue(), 1e-10f);
        n9.updateValue(15, updater);
        Assert.assertEquals(15, n9.getValue(), 1e-10f);
        n10.updateValue(14, updater);
        Assert.assertEquals(14, n10.getValue(), 1e-10f);
        n11.updateValue(3, updater);
        Assert.assertEquals(3, n11.getValue(), 1e-10f);
        n12.updateValue(2, updater);
        Assert.assertEquals(2, n12.getValue(), 1e-10f);
        n13.updateValue(-6, updater);
        Assert.assertEquals(-6, n13.getValue(), 1e-10f);

        root.updateValue(0, updater);
        Assert.assertEquals(15, root.getValue(), 1e-10f);
        n4.updateValue(20, updater);
        root.updateValue(0, updater);
        Assert.assertEquals(20, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(2);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.WORST;
        root.updateValue(0, updater);
        Assert.assertEquals(11, root.getValue(), 1e-10f);
        n9.updateValue(-1, updater);
        Assert.assertEquals(-1, n9.getValue(), 1e-10f);
        root.updateValue(0, updater);
        Assert.assertEquals(8, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(3);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.WORST;
        root.updateValue(0, updater);
        Assert.assertEquals(0, root.getValue(), 1e-10f);
        n13.updateValue(9, updater);
        root.updateValue(0, updater);
        Assert.assertEquals(2, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(4);
        root.updateValue(0, updater);
        Assert.assertEquals(-1, root.getValue(), 1e-10f);
        n8.updateValue(6, updater);
        root.updateValue(0, updater);
        Assert.assertEquals(0, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(5);
        root.updateValue(0, updater);
        Assert.assertEquals(0, root.getValue(), 1e-10f);
        n5.updateValue(1, updater);
        root.updateValue(0, updater);
        Assert.assertEquals(1, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(6);
        root.updateValue(0, updater);
        Assert.assertEquals(-1, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(7);
        root.updateValue(0, updater);
        Assert.assertEquals(-1, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(8);
        root.updateValue(0, updater);
        Assert.assertEquals(-1, root.getValue(), 1e-10f);
    }

    @Test
    public void AverageWindow() {
        // Try for various window sizes.
        ValueUpdater_TopWindow<CommandQWOP> updater = new ValueUpdater_TopWindow<>(1);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.AVERAGE_OPTIMISTIC;

        n1.updateValue(8, updater);
        Assert.assertEquals(8, n1.getValue(), 1e-10f);
        n2.updateValue(10, updater);
        Assert.assertEquals(10, n2.getValue(), 1e-10f);
        n3.updateValue(4, updater);
        Assert.assertEquals(4, n3.getValue(), 1e-10f);
        n4.updateValue(-1, updater);
        Assert.assertEquals(-1, n4.getValue(), 1e-10f);
        n5.updateValue(0, updater);
        Assert.assertEquals(0, n5.getValue(), 1e-10f);
        n6.updateValue(4, updater);
        Assert.assertEquals(4, n6.getValue(), 1e-10f);
        n7.updateValue(11, updater);
        Assert.assertEquals(11, n7.getValue(), 1e-10f);
        n8.updateValue(-2, updater);
        Assert.assertEquals(-2, n8.getValue(), 1e-10f);
        n9.updateValue(15, updater);
        Assert.assertEquals(15, n9.getValue(), 1e-10f);
        n10.updateValue(14, updater);
        Assert.assertEquals(14, n10.getValue(), 1e-10f);
        n11.updateValue(3, updater);
        Assert.assertEquals(3, n11.getValue(), 1e-10f);
        n12.updateValue(2, updater);
        Assert.assertEquals(2, n12.getValue(), 1e-10f);
        n13.updateValue(-6, updater);
        Assert.assertEquals(-6, n13.getValue(), 1e-10f);

        root.updateValue(0f, updater);
        Assert.assertEquals(15f, root.getValue(), 1e-10f);
        n4.updateValue(20f, updater);
        root.updateValue(0f, updater);
        Assert.assertEquals(20f, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(2);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.AVERAGE_OPTIMISTIC;
        root.updateValue(0f, updater);
        Assert.assertEquals(13f, root.getValue(), 1e-10f);
        n9.updateValue(-1f, updater);
        Assert.assertEquals(-1f, n9.getValue(), 1e-10f);
        root.updateValue(0f, updater);
        Assert.assertEquals(9f, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(3);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.AVERAGE_OPTIMISTIC;
        root.updateValue(0f, updater);
        Assert.assertEquals(15f/3f, root.getValue(), 1e-10f);
        n13.updateValue(12f, updater);
        root.updateValue(0f, updater);
        Assert.assertEquals(17f/3f, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(4);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.AVERAGE_OPTIMISTIC;
        root.updateValue(0f, updater);
        Assert.assertEquals(4f, root.getValue(), 1e-10f);
        n8.updateValue(6f, updater);
        root.updateValue(0f, updater);
        Assert.assertEquals(6f, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(5);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.AVERAGE_OPTIMISTIC;
        root.updateValue(0f, updater);
        Assert.assertEquals(7f, root.getValue(), 1e-10f);
        n9.updateValue(20, updater);
        root.updateValue(0, updater);
        Assert.assertEquals(41f/5f, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(6);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.AVERAGE_OPTIMISTIC;
        root.updateValue(0, updater);
        Assert.assertEquals(55f/6f, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(7);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.AVERAGE_OPTIMISTIC;
        root.updateValue(0, updater);
        Assert.assertEquals(55f/6f, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(7);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.AVERAGE_PESSIMISTIC;
        root.updateValue(0, updater);
        Assert.assertEquals(55f/7f, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(8);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.AVERAGE_OPTIMISTIC;
        root.updateValue(0, updater);
        Assert.assertEquals(55f/6f, root.getValue(), 1e-10f);

        updater = new ValueUpdater_TopWindow<>(8);
        updater.windowScoringCriterion = ValueUpdater_TopWindow.Criteria.AVERAGE_PESSIMISTIC;
        root.updateValue(0, updater);
        Assert.assertEquals(55f/8f, root.getValue(), 1e-10f);
    }
}