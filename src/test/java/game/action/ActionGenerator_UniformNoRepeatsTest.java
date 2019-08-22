package game.action;

import distributions.Distribution_Equal;
import game.state.State;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tree.node.NodeQWOPExplorable;

import java.util.*;

import static org.mockito.Mockito.mock;

public class ActionGenerator_UniformNoRepeatsTest {

    private IActionGenerator generator;
    private ActionList[] actionLists;

    @Before
    public void setup() {
        Action a1 = new Action(23, CommandQWOP.WO);
        Action a2 = new Action(24, CommandQWOP.NONE);
        Action a3 = new Action(2, CommandQWOP.WP);
        Action a4 = new Action(3, CommandQWOP.WP);
        Action a5 = new Action(8, CommandQWOP.QP);
        Set<Action> aset1 = new HashSet<>();
        aset1.add(a1);
        aset1.add(a2);

        Set<Action> aset2 = new HashSet<>();
        aset2.add(a3);
        aset2.add(a4);

        Set<Action> aset3 = new HashSet<>();
        aset3.add(a5);

        ActionList alist1 = new ActionList(new Distribution_Equal());
        alist1.addAll(aset1);

        ActionList alist2 = new ActionList(new Distribution_Equal());
        alist2.addAll(aset2);

        ActionList alist3 = new ActionList(new Distribution_Equal());
        alist3.addAll(aset3);

        actionLists = new ActionList[]{alist1, alist2, alist3};

        generator = new ActionGenerator_UniformNoRepeats(actionLists);

    }

    @Test
    public void getPotentialChildActionSet() {

        State st = mock(State.class);
        NodeQWOPExplorable root = new NodeQWOPExplorable(st, generator);
        List<NodeQWOPExplorable> depth1 = new ArrayList<>();
        List<List<NodeQWOPExplorable>> depth2 = new ArrayList<>();

        // Add all possible nodes to depth 2.
        while (root.getUntriedActionCount() > 0) {
            depth1.add(root.addDoublyLinkedChild(root.getUntriedActionRandom(), st));
        }

        for (NodeQWOPExplorable n : depth1) {
            List<NodeQWOPExplorable> subdepth2 = new ArrayList<>();
            while (n.getUntriedActionCount() > 0) {
                subdepth2.add(n.addDoublyLinkedChild(n.getUntriedActionRandom(), st));
            }
            depth2.add(subdepth2);
        }

        // All action are candidates for the first layer.
        Set<Action> allPossible = generator.getAllPossibleActions();
        Assert.assertEquals(allPossible.size(), depth1.size());

        List<Action> actionsDepth1 = new ArrayList<>();
        depth1.forEach(n -> actionsDepth1.add(n.getAction()));
        Assert.assertTrue(actionsDepth1.containsAll(allPossible));

        for (List<NodeQWOPExplorable> nlist : depth2) {
            Assert.assertTrue(nlist.size() < allPossible.size());

            for (NodeQWOPExplorable n : nlist) {
                Assert.assertNotEquals(n.getParent().getAction(), n.getAction()); // May not have the same parent
                // action.

                Assert.assertNotEquals(n.getParent().getAction().peek(), n.getAction().peek()); // Also may not have
                // the same keys too.
            }
        }
    }

    @Test
    public void getAllPossibleActions() {
        ActionList alist = new ActionList(new Distribution_Equal());
        for (ActionList a : actionLists) {
            alist.addAll(a);
        }

        Set<Action> allPossible = generator.getAllPossibleActions();
        Assert.assertEquals(allPossible.size(), alist.size());
        Assert.assertTrue(alist.containsAll(allPossible));
    }
}