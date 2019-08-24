package tree.sampler;

import distributions.Distribution_Equal;
import game.GameUnified;
import game.action.*;
import game.state.State;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tree.node.NodeQWOPExplorable;
import tree.node.NodeQWOPExplorableBase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Sampler_DeterministicTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.


    private State unfailedState = mock(State.class);
    private State failedState = mock(State.class);
    {
        when(unfailedState.isFailed()).thenReturn(false);
        when(failedState.isFailed()).thenReturn(true);
    }
    @Test
    public void treeBuild() {

        ActionList<CommandQWOP> alist1 = new ActionList<>(new Distribution_Equal<>());
        alist1.add(new Action<>(1, CommandQWOP.WO));
        alist1.add(new Action<>(2, CommandQWOP.NONE));
        ActionList<CommandQWOP> alist2 = new ActionList<>(new Distribution_Equal<>());
        alist2.add(new Action<>(3, CommandQWOP.QO));
        alist2.add(new Action<>(4, CommandQWOP.Q));
        alist2.add(new Action<>(5, CommandQWOP.P));

        ActionGenerator_FixedSequence<CommandQWOP> generator = new ActionGenerator_FixedSequence<>
        (new ActionList[] {alist1, alist2});

        Sampler_Deterministic<CommandQWOP> sampler = new Sampler_Deterministic<>();

        // Tree policy from root with no other nodes just stays there.
        NodeQWOPExplorable<CommandQWOP> root = new NodeQWOPExplorable<>(GameUnified.getInitialState(), generator);
        Assert.assertEquals(2, root.getUntriedActionCount());
        NodeQWOPExplorableBase<?, CommandQWOP> tp1 = sampler.treePolicy(root);
        Assert.assertEquals(root, tp1);

        sampler.treePolicyActionDone(tp1);
        Assert.assertTrue(sampler.treePolicyGuard(tp1));
        Assert.assertFalse(sampler.expansionPolicyGuard(tp1));

        // Expansion from root. Always gets the first action.
        Action<CommandQWOP> expansionAction1 = sampler.expansionPolicy(tp1);
        Assert.assertEquals(alist1.get(0), expansionAction1);
        Assert.assertFalse(sampler.expansionPolicyGuard(tp1));

        NodeQWOPExplorableBase<?, CommandQWOP> expansionNode1 = tp1.addDoublyLinkedChild(expansionAction1, unfailedState);
        Assert.assertFalse(expansionNode1.isFullyExplored());
        sampler.expansionPolicyActionDone(expansionNode1);
        Assert.assertFalse(sampler.expansionPolicyGuard(expansionNode1));

        // Depth 2 expansion, gets failed state.
        Action<CommandQWOP> expansionAction2 = sampler.expansionPolicy(expansionNode1);
        Assert.assertEquals(alist2.get(0), expansionAction2);
        NodeQWOPExplorableBase<?, CommandQWOP> expansionNode2 = expansionNode1.addDoublyLinkedChild(expansionAction2,
                failedState);
        sampler.expansionPolicyActionDone(expansionNode2);
        Assert.assertTrue(expansionNode2.isFullyExplored());
        Assert.assertTrue(sampler.expansionPolicyGuard(expansionNode2));

        // No rollout for this sampler.
        Assert.assertTrue(sampler.rolloutPolicyGuard(expansionNode2));
        Assert.assertFalse(sampler.treePolicyGuard(root));

        root.releaseExpansionRights();

        // Second sampling from root. Should expand from root again.
        NodeQWOPExplorableBase<?, CommandQWOP> tp2 = sampler.treePolicy(root);
        Assert.assertEquals(root, tp2);
        sampler.treePolicyActionDone(tp1);
        Assert.assertTrue(sampler.treePolicyGuard(tp1));
        Assert.assertFalse(sampler.expansionPolicyGuard(tp1));

        Action<CommandQWOP> expansionAction3 = sampler.expansionPolicy(root);
        Assert.assertEquals(alist1.get(1), expansionAction3);
        NodeQWOPExplorableBase<?, CommandQWOP> expansionNode3 = root.addDoublyLinkedChild(expansionAction3, failedState);
        sampler.expansionPolicyActionDone(expansionNode3);
        Assert.assertTrue(expansionNode3.isFullyExplored());
        Assert.assertTrue(sampler.expansionPolicyGuard(expansionNode3));
        Assert.assertTrue(sampler.rolloutPolicyGuard(expansionNode3));

        root.releaseExpansionRights();
        Assert.assertFalse(root.isLocked());

        // Another failed node, this time should be added at depth 2.
        NodeQWOPExplorableBase<?, CommandQWOP> tp3 = sampler.treePolicy(root);
        Assert.assertEquals(expansionNode1, tp3);
        sampler.treePolicyActionDone(tp3);
        Assert.assertTrue(sampler.treePolicyGuard(tp3));
        Assert.assertFalse(sampler.expansionPolicyGuard(tp3));
        Assert.assertTrue(root.isLocked());
        Assert.assertTrue(expansionNode1.isLocked());
        Action<CommandQWOP> expansionAction4 = sampler.expansionPolicy(tp3);
        Assert.assertEquals(alist2.get(1), expansionAction4);
        NodeQWOPExplorableBase<?, CommandQWOP> expansionNode4 = tp3.addDoublyLinkedChild(expansionAction4, failedState);
        Assert.assertTrue(expansionNode4.isFullyExplored());
        sampler.expansionPolicyActionDone(expansionNode4);
        Assert.assertTrue(sampler.expansionPolicyGuard(expansionNode4));
        Assert.assertTrue(sampler.rolloutPolicyGuard(expansionNode4));

        tp3.releaseExpansionRights();
        Assert.assertFalse(tp3.isLocked());
        Assert.assertFalse(expansionNode2.isLocked());
        Assert.assertFalse(root.isLocked());

        // Add the last fully-explored node. Should cause the full tree to become fully-explored.
        Assert.assertFalse(sampler.treePolicyGuard(root));
        NodeQWOPExplorableBase<?, CommandQWOP> tp4 = sampler.treePolicy(root);
        Assert.assertEquals(expansionNode1, tp4);
        Assert.assertTrue(tp4.isLocked());
        Assert.assertTrue(root.isLocked());

        Assert.assertFalse(sampler.treePolicyGuard(tp4));
        sampler.treePolicyActionDone(tp4);
        Assert.assertTrue(sampler.treePolicyGuard(tp4));
        Assert.assertFalse(sampler.expansionPolicyGuard(tp4));
        Action<CommandQWOP> expansionAction5 = sampler.expansionPolicy(tp4);
        NodeQWOPExplorableBase<?, CommandQWOP> expansionNode5 = tp4.addDoublyLinkedChild(expansionAction5, failedState);
        Assert.assertFalse(sampler.expansionPolicyGuard(expansionNode5));
        sampler.expansionPolicyActionDone(expansionNode5);
        Assert.assertTrue(sampler.expansionPolicyGuard(expansionNode5));
        Assert.assertFalse(expansionNode5.isLocked());
        Assert.assertTrue(expansionNode5.isFullyExplored());

        Assert.assertEquals(0, tp4.getUntriedActionCount());
        Assert.assertTrue(sampler.rolloutPolicyGuard(expansionNode5));

        tp4.releaseExpansionRights();

        Assert.assertTrue(root.isFullyExplored());
        Assert.assertFalse(root.isLocked());
        Assert.assertFalse(tp4.isLocked());

        // Since root should be fully-explored, this should throw.
        exception.expect(IllegalStateException.class);
        sampler.treePolicy(root);
    }
}