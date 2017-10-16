/**
 * 
 */
package test;
import main.Action;
import main.ActionGenerator_FixedSequence;
import main.Node;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * @author Matt
 *
 */
public class ActionGenerator_FixedSequence_getPotentialChildActionSet {
	/**
	 * Test method for {@link ActionGenerator_FixedSequence#ActionGenerator_FixedSequence(boolean[][], java.lang.Integer[][], java.util.Map)}.
	 * and {@link ActionGenerator_FixedSequence#getPotentialChildActionSet(Node)}.
	 */
	@Test
	public final void testActionGenerator_FixedSequence() {
		boolean[][] keySequence = new boolean[][]{{false,false,false,false}, {false,true,true,false}, {false,false,false,false},{true,false,false,true}};
		Integer[][] actionRepeats = new Integer[][]{{0,1,2,3},{20,22},{4,7,8},{10,11,12,13,15}};
		Map<Integer,Integer[]> actionExceptions = new HashMap<Integer,Integer[]>();
		actionExceptions.put(2, new Integer[]{100,110,120});
		
		
		ActionGenerator_FixedSequence generator = new ActionGenerator_FixedSequence(keySequence,actionRepeats,actionExceptions);
		
		Node fakeNodeRoot = new Node(false);
		Node fakeNode1 = new Node(fakeNodeRoot,new Action(1,false,false,false,false));
		Node fakeNode2 = new Node(fakeNode1,new Action(1,false,false,false,false));
		
		Action[] rootActions = generator.getPotentialChildActionSet(fakeNodeRoot);
		Action[] actions1 = generator.getPotentialChildActionSet(fakeNode1);
		Action[] actions2 = generator.getPotentialChildActionSet(fakeNode2);
		
		assertEquals("Check actions from root node: ", 0, rootActions[0].getTimestepsTotal());
		assertEquals("Check actions from another node: ", 22, actions1[1].getTimestepsTotal());
		assertEquals("Check actions from an exception node: ", 120, actions2[2].getTimestepsTotal());
		assertFalse("Check that the keypresses line up for root: ",rootActions[0].peek()[1]);
		assertTrue("Check that the keypresses line up for another node: ",actions1[0].peek()[1]);

	}
}
