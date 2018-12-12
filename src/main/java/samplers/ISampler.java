package samplers;

import game.GameThreadSafe;
import tree.Node;

/**
 * Defines a strategy for sampling nodes.
 *
 * @author Matt
 */
public interface ISampler {

    /**
     * Decide a path through the existing tree to a place where a new node will be added.
     **/
    Node treePolicy(Node startNode);

    /**
     * Lets the sampler know that the previously requested game moves have occurred and the tree FSM is ready to do
     * more stuff.
     **/
    void treePolicyActionDone(Node currentNode);

    /**
     * Are we ready to switch from tree policy to expansion policy?
     **/
    boolean treePolicyGuard(Node currentNode);

    /**
     * Strategy for adding a single node at a depth of 1 greater than the given startNode.
     **/
    Node expansionPolicy(Node startNode);

    /**
     * Lets the sampler know that the previously requested game moves have occurred and the tree FSM is ready to do
     * more stuff.
     **/
    void expansionPolicyActionDone(Node currentNode);

    /**
     * Are we ready to switch from expansion policy to rollout policy?
     **/
    boolean expansionPolicyGuard(Node currentNode);

    /**
     * Continued expansion which is NOT added to the tree as nodes. Only used for scoring as in UCB.
     **/
    void rolloutPolicy(Node startNode, GameThreadSafe game);

    /**
     * Are we ready to switch from rollout policy to tree policy?
     **/
    boolean rolloutPolicyGuard(Node currentNode);

    /**
     * Copy this sampler and its settings. Each worker needs an individual copy.
     **/
    ISampler getCopy();
}
