package tree.sampler;

import distributions.Distribution_Equal;
import distributions.Distribution_Normal;
import game.action.*;
import game.qwop.CommandQWOP;
import game.qwop.GameQWOP;
import game.qwop.StateQWOP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tree.node.NodeGameExplorable;
import tree.node.NodeGameExplorableBase;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.evaluator.IEvaluationFunction;
import tree.sampler.rollout.RolloutPolicy_JustEvaluate;
import value.updaters.IValueUpdater;
import value.updaters.ValueUpdater_Average;
import value.updaters.ValueUpdater_HardSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Sampler_UCBTest {

    private Action<CommandQWOP> a1, a1_1, a1_1_1, a2, a2_2;
    private StateQWOP s1, s1_1, s1_1_1, s2, s2_2;
    private NodeGameExplorableBase<?, CommandQWOP, StateQWOP> root, n1, n1_1, n1_1_1, n2, n2_2;

    private StateQWOP failedState = mock(StateQWOP.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    @Before
    public void setup() {
        /* Make a little bit of a tree manually for testing.
        root -> node1 -> node1_1 -> node1_1_1
                         __
                node2 -> __
                        node2_2
         */
        when(failedState.isFailed()).thenReturn(true);

        GameQWOP game = new GameQWOP();
        ActionQueue<CommandQWOP> actionQueue = new ActionQueue<>();

        ActionList<CommandQWOP> alist1 = new ActionList<>(new Distribution_Equal<>());
        alist1.add(new Action<>(1, CommandQWOP.WO));
        alist1.add(new Action<>(2, CommandQWOP.NONE));
        ActionList<CommandQWOP> alist2 = new ActionList<>(new Distribution_Equal<>());
        alist2.add(new Action<>(3, CommandQWOP.Q));
        alist2.add(new Action<>(4, CommandQWOP.W));
        alist2.add(new Action<>(5, CommandQWOP.P));

        ActionGenerator_FixedSequence<CommandQWOP> generator = new ActionGenerator_FixedSequence<>(alist1, alist2);
        root = new NodeGameExplorable<>(GameQWOP.getInitialState(), generator);

        // Node 1 off root.
        a1 = root.getUntriedActionByIndex(0);
        Assert.assertEquals(alist1.get(0), a1);
        actionQueue.addAction(a1);
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
        }
        s1 = game.getCurrentState();
        n1 = root.addDoublyLinkedChild(a1, s1);

        // Node 1_1 off of node 1
        a1_1 = n1.getUntriedActionByIndex(0);
        Assert.assertEquals(alist2.get(0), a1_1);
        actionQueue.addAction(a1_1);
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
        }
        s1_1 = game.getCurrentState();
        n1_1 = n1.addDoublyLinkedChild(a1_1, s1_1);

        // Node 1_1_1 off of node 1_1
        a1_1_1 = n1_1.getUntriedActionByIndex(0);
        Assert.assertEquals(alist1.get(0), a1_1_1);
        actionQueue.addAction(a1_1_1);
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
        }
        s1_1_1 = game.getCurrentState();
        n1_1_1 = n1_1.addDoublyLinkedChild(a1_1_1, s1_1_1);

        // Node 2 off root.
        game.resetGame();
        a2 = root.getUntriedActionByIndex(0);
        Assert.assertEquals(alist1.get(1), a2);
        actionQueue.addAction(a2);
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
        }
        s2 = game.getCurrentState();
        n2 = root.addDoublyLinkedChild(a2, s2);

        // Node 2_2 off node 2.
        a2_2 = n2.getUntriedActionByIndex(1);
        Assert.assertEquals(alist2.get(1), a2_2);
        actionQueue.addAction(a2_2);
        while (!actionQueue.isEmpty()) {
            game.step(actionQueue.pollCommand());
        }
        s2_2 = game.getCurrentState();
        n2_2 = n2.addDoublyLinkedChild(a2_2, s2_2);
    }

//    @Test
//    public void setEvaluationFunction() {
//        IEvaluationFunction evalFun1 = new EvaluationFunction_Constant(5f);
//        Sampler_UCB sampler = new Sampler_UCB(evalFun1, new RolloutPolicy_JustEvaluate(evalFun1));
//
//        sampler.rolloutPolicy(n1_1_1, null);
//        Assert.assertEquals(5f, n1_1_1.getValue(), 1e-12f);
//
//        sampler.setEvaluationFunction(new EvaluationFunction_Constant(3f));
//        sampler.rolloutPolicy(n2_2, null);
//        Assert.assertEquals(5f, n2_2.getValue(), 1e-12f); // Changing the evaluation function shouldn't change the
//        // results when the rollout is doing the evaluating.
//
//        IState failedState = mock(StateQWOP.class);
//        when(failedState.isFailed()).thenReturn(true);
//
//        NodeGameExplorableBase<?> badNode = n1_1_1.addDoublyLinkedChild(new Action(1010101, false, false, false,
//                false), failedState);
//
//        sampler.expansionPolicyActionDone(badNode);
//        Assert.assertEquals(3f, badNode.getValue(), 1e-12); // When an expanded node is failed, then the rollout is
//        // never called, and the sampler's default evaluation function is used.
//    }

    @Test
    public void treePolicy() {
        IEvaluationFunction<CommandQWOP, StateQWOP> evalFun1 = new EvaluationFunction_Constant<>(5f);
        Sampler_UCB<CommandQWOP, StateQWOP> sampler = new Sampler_UCB<>(
                evalFun1,
                new RolloutPolicy_JustEvaluate<>(evalFun1),
                new ValueUpdater_Average<>(), 5, 1);
        IValueUpdater<CommandQWOP, StateQWOP> valueUpdater = new ValueUpdater_HardSet<>();

        // Node 1 has a much higher value and it has un-added potential children. It should be the choice here.
        root.updateValue(0, valueUpdater);
        n1.updateValue(1e6f, valueUpdater);
        n2.updateValue(5f, valueUpdater);
        NodeGameExplorableBase<?, CommandQWOP, StateQWOP> treePolicyNode = sampler.treePolicy(root);
        Assert.assertEquals(n1, treePolicyNode);
        treePolicyNode.releaseExpansionRights();

        // Now, node 1 has higher value but is much more visited than node 2. Node 2 should be the choice.
        for (int i = 0; i < 100000; i++) {
            root.updateValue(0f, valueUpdater);
            n1.updateValue(10f, valueUpdater);
        }
        treePolicyNode = sampler.treePolicy(root);
        Assert.assertEquals(n2, treePolicyNode);
        treePolicyNode.releaseExpansionRights();

        // Now make node 2 not have any untried potential children. It should decide to go deeper.
        NodeGameExplorableBase<?, CommandQWOP, StateQWOP> n2_1 = n2.addDoublyLinkedChild(n2.getUntriedActionRandom(),
                GameQWOP.getInitialState());
        NodeGameExplorableBase<?, CommandQWOP, StateQWOP> n2_3 = n2.addDoublyLinkedChild(n2.getUntriedActionRandom(), failedState);
        Assert.assertEquals(0, n2.getUntriedActionCount());

        n2_1.updateValue(0f, valueUpdater);
        n2_3.updateValue(1e8f, valueUpdater); // Still shouldn't pick this, since the state is failed.
        n2_2.updateValue(5f, valueUpdater);

        treePolicyNode = sampler.treePolicy(root);
        Assert.assertEquals(n2_2, treePolicyNode);

        // n2_2 is locked, so another on this branch should be chosen.
        treePolicyNode = sampler.treePolicy(root);
        Assert.assertEquals(n2_1, treePolicyNode);

        n2_2.releaseExpansionRights();
        n2_1.releaseExpansionRights();

        Assert.assertFalse(sampler.treePolicyGuard(n2_2));
    }

    @Test
    public void treePolicyActionDoneAndGuard() {
        IEvaluationFunction<CommandQWOP, StateQWOP> evalFun1 = new EvaluationFunction_Constant<>(5f);
        Sampler_UCB<CommandQWOP, StateQWOP> sampler = new Sampler_UCB<>(evalFun1, new RolloutPolicy_JustEvaluate<>(evalFun1),
                new ValueUpdater_Average<>(),5, 1);

        Assert.assertFalse(sampler.treePolicyGuard(root));
        Assert.assertFalse(sampler.treePolicyGuard(n1_1));
        Assert.assertFalse(sampler.treePolicyGuard(n2));
        sampler.treePolicyActionDone(n1);

        // Shouldn't matter which node is reported in treePolicyActionDone.
        Assert.assertTrue(sampler.treePolicyGuard(root));
        Assert.assertTrue(sampler.treePolicyGuard(n1_1));
        Assert.assertTrue(sampler.treePolicyGuard(n2));
    }

    @Test
    public void expansionPolicy() {
        IEvaluationFunction<CommandQWOP, StateQWOP> evalFun1 = new EvaluationFunction_Constant<>(5f);
        Sampler_UCB<CommandQWOP, StateQWOP> sampler = new Sampler_UCB<>(evalFun1, new RolloutPolicy_JustEvaluate<>(evalFun1),
                new ValueUpdater_Average<>(),5, 1);

        Action<CommandQWOP> expansionAction = sampler.expansionPolicy(n2);
        Assert.assertTrue(n2.getUntriedActionListCopy().contains(expansionAction));

        expansionAction = sampler.expansionPolicy(n1);
        Assert.assertTrue(n1.getUntriedActionListCopy().contains(expansionAction));

        ActionList<CommandQWOP> actionList = new ActionList<>(new Distribution_Normal<>(5f, 0.001f));
        actionList.add(new Action<>(5, CommandQWOP.WO));
        actionList.add(new Action<>(1, CommandQWOP.NONE));
        actionList.add(new Action<>(10, CommandQWOP.NONE));

        ActionGenerator_FixedActions<CommandQWOP> generator = new ActionGenerator_FixedActions<>(actionList);
        NodeGameExplorableBase<?, CommandQWOP, StateQWOP> n2_1 = n2.addDoublyLinkedChild(n2.getUntriedActionRandom(),
                GameQWOP.getInitialState(), generator);

        // Based on the really narrow normal distribution, should always return this duration command.
        for (int i = 0; i < 100; i++) {
            expansionAction = sampler.expansionPolicy(n2_1);
            Assert.assertEquals(5, expansionAction.getTimestepsTotal());
        }

        exception.expect(IndexOutOfBoundsException.class);
        sampler.expansionPolicy(root);
    }

    @Test
    public void expansionPolicyActionDoneAndGuard() {
        IEvaluationFunction<CommandQWOP, StateQWOP> evalFun1 = new EvaluationFunction_Constant<>(5f);
        Sampler_UCB<CommandQWOP, StateQWOP> sampler = new Sampler_UCB<>(evalFun1, new RolloutPolicy_JustEvaluate<>(evalFun1),
                new ValueUpdater_Average<>(),5, 1);

        Assert.assertFalse(sampler.expansionPolicyGuard(n2));
        sampler.expansionPolicyActionDone(n2);
        Assert.assertTrue(sampler.expansionPolicyGuard(n2));

        Assert.assertEquals(0f, n2.getValue(), 1e-8f);

        // If the state is failed, then reporting that the expansion policy is done will also do whatever default
        // evaluation function is added to the sampler (constant 5 in this case).
        NodeGameExplorableBase<?, CommandQWOP, StateQWOP> n2_1 = n2.addDoublyLinkedChild(n2.getUntriedActionRandom(), failedState);
        sampler.expansionPolicyActionDone(n2_1);
        Assert.assertEquals(5f, n2_1.getValue(), 1e-8f);
    }

    @Test
    public void rolloutPolicyAndGuard() {
        IEvaluationFunction<CommandQWOP, StateQWOP> evalFun1 = new EvaluationFunction_Constant<>(5f);
        Sampler_UCB<CommandQWOP, StateQWOP> sampler = new Sampler_UCB<>(evalFun1, new RolloutPolicy_JustEvaluate<>(evalFun1),
                new ValueUpdater_Average<>(),5, 1);

        Assert.assertFalse(sampler.rolloutPolicyGuard(n2_2));
        Assert.assertEquals(0f, n2_2.getValue(), 1e-8f);
        Assert.assertEquals(0f, n2.getValue(), 1e-8f);
        Assert.assertEquals(0f, root.getValue(), 1e-8f);
        sampler.rolloutPolicy(n2_2, null);
        Assert.assertEquals(5f, n2_2.getValue(), 1e-8f);
        Assert.assertEquals(5f, n2.getValue(), 1e-8f);
        Assert.assertEquals(5f, root.getValue(), 1e-8f);
        Assert.assertEquals(0f, n1.getValue(), 1e-8f);
        Assert.assertTrue(sampler.rolloutPolicyGuard(n2_2));

        // Shouldn't do a rollout on a failed node. This would be the fault of whoever calls this (i.e. TreeWorker).
        exception.expect(IllegalStateException.class);
        NodeGameExplorableBase<?, CommandQWOP, StateQWOP> failedNode = n2_2.addDoublyLinkedChild(n2_2.getUntriedActionRandom(),
                failedState);
        sampler.rolloutPolicy(failedNode, null);
    }

    @Test
    public void getCopy() {
        IEvaluationFunction<CommandQWOP, StateQWOP> evalFun1 = new EvaluationFunction_Constant<>(5f);
        Sampler_UCB<CommandQWOP, StateQWOP> sampler = new Sampler_UCB<>(evalFun1, new RolloutPolicy_JustEvaluate<>(evalFun1),
                new ValueUpdater_Average<>(),101, 6);
        Assert.assertFalse(sampler.expansionPolicyGuard(root));
        sampler.expansionPolicyActionDone(root);
        Assert.assertTrue(sampler.expansionPolicyGuard(root));

        // Should copy this parameter.
        Sampler_UCB<CommandQWOP, StateQWOP> samplerCopy = sampler.getCopy();
        Assert.assertEquals(sampler.explorationConstant, samplerCopy.explorationConstant, 1e-8f);

        // Should NOT copy the current status of the sampler.
        Assert.assertFalse(samplerCopy.expansionPolicyGuard(root));
    }
}
