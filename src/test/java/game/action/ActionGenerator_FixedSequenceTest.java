package game.action;

import distributions.Distribution_Equal;
import game.qwop.CommandQWOP;
import game.qwop.StateQWOP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tree.node.NodeGameExplorable;

import java.util.*;

import static org.mockito.Mockito.mock;

public class ActionGenerator_FixedSequenceTest {

    private ActionList<CommandQWOP> alist1, alist2, alist3, alist4;
    private List<ActionList<CommandQWOP>> allActionLists;

    @SuppressWarnings("FieldCanBeLocal")
    private Set<Action<CommandQWOP>> aset1, aset2, aset3, aset4;

    private StateQWOP st = mock(StateQWOP.class);

    @SuppressWarnings("unchecked")
    private Action<CommandQWOP> act = mock(Action.class);

    private Action<CommandQWOP> actionException = new Action<>(100, CommandQWOP.O);

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    @Before
    public void setup() {
        Action<CommandQWOP> a1 = new Action<>(23, CommandQWOP.WO);
        Action<CommandQWOP> a2 = new Action<>(24, CommandQWOP.NONE);
        Action<CommandQWOP> a3 = new Action<>(2, CommandQWOP.WP);
        Action<CommandQWOP> a4 = new Action<>(3, CommandQWOP.WP);
        Action<CommandQWOP> a5 = new Action<>(8, CommandQWOP.P);
        aset1 = new HashSet<>();
        aset1.add(a1);
        aset1.add(a2);

        aset2 = new HashSet<>();
        aset2.add(a3);
        aset2.add(a4);

        aset3 = new HashSet<>();
        aset3.add(a5);

        aset4 = new HashSet<>();
        aset4.add(a2);
        aset4.add(a3);
        aset4.add(a5);

        alist1 = new ActionList<>(new Distribution_Equal<>());
        alist1.addAll(aset1);

        alist2 = new ActionList<>(new Distribution_Equal<>());
        alist2.addAll(aset2);

        alist3 = new ActionList<>(new Distribution_Equal<>());
        alist3.addAll(aset3);

        alist4 = new ActionList<>(new Distribution_Equal<>());
        alist4.addAll(aset4);

        allActionLists = new ArrayList<>();
        allActionLists.add(alist1);
        allActionLists.add(alist2);
        allActionLists.add(alist3);
        allActionLists.add(alist4);
    }

    @Test
    public void factoryMethods() {
        // For now, just make sure they are free of runtime errors.
        ActionGenerator_FixedSequence.makeDefaultGenerator(-1);
        ActionGenerator_FixedSequence.makeDefaultGenerator(5);
        ActionGenerator_FixedSequence.makeDefaultGenerator(25);

        ActionGenerator_FixedSequence.makeExtendedGenerator(-1);
        ActionGenerator_FixedSequence.makeExtendedGenerator(5);
        ActionGenerator_FixedSequence.makeExtendedGenerator(25);
    }

    @Test
    public void singleLengthSequence() {
        // If only one command list is given, then the available actions will always be the same.
        int numNodes = 10;
        for (ActionList<CommandQWOP> alist : allActionLists) {
            List<ActionList<CommandQWOP>> singleActionList = new ArrayList<>();
            singleActionList.add(alist);

            ActionGenerator_FixedSequence<CommandQWOP> gen = new ActionGenerator_FixedSequence<>(singleActionList);

            NodeGameExplorable<CommandQWOP> n0 = new NodeGameExplorable<>(st, gen);
            List<NodeGameExplorable<CommandQWOP>> nodes = new ArrayList<>(numNodes);
            nodes.add(n0);
            for (int i = 1; i < numNodes; i++) {
                nodes.add(nodes.get(i - 1).addDoublyLinkedChild(act, st));
            }

            for (NodeGameExplorable<CommandQWOP> n : nodes) {
                ActionList<CommandQWOP> actionList = gen.getPotentialChildActionSet(n);
                Assert.assertEquals(alist.size(), actionList.size());
                Assert.assertTrue(actionList.containsAll(alist));
                Assert.assertEquals(alist, actionList);
            }
            Set<Action<CommandQWOP>> allPossibleActions = gen.getAllPossibleActions();
            Assert.assertEquals(alist.size(), allPossibleActions.size());
            Assert.assertTrue(allPossibleActions.containsAll(alist));
        }
    }

    @Test
    public void multiLengthSequence() {
        List<List<ActionList<CommandQWOP>>> sequences = new ArrayList<>();

        sequences.add(makeActionListList(alist1));
        sequences.add(makeActionListList(alist2)); // Include some single-length sequences too for good measure.
        sequences.add(makeActionListList(alist3, alist4));
        sequences.add(makeActionListList(alist1, alist1));
        sequences.add(makeActionListList(alist2, alist3, alist4));
        sequences.add(makeActionListList(alist4, alist4, alist1));
        sequences.add(makeActionListList(alist1, alist2, alist3, alist4));
        sequences.add(makeActionListList(alist1, alist1, alist1, alist1)); // Stupid, but valid!
        sequences.add(makeActionListList(alist1, alist3, alist4, alist2, alist3));
        sequences.add(makeActionListList(alist1, alist3, alist4, alist2, alist3, alist1, alist4, alist3, alist2,
                alist3, alist1));

        for (List<ActionList<CommandQWOP>> sequence : sequences) {
            for (int numNodes = 1; numNodes < 20; numNodes++) { // Do for varying depth number of nodes.
                String failMsg = "Sequence length: " + sequence.size() + " Number of nodes: " + numNodes;
                ActionList<CommandQWOP> allActionsInSequence = new ActionList<>(new Distribution_Equal<>());
                for (ActionList<CommandQWOP> a : sequence) {
                    allActionsInSequence.addAll(a);
                }
                ActionGenerator_FixedSequence<CommandQWOP> gen = new ActionGenerator_FixedSequence<>(sequence);

                NodeGameExplorable<CommandQWOP> n0 = new NodeGameExplorable<>(st, gen);
                List<NodeGameExplorable<CommandQWOP>> nodes = new ArrayList<>(numNodes);
                nodes.add(n0);
                for (int i = 1; i < numNodes; i++) {
                    nodes.add(nodes.get(i - 1).addDoublyLinkedChild(act, st));
                }

                for (int i = 0; i < numNodes; i++) {
                    ActionList<CommandQWOP> expectedActionList = sequence.get(i % sequence.size());
                    ActionList<CommandQWOP> actualActionList = gen.getPotentialChildActionSet(nodes.get(i));
                    Assert.assertEquals(failMsg, expectedActionList.size(), actualActionList.size());
                    Assert.assertTrue(failMsg, expectedActionList.containsAll(actualActionList));
                    Assert.assertEquals(failMsg, expectedActionList, actualActionList);
                }

                Set<Action<CommandQWOP>> allPossibleActions = gen.getAllPossibleActions();
                Assert.assertEquals(failMsg, allActionsInSequence.size(), allPossibleActions.size());
                Assert.assertTrue(failMsg, allActionsInSequence.containsAll(allPossibleActions));
            }
        }
    }

    @Test
    public void multiLengthActionExceptions() {

        Map<Integer, ActionList<CommandQWOP>> actionExceptions = new HashMap<>();
        ActionList<CommandQWOP> actionListException = new ActionList<>(new Distribution_Equal<>());
        actionListException.add(actionException);
        actionExceptions.put(3, actionListException);
        actionExceptions.put(5, actionListException);


        List<List<ActionList<CommandQWOP>>> sequences = new ArrayList<>();

        sequences.add(makeActionListList(alist1));
        sequences.add(makeActionListList(alist2)); // Include some single-length sequences too for good measure.
        sequences.add(makeActionListList(alist3, alist4));
        sequences.add(makeActionListList(alist1, alist1));
        sequences.add(makeActionListList(alist2, alist3, alist4));
        sequences.add(makeActionListList(alist4, alist4, alist1));
        sequences.add(makeActionListList(alist1, alist2, alist3, alist4));
        sequences.add(makeActionListList(alist1, alist1, alist1, alist1)); // Stupid, but valid!
        sequences.add(makeActionListList(alist1, alist3, alist4, alist2, alist3));
        sequences.add(makeActionListList(alist1, alist3, alist4, alist2, alist3, alist1, alist4, alist3, alist2,
                alist3, alist1));

        for (List<ActionList<CommandQWOP>> sequence : sequences) {
            for (int numNodes = 1; numNodes < 20; numNodes++) { // Do for varying depth number of nodes.
                String failMsg = "Sequence length: " + sequence.size() + " Number of nodes: " + numNodes;
                ActionList<CommandQWOP> allActionsInSequence = new ActionList<>(new Distribution_Equal<>());
                for (ActionList<CommandQWOP> a : sequence) {
                    allActionsInSequence.addAll(a);
                }
                ActionGenerator_FixedSequence<CommandQWOP> gen = new ActionGenerator_FixedSequence<>(sequence,
                        actionExceptions);

                NodeGameExplorable<CommandQWOP> n0 = new NodeGameExplorable<>(st, gen);
                List<NodeGameExplorable<CommandQWOP>> nodes = new ArrayList<>(numNodes);
                nodes.add(n0);
                for (int i = 1; i < numNodes; i++) {
                    nodes.add(nodes.get(i - 1).addDoublyLinkedChild(act, st));
                }
                for (int i = 0; i < numNodes; i++) {
                    if (i == 3 || i == 5) {
                        ActionList actualActionList = gen.getPotentialChildActionSet(nodes.get(i));
                        Assert.assertEquals(1, actualActionList.size());
                        Assert.assertEquals(actionException, actualActionList.get(0));
                    } else {
                        ActionList<CommandQWOP> expectedActionList = sequence.get(i % sequence.size());
                        ActionList<CommandQWOP> actualActionList = gen.getPotentialChildActionSet(nodes.get(i));
                        Assert.assertEquals(failMsg, expectedActionList.size(), actualActionList.size());
                        Assert.assertTrue(failMsg, expectedActionList.containsAll(actualActionList));
                        Assert.assertEquals(failMsg, expectedActionList, actualActionList);
                    }
                }

                Set<Action<CommandQWOP>> allPossibleActions = gen.getAllPossibleActions();
                Assert.assertEquals(failMsg, allActionsInSequence.size() + 1, allPossibleActions.size()); // +1 for
                // command exception.
                ActionList<CommandQWOP> alistCopy = allActionsInSequence.getCopy();
                alistCopy.add(actionException);
                Assert.assertTrue(failMsg, alistCopy.containsAll(allPossibleActions));
            }
        }
    }

    @Test
    public void badConstructor() {
        exception.expect(IllegalArgumentException.class);
        new ActionGenerator_FixedSequence<>(new ArrayList<>());
    }

    // Just for convenience.
    @SafeVarargs
    private static List<ActionList<CommandQWOP>> makeActionListList(ActionList<CommandQWOP>... individualLists) {
        List<ActionList<CommandQWOP>> l = new ArrayList<>();
        Collections.addAll(l, individualLists);
        return l;
    }
}