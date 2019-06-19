package game.actions;

import distributions.Distribution_Equal;
import game.state.State;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tree.node.NodeQWOPExplorable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;

public class ActionGenerator_FixedActionsTest {

    private ActionGenerator_FixedActions generator;
    private Set<Action> actions;

    @Before
    public void setup() {
        Action a1 = new Action(23, false, true, true, false);
        Action a2 = new Action(24, false, false, false, false);
        Action a3 = new Action(2, false, true, false, true);
        Action a4 = new Action(3, false, true, false, true);
        Action a5 = new Action(8, false, false, false, true);
        Action a6 = a5.getCopy(); // A duplicate!

        actions = new HashSet<>();
        actions.add(a1);
        actions.add(a2);
        actions.add(a3);
        actions.add(a4);
        actions.add(a5);
        actions.add(a6);

        Assert.assertEquals(5, actions.size()); // Only 5 because duplicate.

        ActionList actionList = new ActionList(new Distribution_Equal());
        actionList.addAll(actions);

        generator = new ActionGenerator_FixedActions(actionList);
    }

    @Test
    public void getPotentialChildActionSet() {
        State st = mock(State.class);
        NodeQWOPExplorable root = new NodeQWOPExplorable(st, generator);
        NodeQWOPExplorable n1 = root.addDoublyLinkedChild(new Action(55, false, false, false, true), st);
        NodeQWOPExplorable n2 = n1.addDoublyLinkedChild(new Action(33, true, false, false, true), st);
        NodeQWOPExplorable n3 = root.addDoublyLinkedChild(new Action(11, true, true, false, true), st);

        List<NodeQWOPExplorable> nodes = new ArrayList<>();
        nodes.add(root);
        nodes.add(n1);
        nodes.add(n2);
        nodes.add(n3);

        for (NodeQWOPExplorable n : nodes) {
            ActionList l = generator.getPotentialChildActionSet(n);
            Assert.assertEquals(actions.size(), l.size());
            Assert.assertTrue(l.containsAll(actions));
        }
    }

    @Test
    public void getAllPossibleActions() {
        Set<Action> actionSet = generator.getAllPossibleActions();

        Assert.assertEquals(actions.size(), actionSet.size());
        Assert.assertTrue(actionSet.containsAll(actions));
    }
}