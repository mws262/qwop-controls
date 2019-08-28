package tree.sampler.rollout;

import controllers.Controller_Random;
import game.qwop.GameQWOP;
import game.action.Action;
import game.action.ActionGenerator_FixedSequence;
import game.qwop.CommandQWOP;
import game.action.IActionGenerator;
import game.state.IState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tree.node.NodeGame;
import tree.node.NodeGameExplorable;
import tree.node.evaluator.EvaluationFunction_Distance;
import tree.node.evaluator.IEvaluationFunction;

import java.util.Set;

// Also includes most of the testing for abstract RolloutPolicyBase class.
public class RolloutPolicy_DeltaScoreTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    private EvaluationFunction_Distance<CommandQWOP> evaluator = new EvaluationFunction_Distance<>();
    private RolloutPolicyBase<CommandQWOP> rollout = new RolloutPolicy_DeltaScore<>(evaluator,
            RolloutPolicyBase.getQWOPRolloutActionGenerator(), new Controller_Random<>());

    private Action<CommandQWOP> a1 = new Action<>(10, CommandQWOP.WO),
            a2 = new Action<>(5, CommandQWOP.QP),
            a3 = new Action<>(15, CommandQWOP.NONE),
            a4 = new Action<>(3, CommandQWOP.Q),
            a5 = new Action<>(12, CommandQWOP.W);

    private IState s1, s1_2, s1_2_3;
    private IState s1_2_4;
    private IState s2, s2_3, s2_3_5;

    private NodeGameExplorable<CommandQWOP> root;
    private NodeGameExplorable<CommandQWOP> n1, n1_2, n1_2_3;
    private NodeGameExplorable<CommandQWOP> n1_2_4;
    private NodeGameExplorable<CommandQWOP> n2, n2_3, n2_3_5;

    private IActionGenerator<CommandQWOP> rolloutActionGenerator = RolloutPolicyBase.getQWOPRolloutActionGenerator();
    private Set<Action<CommandQWOP>> possibleRolloutActions;
    {
        possibleRolloutActions = rolloutActionGenerator.getAllPossibleActions();
    }

    @Before
    public void setup() {
        // Set up a demo tree.
        GameQWOP game = new GameQWOP();
        root = new NodeGameExplorable<>(GameQWOP.getInitialState(),
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
        game.resetGame();
        game.doAction(a1);
        game.doAction(a2);
        game.doAction(a4);
        s1_2_4 = game.getCurrentState();
        n1_2_4 = n1_2.addBackwardsLinkedChild(a4, s1_2_4);

        // Another branch.
        game.resetGame();
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
        GameQWOP game = new GameQWOP();

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
        GameQWOP game = new GameQWOP();
        game.step(true, false, false, true);
        IState st1 = game.getCurrentState();

        game.resetGame();
        rollout.coldStartGameToNode(new NodeGame<>(st1), game);
        IState st2 = game.getCurrentState();

        Assert.assertEquals(st1, st2);
    }

    @Test
    public void randomRollout() {
//        NodeGameExplorableBase<?> startNode = n1_2_4;
//        Assert.assertFalse(n1_2_4.isFullyExplored());
//        Assert.assertFalse(n1_2_4.getState().isFailed());
//        int startNodeChildOptions = startNode.getUntriedActionCount();
//        GameQWOP game = new GameQWOP();
//
//        // For no maximum of timesteps. Always goes until failure.
//        Set<NodeGameExplorableBase<?>> rolloutResults = new HashSet<>();
//        for (int i = 0; i < 10; i++) {
//            rollout.simGameToNode(startNode, game);
//            rolloutResults.add(rollout.rollout(startNode, game));
//        }
//
//        for (NodeGameExplorableBase<?> node : rolloutResults) {
//            // Make sure that all command used in the rollout are ones that the ActionGenerator can generate.
//            Set<Action> rolloutActions = new HashSet<>();
//            node.recurseUpTreeInclusive(n -> {
//                if (n.getTreeDepth() > startNode.getTreeDepth()) {
//                    rolloutActions.add(n.getAction());
//                }
//            });
//
//            Assert.assertTrue(possibleRolloutActions.containsAll(rolloutActions));
//            Assert.assertTrue(((NodeGameExplorable) node).isOtherNodeAncestor(n1_2_4));
//            Assert.assertTrue(node.isFullyExplored());
//            Assert.assertTrue(node.getState().isFailed());
//
//            // Rollout should not affect the normal tree building and adding of children.
//            Assert.assertEquals(startNodeChildOptions, startNode.getUntriedActionCount());
//        }
//
//        for (NodeGameExplorableBase<?> node : rolloutResults) {
//            // Make sure that all command used in the rollout are ones that the ActionGenerator can generate.
//            Set<Action> rolloutActions = new HashSet<>();
//            node.recurseUpTreeInclusive(n -> {
//                if (n.getTreeDepth() > startNode.getTreeDepth()) {
//                    rolloutActions.add(n.getAction());
//                }
//            });
//
//            Assert.assertTrue(possibleRolloutActions.containsAll(rolloutActions));
//            Assert.assertTrue(((NodeGameExplorable) node).isOtherNodeAncestor(n1_2_4));
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
        IActionGenerator<CommandQWOP> gen = RolloutPolicyBase.getQWOPRolloutActionGenerator();
        Assert.assertTrue(!gen.getAllPossibleActions().isEmpty());
    }

    @Test
    public void rollout() {

        IEvaluationFunction<CommandQWOP> evalFun = new EvaluationFunction_Distance<>();

        for (int ts = 5; ts < 400; ts += 50) {
            RolloutPolicy_DeltaScore<CommandQWOP> rollout = new RolloutPolicy_DeltaScore<>(evalFun,
                    RolloutPolicyBase.getQWOPRolloutActionGenerator(),
                    new Controller_Random<>(), ts);
            GameQWOP game = new GameQWOP();
            NodeGameExplorable<CommandQWOP> startNode = n1_2_3;
            int startIterations = game.iterations;
            rollout.simGameToNode(startNode, game);
            float rolloutScore = rollout.rollout(startNode, game);

            Assert.assertTrue((game.iterations - startIterations) <= rollout.maxTimesteps);
            IState finalState = game.getCurrentState();
            float expectedScore = (evalFun.getValue(new NodeGame<>(finalState)) - evalFun.getValue(startNode))
                    * (finalState.isFailed() ? rollout.failureMultiplier : 1.0f);

            Assert.assertEquals(expectedScore, rolloutScore, 1e-12f);
        }
    }

    @Test
    public void getCopy() {
        IEvaluationFunction<CommandQWOP> evalFun = new EvaluationFunction_Distance<>();
        RolloutPolicy_DeltaScore<CommandQWOP> rollout = new RolloutPolicy_DeltaScore<>(evalFun,
                RolloutPolicyBase.getQWOPRolloutActionGenerator(), new Controller_Random<>());
        RolloutPolicy_DeltaScore<CommandQWOP> copy = rollout.getCopy();

        Assert.assertEquals(rollout.getEvaluationFunction().getValue(n1_2_3), copy.getEvaluationFunction().getValue(n1_2_3),
                1e-12f);
        Assert.assertEquals(rollout.maxTimesteps, copy.maxTimesteps);
        Assert.assertEquals(rollout.failureMultiplier, copy.failureMultiplier, 1e-12f);
    }
}