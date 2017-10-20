package main;

import java.awt.Color;
import java.util.ArrayList;

public class Sampler_Greedy implements ISampler {
	
	/******* NODE EVALUATION *******/
	
	/** How are individual nodes scored? **/
	private IEvaluationFunction evaluationFunction;
	
	/******* HOW MANY SAMPLES BETWEEN JUMPS ********/
	/** Number of samples to take before moving on from tree depth 0. **/
	public int samplesAt0 = 500;
	/** Another depth specified. **/
	public int depthN = 5;
	/** Number of samples to take before moving on from tree depth N. **/
	public int samplesAtN= 300;
	/** Number of samples to take before moving on from large depth. **/
	public int samplesAtInf = 100;
	
	/****** JUMP SIZES ******/
	public int forwardJump = 1;
	public int backwardsJump = 5;
	
	
	/** Are we done with the tree policy? **/
	private boolean treePolicyDone = false;
	
	/** Are we done with the expansion policy? **/
	private boolean expansionPolicyDone = false;
	
	/** Are we done with the rollout policy? **/
	private boolean rolloutPolicyDone = true; // Rollout policy not in use in the random sampler.
	
	/** Current node from which all sampling is done further down the tree among its descendents. **/
	private Node currentRoot;
	
	/** Number of samples being taken from this node before going deeper into the tree. **/
	private int totalSamplesToTakeAtThisNode;
	
	/** Number of samples done so far at this node and depth. **/
	private int samplesSoFarAtThisNode;
	
	/** Some random sampling features used. **/
	private Sampler_Random randomSampler = new Sampler_Random();
	
	
	public Sampler_Greedy(IEvaluationFunction evaluationFunction) {
		this.evaluationFunction = evaluationFunction;
	}
	
	/**
	 * Top half of a hyperbola to decide how many samples to take from a root at depth treeDepth.
	 * Takes samplesAt0 samples at tree depth 0, passes through samplesAtN, and asymptotes to samplesAtInf.
	 * Basically just a nice smooth thing.
	 * @param treeDepth
	 * @return
	 */
	public int numSamplesAtDepth(int treeDepth) {
		
		float a = (float)(depthN*depthN)*(samplesAtN - samplesAtInf)/(float)(samplesAt0 - samplesAtN);
		float samples = a*(samplesAt0 - samplesAtInf)/(((float)treeDepth*treeDepth) + a) + samplesAtInf;	
		return Math.round(samples);
	}
	
	/** We've changed the node we're sampling from. Reset the appropriate stuff. **/
	private void chooseNewRoot(Node newRoot) {
		if (currentRoot != null) {
			currentRoot.nodeColor = null;
			currentRoot.displayPoint = false;
		}
		currentRoot = newRoot;
		currentRoot.nodeColor = Color.RED;
		currentRoot.displayPoint = true;
		
		totalSamplesToTakeAtThisNode = numSamplesAtDepth(currentRoot.treeDepth);
		samplesSoFarAtThisNode = 0;
	}
	
	/** Set a new evaluation function for this sampler. Should be hot-swappable at any point. **/
	public void setEvaluationFunction(IEvaluationFunction evaluationFunction) {
		this.evaluationFunction = evaluationFunction;
	}
	
	@Override
	public Node treePolicy(Node startNode) {
		// Decide what to do with the given start node.
		if (currentRoot == null) {
			chooseNewRoot(startNode);
		}else if (startNode.isOtherNodeAncestor(currentRoot)) { // If currentRoot is the ancestor of startNode, switch to startNode.
			chooseNewRoot(startNode);
		}else if (!startNode.equals(currentRoot) && !currentRoot.isOtherNodeAncestor(startNode)) { // If current root is not the ancestor, and start node is NOT the ancestor, they are parallel in some way, and startnode wins.
			chooseNewRoot(startNode);
		}
		
		// Current node fully exhausted, we need to back the root up.
		if (currentRoot.fullyExplored) {
			int count = 0;
			Node movingNode = currentRoot;
			while (movingNode.treeDepth > 0 && (movingNode.fullyExplored || count < backwardsJump)) {
				movingNode = movingNode.parent;
				count++;
			}
			chooseNewRoot(movingNode);
		}

		// Normal random sampling now.
		return randomSampler.treePolicy(currentRoot);
	}

	@Override
	public void treePolicyActionDone(Node currentNode) {
		treePolicyDone = true; // Enable transition to next through the guard.
		expansionPolicyDone = false; // Prevent transition before it's done via the guard.

	}

	@Override
	public boolean treePolicyGuard(Node currentNode) {
		return treePolicyDone;
	}

	@Override
	public Node expansionPolicy(Node startNode) {
		if (startNode.uncheckedActions.size() == 0) throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");
		
		Action childAction = startNode.uncheckedActions.get(Node.randInt(0,startNode.uncheckedActions.size() - 1));
		
		return startNode.addChild(childAction);
	}

	@Override
	public void expansionPolicyActionDone(Node currentNode) {
		treePolicyDone = false;
		if (currentNode.state.failedState) {
			expansionPolicyDone = true;
			
			/****** Need to decide whether or not to advance the currentRoot *******/
			samplesSoFarAtThisNode++;
		
			//Sampled enough. Go deeper.
			if (samplesSoFarAtThisNode >= totalSamplesToTakeAtThisNode) {
				// Pick the best leaf
				ArrayList<Node> leaves = new ArrayList<Node>();
				currentRoot.getLeaves(leaves);
				float rootX = currentRoot.state.body.x;
				Node bestNode = currentRoot;
				float bestScore = -Float.MAX_VALUE;
				for (Node leaf : leaves) {
					float score = (leaf.state.body.x - rootX)/rootX;// + 5f*leaf.state.body.th;
					if (score > bestScore) {
						bestScore = score;
						bestNode = leaf;
					}
				}
				// Go back from the leaf to its ancestor only forwardJump ahead of the current root.
				while (bestNode.treeDepth > currentRoot.treeDepth + forwardJump) {
					bestNode = bestNode.parent;
				}
				chooseNewRoot(bestNode);
			}
		}else {
			expansionPolicyDone = false;
		}
	}

	@Override
	public boolean expansionPolicyGuard(Node currentNode) {
		return expansionPolicyDone;
	}

	@Override
	public Node rolloutPolicy(Node startNode) {
		// No rollout policy.
		return null;
	}

	@Override
	public void rolloutPolicyActionDone(Node currentNode) {}

	@Override
	public boolean rolloutPolicyGuard(Node currentNode) {
		return rolloutPolicyDone; // No rollout policy
	}

}
