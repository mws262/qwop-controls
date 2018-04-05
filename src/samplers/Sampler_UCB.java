package samplers;

import org.jblas.util.Random;

import main.Action;
import main.IEvaluationFunction;
import main.ISampler;
import main.Node;
import main.Utility;

/**
 * Implements upper confidence bound for trees (UCBT, UCT, UCB, depending on who you ask).
 * Key feature is that it lets the user define a value, c, that weights exploration vs exploitation.
 * @author Matt
 *
 */
public class Sampler_UCB implements ISampler {

	/** Evaluation function used to score single nodes after rollouts are done. **/
	private IEvaluationFunction evaluationFunction;
	
	/** Explore/exploit tradeoff parameter. Higher means more exploration. Lower means more exploitation. **/
	float c = 12f; // 7 during most long batch runs.

	/** Are we done with the tree policy? **/
	private boolean treePolicyDone = false;
	/** Are we done with the expansion policy? **/
	private boolean expansionPolicyDone = false;
	/** Are we done with the rollout policy? **/
	private boolean rolloutPolicyDone = false;

	/** Individual workers can deadlock rarely. This causes overflow errors when the tree policy is recursively called.
	 * Solution here is to wait a short period of time, doubling it until the worker is successful again. This happens
	 * maybe 1 in 5k games or so near the beginning only, so it's not worth finding something more elegant. **/
	private long deadlockDelayCurrent = 0;
	
	/** Must provide an evaluationFunction to get a numeric score for nodes after a rollout. **/
	public Sampler_UCB(IEvaluationFunction evaluationFunction) {
		this.evaluationFunction = evaluationFunction;
		c = 10*Random.nextFloat()*c + 0.1f;
	}
	
	/** Propagate the score and visit count back up the tree. **/
	private void propagateScore(Node failureNode) {
		float score = evaluationFunction.getValue(failureNode);

		// Do evaluation and propagation of scores.
		failureNode.visitCount.incrementAndGet();
		failureNode.addToValue(score);
		while (failureNode.treeDepth > 0){//TODO test 0
			failureNode = failureNode.parent;
			failureNode.visitCount.incrementAndGet();
			failureNode.addToValue(score);
		}	
	}
	
	/** Set a new evaluation function for this sampler. Should be hot-swappable at any point. **/
	public void setEvaluationFunction(IEvaluationFunction evaluationFunction) {
		this.evaluationFunction = evaluationFunction;
	}
	
	@Override
	public Node treePolicy(Node startNode) {
		if (!startNode.uncheckedActions.isEmpty() && startNode.reserveExpansionRights()) { // We immediately expand if there's an untried action.
			return startNode;
		}

		double bestScoreSoFar = -Double.MAX_VALUE;
		Node bestNodeSoFar = null;

		// Otherwise, we go through the existing tree picking the "best."
		Node parent = startNode;
		for (Node child: parent.children){

			if (!child.fullyExplored.get() && !child.getLockStatus()) {// && child.reserveExpansionRights()){
				float val = (float)(child.getValue()/child.visitCount.doubleValue() + c*(float)Math.sqrt(2.*Math.log(parent.visitCount.doubleValue())/child.visitCount.doubleValue()));
				if (val > bestScoreSoFar){
					bestNodeSoFar = child;
					bestScoreSoFar = val;
				}else {
					child.releaseExpansionRights();
				}
			}
		}
		if (bestNodeSoFar == null) { // This worker can't get a lock on any of the children it wants. Starting back at startNode.
			try {
				Thread.sleep(deadlockDelayCurrent);
				deadlockDelayCurrent = deadlockDelayCurrent * 2 + 1;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			bestNodeSoFar = startNode;
		}else {
			deadlockDelayCurrent = 0; // Reset delay if we're successful again.
		}
		
		return treePolicy(bestNodeSoFar); // Recurse until we reach a node with an unchecked action.;
	}

	@Override
	public void treePolicyActionDone(Node currentNode) {
		treePolicyDone = true; // Enable transition to next through the guard.
		expansionPolicyDone = false; // Prevent transition before it's done via the guard.
	}

	@Override
	public boolean treePolicyGuard(Node currentNode) {
		return treePolicyDone; // True means ready to move on to the next.
	}

	@Override
	public Node expansionPolicy(Node startNode) {
		if (startNode.uncheckedActions.size() == 0) throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");
		Action childAction = startNode.uncheckedActions.get(Utility.randInt(0,startNode.uncheckedActions.size() - 1));
		return startNode.addChild(childAction);
	}

	@Override
	public void expansionPolicyActionDone(Node currentNode) {
		treePolicyDone = false;
		expansionPolicyDone = true; // We move on after adding only one node.
		if (currentNode.state.failedState) { // If expansion is to failed node, no need to do rollout.
			rolloutPolicyDone = true;
			propagateScore(currentNode);
		}else {
			rolloutPolicyDone = false;
		}
	}

	@Override
	public boolean expansionPolicyGuard(Node currentNode) {
		return expansionPolicyDone;
	}

	@Override
	public Node rolloutPolicy(Node startNode) {
		if (startNode.state.failedState) throw new RuntimeException("Rollout policy received a starting node which corresponds to an already failed state.");
		// Do shit without adding nodes to the rest of the tree hierarchy.
		Action childAction = startNode.uncheckedActions.get(Utility.randInt(0,startNode.uncheckedActions.size() - 1));

		Node rolloutNode = new Node(startNode,childAction,false);

		return rolloutNode;
	}

	@Override
	public void rolloutPolicyActionDone(Node currentNode) {
		expansionPolicyDone = false;

		if (currentNode.state.failedState) {
			rolloutPolicyDone = true;
			propagateScore(currentNode);
		}else {
			rolloutPolicyDone = false;
		}
	}

	@Override
	public boolean rolloutPolicyGuard(Node currentNode) {
		return rolloutPolicyDone;
	}
	
	@Override
	public Sampler_UCB clone() {
		return new Sampler_UCB(evaluationFunction.clone());
	}
}