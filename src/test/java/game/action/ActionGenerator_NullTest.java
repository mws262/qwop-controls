package game.action;

import game.state.State;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeQWOPExplorable;

import static org.mockito.Mockito.mock;

public class ActionGenerator_NullTest {

    @Test
    public void getPotentialChildActionSet() {
        IActionGenerator actionGenerator = new ActionGenerator_Null();
        State st = mock(State.class);
        NodeQWOPExplorable root = new NodeQWOPExplorable(st, actionGenerator);
        NodeQWOPExplorable n1 = root.addDoublyLinkedChild(new Action(55, false, false, false, true), st);
        NodeQWOPExplorable n2 = n1.addDoublyLinkedChild(new Action(33, true, false, false, true), st);
        NodeQWOPExplorable n3 = root.addDoublyLinkedChild(new Action(11, true, true, false, true), st);

        ActionList potentialChildActionSet = actionGenerator.getPotentialChildActionSet(root);
        Assert.assertEquals(0, potentialChildActionSet.size());
        potentialChildActionSet = actionGenerator.getPotentialChildActionSet(n1);
        Assert.assertEquals(0, potentialChildActionSet.size());
        potentialChildActionSet = actionGenerator.getPotentialChildActionSet(n2);
        Assert.assertEquals(0, potentialChildActionSet.size());
        potentialChildActionSet = actionGenerator.getPotentialChildActionSet(n3);
        Assert.assertEquals(0, potentialChildActionSet.size());
    }

    @Test
    public void getAllPossibleActions() {
        IActionGenerator actionGenerator = new ActionGenerator_Null();
        Assert.assertEquals(0, actionGenerator.getAllPossibleActions().size());
    }
}