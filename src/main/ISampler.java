package main;
/**
 * Defines a strategy for sampling nodes.
 * 
 * @author Matt
 *
 */
public interface ISampler {
	
	/** Decide a path through the existing tree to a place where a new node will be added. **/
	public Node treePolicy(Node startNode);
	
	/** Are we ready to switch from tree policy to expansion policy? **/
	public boolean treePolicyGuard(Node currentNode);
	
	/** Strategy for adding a single node at a depth of 1 greater than the given startNode. **/
	public Node expansionPolicy(Node startNode);
	
	/** Are we ready to switch from expansion policy to rollout policy? **/
	public boolean expansionPolicyGuard(Node currentNode);
	
	/** Continued expansion which is NOT added to the tree as nodes. Only used for scoring as in UCB. **/
	public Node rolloutPolicy(Node startNode);
	
	/** Are we ready to switch from rollout policy to tree policy? **/
	public boolean rolloutPolicyGuard(Node currentNode);
}
