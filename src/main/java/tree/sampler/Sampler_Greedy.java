package tree.sampler;

import java.util.ArrayList;

import game.actions.Action;
import tree.node.evaluator.IEvaluationFunction;
import game.IGameInternal;
import tree.node.NodeQWOPExplorableBase;
import tree.Utility;

public class Sampler_Greedy implements ISampler {

    /* NODE EVALUATION */
    /**
     * How are individual nodes scored?
     */
    private IEvaluationFunction evaluationFunction;

    /* HOW MANY SAMPLES BETWEEN JUMPS */
    /**
     * Number of samples to take before moving on from tree depth 0.
     */
    public int samplesAt0 = 1000;
    /**
     * Another depth specified.
     */
    public int depthN = 5;
    /**
     * Number of samples to take before moving on from tree depth N.
     */
    public int samplesAtN = 200;
    /**
     * Number of samples to take before moving on from large depth.
     */
    public int samplesAtInf = 75;

    /* JUMP SIZES */
    public int forwardJump = 1;
    public int backwardsJump = 10;
    public int backwardsJumpMin = 5;
    public float backwardsJumpFailureMultiplier = 1.5f;

    /**
     * Are we done with the tree policy?
     */
    private boolean treePolicyDone = false;

    /**
     * Are we done with the expansion policy?
     */
    private boolean expansionPolicyDone = false;

    /**
     * Current node from which all sampling is done further down the tree among its descendants.
     */
    private NodeQWOPExplorableBase<?> currentRoot;

    /**
     * Number of samples being taken from this node before going deeper into the tree.
     */
    private int totalSamplesToTakeAtThisNode;

    /**
     * Number of samples done so far at this node and depth.
     */
    private int samplesSoFarAtThisNode;

    /**
     * Some random sampling features used.
     */
    private Sampler_Distribution distributionSampler = new Sampler_Distribution();

    public Sampler_Greedy(IEvaluationFunction evaluationFunction) {
        this.evaluationFunction = evaluationFunction;
    }

    /**
     * Top half of a hyperbola to decide how many samples to take from a root at depth treeDepth.
     * Takes samplesAt0 samples at tree depth 0, passes through samplesAtN, and asymptotes to samplesAtInf.
     * Basically just a nice smooth thing.
     *
     * @param treeDepth
     * @return
     */
    public int numSamplesAtDepth(int treeDepth) {

        float a = (float) (depthN * depthN) * (samplesAtN - samplesAtInf) / (samplesAt0 - samplesAtN);
        float samples = a * (samplesAt0 - samplesAtInf) / (((float) treeDepth * treeDepth) + a) + samplesAtInf;
        return Math.round(samples);
    }

    /**
     * We've changed the node we're sampling from. Reset the appropriate stuff.
     */
    private void chooseNewRoot(NodeQWOPExplorableBase<?> newRoot) {
        currentRoot = newRoot;
        totalSamplesToTakeAtThisNode = numSamplesAtDepth(currentRoot.getTreeDepth());
        samplesSoFarAtThisNode = 0;
    }

    /**
     * Set a new evaluation function for this sampler. Should be hot-swappable at any point.
     */
    public void setEvaluationFunction(IEvaluationFunction evaluationFunction) {
        this.evaluationFunction = evaluationFunction;
    }

    @Override
    public NodeQWOPExplorableBase<?> treePolicy(NodeQWOPExplorableBase<?> startNode) {
        // Decide what to do with the given start node.
        if (currentRoot == null) {
            chooseNewRoot(startNode);
            // TODO find a way to avoid an unchecked cast
        } else if (((NodeQWOPExplorableBase) startNode).isOtherNodeAncestor(currentRoot)) { // If currentRoot is the
            // ancestor of startNode,
        	// switch to startNode.
            chooseNewRoot(startNode);
            // TODO find a way to avoid an unchecked cast
        } else if (!startNode.equals(currentRoot) && !((NodeQWOPExplorableBase)currentRoot).isOtherNodeAncestor(startNode)) { // If current
        	// root is not the ancestor, and start node is NOT the ancestor, they are parallel in some way, and
			// startnode wins.
            chooseNewRoot(startNode);
        }

        // Current node fully exhausted, we need to back the root up.
        if (currentRoot.isFullyExplored()) {
            int count = 0;
            NodeQWOPExplorableBase<?> movingNode = currentRoot;
            while (movingNode.getTreeDepth() > 0 && (movingNode.isFullyExplored() || count < backwardsJump)) {
                movingNode = movingNode.getParent();
                count++;
            }
            backwardsJump *= backwardsJumpFailureMultiplier;
            chooseNewRoot(movingNode);
        }

        return distributionSampler.treePolicy(currentRoot);
    }

    @Override
    public void treePolicyActionDone(NodeQWOPExplorableBase<?> currentNode) {
        treePolicyDone = true; // Enable transition to next through the guard.
        expansionPolicyDone = false; // Prevent transition before it's done via the guard.
    }

    @Override
    public boolean treePolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        return treePolicyDone;
    }

    @Override
    public Action expansionPolicy(NodeQWOPExplorableBase<?> startNode) {
        if (startNode.getUntriedActionCount() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");

        return startNode.getUntriedActionByIndex(Utility.randInt(0, startNode.getUntriedActionCount() - 1));
    }

    @Override
    public void expansionPolicyActionDone(NodeQWOPExplorableBase<?> currentNode) {
        treePolicyDone = false;
        if (currentNode.getState().isFailed()) {
            expansionPolicyDone = true;

            // Need to decide whether or not to advance the currentRoot
            samplesSoFarAtThisNode++;

            //Sampled enough. Go deeper.
            if (samplesSoFarAtThisNode >= totalSamplesToTakeAtThisNode) {
                // Pick the best leaf
                ArrayList<NodeQWOPExplorableBase<?>> leaves = new ArrayList<>();

                currentRoot.recurseDownTreeInclusive(n -> {
                    if (n.getChildCount() == 0) {
                        leaves.add(n);
                    }
                });
                //float rootX = currentRoot.state.body.x;
                NodeQWOPExplorableBase<?> bestNode = currentRoot;
                float bestScore = -Float.MAX_VALUE;
                for (NodeQWOPExplorableBase<?> leaf : leaves) {
                    float score = evaluationFunction.getValue(leaf);
                    if (score > bestScore) {
                        bestScore = score;
                        bestNode = leaf;
                    }
                }
                // Go back from the leaf to its ancestor only forwardJump ahead of the current root.
                while (bestNode.getTreeDepth() > currentRoot.getTreeDepth() + forwardJump) {
                    bestNode = bestNode.getParent();
                }
                backwardsJump -= 1;
                backwardsJump = Math.max(backwardsJump, backwardsJumpMin);
                chooseNewRoot(bestNode);
            }
        } else {
            expansionPolicyDone = false;
        }
    }

    @Override
    public boolean expansionPolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        return expansionPolicyDone;
    }

    @Override
    public void rolloutPolicy(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        // No rollout policy.
    }

    @Override
    public boolean rolloutPolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        // Rollout policy not in use in the random sampler.
        return true;
    }

    @Override
    public Sampler_Greedy getCopy() {
        return new Sampler_Greedy(evaluationFunction.getCopy());
    }
}
