package tree.sampler;

import distributions.Distribution_Equal;
import game.GameUnified;
import game.action.Action;
import game.action.ActionGenerator_FixedSequence;
import game.action.ActionList;
import game.action.ActionQueue;
import game.state.IState;
import game.state.State;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tree.node.NodeQWOPExplorable;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.EvaluationFunction_Constant;
import tree.node.evaluator.IEvaluationFunction;
import tree.sampler.rollout.RolloutPolicy_JustEvaluate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Sampler_UCBTest {

    private Action a1, a1_1, a1_1_1, a2, a2_2;
    private IState s1, s1_1, s1_1_1, s2, s2_2;
    private NodeQWOPExplorableBase<?> root, n1, n1_1, n1_1_1, n2, n2_2;

    @Before
    public void setup() {
        /*
        root -> node1 -> node1_1 -> node1_1_1
                        __
                node2 -> __
                        node2_2
         */
        GameUnified game = new GameUnified();
        ActionQueue actionQueue = new ActionQueue();

        ActionList alist1 = new ActionList(new Distribution_Equal());
        alist1.add(new Action(1, false, true, true, false));
        alist1.add(new Action(2, false, false, false, false));
        ActionList alist2 = new ActionList(new Distribution_Equal());
        alist2.add(new Action(3, true, true, true, false));
        alist2.add(new Action(4, true, false, false, false));
        alist2.add(new Action(5, false, false, false, true));

        ActionGenerator_FixedSequence generator = new ActionGenerator_FixedSequence(new ActionList[] {alist1, alist2});

        root = new NodeQWOPExplorable(GameUnified.getInitialState(), generator);

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
        game.makeNewWorld();
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

    @Test
    public void setEvaluationFunction() {
        IEvaluationFunction evalFun1 = new EvaluationFunction_Constant(5f);
        Sampler_UCB sampler = new Sampler_UCB(evalFun1, new RolloutPolicy_JustEvaluate(evalFun1));

        sampler.rolloutPolicy(n1_1_1, null);
        Assert.assertEquals(5f, n1_1_1.getValue(), 1e-12f);

        sampler.setEvaluationFunction(new EvaluationFunction_Constant(3f));
        sampler.rolloutPolicy(n2_2, null);
        Assert.assertEquals(5f, n2_2.getValue(), 1e-12f); // Changing the evaluation function shouldn't change the
        // results when the rollout is doing the evaluating.

        IState failedState = mock(State.class);
        when(failedState.isFailed()).thenReturn(true);

        NodeQWOPExplorableBase<?> badNode = n1_1_1.addDoublyLinkedChild(new Action(1010101, false, false, false,
                false), failedState);

        sampler.expansionPolicyActionDone(badNode);
        Assert.assertEquals(3f, badNode.getValue(), 1e-12); // When an expanded node is failed, then the rollout is
        // never called, and the sampler's default evaluation function is used.
    }

    @Test
    public void treePolicy() {
    }

    @Test
    public void treePolicyActionDone() {
    }

    @Test
    public void treePolicyGuard() {
    }

    @Test
    public void expansionPolicy() {
    }

    @Test
    public void expansionPolicyActionDone() {
    }

    @Test
    public void expansionPolicyGuard() {
    }

    @Test
    public void rolloutPolicy() {
    }

    @Test
    public void rolloutPolicyGuard() {
    }

    @Test
    public void getCopy() {
    }
}