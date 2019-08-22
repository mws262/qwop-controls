package tree.sampler.rollout;

import controllers.Controller_Random;
import game.GameUnified;
import game.action.Action;
import game.action.ActionGenerator_FixedSequence;
import game.action.CommandQWOP;
import game.action.IActionGenerator;
import game.state.IState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tree.node.NodeQWOP;
import tree.node.NodeQWOPExplorable;
import tree.node.evaluator.EvaluationFunction_Distance;
import tree.node.evaluator.IEvaluationFunction;

import java.util.Set;

// Also includes most of the testing for abstract RolloutPolicyBase class.
public class RolloutPolicy_DeltaScoreTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    private EvaluationFunction_Distance evaluator = new EvaluationFunction_Distance();
    private RolloutPolicyBase rollout = new RolloutPolicy_DeltaScore(evaluator, new Controller_Random());

    private Action a1 = new Action(10, CommandQWOP.WO);
    private Action a2 = new Action(5, CommandQWOP.QP);
    private Action a3 = new Action(15, CommandQWOP.NONE);
    private Action a4 = new Action(3, CommandQWOP.Q);
    private Action a5 = new Action(12, CommandQWOP.W);

    private IState s1, s1_2, s1_2_3;
    private IState s1_2_4;
    private IState s2, s2_3, s2_3_5;

    private NodeQWOPExplorable root;
    private NodeQWOPExplorable n1, n1_2, n1_2_3;
    private NodeQWOPExplorable n1_2_4;
    private NodeQWOPExplorable n2, n2_3, n2_3_5;

    private IActionGenerator rolloutActionGenerator = RolloutPolicyBase.getRolloutActionGenerator();
    private Set<Action> possibleRolloutActions;
    {
        possibleRolloutActions = rolloutActionGenerator.getAllPossibleActions();
    }

    @Before
    public void setup() {
        // Set up a demo tree.
        GameUnified game = new GameUnified();
        root = new NodeQWOPExplorable(GameUnified.getInitialState(),
                ActionGenerator_FixedSequence.makeDefaultGenerator(-1));

        // First branch.
        game.doAction(a1);
        s1 = game.getCurrentState();
        n1 = root.addDoublyLinkedChild(a1, s1);

        game.doAction(a2);
        s1_2 = game.getCurrentState();
        n1_2 = n1.addDoublyLinkedChild(a2, s1_2);

        game.doAction(a3);
        s1_2_3 = game.getCurrentState();
        n1_2_3 = n1_2.addDoublyLinkedChild(a3, s1_2_3);

        // Fork off first branch.
        game.makeNewWorld();
        game.doAction(a1);
        game.doAction(a2);
        game.doAction(a4);
        s1_2_4 = game.getCurrentState();
        n1_2_4 = n1_2.addBackwardsLinkedChild(a4, s1_2_4);

        // Another branch.
        game.makeNewWorld();
        game.doAction(a2);
        s2 = game.getCurrentState();
        n2 = root.addDoublyLinkedChild(a2, s2);
        game.doAction(a3);
        s2_3 = game.getCurrentState();
        n2_3 = n2.addDoublyLinkedChild(a3, s2_3);
        game.doAction(a5);
        s2_3_5 = game.getCurrentState();
        n2_3_5 = n2_3.addDoublyLinkedChild(a5, s2_3_5);
    }

    @Test
    public void simGameToNode() {
        GameUnified game = new GameUnified();

        rollout.simGameToNode(n1_2_4, game);
        IState st1 = game.getCurrentState();

        game.step(false, true, true, false);
        rollout.simGameToNode(n1_2_4, game);
        IState st2 = game.getCurrentState();

        Assert.assertEquals(n1_2_4.getState(), st1);
        Assert.assertEquals(n1_2_4.getState(), st2);
    }

    @Test
    public void coldStartGameToNode() {
        GameUnified game = new GameUnified();
        game.step(true, false, false, true);
        IState st1 = game.getCurrentState();

        game.makeNewWorld();
        rollout.coldStartGameToNode(new NodeQWOP(st1), game);
        IState st2 = game.getCurrentState();

        Assert.assertEquals(st1, st2);
    }

    @Test
    public void randomRollout() {
//        NodeQWOPExplorableBase<?> startNode = n1_2_4;
//        Assert.assertFalse(n1_2_4.isFullyExplored());
//        Assert.assertFalse(n1_2_4.getState().isFailed());
//        int startNodeChildOptions = startNode.getUntriedActionCount();
//        GameUnified game = new GameUnified();
//
//        // For no maximum of timesteps. Always goes until failure.
//        Set<NodeQWOPExplorableBase<?>> rolloutResults = new HashSet<>();
//        for (int i = 0; i < 10; i++) {
//            rollout.simGameToNode(startNode, game);
//            rolloutResults.add(rollout.rollout(startNode, game));
//        }
//
//        for (NodeQWOPExplorableBase<?> node : rolloutResults) {
//            // Make sure that all action used in the rollout are ones that the ActionGenerator can generate.
//            Set<Action> rolloutActions = new HashSet<>();
//            node.recurseUpTreeInclusive(n -> {
//                if (n.getTreeDepth() > startNode.getTreeDepth()) {
//                    rolloutActions.add(n.getAction());
//                }
//            });
//
//            Assert.assertTrue(possibleRolloutActions.containsAll(rolloutActions));
//            Assert.assertTrue(((NodeQWOPExplorable) node).isOtherNodeAncestor(n1_2_4));
//            Assert.assertTrue(node.isFullyExplored());
//            Assert.assertTrue(node.getState().isFailed());
//
//            // Rollout should not affect the normal tree building and adding of children.
//            Assert.assertEquals(startNodeChildOptions, startNode.getUntriedActionCount());
//        }
//
//        for (NodeQWOPExplorableBase<?> node : rolloutResults) {
//            // Make sure that all action used in the rollout are ones that the ActionGenerator can generate.
//            Set<Action> rolloutActions = new HashSet<>();
//            node.recurseUpTreeInclusive(n -> {
//                if (n.getTreeDepth() > startNode.getTreeDepth()) {
//                    rolloutActions.add(n.getAction());
//                }
//            });
//
//            Assert.assertTrue(possibleRolloutActions.containsAll(rolloutActions));
//            Assert.assertTrue(((NodeQWOPExplorable) node).isOtherNodeAncestor(n1_2_4));
//
//            Assert.assertFalse(node.getState().isFailed());
//            Assert.assertFalse(node.isFullyExplored());
//
//            // Rollout should not affect the normal tree building and adding of children.
//            Assert.assertEquals(startNodeChildOptions, startNode.getUntriedActionCount());
//        }
    }

    @Test
    public void getRolloutActionGenerator() {
        IActionGenerator gen = RolloutPolicyBase.getRolloutActionGenerator();
        Assert.assertTrue(!gen.getAllPossibleActions().isEmpty());
    }

    @Test
    public void rollout() {

        IEvaluationFunction evalFun = new EvaluationFunction_Distance();

        for (int ts = 5; ts < 400; ts += 50) {
            RolloutPolicy_DeltaScore rollout = new RolloutPolicy_DeltaScore(evalFun, new Controller_Random(), ts);
            GameUnified game = new GameUnified();
            NodeQWOPExplorable startNode = n1_2_3;
            int startIterations = game.iterations;
            rollout.simGameToNode(startNode, game);
            float rolloutScore = rollout.rollout(startNode, game);

            Assert.assertTrue((game.iterations - startIterations) <= rollout.maxTimesteps);
            IState finalState = game.getCurrentState();
            float expectedScore = (evalFun.getValue(new NodeQWOP(finalState)) - evalFun.getValue(startNode))
                    * (finalState.isFailed() ? rollout.failureMultiplier : 1.0f);

            Assert.assertEquals(expectedScore, rolloutScore, 1e-12f);
        }
    }

    @Test
    public void getCopy() {
        IEvaluationFunction evalFun = new EvaluationFunction_Distance();
        RolloutPolicy_DeltaScore rollout = new RolloutPolicy_DeltaScore(evalFun, new Controller_Random());
        RolloutPolicy_DeltaScore copy = rollout.getCopy();

        Assert.assertEquals(rollout.getEvaluationFunction().getValue(n1_2_3), copy.getEvaluationFunction().getValue(n1_2_3),
                1e-12f);
        Assert.assertEquals(rollout.maxTimesteps, copy.maxTimesteps);
        Assert.assertEquals(rollout.failureMultiplier, copy.failureMultiplier, 1e-12f);
    }
}