package game.action;

import game.qwop.CommandQWOP;
import game.qwop.StateQWOP;
import org.junit.Assert;
import org.junit.Test;
import tree.node.NodeGameExplorable;

import static org.mockito.Mockito.mock;

public class ActionGenerator_NullTest {

    @Test
    public void getPotentialChildActionSet() {
        IActionGenerator actionGenerator = new ActionGenerator_Null();
        StateQWOP st = mock(StateQWOP.class);
        NodeGameExplorable root = new NodeGameExplorable(st, actionGenerator);
        NodeGameExplorable n1 = root.addDoublyLinkedChild(new Action(55, CommandQWOP.P), st);
        NodeGameExplorable n2 = n1.addDoublyLinkedChild(new Action(33, CommandQWOP.QP), st);
        NodeGameExplorable n3 = root.addDoublyLinkedChild(new Action(11, CommandQWOP.Q), st);

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