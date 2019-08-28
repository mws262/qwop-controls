package game.action;

import distributions.Distribution_Equal;
import game.qwop.CommandQWOP;
import game.qwop.StateQWOP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tree.node.NodeQWOPExplorable;

import java.util.*;

import static org.mockito.Mockito.mock;

public class ActionGenerator_FixedSequenceTest {

    private ActionList alist1, alist2, alist3, alist4;
    private ActionList[] allActionLists;
    private Set<Action> aset1, aset2, aset3, aset4;

    private StateQWOP st = mock(StateQWOP.class);
    private Action act = mock(Action.class);

    private Action actionException = new Action(100, CommandQWOP.O);

    @Rule
    public final ExpectedException exception = ExpectedException.none(); // For asserting that exceptions should occur.

    @Before
    public void setup() {
        Action a1 = new Action(23, CommandQWOP.WO);
        Action a2 = new Action(24, CommandQWOP.NONE);
        Action a3 = new Action(2, CommandQWOP.WP);
        Action a4 = new Action(3, CommandQWOP.WP);
        Action a5 = new Action(8, CommandQWOP.P);
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

        alist1 = new ActionList(new Distribution_Equal());
        alist1.addAll(aset1);

        alist2 = new ActionList(new Distribution_Equal());
        alist2.addAll(aset2);

        alist3 = new ActionList(new Distribution_Equal());
        alist3.addAll(aset3);

        alist4 = new ActionList(new Distribution_Equal());
        alist4.addAll(aset4);

        allActionLists = new ActionList[] {alist1, alist2, alist3, alist4};
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
        for (ActionList alist : allActionLists) {
            ActionGenerator_FixedSequence gen = new ActionGenerator_FixedSequence(new ActionList[]{alist});

            NodeQWOPExplorable n0 = new NodeQWOPExplorable(st, gen);
            NodeQWOPExplorable[] nodes = new NodeQWOPExplorable[numNodes];
            nodes[0] = n0;
            for (int i = 1; i < numNodes; i++) {
                nodes[i] = nodes[i - 1].addDoublyLinkedChild(act, st);
            }
            for (NodeQWOPExplorable n : nodes) {
                ActionList actionList = gen.getPotentialChildActionSet(n);
                Assert.assertEquals(alist.size(), actionList.size());
                Assert.assertTrue(actionList.containsAll(alist));
                Assert.assertEquals(alist, actionList);
            }
            Set<Action> allPossibleActions = gen.getAllPossibleActions();
            Assert.assertEquals(alist.size(), allPossibleActions.size());
            Assert.assertTrue(allPossibleActions.containsAll(alist));
        }
    }

    @Test
    public void multiLengthSequence() {
        List<ActionList[]> sequences = new ArrayList<>();

        sequences.add(new ActionList[] {alist1});
        sequences.add(new ActionList[] {alist2}); // Include some single-length sequences too for good measure.
        sequences.add(new ActionList[] {alist3, alist4});
        sequences.add(new ActionList[] {alist1, alist1});
        sequences.add(new ActionList[] {alist2, alist3, alist4});
        sequences.add(new ActionList[] {alist4, alist4, alist1});
        sequences.add(new ActionList[] {alist1, alist2, alist3, alist4});
        sequences.add(new ActionList[] {alist1, alist1, alist1, alist1}); // Stupid, but valid!
        sequences.add(new ActionList[] {alist1, alist3, alist4, alist2, alist3});
        sequences.add(new ActionList[] {alist1, alist3, alist4, alist2, alist3, alist1, alist4, alist3, alist2,
                alist3, alist1});

        for (ActionList[] sequence : sequences) {
            for (int numNodes = 1; numNodes < 20; numNodes++) { // Do for varying depth number of nodes.
                String failMsg = "Sequence length: " + sequence.length + " Number of nodes: " + numNodes;
                ActionList allActionsInSequence = new ActionList(new Distribution_Equal());
                for (ActionList a : sequence) {
                    allActionsInSequence.addAll(a);
                }
                ActionGenerator_FixedSequence gen = new ActionGenerator_FixedSequence(sequence);

                NodeQWOPExplorable n0 = new NodeQWOPExplorable(st, gen);
                NodeQWOPExplorable[] nodes = new NodeQWOPExplorable[numNodes];
                nodes[0] = n0;
                for (int i = 1; i < numNodes; i++) {
                    nodes[i] = nodes[i - 1].addDoublyLinkedChild(act, st);
                }

                for (int i = 0; i < numNodes; i++) {
                    ActionList expectedActionList = sequence[i % sequence.length];
                    ActionList actualActionList = gen.getPotentialChildActionSet(nodes[i]);
                    Assert.assertEquals(failMsg, expectedActionList.size(), actualActionList.size());
                    Assert.assertTrue(failMsg, expectedActionList.containsAll(actualActionList));
                    Assert.assertEquals(failMsg, expectedActionList, actualActionList);
                }

                Set<Action> allPossibleActions = gen.getAllPossibleActions();
                Assert.assertEquals(failMsg, allActionsInSequence.size(), allPossibleActions.size());
                Assert.assertTrue(failMsg, allActionsInSequence.containsAll(allPossibleActions));
            }
        }
    }

    @Test
    public void multiLengthActionExceptions() {

        Map<Integer, ActionList> actionExceptions = new HashMap<>();
        ActionList actionListException = new ActionList(new Distribution_Equal());
        actionListException.add(actionException);
        actionExceptions.put(3, actionListException);
        actionExceptions.put(5, actionListException);


        List<ActionList[]> sequences = new ArrayList<>();

        sequences.add(new ActionList[] {alist1});
        sequences.add(new ActionList[] {alist2}); // Include some single-length sequences too for good measure.
        sequences.add(new ActionList[] {alist3, alist4});
        sequences.add(new ActionList[] {alist1, alist1});
        sequences.add(new ActionList[] {alist2, alist3, alist4});
        sequences.add(new ActionList[] {alist4, alist4, alist1});
        sequences.add(new ActionList[] {alist1, alist2, alist3, alist4});
        sequences.add(new ActionList[] {alist1, alist1, alist1, alist1}); // Stupid, but valid!
        sequences.add(new ActionList[] {alist1, alist3, alist4, alist2, alist3});
        sequences.add(new ActionList[] {alist1, alist3, alist4, alist2, alist3, alist1, alist4, alist3, alist2,
                alist3, alist1});

        for (ActionList[] sequence : sequences) {
            for (int numNodes = 1; numNodes < 20; numNodes++) { // Do for varying depth number of nodes.
                String failMsg = "Sequence length: " + sequence.length + " Number of nodes: " + numNodes;
                ActionList allActionsInSequence = new ActionList(new Distribution_Equal());
                for (ActionList a : sequence) {
                    allActionsInSequence.addAll(a);
                }
                ActionGenerator_FixedSequence gen = new ActionGenerator_FixedSequence(sequence, actionExceptions);

                NodeQWOPExplorable n0 = new NodeQWOPExplorable(st, gen);
                NodeQWOPExplorable[] nodes = new NodeQWOPExplorable[numNodes];
                nodes[0] = n0;
                for (int i = 1; i < numNodes; i++) {
                    nodes[i] = nodes[i - 1].addDoublyLinkedChild(act, st);
                }

                for (int i = 0; i < numNodes; i++) {
                    if (i == 3 || i == 5) {
                        ActionList actualActionList = gen.getPotentialChildActionSet(nodes[i]);
                        Assert.assertEquals(1, actualActionList.size());
                        Assert.assertEquals(actionException, actualActionList.get(0));
                    } else {
                        ActionList expectedActionList = sequence[i % sequence.length];
                        ActionList actualActionList = gen.getPotentialChildActionSet(nodes[i]);
                        Assert.assertEquals(failMsg, expectedActionList.size(), actualActionList.size());
                        Assert.assertTrue(failMsg, expectedActionList.containsAll(actualActionList));
                        Assert.assertEquals(failMsg, expectedActionList, actualActionList);
                    }
                }

                Set<Action> allPossibleActions = gen.getAllPossibleActions();
                Assert.assertEquals(failMsg, allActionsInSequence.size() + 1, allPossibleActions.size()); // +1 for
                // command exception.
                ActionList alistCopy = allActionsInSequence.getCopy();
                alistCopy.add(actionException);
                Assert.assertTrue(failMsg, alistCopy.containsAll(allPossibleActions));
            }
        }
    }

    @Test
    public void badConstructor() {
        exception.expect(IllegalArgumentException.class);
        ActionGenerator_FixedSequence gen = new ActionGenerator_FixedSequence(new ActionList[]{});
    }
}