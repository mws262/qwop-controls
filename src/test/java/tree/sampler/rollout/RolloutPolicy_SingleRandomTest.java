package tree.sampler.rollout;

import game.GameUnified;
import game.actions.Action;
import game.actions.ActionGenerator_FixedSequence;
import game.actions.IActionGenerator;
import game.state.IState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tree.node.NodeQWOPExplorable;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.EvaluationFunction_Distance;

import java.util.HashSet;
import java.util.Set;

public class RolloutPolicy_SingleRandomTest {

    private EvaluationFunction_Distance evaluator = new EvaluationFunction_Distance();
    private RolloutPolicy_SingleRandom rollout = new RolloutPolicy_SingleRandom(evaluator);

    private Action a1 = new Action(10, false, true, true, false);
    private Action a2 = new Action(5, true, false, false, true);
    private Action a3 = new Action(15, false, false, false, false);
    private Action a4 = new Action(3, true, false, false, false);
    private Action a5 = new Action(12, false, true, false, false);

    private IState s1, s1_2, s1_2_3;
    private IState s1_2_4;
    private IState s2, s2_3, s2_3_5;

    private NodeQWOPExplorable root;
    private NodeQWOPExplorable n1, n1_2, n1_2_3;
    private NodeQWOPExplorable n1_2_4;
    private NodeQWOPExplorable n2, n2_3, n2_3_5;

    private IActionGenerator rolloutActionGenerator = RolloutPolicy.getRolloutActionGenerator();
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
    }

    @Test
    public void coldStartGameToNode() {
    }

    @Test
    public void randomRollout() {
        NodeQWOPExplorableBase<?> startNode = n1_2_4;
        GameUnified game = new GameUnified();
        rollout.simGameToNode(startNode, game);
        NodeQWOPExplorableBase<?> rolloutResult = rollout.randomRollout(startNode, game);

        // Make sure that all actions used in the rollout are ones that the ActionGenerator can generate.
        Set<Action> rolloutActions = new HashSet<>();
        rolloutResult.recurseUpTreeInclusive(n -> {
            if (n.getTreeDepth() > startNode.getTreeDepth()) {
                rolloutActions.add(n.getAction());
            }
        });

        // TODO there are actions in rolloutActions which are not in possible. Need to debug this. currently on a
        //  side-track testing the action generators as a result.
        Assert.assertTrue(possibleRolloutActions.containsAll(rolloutActions));

        Assert.assertTrue(((NodeQWOPExplorable)rolloutResult).isOtherNodeAncestor(n1_2_4));
    }

    @Test
    public void randomRollout1() {
    }

    @Test
    public void getRolloutActionGenerator() {
    }

    @Test
    public void rollout() {
    }

    @Test
    public void getCopy() {
    }
}