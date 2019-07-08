package value.updaters;

import game.GameUnified;
import game.action.Action;
import game.state.IState;
import game.state.State;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tree.node.NodeQWOP;
import tree.node.NodeQWOPBase;
import tree.node.NodeQWOPExplorable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static game.action.Action.Keys.*;
import static org.junit.Assert.*;

public class ValueUpdater_TopWindowTest {

    @Test
    public void TopWindow() {
        List<NodeQWOPBase<?>> nlist = new ArrayList<>();

        IState s = GameUnified.getInitialState();
        Action a3 = new Action(1, none);
        Action a1 = new Action(5, none);
        Action a2 = new Action(6, none);
        Action a11 = new Action(8, none);
        Action a12 = new Action(9, none);
        Action a13 = new Action(10, none);
        Action a9 = new Action(3, wo);
        Action a7 = new Action(4, wo);
        Action a6 = new Action(5, wo);
        Action a5 = new Action(6, wo);
        Action a8 = new Action(7, wo);
        Action a10 = new Action(8, wo);
        Action a4 = new Action(3, qp);

        NodeQWOP root = new NodeQWOP(s);
        NodeQWOP n1 = root.addDoublyLinkedChild(a1, s);
        NodeQWOP n2 = root.addDoublyLinkedChild(a2, s);
        NodeQWOP n3 = root.addDoublyLinkedChild(a3, s);
        NodeQWOP n4 = root.addDoublyLinkedChild(a4, s);
        NodeQWOP n5 = root.addDoublyLinkedChild(a5, s);
        NodeQWOP n6 = root.addDoublyLinkedChild(a6, s);
        NodeQWOP n7 = root.addDoublyLinkedChild(a7, s);
        NodeQWOP n8 = root.addDoublyLinkedChild(a8, s);
        NodeQWOP n9 = root.addDoublyLinkedChild(a9, s);
        NodeQWOP n10 = root.addDoublyLinkedChild(a10, s);
        NodeQWOP n11 = root.addDoublyLinkedChild(a11, s);
        NodeQWOP n12 = root.addDoublyLinkedChild(a12, s);
        NodeQWOP n13 = root.addDoublyLinkedChild(a13, s);

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

        nlist.sort(Comparator.comparing(NodeQWOPBase::getAction));

        List<List<NodeQWOPBase<?>>> lists = ValueUpdater_TopWindow.separateClustersInSortedList(nlist);

        Assert.assertEquals(1, lists.get(0).size());
        Assert.assertEquals(a3, lists.get(0).get(0).getAction());

        Assert.assertEquals(2, lists.get(1).size());
        Assert.assertEquals(a1, lists.get(1).get(0).getAction());
        Assert.assertEquals(a2, lists.get(1).get(1).getAction());

        Assert.assertEquals(3, lists.get(2).size());
        Assert.assertEquals(a11, lists.get(2).get(0).getAction());
        Assert.assertEquals(a12, lists.get(2).get(1).getAction());
        Assert.assertEquals(a13, lists.get(2).get(2).getAction());

        Assert.assertEquals(6, lists.get(3).size());
        Assert.assertEquals(a9, lists.get(3).get(0).getAction());
        Assert.assertEquals(a7, lists.get(3).get(1).getAction());
        Assert.assertEquals(a6, lists.get(3).get(2).getAction());
        Assert.assertEquals(a5, lists.get(3).get(3).getAction());
        Assert.assertEquals(a8, lists.get(3).get(4).getAction());
        Assert.assertEquals(a10, lists.get(3).get(5).getAction());

        Assert.assertEquals(5, lists.size());
        Assert.assertEquals(1, lists.get(4).size());
        Assert.assertEquals(a4, lists.get(4).get(0).getAction());

        ValueUpdater_TopWindow updater = new ValueUpdater_TopWindow(3);
        updater.update(8, n1);
        Assert.assertEquals(8, n1.getValue(), 1e-10f);
        updater.update(10, n2);
        Assert.assertEquals(10, n2.getValue(), 1e-10f);
        updater.update(4, n3);
        Assert.assertEquals(4, n3.getValue(), 1e-10f);
        updater.update(-1, n4);
        Assert.assertEquals(-1, n4.getValue(), 1e-10f);
        updater.update(0, n5);
        Assert.assertEquals(0, n5.getValue(), 1e-10f);
        updater.update(4, n6);
        Assert.assertEquals(4, n6.getValue(), 1e-10f);
        updater.update(11, n7);
        Assert.assertEquals(11, n7.getValue(), 1e-10f);
        updater.update(-2, n8);
        Assert.assertEquals(-2, n8.getValue(), 1e-10f);
        updater.update(15, n9);
        Assert.assertEquals(15, n9.getValue(), 1e-10f);
        updater.update(14, n10);
        Assert.assertEquals(14, n10.getValue(), 1e-10f);
        updater.update(3, n11);
        Assert.assertEquals(3, n11.getValue(), 1e-10f);
        updater.update(2, n12);
        Assert.assertEquals(2, n12.getValue(), 1e-10f);
        updater.update(-6, n13);
        Assert.assertEquals(-6, n13.getValue(), 1e-10f);

        // TODO finish this.
    }
}