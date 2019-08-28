package game.action;

import distributions.Distribution_Equal;
import game.qwop.CommandQWOP;
import game.qwop.StateQWOP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tree.node.NodeQWOPExplorable;

import java.util.*;

import static org.mockito.Mockito.mock;

public class ActionGenerator_UniformNoRepeatsTest {

    private IActionGenerator<CommandQWOP> generator;
    private ActionList<CommandQWOP>[] actionLists;

    @Before
    public void setup() {
        Action<CommandQWOP>
                a1 = new Action<>(23, CommandQWOP.WO),
                a2 = new Action<>(24, CommandQWOP.NONE),
                a3 = new Action<>(2, CommandQWOP.WP),
                a4 = new Action<>(3, CommandQWOP.WP),
                a5 = new Action<>(8, CommandQWOP.QP);
        Set<Action<CommandQWOP>> aset1 = new HashSet<>();
        aset1.add(a1);
        aset1.add(a2);

        Set<Action<CommandQWOP>> aset2 = new HashSet<>();
        aset2.add(a3);
        aset2.add(a4);

        Set<Action<CommandQWOP>> aset3 = new HashSet<>();
        aset3.add(a5);

        ActionList<CommandQWOP> alist1 = new ActionList<>(new Distribution_Equal<>());
        alist1.addAll(aset1);

        ActionList<CommandQWOP> alist2 = new ActionList<>(new Distribution_Equal<>());
        alist2.addAll(aset2);

        ActionList<CommandQWOP> alist3 = new ActionList<>(new Distribution_Equal<>());
        alist3.addAll(aset3);

        actionLists = new ActionList[]{alist1, alist2, alist3};

        generator = new ActionGenerator_UniformNoRepeats<>(actionLists);

    }

    @Test
    public void getPotentialChildActionSet() {

        StateQWOP st = mock(StateQWOP.class);
        NodeQWOPExplorable<CommandQWOP> root = new NodeQWOPExplorable<>(st, generator);
        List<NodeQWOPExplorable<CommandQWOP>> depth1 = new ArrayList<>();
        List<List<NodeQWOPExplorable<CommandQWOP>>> depth2 = new ArrayList<>();

        // Add all possible nodes to depth 2.
        while (root.getUntriedActionCount() > 0) {
            depth1.add(root.addDoublyLinkedChild(root.getUntriedActionRandom(), st));
        }

        for (NodeQWOPExplorable<CommandQWOP> n : depth1) {
            List<NodeQWOPExplorable<CommandQWOP>> subdepth2 = new ArrayList<>();
            while (n.getUntriedActionCount() > 0) {
                subdepth2.add(n.addDoublyLinkedChild(n.getUntriedActionRandom(), st));
            }
            depth2.add(subdepth2);
        }

        // All command are candidates for the first layer.
        Set<Action<CommandQWOP>> allPossible = generator.getAllPossibleActions();
        Assert.assertEquals(allPossible.size(), depth1.size());

        List<Action<CommandQWOP>> actionsDepth1 = new ArrayList<>();
        depth1.forEach(n -> actionsDepth1.add(n.getAction()));
        Assert.assertTrue(actionsDepth1.containsAll(allPossible));

        for (List<NodeQWOPExplorable<CommandQWOP>> nlist : depth2) {
            Assert.assertTrue(nlist.size() < allPossible.size());

            for (NodeQWOPExplorable<CommandQWOP> n : nlist) {
                Assert.assertNotEquals(n.getParent().getAction(), n.getAction()); // May not have the same parent
                // command.

                Assert.assertNotEquals(n.getParent().getAction().peek(), n.getAction().peek()); // Also may not have
                // the same keys too.
            }
        }
    }

    @Test
    public void getAllPossibleActions() {
        ActionList<CommandQWOP> alist = new ActionList<>(new Distribution_Equal<>());
        for (ActionList<CommandQWOP> a : actionLists) {
            alist.addAll(a);
        }

        Set<Action<CommandQWOP>> allPossible = generator.getAllPossibleActions();
        Assert.assertEquals(allPossible.size(), alist.size());
        Assert.assertTrue(alist.containsAll(allPossible));
    }
}