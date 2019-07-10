package tree.sampler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import game.IGameInternal;
import game.action.Action;
import org.jblas.util.Random;
import tree.node.NodeQWOPExplorableBase;
import tree.node.evaluator.IEvaluationFunction;
import tree.sampler.rollout.IRolloutPolicy;
import value.updaters.IValueUpdater;
import value.updaters.ValueUpdater_Average;

/**
 * Implements upper confidence bound for trees (UCBT, UCT, UCB, depending on who you ask).
 * Key feature is that it lets the user define a value, c, that weights exploration vs exploitation.
 *
 * @author Matt
 */
public class Sampler_UCB implements ISampler {

    /**
     * Constant term on UCB exploration factor. Higher means more exploration.
     */
    public final float explorationConstant;

    /**
     *
     */
    public final float explorationRandomFactor;

    /**
     * Evaluation function used to score single nodes after rollouts are done.
     */
    private final IEvaluationFunction evaluationFunction;

    /**
     * Policy used to evaluate the score of a tree expansion Node by doing rollout(s).
     */
    private final IRolloutPolicy rolloutPolicy;

    public IValueUpdater valueUpdater = new ValueUpdater_Average(); //TopNChildren(8); // TODO make this an
    // assignable
    // parameter.

    /**
     * Explore/exploit trade-off parameter. Higher means more exploration. Lower means more exploitation.
     */
    private final float c;

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
     * Also specify a rollout policy to use.
     */
    public Sampler_UCB(
            @JsonProperty("evaluationFunction") IEvaluationFunction evaluationFunction,
            @JsonProperty("rolloutPolicy") IRolloutPolicy rolloutPolicy,
            @JsonProperty("explorationConstant") float explorationConstant,
            @JsonProperty("explorationRandomFactor") float explorationRandomFactor) {
        this.evaluationFunction = evaluationFunction;
        this.rolloutPolicy = rolloutPolicy;
        this.explorationConstant = explorationConstant;
        this.explorationRandomFactor = explorationRandomFactor;
        c = explorationRandomFactor * Random.nextFloat() + explorationConstant;
    }

    /**
     * Propagate the score and visit count back up the tree.
     */
    private void propagateScore(NodeQWOPExplorableBase<?> failureNode, float score) {
        // Do evaluation and propagation of scores.
        failureNode.recurseUpTreeInclusive(n -> n.updateValue(score, valueUpdater));
    }

    @Override
    public NodeQWOPExplorableBase<?> treePolicy(NodeQWOPExplorableBase<?> startNode) {
        if (startNode.getUntriedActionCount() != 0) {
            if (startNode.reserveExpansionRights()) { // We immediately expand
                // if there's an untried action.
                assert startNode.isLocked();
                return startNode;
            } else {
                return null;
            }
        }

        double bestScoreSoFar = -Double.MAX_VALUE;
        NodeQWOPExplorableBase<?> bestNodeSoFar = null;

        // Otherwise, we go through the existing tree picking the "best."
        for (NodeQWOPExplorableBase<?> child : startNode.getChildren()) {

            if (!child.isFullyExplored() && !child.isLocked() && child.getUpdateCount() > 0) {
                float val = (child.getValue() + c * (float) Math.sqrt(2. * Math.log((double) startNode.getUpdateCount()) / (double) child.getUpdateCount()));
                assert !Float.isNaN(val);
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
    public void treePolicyActionDone(NodeQWOPExplorableBase<?> currentNode) {
        treePolicyDone = true; // Enable transition to next through the guard.
        expansionPolicyDone = false; // Prevent transition before it's done via the guard.
    }

    @Override
    public boolean treePolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        return treePolicyDone; // True means ready to move on to the next.
    }

    @Override
    public Action expansionPolicy(NodeQWOPExplorableBase<?> startNode) {
        if (startNode.getUntriedActionCount() == 0)
            throw new IndexOutOfBoundsException("Expansion policy received a node from which there are no new nodes to try!");
        return startNode.getUntriedActionOnDistribution();
    }

    @Override
    public void expansionPolicyActionDone(NodeQWOPExplorableBase<?> currentNode) {
        treePolicyDone = false;
        expansionPolicyDone = true; // We move on after adding only one node.
        if (currentNode.getState().isFailed()) { // If expansion is to failed node, no need to do rollout.
            rolloutPolicyDone = true;
            propagateScore(currentNode, evaluationFunction.getValue(currentNode));
        } else {
            rolloutPolicyDone = false;
        }
    }

    @Override
    public boolean expansionPolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        return expansionPolicyDone;
    }

    @Override
    public void rolloutPolicy(NodeQWOPExplorableBase<?> startNode, IGameInternal game) {
        if (startNode.getState().isFailed())
            throw new IllegalStateException("Rollout policy received a starting node which corresponds to an already failed " +
                    "state.");
        float score = rolloutPolicy.rollout(startNode, game);
        propagateScore(startNode, score);

        rolloutPolicyDone = true;
    }

    @Override
    public boolean rolloutPolicyGuard(NodeQWOPExplorableBase<?> currentNode) {
        return rolloutPolicyDone;
    }

    @JsonIgnore
    @Override
    public Sampler_UCB getCopy() {
        Sampler_UCB sampler = new Sampler_UCB(evaluationFunction.getCopy(), rolloutPolicy.getCopy(),
                explorationConstant, explorationRandomFactor);
        sampler.valueUpdater = valueUpdater;
        return sampler;
    }

    public IEvaluationFunction getEvaluationFunction() {
        return evaluationFunction;
    }

    public IRolloutPolicy getRolloutPolicy() {
        return rolloutPolicy;
    }

    @JsonIgnore
    public float getC() {
        return c;
    }
}
