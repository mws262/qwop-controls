package main;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import com.jogamp.opengl.GL2;

import data.SavableSingleGame;
import game.GameLoader;
import game.State;

/**
 * Representation of a node in a tree of actions for QWOP. Most of the tree management stuff is handled by recursing
 * through up and down the linked connections of nodes. All nodes past the root node contain a game state and the
 * single action since the previous node taken to get there. The root node is the same, except it has only a state
 * and not an action associated with it.
 * <p>
 * The convention "up the tree" is used to mean towards the root node, or towards lower tree depth. "Down the tree"
 * means towards the leaves, or to greater tree depth.
 *
 * @author matt
 */

public class Node {

    /**
     * Node which leads up to this node.
     **/
    private Node parent; // Parentage may not be changed.

    /**
     * Child nodes. Not fixed size any more.
     **/
    private final List<Node> children = new ArrayList<>();

    /**
     * Action which takes the game from the parent node's state to this node's state.
     **/
    public final Action action;

    /**
     * State after taking this node's action from the parent node's state.
     **/
    public State state;

    /**
     * True if this node represents a failed state.
     */
    private final AtomicBoolean isFailed = new AtomicBoolean();

    /**
     * A "score" or value associated with this node's performance. The usage of this field is up to the sampler.
     */
    private float value = 0;

    /**
     * Some sampling methods want to track how many times this node has been visited during tree sampling.
     **/
    public AtomicLong visitCount = new AtomicLong();


    /* Global, thread-safe sampling statistics. */

    /**
     * Total number of nodes created this execution.
     */
    private static final LongAdder nodesCreated = new LongAdder();

    /**
     * Total number of games played during this execution. Should be greater than or equal to the number of nodes
     * created this execution.
     */
    private static final LongAdder gamesCreated = new LongAdder();

    /**
     * Total number of nodes imported from a save file.
     */
    private static final LongAdder nodesImported = new LongAdder();

    /**
     * Total number of games imported from a save file.
     */
    private static final LongAdder gamesImported = new LongAdder();

    /**
     * Maximum depth anywhere below this node in the tree. The tree root is 0.
     */
    public final AtomicInteger maxBranchDepth = new AtomicInteger();


    /**
     * If assigned, this automatically adds potential actions to children when they are created. This makes the
     * functionality a little
     * more like the old version which selected from a fixed pool of durations for each action in the sequence. This
     * time, the action
     * generator can use anything arbitrary to decide what the potential children are, and the
     * potentialActionGenerator can be hot swapped
     * at any point.
     **/
    public static IActionGenerator potentialActionGenerator;


    /**
     * Untried child actions.
     **/
    public ActionSet uncheckedActions;

    /**
     * Are there any untried things below this node?
     **/
    public final AtomicBoolean fullyExplored = new AtomicBoolean(false);

    /**
     * Does this node represent a failed state? Stronger than fullyExplored.
     **/
    public boolean isTerminal = false;

    /**
     * If one TreeWorker is expanding from this leaf node (if it is one), then no other worker should try to
     * simultaneously expand from here too.
     **/
    private final AtomicBoolean locked = new AtomicBoolean(false);

    /**
     * How deep is this node down the tree? 0 is root.
     **/
    public final int treeDepth;

    /**
     * Maximum depth yet seen in this tree.
     **/
    private static int maxDepthYet = 1;

    /********* TREE VISUALIZATION ************/
    public Color overrideLineColor = null; // Only set when the color is overridden.
    public Color overrideNodeColor = null; // Only set when the color is overridden.

    public Color nodeColor = Color.GREEN;

    private static final float lineBrightness_default = 0.85f;
    private float lineBrightness = lineBrightness_default;

    public boolean displayPoint = false; // Round dot at this node. Is it on?
    private boolean displayLine = true; // Line from this node to parent. Is it on?
    private static final boolean debugDrawNodeLocking = false; // Draw which nodes are locked by command from the
    // TreeWorkers.

    // Limiting number of display nodes.
    private static final boolean limitDrawing = false;
    public static final Set<Node> pointsToDraw = ConcurrentHashMap.newKeySet();
    private boolean notDrawnForSpeed = false;

    // Disable node position calculations (for when running headless)
    private static final boolean calculateNodeVisPositions = true;

    /* NODE PLACEMENT */

    /**
     * Parameters for visualizing the tree
     **/
    public float[] nodeLocation = new float[3]; // Location that this node appears on the tree visualization
    private float nodeAngle = 0; // Keep track of the angle that the line previous node to this node makes.
    private float sweepAngle = 2f * (float) Math.PI;

    private float edgeLength = 1.f;

    /**
     * If we want to bring out a certain part of the tree so it doesn't hide under other parts, give it a small z
     * offset.
     **/
    private float zOffset = 0.f;

    /* UTILITY */

    /**
     * Are we bulk adding saved nodes? If so, some construction things are deferred.
     **/
    private static boolean currentlyAddingSavedNodes = false;

    /**********************************/
    /********** CONSTRUCTORS **********/
    /**********************************/

    /**
     * Make a new node which is NOT the root. Add this to the tree hierarchy.
     **/
    private Node(Node parent, Action action) {
        this(parent, action, true);
    }

    /**
     * Make a new node which is NOT the root. connectNodeToTree is the default.
     **/
    public Node(Node parent, Action action, boolean connectNodeToTree) {
        this.parent = parent;
        treeDepth = parent.treeDepth + 1;
        this.action = action;

        // Error check for duplicate actions.
        for (Node parentChildren : parent.children) {
            if (parentChildren.action == action) {
                throw new RuntimeException("Tried to add a duplicate action node at depth " + treeDepth + ". Action " +
                        "was: " + action.toString() + ".");
            }
        }

        // Add some child actions to try if an action generator is assigned.
        autoAddUncheckedActions();

        edgeLength = 5.00f * (float) Math.pow(0.6947, 0.1903 * treeDepth) + 1.5f;

        lineBrightness = parent.lineBrightness;
        zOffset = parent.zOffset;

        // Should only add to parent's list after everything else has been done to prevent half-initialized nodes
        // from being used by other workers.
        if (connectNodeToTree) {
            parent.children.add(this);
            if (treeDepth > maxDepthYet) {
                maxDepthYet = treeDepth;
            }

            // Update max branch depth
            maxBranchDepth.set(treeDepth);
            Node currentNode = this;
            while (currentNode.treeDepth > 0 && parent.maxBranchDepth.get() < currentNode.maxBranchDepth.get()) {
                parent.maxBranchDepth.set(currentNode.maxBranchDepth.get());
                currentNode = parent;
            }
        }

        if (connectNodeToTree && !currentlyAddingSavedNodes && calculateNodeVisPositions) {
            calcNodePos(); // Must be called after this node has been added to its parent's list!
        }
    }

    /**
     * Make the root of a new tree.
     **/
    public Node() {
        parent = null;
        action = null;
        treeDepth = 0;
        nodeLocation[0] = 0f;
        nodeLocation[1] = 0f;
        nodeLocation[2] = 0f;

        nodeColor = Color.BLUE;
        displayPoint = true;

        // Root node gets the QWOP initial condition.
        setState(GameLoader.getInitialState());

        // Add some child actions to try if an action generator is assigned.
        autoAddUncheckedActions();
    }


    /**
     * Add a new child node from a given action. If the action is in uncheckedActions, remove it.
     *
     * @param childAction The action which defines a child node, i.e. it takes this node's state to the new child's
     *                    state.
     * @return A new child node defined by the provided action.
     */
    public synchronized Node addChild(Action childAction) {
        uncheckedActions.remove(childAction);
        nodesCreated.increment();
        return new Node(this, childAction);
    }

    /**
     * If we've assigned a potentialActionGenerator, this can auto-add potential child actions. Ignores duplicates.
     */
    private synchronized void autoAddUncheckedActions() {
        // If we've set rules to auto-select potential children, do so.
        if (potentialActionGenerator != null) {
            ActionSet potentialActions = potentialActionGenerator.getPotentialChildActionSet(this);

            // If no unchecked actions have been previously added (must have assigned a sampling distribution to do so),
            // then just use the new one outright.
            if (uncheckedActions == null) {
                uncheckedActions = potentialActions;
            } else { // Otherwise, just use the existing distribution, but add the new actions anyway.
                for (Action potentialAction : potentialActions) {
                    if (!uncheckedActions.contains(potentialAction)) {
                        uncheckedActions.add(potentialAction);
                    }
                }
            }
        }
    }

    /*
     * LOCKING AND UNLOCKING NODES:
     *
     * Due to multithreading, it is a major headache if several threads are sampling from the same node at the same
     * time. Hence, we lock nodes to prevent sampling by other threads while another one is working in that part of
     * the tree. In general, we try to be overzealous in locking. For broad trees, there is minimal blocking slowdown
     * . For narrow trees, however, we may get only minimal gains from multithreading.
     *
     * Cases:
     * 1. Select node to expand. It has only 1 untried option. We lock the node, and expand.
     * 2. Select node to expand. It has multiple options. We still lock the node for now. This could be changed.
     * 3. Select node to expand. It is now locked according to 1 and 2. This node's parent only has fully explored
     * children and locked children. For all
     * practical purposes, this node is also out of play. Lock it too and recurse up the tree until we reach a node
     * with at least one unlocked and not
     * fully explored child.
     * When unlocking, we should propagate fully-explored statuses back up the tree first, and then remove locks as
     * far up the tree as possible.
     */

    /**
     * Set a flag to indicate that the invoking TreeWorker has temporary exclusive rights to expand from this node.
     *
     * @return Whether the lock was successfully obtained. True means that the caller obtained the lock. False means
     * that someone else got to it first.
     */
    public synchronized boolean reserveExpansionRights() {
        if (locked.get()) { // Already owned by another worker.
            return false;

        } else {
            if (uncheckedActions.isEmpty()) // No child actions remain to sample. FIXME: This shouldn't actually
                // occur. Replace with an exception.
                return false;

            locked.set(true);

            if (debugDrawNodeLocking) { // For highlighting points in the visualizer representing nodes which have
                // locks.
                displayPoint = true;
                overrideNodeColor = Color.RED;
            }

            // May need to add locks to nodes further up the tree towards root. For example, if calling
            // reserveExpansionRights locks the final available node of this node's parent, then the parent should be
            // locked off too. This effect can chain all the way up the tree towards the root.
            if (treeDepth > 0) parent.propagateLock();

            return true;
        }
    }

    /**
     * Set a flag to indicate that the invoking TreeWorker has released exclusive rights to expand from this node.
     **/
    public synchronized void releaseExpansionRights() {
        locked.set(false); // Release the lock.

        if (debugDrawNodeLocking) { // Stop drawing red dots for locked nodes, if this is on.
            displayPoint = false;
            overrideNodeColor = null;
        }

        // Unlocking this node may cause nodes further up the tree to become available.
        if (treeDepth > 0) parent.propagateUnlock();
    }

    /**
     * Locking one node may cause some parent nodes to become unavailable also. propagateLock will check as far up
     * the tree towards the root node as necessary.
     */
    private synchronized void propagateLock() {

        // Lock this node unless we find evidence that we don't need to.
        for (Node child : children) {
            if (!child.isTerminal && !child.getLockStatus()) { // Neither terminal node, nor locked.
                return; // In this case, we don't need to continue locking things further up the tree.
            }
        }
        reserveExpansionRights();
    }

    /**
     * Releasing one node's lock may make others further up the tree towards the root node become available.
     */
    private synchronized void propagateUnlock() {
        if (!getLockStatus()) return; // We've worked our way up to a node which is already not locked. No need to
        // propagate further.

        // A single free child means we can unlock this node.
        for (Node child : children) {
            if (!child.isTerminal && !child.getLockStatus()) {  // Neither terminal node, nor locked -> does not need
                // to stay locked.
                releaseExpansionRights();

                return;
            }
        }
    }

    /**
     * Determine whether any sampler has exclusive rights to sample from this node.
     *
     * @return Whether any worker has exclusive rights to expand from this node (true/false).
     */
    public boolean getLockStatus() {
        return locked.get();
    }

    /**
     * Get the node's value in a thread-safe way. The value or 'score' is totally at the discretion of the sampler.
     * The node value is 0 until otherwise set.
     *
     * @return A score/cost/value associated with this node.
     */
    public synchronized float getValue() {
        return value;
    }

    /**
     * Set the node's value in a thread-safe way. The sampler must decide how to use this.
     *
     * @param val A new value for this node.
     */
    public synchronized void setValue(float val) {
        value = val;
    }

    /**
     * Add to the node's existing value in a thread-safe way. The sampler must decide how to use this.
     *
     * @param val A value to add to this node's existing value.
     */
    public synchronized void addToValue(float val) {
        value += val;
    }

    /* ********************************************* */
    /* ****** GETTING CERTAIN SETS OF NODES ******** */
    /* ********************************************* */

    /**
     * Get the parent node of this node. If called from root, will return null.
     *
     * @return Parent node of this node.
     */
    public Node getParent() {
        return parent;
    }

    /**
     * Get the children of this node.
     *
     * @return A copy of the list of children of this node. Removing the nodes from this list will not remove them
     * from this node's actual children, but the nodes in the list are the originals.
     */
    public List<Node> getChildren() {
        return new ArrayList<>(children);
    }

    /**
     * Get the index of this node in it's parent list of nodes. Hence, parent.children.get(index) == this.
     *
     * @return This node's index in its parent's list of nodes.
     */
    public int getIndexAccordingToParent() {
        return parent.children.indexOf(this);
    }

    /**
     * Get the number of siblings, i.e. other nodes in this node's parent's list of children. Does not include this
     * node itself.
     *
     * @return Number of sibling nodes of this one.
     */
    public int getSiblingCount() {
        int siblings = parent.children.size() - 1;
        assert siblings >= 0;
        return siblings;
    }

    /**
     * Get the total number of children of this node. Only includes actually created children, not potential children.
     *
     * @return Total created children of this node.
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Get a created child node of this node by its index (order of creation).
     *
     * @param childIndex
     * @return A child of this node, with the index as specified.
     */
    public Node getChildByIndex(int childIndex) {
        return children.get(childIndex);
    }

    /**
     * Get a random already-created child of this node. Can be useful for sampling.
     *
     * @return A random child node of this node.
     */
    public Node getRandomChild() {
        return children.get(Utility.randInt(0, children.size() - 1));
    }

    /**
     * Add all the nodes below and including this one to a list. Does not include nodes whose state have not yet been
     * assigned.
     *
     * @param nodeList A list to add all of this branches' nodes to. This list must be caller-provided, and will not
     *                 be cleared.
     * @return Returns the list of nodes below. This is done in place, so the object is the same as the argument one.
     */
    public List<Node> getNodesBelow(List<Node> nodeList) {
        if (state != null) {
            nodeList.add(this);
        }
        for (Node child : children) {
            child.getNodesBelow(nodeList);
        }
        return nodeList;
    }


    /**
     * Get a list of all tree endpoints (leaves) below this node, i.e. on this branch.
     *
     * @param leaves A list of leaves below this node. The list must be provided by the caller, and will not be
     *               cleared by this method.
     * @return Returns the list of nodes below. This is done in place, so the object is the same as the argument one.
     */
    public List<Node> getLeaves(List<Node> leaves) {
        for (Node child : children) {
            if (child.children.isEmpty()) {
                leaves.add(child);
            } else {
                child.getLeaves(leaves);
            }
        }
        return leaves;
    }

    /**
     * Returns the tree root no matter which node in the tree this is called from. This method defines the tree root
     * to be the node with a depth of 0.
     *
     * @return The tree root node.
     */
    public Node getRoot() {
        Node currentNode = this;
        while (currentNode.treeDepth > 0) {
            currentNode = currentNode.parent;
        }
        return currentNode;
    }

    /**
     * Count the number of descendants this node has.
     *
     * @return Number of descendants, i.e. number of nodes on the branch below this node.
     */
    public int countDescendants() {
        int count = 0;
        for (Node current : children) {
            count++;
            count += current.countDescendants(); // Recurse down through the tree.
        }
        return count;
    }

    /**
     * Check whether a node is an ancestor of this node. This means that there is a direct path from this node to the
     * given node that only requires decreasing tree depth.
     *
     * @param possibleAncestorNode Node to check whether is an ancestor of this node.
     * @return Whether the provided node is an ancestor of this node (true/false).
     */
    public boolean isOtherNodeAncestor(Node possibleAncestorNode) {
        if (possibleAncestorNode.treeDepth >= this.treeDepth) { // Don't need to check if this is as far down the tree.
            return false;
        }
        Node currNode = parent;

        while (currNode.treeDepth != possibleAncestorNode.treeDepth) { // Find the node at the same depth as the one
            // we're
            // checking.
            currNode = currNode.parent;
        }

        return currNode.equals(possibleAncestorNode);

    }

    /**
     * Change whether this node or any above it have become fully explored. Generally called from a leaf node which
     * has just been assigned fully-explored status, and we need to propagate the effects back up the tree.
     * <p>
     * This is the "lite" version because it assumes that all child nodes it encounters have correct fully-explored
     * statuses already assigned. This should be true during normal operation, but when a bunch of saved nodes are
     * imported, it is useful to do a {@link Node#propagateFullyExplored_complete() complete check}.
     */
    private void propagateFullyExploredStatus_lite() {
        boolean flag = true; // Assume this node is fully-explored and negate if we find evidence that it is not.

        if (!isFailed.get()) {
            if (uncheckedActions != null && !uncheckedActions.isEmpty()) {
                flag = false;
            }
            for (Node child : children) {
                if (!child.fullyExplored.get()) { // If any child is not fully explored, then this node isn't too.
                    flag = false;
                }
            }
        }
        fullyExplored.set(flag);

        if (treeDepth > 0) { // We already know this node is fully explored, check the parent.
            parent.propagateFullyExploredStatus_lite();
        }
    }

    /**
     * Change whether this node or any above it have become fully explored. This is the complete version, which
     * resets any existing fully-explored tags from the descendants of this node before redoing all checks. Call from
     * root to re-label the whole tree. During normal tree-building, a {@link Node#propagateFullyExplored_complete()
     * lite check} should suffice and is more computationally efficient.
     * <p>
     * This should only be used when a bunch of nodes are imported at once and need to all be checked, or if we need
     * to validate correct behavior of some feature.
     **/
    private void propagateFullyExplored_complete() {
        ArrayList<Node> leaves = new ArrayList<>();
        getLeaves(leaves);

        // Reset all existing exploration flags out there.
        for (Node leaf : leaves) {
            Node currNode = leaf;
            while (currNode.treeDepth > treeDepth) {
                currNode.fullyExplored.set(false);
                currNode = currNode.parent;
            }
            currNode.fullyExplored.set(false);
        }

        for (Node leaf : leaves) {
            leaf.propagateFullyExploredStatus_lite();
        }
    }

    /**
     * Destroy a branch and try to free up its memory. Mark the trimmed branch as fully explored and propagate the
     * status. This method can be useful when the sampler or user decides that one branch is bad and wants to keep it
     * from being used later in sampling.
     **/
    public void destroyAllNodesBelowAndCheckExplored() {
        destroyAllNodesBelow();
        propagateFullyExploredStatus_lite();
    }

    /**
     * Destroy a branch and try to free up its memory.
     **/
    private void destroyAllNodesBelow() {
        for (Node child : children) {
            child.state = null;
            child.parent = null;
            child.destroyAllNodesBelow();
        }
        pointsToDraw.remove(this);
        uncheckedActions.clear();
        children.clear();
        displayPoint = false;
        displayLine = false;
        nodeLocation = null;
    }

    /**
     * Get the number of nodes in this or any tree.
     *
     * @return Total number of nodes, both imported and created.
     */
    public static long getTotalNodeCount() {
        return nodesImported.longValue() + nodesCreated.longValue();
    }

    /**
     * Get the number of nodes imported from save file.
     *
     * @return Number of nodes imported from a save file.
     */
    public static long getImportedNodeCount() {
        return nodesImported.longValue();
    }

    /**
     * Get the total number of nodes created this session.
     *
     * @return Number of nodes created during this execution.
     */
    public static long getCreatedNodeCount() {
        return nodesCreated.longValue();
    }

    /**
     * Get the total number of games (not nodes) imported from save file.
     *
     * @return Number of imported games.
     */
    public static long getImportedGameCount() {
        return gamesImported.longValue();
    }

    /**
     * Get the total number of games created during this session. Only includes games for which nodes are created.
     *
     * @return Total number of created games during this session.
     */
    public static long getCreatedGameCount() {
        return gamesCreated.longValue();
    }

    /**
     * Mark this node as representing a terminal state.
     **/
    public void markTerminal() {
        isTerminal = true;
        gamesCreated.increment();
    }

    /**
     * Does this node represent a failed state?
     *
     * @return Whether this node represents a failed state (true/false).
     */
    public boolean isFailed() {
        return isFailed.get();
    }

    /**
     * Manually set that this node represents a failed state. Failure status will be automatically set when assigning
     * this node a state and does not need to be manually done. Only useful if we want to assign failure status
     * without assigning a state.
     *
     * @param failed Whether this node should represent a failed state.
     */
    public void setFailed(boolean failed) {
        isFailed.set(failed);
    }
    /***************************************************/
    /******* STATE & SEQUENCE SETTING/GETTING **********/
    /***************************************************/

    /**
     * Assign a state to this node. This state should represent the state after performing this node's action from
     * its parent's node's state.
     *
     * @param newState State to assign to this node.
     */
    public synchronized void setState(State newState) {
        state = newState;
        try {
            isFailed.set(state.isFailed());
        } catch (NullPointerException e) {
            System.out.println("WARNING: node state had no failure state assigned. This is bad unless we're just " +
                    "playing old runs back.");
        }
    }

    /**
     * Get the action object (most likely keypress + duration) that leads to this node from its parent.
     *
     * @return This node's action.
     */
    public Action getAction() {
        action.reset(); // Make sure internal counter for executing this action is reset.
        return action;
    }

    /**
     * Get the sequence of actions up to, and including this node.
     *
     * @return An array of actions which, when executed from the initial state will lead to the state at this node.
     */
    public synchronized Action[] getSequence() {
        Action[] sequence = new Action[treeDepth];
        if (treeDepth == 0) return sequence; // Empty array for root node.
        Node current = this;
        sequence[treeDepth - 1] = current.action;
        for (int i = treeDepth - 2; i >= 0; i--) {
            current = current.parent;
            sequence[i] = current.action;
        }
        return sequence;
    }

    /****************************************/
    /******* SAVE/LOAD AND UTILITY **********/
    /****************************************/

    /* Takes a list of runs and figures out the tree hierarchy without duplicate objects. Adds to an existing given
    root.
     * If trimActionAddingToDepth is >= than 0, then actions will be stripped from the imported nodes up to, and
     * including the depth specified.
     * Set to -1 or something if you don't want this.**/
    public static synchronized Node makeNodesFromRunInfo(List<SavableSingleGame> runs, Node existingRootToAddTo,
                                                         int trimActionAddingToDepth) {
        Node rootNode;
        rootNode = existingRootToAddTo;
        currentlyAddingSavedNodes = true;
        for (SavableSingleGame run : runs) { // Go through all runs, placing them in the tree.
            Node currentNode = rootNode;

            for (int i = 0; i < run.actions.length; i++) { // Iterate through individual actions of this run,
                // travelling down the tree in the process.

                boolean foundExistingMatch = false;
                for (Node child : currentNode.children) { // Look at each child of the currently investigated node.
                    if (child.action == run.actions[i]) { // If there is already a node for the action we are trying
                        // to place, just use it.
                        currentNode = child;
                        foundExistingMatch = true;
                        break; // We found a match, move on.
                    }
                }
                // If this action is unique at this point in the tree, we need to add a new node there.
                if (!foundExistingMatch) {
                    Node newNode = new Node(currentNode, run.actions[i]);
                    newNode.setState(run.states[i]);
                    if (rootNode.uncheckedActions != null) newNode.calcNodePos();
                    currentNode = newNode;
                    nodesImported.increment();
                }
            }
            gamesImported.increment();
        }
        //if (rootNode.uncheckedActions != null) {
        rootNode.propagateFullyExplored_complete(); // Handle marking the nodes which are fully explored.
        if (trimActionAddingToDepth >= 0) stripUncheckedActionsExceptOnLeaves(rootNode, trimActionAddingToDepth);
        rootNode.calcNodePosBelow();
        //}
        currentlyAddingSavedNodes = false;
        return rootNode;
    }

    /**
     * Helper for node adding from file. Clears unchecked actions from non-leaf nodes.
     * Only does it for things below minDepth. Forces new building to happen only at the boundaries of this.
     **/
    private static void stripUncheckedActionsExceptOnLeaves(Node node, int minDepth) {
        if (!node.children.isEmpty() && node.treeDepth <= minDepth) node.uncheckedActions.clear();
        for (Node child : node.children) {
            stripUncheckedActionsExceptOnLeaves(child, minDepth);
        }
    }

    /**
     * Add nodes based on saved action sequences. Has to re-simulate each to get the states.
     **/
    static void makeNodesFromActionSequences(List<Action[]> actions, Node root, GameLoader game) {

        ActionQueue actQueue = new ActionQueue();
        root.setState(GameLoader.getInitialState());

        for (Action[] acts : actions) {
            game.makeNewWorld();
            Node currNode = root;
            actQueue.clearAll();

            for (Action act : acts) {
                act.reset();

                // If there is already a node for this action, use it.
                boolean foundExisting = false;
                for (Node child : currNode.children) {
                    if (child.action.equals(act)) {
                        currNode = child;
                        foundExisting = true;
                        break;
                    }
                }

                // Otherwise, make a new one.
                if (!foundExisting) currNode = currNode.addChild(act);

                // Simulate
                actQueue.addAction(act);
                while (!actQueue.isEmpty()) {
                    boolean[] nextCommand = actQueue.pollCommand(); // Get and remove the next keypresses
                    boolean Q = nextCommand[0];
                    boolean W = nextCommand[1];
                    boolean O = nextCommand[2];
                    boolean P = nextCommand[3];
                    game.stepGame(Q, W, O, P);
                }
                currNode.setState(game.getCurrentState());
            }
        }
    }

    /************************************************/
    /*******         NODE POSITIONING      **********/
    /************************************************/

    /**
     * Vanilla node positioning from all previous versions.
     * Makes a rough best guess for position based on its parent.
     * Should still be used for initial conditions before letting
     * the physics kick in.
     **/
    private void calcNodePos(float[] nodeLocationsToAssign) {
        //Angle of this current node -- parent node's angle - half the total sweep + some increment so that all will
        // span the required sweep.
        if (treeDepth >= 0) {
            if (parent.children.size() + ((parent.uncheckedActions == null) ? 0 : parent.uncheckedActions.size()) > 1) { //Catch the div by 0
                int division = parent.children.size() + ((parent.uncheckedActions == null) ? 0 :
                        parent.uncheckedActions.size()); // Split into this many chunks.
                int childNo = parent.children.indexOf(this);

                sweepAngle = (float) Math.max((parent.sweepAngle / division) * (1 + treeDepth * 0.05f), 0.02);

                // This is to straighten out branches that are curving off to one side due to asymmetric expansion.
                // Acts like a controller to bring the angle
                // towards the angle of the first node in this branch after root.
                float angleAdj;
                Node ancestor = this;
                while (ancestor.treeDepth > 1) {
                    ancestor = ancestor.parent;
                }
                angleAdj = -0.2f * (parent.nodeAngle - ancestor.nodeAngle);


                if (childNo == 0) {
                    nodeAngle = parent.nodeAngle + angleAdj;
                } else if (childNo % 2 == 0) {
                    nodeAngle = parent.nodeAngle + sweepAngle * childNo / 2 + angleAdj;
                } else {
                    nodeAngle = parent.nodeAngle - sweepAngle * (childNo + 1) / 2 + angleAdj;
                }
            } else {
                sweepAngle = parent.sweepAngle; //Only reduce the sweep angle if the parent one had more than one child.
                nodeAngle = parent.nodeAngle;
            }
        }

        nodeLocationsToAssign[0] = (float) (parent.nodeLocation[0] + edgeLength * Math.cos(nodeAngle));
        nodeLocationsToAssign[1] = (float) (parent.nodeLocation[1] + edgeLength * Math.sin(nodeAngle));
        nodeLocationsToAssign[2] = 0f; // No out of plane stuff yet.

        // Since drawing speed is a UI bottleneck, we may want to filter out some of the points that are REALLY close.
        if (limitDrawing) {
            float xDiff;
            float yDiff;
            for (Node n : pointsToDraw) {
                xDiff = n.nodeLocation[0] - nodeLocationsToAssign[0];
                yDiff = n.nodeLocation[1] - nodeLocationsToAssign[1];
                // Actually distance squared to avoid sqrt
                float drawFilterDistance = 0.1f;
                if ((xDiff * xDiff + yDiff * yDiff) < drawFilterDistance) {
                    notDrawnForSpeed = true;
                    return; // To close. Turn it off and get out.
                }
            }
            notDrawnForSpeed = false;
            pointsToDraw.add(this);
        }
    }

    /**
     * Same, but assumes that we're talking about this node's nodeLocation
     **/
    private void calcNodePos() {
        calcNodePos(nodeLocation);
    }

    /**
     * Recalculate all node positions below this one (NOT including this one for the sake of root).
     **/
    public void calcNodePosBelow() {
        for (Node current : children) {
            current.calcNodePos();
            current.calcNodePosBelow(); // Recurse down through the tree.
        }
    }

    /*******************************/
    /******* TREE DRAWING **********/
    /*******************************/

    /**
     * Draw the line connecting this node to its parent.
     **/
    private void drawLine(GL2 gl) {
        if (treeDepth > 0 && displayLine) { // No lines for root.
            if (overrideLineColor == null) {
                gl.glColor3fv(getColorFromTreeDepth(treeDepth, lineBrightness).getColorComponents(null), 0);
                //getColorFromScaledValue(value/visitCount.floatValue()/(float)treeDepth, 20, lineBrightness)
                // .getColorComponents(null),0);
            } else {
                // The most obtuse possible way to change just the brightness of the color. Ridiculous, but I can't
                // find anything else.
                float[] color = Color.RGBtoHSB(overrideLineColor.getRed(), overrideLineColor.getGreen(),
                        overrideLineColor.getBlue(), null);
                gl.glColor3fv(Color.getHSBColor(color[0], color[1], lineBrightness).getColorComponents(null), 0);
            }
            gl.glVertex3d(parent.nodeLocation[0], parent.nodeLocation[1], parent.nodeLocation[2] + zOffset);
            gl.glVertex3d(nodeLocation[0], nodeLocation[1], nodeLocation[2] + zOffset);
        }
    }

    /**
     * Draw the node point if enabled
     **/
    private void drawPoint(GL2 gl) {
        if (displayPoint) {
            if (overrideNodeColor == null) {
                gl.glColor3fv(nodeColor.getColorComponents(null), 0);
            } else {
                gl.glColor3fv(overrideNodeColor.getColorComponents(null), 0);
            }
            gl.glVertex3d(nodeLocation[0], nodeLocation[1], nodeLocation[2] + zOffset);
        }
    }


    /**
     * Draw all lines in the subtree below this node
     **/
    public void drawLines_below(GL2 gl) {
        for (Node current : children) {
            if (!current.notDrawnForSpeed) current.drawLine(gl);
            if (current.treeDepth <= this.treeDepth)
                throw new RuntimeException("Node hierarchy problem. Node with an " +
                        "equal or lesser depth is below another. At " + current.treeDepth + " and " + this.treeDepth + ".");
            current.drawLines_below(gl); // Recurse down through the tree.
        }
    }

    /**
     * Draw all nodes in the subtree below this node.
     **/
    public void drawNodes_below(GL2 gl) {
        if (!notDrawnForSpeed) drawPoint(gl);
        for (Node child : children) {
            child.drawPoint(gl);
            child.drawNodes_below(gl); // Recurse down through the tree.
        }
    }

    /**
     * Turn off all display for this node onward.
     **/
    public void turnOffBranchDisplay() {
        displayLine = false;
        displayPoint = false;
        notDrawnForSpeed = false;
        pointsToDraw.remove(this);

        for (Node child : children) {
            child.turnOffBranchDisplay();
        }
    }

    /**
     * Single out one run up to this node to highlight the lines, while dimming the others.
     **/
    public void highlightSingleRunToThisNode() {
        Node rt = getRoot();
        rt.setLineBrightness_below(0.4f); // Fade the entire tree, then go and highlight the run we care about.

        Node currentNode = this;
        while (currentNode.treeDepth > 0) {
            currentNode.setLineBrightness(0.85f);
            currentNode = currentNode.parent;
        }
    }

    /**
     * Fade a single line going from this node to its parent.
     **/
    private void setLineBrightness(float brightness) {
        lineBrightness = brightness;
    }

    /**
     * Fade a certain part of the tree.
     **/
    private void setLineBrightness_below(float brightness) {
        setLineBrightness(brightness);
        for (Node child : children) {
            child.setLineBrightness_below(brightness);
        }
    }

    /**
     * Reset line brightnesses to default.
     **/
    public void resetLineBrightness_below() {
        setLineBrightness_below(lineBrightness_default);
    }

    /**
     * Color the node scaled by depth in the tree. Skip the brightness argument for default value.
     **/
    private static Color getColorFromTreeDepth(int depth, float brightness) {
        float colorOffset = 0.35f;
        float scaledDepth = (float) depth / (float) maxDepthYet;
        float H = scaledDepth * 0.38f + colorOffset;
        float S = 0.8f;
        return Color.getHSBColor(H, S, brightness);
    }

    /**
     * Color the node scaled by depth in the tree. Totally for gradient pleasantness.
     **/
    public static Color getColorFromTreeDepth(int depth) {
        return getColorFromTreeDepth(depth, lineBrightness_default);
    }

    public static Color getColorFromScaledValue(float val, float max, float brightness) {
        float colorOffset = 0.35f;
        float scaledDepth = val / max;
        float H = scaledDepth * 0.38f + colorOffset;
        float S = 0.8f;
        return Color.getHSBColor(H, S, brightness);
    }

    /**
     * Set an override line color for this branch (all descendants).
     **/
    public void setBranchColor(Color newColor) {
        overrideLineColor = newColor;
        for (Node child : children) {
            child.setBranchColor(newColor);
        }
    }

    /**
     * Set an override line color for this path (all ancestors).
     **/
    public void setBackwardsBranchColor(Color newColor) {
        overrideLineColor = newColor;
        if (treeDepth > 0) {
            parent.setBackwardsBranchColor(newColor);
        }
    }

    /**
     * Clear an overridden line color on this branch. Call from root to get all line colors back to default.
     **/
    public void clearBranchColor() {
        overrideLineColor = null;
        for (Node child : children) {
            child.clearBranchColor();
        }
    }

    /**
     * Clear an overridden line color on this branch. Goes back towards root.
     **/
    public void clearBackwardsBranchColor() {
        overrideLineColor = null;
        if (treeDepth > 0) {
            parent.clearBackwardsBranchColor();
        }
    }

    /**
     * Clear node override colors from this node onward. Only clear the specified color. Call from root to clear all.
     **/
    private void clearNodeOverrideColor(Color colorToClear) {
        if (overrideNodeColor == colorToClear) {
            overrideNodeColor = null;
            displayPoint = false;
        }
        for (Node child : children) {
            child.clearNodeOverrideColor(colorToClear);
        }
    }

    /**
     * Clear node override colors from this node backwards. Only clear the specified color. Goes towards root.
     **/
    private void clearBackwardsNodeOverrideColor(Color colorToClear) {
        if (overrideNodeColor == colorToClear) {
            overrideNodeColor = null;
            displayPoint = false;
        }
        if (treeDepth > 0) {
            parent.clearBackwardsNodeOverrideColor(colorToClear);
        }
    }

    /**
     * Clear all node override colors from this node onward. Call from root to clear all.
     **/
    public void clearNodeOverrideColor() {
        if (overrideNodeColor != null) {
            overrideNodeColor = null;
            displayPoint = false;
        }
        for (Node child : children) {
            child.clearNodeOverrideColor();
        }
    }

    /**
     * Clear all node override colors from this node onward. Call from root to clear all.
     **/
    private void clearBackwardsNodeOverrideColor() {
        if (overrideNodeColor != null) {
            overrideNodeColor = null;
            displayPoint = false;
        }
        if (treeDepth > 0) {
            parent.clearBackwardsNodeOverrideColor();
        }
    }

    /**
     * Give this branch a zOffset to make it stand out.
     **/
    public void setBranchZOffset(float zOffset) {
        this.zOffset = zOffset;
        for (Node child : children) {
            child.setBranchZOffset(zOffset);
        }
    }

    /**
     * Give this branch a zOffset to make it stand out. Goes backwards towards root.
     **/
    public void setBackwardsBranchZOffset(float zOffset) {
        this.zOffset = zOffset;
        Node currentNode = this;
        while (currentNode.treeDepth > 0) {
            currentNode.zOffset = zOffset;
            currentNode = currentNode.parent;
        }
    }

    /**
     * Clear z offsets in this branch. Works backwards towards root.
     **/
    public void clearBackwardsBranchZOffset() {
        setBackwardsBranchZOffset(0f);
    }

    /**
     * Clear z offsets in this branch. Called from root, it resets all back to 0.
     **/
    public void clearBranchZOffset() {
        setBranchZOffset(0f);
    }
}
