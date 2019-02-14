package samplers;

import game.GameThreadSafe;
import org.jblas.util.Random;

import actions.Action;
import evaluators.IEvaluationFunction;
import samplers.rollout.RolloutPolicy;
import samplers.rollout.RolloutPolicy_RandomColdStart;
import tree.Node;
import tree.Utility;

/**
 * Implements upper confidence bound for trees (UCBT, UCT, UCB, depending on who you ask).
 * Key feature is that it lets the user define a value, c, that weights exploration vs exploitation.
 *
 * @author Matt
 */
public class Sampler_UCB implements ISampler {

    /**
     * Evaluation function used to score single nodes after rollouts are done.
     */
    private IEvaluationFunction evaluationFunction;

    /**
     * Policy used to evaluate the score of a tree expansion Node by doing rollout(s).
     */
    private RolloutPolicy rolloutPolicy;

    /**
     * Explore/exploit trade-off parameter. Higher means more exploration. Lower means more exploitation.
     */
    public float c = 6; // 7 during most long batch runs.

    /**
     * A multiplier to tone down or amp up exploration. Higher means more exploration.
     */
    public static float explorationMultiplier = 1f;

    /**
     * Constant term on UCB exploration factor. Higher means more exploration.
     */
    public static float explorationConstant = 1f;

    /**
     * Are we done with the tree policy?
     */
    private boolean treePolicyDone = false;

    /**
     * Are we done with the expansion policy?
     */
    private boolean expansionPolicyDone = false;

    /**
     * Are we done with the rollout policy?
     */
    private boolean rolloutPolicyDone = false;

    /**
     * Individual workers can deadlock rarely. This causes overflow errors when the tree policy is recursively called.
     * Solution here is to wait a short period of time, doubling it until the worker is successful again. This happens
     * maybe 1 in 5k games or so near the beginning only, so it's not worth finding something more elegant.
     */
    private long deadlockDelayCurrent = 0;

    /**
     * Must provide an evaluationFunction to get a numeric score for nodes after a rollout.
     */
    public Sampler_UCB(IEvaluationFunction evaluationFunction) {
        this.evaluationFunction = evaluationFunction;
        rolloutPolicy = new RolloutPolicy_RandomColdStart(evaluationFunction);// RolloutPolicy_RandomColdStart
        // (evaluationFunction);
        c =  explorationMultiplier * (Random.nextFloat() * c + explorationConstant);
    }

    /**
     * Must provide an evaluationFunction to get a numeric score for nodes after a rollout.
     * Also specify a rollout policy to use.
     */
    public Sampler_UCB(IEvaluationFunction evaluationFunction, RolloutPolicy rolloutPolicy) {
        this.evaluationFunction = evaluationFunction;
        this.rolloutPolicy = rolloutPolicy;
        c = explorationMultiplier * (Random.nextFloat() * c + explorationConstant);
    }

    /**
     * Propagate the score and visit count back up the tree.
     */
    private void propagateScore(Node failureNode, float score) {
        // Do evaluation and propagation of scores.
        failureNode.visitCount.incrementAndGet();
        failureNode.addToValue(score);
        while (failureNode.getTreeDepth() > 0) { // TODO test 0
            failureNode = failureNode.getParent();
            failureNode.visitCount.incrementAndGet();
            failureNode.addToValue(score);
        }
    }

    /**
     * Set a new evaluation function for this sampler. Should be hot-swappable at any point.
     */
    public void setEvaluationFunction(IEvaluationFunction evaluationFunction) {
        this.evaluationFunction = evaluationFunction;
    }

    @Override
    public Node treePolicy(Node startNode) {
        if (!startNode.uncheckedActions.isEmpty() && startNode.reserveExpansionRights()) { // We immediately expand
        	// if there's an untried action.
            return startNode;
        }

        double bestScoreSoFar = -Double.MAX_VALUE;
        Node bestNodeSoFar = null;

        // Otherwise, we go through the existing tree picking the "best."
        for (Node child : startNode.getChildren()) {

            if (!child.fullyExplored.get() && !child.isLocked()) {
                float val =
						(float) (child.getValue() / child.visitCount.doubleValue() + c * (float) Math.sqrt(2. * Math.log(startNode.visitCount.doubleValue()) / child.visitCount.doubleValue()));
                if (val > bestScoreSoFar) {
                    bestNodeSoFar = child;
                    bestScoreSoFar = val;
                }
            }
        }
        if (bestNodeSoFar == null) { // This worker can't get a lock on any of the children it wants. Starting back
        	// at startNode.
            if (deadlockDelayCurrent > 5000) {
                System.out.println("UCB sampler worker got really jammed up. Terminating this one.");
                return null;
            }
            try {
                Thread.sleep(deadlockDelayCurrent);
                deadlockDelayCurrent = deadlockDelayCurrent * 2 + 1;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            bestNodeSoFar = startNode;
        } else {
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
        if (startNode.uncheckedActions.size() == 0)
            throw new RuntimeException("Expansion policy received a node from which there are no new nodes to try!");
        Action childAction = startNode.uncheckedActions.get(Utility.randInt(0, startNode.uncheckedActions.size() - 1));
        return startNode.addChild(childAction);
    }

    @Override
    public void expansionPolicyActionDone(Node currentNode) {
        treePolicyDone = false;
        expansionPolicyDone = true; // We move on after adding only one node.
        if (currentNode.isFailed()) { // If expansion is to failed node, no need to do rollout.
            rolloutPolicyDone = true;
            propagateScore(currentNode, evaluationFunction.getValue(currentNode));
        } else {
            rolloutPolicyDone = false;
        }
    }

    @Override
    public boolean expansionPolicyGuard(Node currentNode) {
        return expansionPolicyDone;
    }

    //Controller_Tensorflow_ClassifyActionsPerTimestep con = new Controller_Tensorflow_ClassifyActionsPerTimestep(
    //        "frozen_model.pb", "src/main/resources/tflow_models", "tfrecord_input/split", "softmax/Softmax");

    @Override
    public void rolloutPolicy(Node startNode, GameThreadSafe game) {
        if (startNode.isFailed())
            throw new IllegalStateException("Rollout policy received a starting node which corresponds to an already failed " +
                    "state.");
        float score = rolloutPolicy.rollout(startNode, game);
        propagateScore(startNode, score);

        rolloutPolicyDone = true;
    }

    @Override
    public boolean rolloutPolicyGuard(Node currentNode) {
        return rolloutPolicyDone;
    }

    @Override
    public Sampler_UCB getCopy() {
        return new Sampler_UCB(evaluationFunction.getCopy(), rolloutPolicy.getCopy());
    }
}
