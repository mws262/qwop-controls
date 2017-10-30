/**
 * 
 */
package test;
import main.Action;
import main.ActionGenerator_FixedSequence;
import main.ActionSet;
import main.Distribution;
import main.Distribution_Normal;
import main.Distribution_Uniform;
import main.IActionGenerator;
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
		/***** Space of allowed actions to sample ******/
		Distribution<Action> uniform_dist = new Distribution_Uniform();
		
		/********** Repeated action 1 -- no keys pressed. ***********/
		Integer[] durations1 = new Integer[]{5,6,7,8,9,10,11,12,13,14,15};
		boolean[][] keySet1 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durations1.length);
		
		//Distribution<Action> dist1 = new Distribution_Uniform();
		Distribution<Action> dist1 = new Distribution_Normal(10f,1f);
		ActionSet actionSet1 = ActionSet.makeActionSet(durations1, keySet1, dist1);
		
		/**********  Repeated action 2 -- W-O pressed ***********/
		Integer[] durations2 = new Integer[]{30,31,32,33,34,35,36,37,38,39,40};
		boolean[][] keySet2 = ActionSet.replicateKeyString(new boolean[]{false,true,true,false},durations2.length);
		
//		Distribution<Action> dist2 = new Distribution_Uniform();
		Distribution<Action> dist2 = new Distribution_Normal(35f,1f);
		ActionSet actionSet2 = ActionSet.makeActionSet(durations2, keySet2, dist2);
		
		/**********  Repeated action 3 -- W-O pressed ***********/
		Integer[] durations3 = new Integer[]{5,6,7,8,9,10,11,12,13,14,15};
		boolean[][] keySet3 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durations3.length);
		
//		Distribution<Action> dist3 = new Distribution_Uniform();
		Distribution<Action> dist3 = new Distribution_Normal(8f,1f);
		ActionSet actionSet3 = ActionSet.makeActionSet(durations3, keySet3, dist3);
		
		/**********  Repeated action 4 -- Q-P pressed ***********/
		Integer[] durations4 = new Integer[]{30,31,32,33,34,35,36,37,38,39,40};
		boolean[][] keySet4 = ActionSet.replicateKeyString(new boolean[]{true,false,false,true},durations4.length);
		
//		Distribution<Action> dist4 = new Distribution_Uniform();
		Distribution<Action> dist4 = new Distribution_Normal(35f,1f);
		ActionSet actionSet4 = ActionSet.makeActionSet(durations4, keySet4, dist4);
								
		ActionSet[] repeatedActions = new ActionSet[] {actionSet1,actionSet2,actionSet3,actionSet4};
		
		
		/////// Action Exceptions ////////
		/********** Repeated action exceptions 1 -- no keys pressed. ***********/
		Integer[] durationsE1 = new Integer[]{4,5,6};
		boolean[][] keySetE1 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durationsE1.length);
		
		//Distribution<Action> distE1 = new Distribution_Uniform();
		Distribution<Action> distE1 = new Distribution_Normal(5f,0.4f);
		ActionSet actionSetE1 = ActionSet.makeActionSet(durationsE1, keySetE1, distE1);
		
		/**********  Repeated action exceptions 2 -- W-O pressed ***********/
		Integer[] durationsE2 = new Integer[]{31,32,33,34,35,36};
		boolean[][] keySetE2 = ActionSet.replicateKeyString(new boolean[]{false,true,true,false},durationsE2.length);
		
		Distribution<Action> distE2 = new Distribution_Uniform();
		ActionSet actionSetE2 = ActionSet.makeActionSet(durationsE2, keySetE2, distE2);
		
		/**********  Repeated action exceptions 3 -- W-O pressed ***********/
		Integer[] durationsE3 = new Integer[]{21,22,23,24,25};
		boolean[][] keySetE3 = ActionSet.replicateKeyString(new boolean[]{false,false,false,false},durationsE3.length);
		
		Distribution<Action> distE3 = new Distribution_Uniform();
		ActionSet actionSetE3 = ActionSet.makeActionSet(durationsE3, keySetE3, distE3);
		
		/**********  Repeated action exceptions 4 -- Q-P pressed ***********/
		Integer[] durationsE4 = new Integer[]{45,46,47,48,49,50};
		boolean[][] keySetE4 = ActionSet.replicateKeyString(new boolean[]{true,false,false,true},durationsE4.length);
		
		Distribution<Action> distE4 = new Distribution_Uniform();
		ActionSet actionSetE4 = ActionSet.makeActionSet(durationsE4, keySetE4, distE4);
								
		Map<Integer,ActionSet> actionExceptions = new HashMap<Integer,ActionSet>();
//		actionExceptions.put(0, actionSetE1);
//		actionExceptions.put(1, actionSetE2);
		actionExceptions.put(2, actionSetE3);
//		actionExceptions.put(3, actionSetE4);
		

		// Define the specific way that these allowed actions are assigned as potential options for nodes.
		IActionGenerator actionGenerator = new ActionGenerator_FixedSequence(repeatedActions, actionExceptions);
		Node fakeNodeRoot = new Node(false);
		Node fakeNode1 = new Node(fakeNodeRoot,new Action(1,false,false,false,false));
		Node fakeNode2 = new Node(fakeNode1,new Action(1,false,false,false,false));
		
		ActionSet rootActions = actionGenerator.getPotentialChildActionSet(fakeNodeRoot);
		ActionSet actions1 = actionGenerator.getPotentialChildActionSet(fakeNode1);
		ActionSet actions2 = actionGenerator.getPotentialChildActionSet(fakeNode2);
		
		assertEquals("Check actions from root node: ", 5, rootActions.get(0).getTimestepsTotal());
		assertEquals("Check actions from another node: ", 31, actions1.get(1).getTimestepsTotal());
		assertEquals("Check actions from an exception node: ", 23, actions2.get(2).getTimestepsTotal());
		assertFalse("Check that the keypresses line up for root: ",rootActions.get(0).peek()[1]);
		assertTrue("Check that the keypresses line up for another node: ",actions1.get(0).peek()[1]);

		
		// Test a uniform distribution.
		double totalRuns = 1e6;
		int count = 0;
		for (int i = 0; i < totalRuns; i++) {
			if (actionSetE4.sampleDistribution().getTimestepsTotal() == 45) {
				count++;
			}
		}
		double probability = count/totalRuns;
		
		assertTrue("Check that the uniform distribution is working about right.",Math.abs(probability - 1./durationsE4.length) < 0.001);
		
		
		// Test the normal distribution. See if the mean is about right.

		double sum = 0;
		for (int i = 0; i < totalRuns; i++) {
			sum += actionSet1.sampleDistribution().getTimestepsTotal();
		}
		double average = sum/totalRuns;
		System.out.println(average);
		Distribution_Normal d = (Distribution_Normal)(dist1);
		assertTrue("Check that the uniform distribution is working about right.", Math.abs(d.mean - average) < 0.01);
		
		
		
	}
}
