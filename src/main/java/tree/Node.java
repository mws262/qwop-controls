package tree;

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
import java.util.function.Consumer;

import actions.Action;
import actions.ActionQueue;
import actions.ActionSet;
import actions.IActionGenerator;
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
     * Node which leads up to this node. Parentage should not be changed externally.
     */
    private Node parent;

    /**
     * Child nodes. Not fixed size any more.
     */
    private final List<Node> children = new CopyOnWriteArrayList<>();

    /**
     * Action which takes the game from the parent node's state to this node's state.
     */
    public final Action action;

    /**
     * State after taking this node's action from the parent node's state.
     */
    private State state;

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
     */
    public AtomicLong visitCount = new AtomicLong();

    /**
     * Total number of nodes created this execution.
     */
    private static final LongAdder nodesCreated = new LongAdder();

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
     * functionality a little more like the old version which selected from a fixed pool of durations for each action
     * in the sequence. This time, the action generator can use anything arbitrary to decide what the potential
     * children are, and the potentialActionGenerator can be hot swapped at any point.
     */
    public static IActionGenerator potentialActionGenerator;

    /**
     * Untried child actions.
     */
    public ActionSet uncheckedActions;

    /**
     * Are there any untried things below this node? This is not necessarily a TERMINAL node, it is simply a node
     * past which there are no potential child actions to try.
     */
    public final AtomicBoolean fullyExplored = new AtomicBoolean(false);

    /**
     * If one TreeWorker is expanding from this leaf node (if it is one), then no other worker should try to
     * simultaneously expand from here too.
     */
    private final AtomicBoolean locked = new AtomicBoolean(false);

    /**
     * Depth of this node in the tree. Root node is 0; its children are 1, etc.
     */
    private final int treeDepth;

    /**
     * Maximum depth yet seen anywhere in this tree.
     */
    private static int maxDepthYet = 1;

    /**
     * Should a round dot be displayed at each node? The color will be determined by {@link Node#nodeColor} or
     * {@link Node#overrideNodeColor}, if set. By default, only lines are shown.
     */
    public boolean displayPoint = false;

    /**
     * Should lines be shown between connected nodes? The color will be determined by tree depth from
     * {@link Node#getColorFromTreeDepth(int)} or {@link Node#overrideLineColor}, if set.
     */
    private boolean displayLine = true;

    /**
     * Tree node connection line color override. Will override the default gradient coloring with this color if not
     * null. If null, the default depth-based coloring will be used. No lines drawn regardless if {@link Node#displayLine} is
     * false.
     */
    public Color overrideLineColor = null;

    /**
     * Tree node dot color override. Will override the default node color if this field is not null and
     * {@link Node#displayPoint} is true.
     * is true.
     */
    public Color overrideNodeColor = null; // Only set when the color is overridden.

    /**
     * Default node point color. Will not be drawn if {@link Node#displayPoint} is not true. Can also be overridden
     * by {@link Node#overrideNodeColor}.
     */
    public Color nodeColor = Color.GREEN;

    private static final float lineBrightness_default = 0.85f;
    private float lineBrightness = lineBrightness_default;

    // TODO Make these drawing "filters" more clear, and make access to them uniform.

    private static final boolean debugDrawNodeLocking = true; // Draw which nodes are locked by command from the
    // TreeWorkers.
    private static final Color lockColor = new Color(194, 148, 184);

    public String nodeLabel = "";

    /**
     * Determines whether very close lines/nodes will be drawn. Can greatly speed up UI for very dense trees.
     */
    private static final boolean limitDrawing = true;
    public static final Set<Node> pointsToDraw = ConcurrentHashMap.newKeySet();

    /**
     * Specifies whether this node has been ignored for drawing purposes. This only gets manipulated if
     * {@link Node#limitDrawing} is true.
     */
    private boolean notDrawnForSpeed = false;
    public static boolean includeZOffsetInDrawFiltering = false;
    private static final float nodeDrawFilterDistSq = 0.1f;
    // Disable node position calculations (for when running headless)
    private static final boolean calculateNodeVisPositions = true;

    /**
     * Parameters for visualizing the tree
     */
    public float[] nodeLocation = new float[3]; // Location that this node appears on the tree visualization
    private float nodeAngle = 0; // Keep track of the angle that the line previous node to this node makes.
    private float sweepAngle = 2f * (float) Math.PI;

    private float edgeLength = 1.f;

    /**
     * If we want to bring out a certain part of the tree so it doesn't hide under other parts, give it a small z
     * offset.
     */
    float nodeLocationZOffset = 0.f;

    /**
     * Are we bulk adding saved nodes? If so, some construction things are deferred.
     */
    private static boolean currentlyAddingSavedNodes = false;

    /**
     * Make a new node which is NOT the root. Add this to the tree hierarchy.
     */
    public Node(Node parent, Action action) {
        this(parent, action, true);
    }

    /**
     * Make a new node which is NOT the root. connectNodeToTree is the default.
     *
     * @param parent            Parent node of this new node to be created.
     * @param action            Action associated with the new node which takes the parent node's state to this node's state.
     * @param connectNodeToTree Whether this node is connected to the tree, i.e. does the parent node know about this
     *                          child.
     * @throws IllegalArgumentException Trying to add a node with the same action as another in the parent.
     */
    public Node(Node parent, Action action, boolean connectNodeToTree) {
        this.parent = parent;
        treeDepth = parent.getTreeDepth() + 1;
        this.action = action;

        // Error check for duplicate actions.
        for (Node parentChildren : parent.children) {
            if (parentChildren.action == action) {
                throw new IllegalArgumentException("Tried to add a duplicate action node at depth " + getTreeDepth() + ". Action " +
                        "was: " + action.toString() + ".");
            }
        }
        // Add some child actions to try if an action generator is assigned.
        autoAddUncheckedActions();

        edgeLength = 5.00f * (float) Math.pow(0.6947, 0.1903 * getTreeDepth()) + 1.5f;

        lineBrightness = parent.lineBrightness;
        nodeLocationZOffset = parent.nodeLocationZOffset;

        // Should only add to parent's list after everything else has been done to prevent half-initialized nodes
        // from being used by other workers.
        if (connectNodeToTree) {
            parent.children.add(this);
            if (getTreeDepth() > maxDepthYet) {
                maxDepthYet = getTreeDepth();
            }

            // Update max branch depth
            maxBranchDepth.set(getTreeDepth());
            Node currentNode = this;
            while (currentNode.getTreeDepth() > 0 && currentNode.parent.maxBranchDepth.get() < currentNode.maxBranchDepth.get()) {
                currentNode.parent.maxBranchDepth.set(currentNode.maxBranchDepth.get());
                currentNode = currentNode.parent;
            }
        }

        if (connectNodeToTree && !currentlyAddingSavedNodes && calculateNodeVisPositions) {
            calcNodePos(); // Must be called after this node has been added to its parent's list!
        }
        nodesCreated.increment();
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

        nodesCreated.increment();
    }

    /**
     * Add a new child node from a given action. If the action is in uncheckedActions, remove it.
     *
     * @param childAction The action which defines a child node, i.e. it takes this node's state to the new child's
     *                    state.
     * @return A new child node defined by the provided action.
     */
    public synchronized Node addChild(Action childAction) {
        if (uncheckedActions != null)
            uncheckedActions.removeIf(action -> action.equals(childAction));
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
     * children and locked children. For all practical purposes, this node is also out of play. Lock it too and
     * recurse up the tree until we reach a node with at least one unlocked and not fully explored child.
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
            // Trying to see if we can remove this check. A smart sampler shouldn't get itself tied up this way anyway.
//            if (uncheckedActions.isEmpty()) // No child actions remain to sample. FIXME: This shouldn't actually
//                // occur. Replace with an exception.
//                return false;
            locked.set(true);

            if (debugDrawNodeLocking) { // For highlighting nodes which are locked in the UI, if desired.
                displayPoint = true;
                overrideNodeColor = lockColor;
            }

            // May need to add locks to nodes further up the tree towards root. For example, if calling
            // reserveExpansionRights locks the final available node of this node's parent, then the parent should be
            // locked off too. This effect can chain all the way up the tree towards the root.
            if (getTreeDepth() > 0) parent.propagateLock();

            return true;
        }
    }

    /**
     * Set a flag to indicate that the invoking TreeWorker has released exclusive rights to expand from this node.
     */
    public synchronized void releaseExpansionRights() {
        locked.set(false); // Release the lock.
        if (debugDrawNodeLocking) { // Stop drawing red dots for locked nodes, if this is on.
            displayPoint = false;
            overrideNodeColor = null;
        }
        // Unlocking this node may cause nodes further up the tree to become available.
        if ((getTreeDepth() > 0) && (parent != null)) parent.propagateUnlock();
    }

    /**
     * Locking one node may cause some parent nodes to become unavailable also. propagateLock will check as far up
     * the tree towards the root node as necessary.
     */
    private synchronized void propagateLock() {
        // Lock this node unless we find evidence that we don't need to.
        for (Node child : children) {
            if (!child.isLocked() && !child.fullyExplored.get()) {
                return; // In this case, we don't need to continue locking things further up the tree.
            }
        }
        reserveExpansionRights();
    }

    /**
     * Releasing one node's lock may make others further up the tree towards the root node become available.
     */
    private synchronized void propagateUnlock() {
        if (!isLocked()) return; // We've worked our way up to a node which is already not locked. No need to
        // propagate further.

        // A single free child means we can unlock this node.
        for (Node child : children) {
            if (!child.isLocked()) {  // Does not need to stay locked.
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
    public boolean isLocked() {
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
     * @return A copy of the list of children of this node. Removing the nodes from this array will not remove them
     * from this node's actual children, but the nodes in the array are the originals.
     */
    public Node[] getChildren() {
        return children.toArray(new Node[0]);
    }

    /**
     * Get the index of this node in it's parent list of nodes. Hence, parent.children.get(index) == this.
     *
     * @return This node's index in its parent's list of nodes.
     * @throws IndexOutOfBoundsException When called on a root node (depth 0).
     */
    public int getIndexAccordingToParent() {
        if (treeDepth == 0)
            throw new IndexOutOfBoundsException("The root node has no parent, and thus this method call doesn't make " +
                    "sense.");
        return parent.children.indexOf(this);
    }

    /**
     * Get the number of siblings, i.e. other nodes in this node's parent's list of children. Does not include this
     * node itself.
     *
     * @return Number of sibling nodes of this one.
     */
    public int getSiblingCount() {
        if (treeDepth == 0) return 0; // Root node has no siblings.
        int siblings = parent.children.size() - 1;
        assert siblings >= 0;
        return siblings;
    }

    /**
     * Get the total number of children of this node. Only includes actually created children, not potential children
     * . Only includes direct children, not all descendants.
     *
     * @return Total created children of this node.
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Get a created child node of this node by its index (order of creation).
     *
     * @param childIndex Index of the child node to retrieve.
     * @return A child of this node, with the index as specified.
     */
    public Node getChildByIndex(int childIndex) {
        return children.get(childIndex);
    }

    /**
     * Get a random already-created child of this node. Can be useful for sampling.
     *
     * @return A random child node of this node.
     * @throws IndexOutOfBoundsException Has no children.
     */
    public Node getRandomChild() {
        if (children.isEmpty())
            throw new IndexOutOfBoundsException("Tried to get a random child from a node with no children.");
        return children.get(Utility.randInt(0, children.size() - 1));
    }

    /**
     * Add all the nodes below and including this one to a list. Does not include nodes whose state have not yet been
     * assigned.
     *
     * @param nodeList                  A list to add all of this branches' nodes to. This list must be caller-provided, and will not
     *                                  be cleared.
     * @param includeOnlyNodesWithState If true, this will only get nodes which have a state assigned to them.
     * @return Returns the list of nodes below. This is done in place, so the object is the same as the argument one.
     */
    public List<Node> getNodesBelow(List<Node> nodeList, boolean includeOnlyNodesWithState) {
        if (!includeOnlyNodesWithState) { // Include regardless.
            nodeList.add(this);
        } else if (!isStateUnassigned()) { // Include only if state is assigned.
            nodeList.add(this);
        }
        for (Node child : children) {
            child.getNodesBelow(nodeList, includeOnlyNodesWithState);
        }
        return nodeList;
    }


    /**
     * Get a list of all tree endpoints (leaves) below this node, i.e. on this branch. If called from a leaf, the
     * list will only contain that leaf itself.
     *
     * @param leaves A list of leaves below this node. The list must be provided by the caller, and will not be
     *               cleared by this method.
     * @return Returns the list of nodes below. This is done in place, so the object is the same as the argument one.
     */
    public List<Node> getLeaves(List<Node> leaves) {

        if (children.isEmpty()) { // If leaf, add itself.
            leaves.add(this);
        } else { // Otherwise keep traversing down.
            for (Node child : children) {
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
        while (currentNode.getTreeDepth() > 0) {
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
        if (possibleAncestorNode.getTreeDepth() >= this.getTreeDepth()) { // Don't need to check if this is as far down the tree.
            return false;
        }
        Node currNode = parent;
        while (currNode.getTreeDepth() != possibleAncestorNode.getTreeDepth()) { // Find the node at the same depth as the one
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
    public void propagateFullyExploredStatus_lite() {
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

        if (getTreeDepth() > 0) { // We already know this node is fully explored, check the parent.
            parent.propagateFullyExploredStatus_lite();
        }
    }

    /**
     * Change whether this node or any above it have become fully explored. This is the complete version, which
     * resets any existing fully-explored tags from the descendants of this node before redoing all checks. Call from
     * root to re-label the whole tree. During normal tree-building, a {@link Node#propagateFullyExploredStatus_lite()
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
            while (currNode.getTreeDepth() > getTreeDepth()) {
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
     */
    public void destroyNodesBelowAndCheckExplored() {
        destroyNodesBelow();
        propagateFullyExploredStatus_lite();
    }

    /**
     * Try to de-reference everything on this branch so garbage collection throws out all the state values and other
     * info stored for this branch to keep memory in check.
     */
    public void destroyNodesBelow() {
        Node[] childrenCopy = children.toArray(new Node[children.size()]);
        children.clear();

        for (Node child : childrenCopy) {
            pointsToDraw.remove(child);
            child.state = null;
            child.parent = null;
            child.nodeLocation = null;
            child.destroyNodesBelow();
        }
        children.clear();
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

    /**
     * Assign a state to this node. This state should represent the state after performing this node's action from
     * its parent's node's state.
     *
     * @param newState State to assign to this node.
     * @throws IllegalStateException Trying to assign a state to this node when one is already assigned.
     */
    public synchronized void setState(State newState) {
        if (state != null)
            throw new IllegalStateException("Trying to assign a node's state after a state has been previously " +
                    "assigned. Examine behavior carefully before allowing this.");
        state = newState;

        try {
            isFailed.set(state.isFailed());
        } catch (NullPointerException e) {
            System.out.println("WARNING: node state had no failure state assigned. This is bad unless we're just " +
                    "playing old runs back.");
        }
    }

    /**
     * Get the game state associated with this node. Represents the state achieved from the parent node's state after
     * performing the action in this node.
     *
     * @return Game state at this node.
     * @throws NullPointerException If state is unassigned.
     */
    public State getState() {
        if (state == null)
            throw new NullPointerException("Node state unassigned. Call isStateUnassigned first to check.");
        return state;
    }

    /**
     * Check whether a game state has been assigned to this node.
     *
     * @return Whether the game state is assigned or not. True means not yet assigned.
     */
    public boolean isStateUnassigned() {
        return state == null;
    }

    /**
     * Get the action object (most likely keypress + duration) that leads to this node from its parent.
     *
     * @return This node's action.
     * @throws NullPointerException When called on a root node (tree depth of 0).
     */
    public Action getAction() {
        if (treeDepth == 0) // Root has no action
            throw new NullPointerException("Root node does not have an action associated with it.");
        return action;
    }

    /**
     * Get the sequence of actions up to, and including this node.
     *
     * @return An array of actions which, when executed from the initial state will lead to the state at this node.
     * @throws IndexOutOfBoundsException If called on the root node.
     */
    public synchronized Action[] getSequence() {
        Action[] sequence = new Action[getTreeDepth()];
        if (getTreeDepth() == 0)
            throw new IndexOutOfBoundsException("Cannot get a sequence at the root node, since it has no actions " +
                    "leading up to it.");

        Node current = this;
        sequence[getTreeDepth() - 1] = current.getAction();
        for (int i = getTreeDepth() - 2; i >= 0; i--) {
            current = current.parent;
            sequence[i] = current.getAction();
        }
        return sequence;
    }

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
     */
    private static void stripUncheckedActionsExceptOnLeaves(Node node, int minDepth) {
        if (!node.children.isEmpty() && node.getTreeDepth() <= minDepth) node.uncheckedActions.clear();
        for (Node child : node.children) {
            stripUncheckedActionsExceptOnLeaves(child, minDepth);
        }
    }

    /**
     * Add nodes based on saved action sequences. Has to re-simulate each to get the states.
     */
    public static void makeNodesFromActionSequences(List<Action[]> actions, Node root, GameLoader game) {

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

    /**
     * Vanilla node positioning from all previous versions.
     * Makes a rough best guess for position based on its parent.
     * Should still be used for initial conditions before letting
     * the physics kick in.
     */
    private void calcNodePos(float[] nodeLocationsToAssign) {
        //Angle of this current node -- parent node's angle - half the total sweep + some increment so that all will
        // span the required sweep.
        if (getTreeDepth() >= 0) {
            if (parent.children.size() + ((parent.uncheckedActions == null) ? 0 : parent.uncheckedActions.size()) > 1) { //Catch the div by 0
                int division = parent.children.size() + ((parent.uncheckedActions == null) ? 0 :
                        parent.uncheckedActions.size()); // Split into this many chunks.
                int childNo = parent.children.indexOf(this);

                sweepAngle = (float) Math.max((parent.sweepAngle / division) * (1 + getTreeDepth() * 0.05f), 0.005);

                // This is to straighten out branches that are curving off to one side due to asymmetric expansion.
                // Acts like a controller to bring the angle
                // towards the angle of the first node in this branch after root.
                float angleAdj;
                Node ancestor = this;
                while (ancestor.getTreeDepth() > 1) {
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
            float zDiff;
            for (Node n : pointsToDraw) {
                xDiff = n.nodeLocation[0] - nodeLocationsToAssign[0];
                yDiff = n.nodeLocation[1] - nodeLocationsToAssign[1];
                zDiff = n.nodeLocation[2] - nodeLocationsToAssign[2] + (Node.includeZOffsetInDrawFiltering ?
                        n.nodeLocationZOffset - nodeLocationZOffset : 0);
                // Actually distance squared to avoid sqrt
                if ((xDiff * xDiff + yDiff * yDiff + zDiff * zDiff) < nodeDrawFilterDistSq) {
                    notDrawnForSpeed = true;
                    return; // Too close. Turn it off and get out.
                }
            }
            notDrawnForSpeed = false;
            pointsToDraw.add(this);
        }
    }

    /**
     * Same, but assumes that we're talking about this node's nodeLocation
     */
    private void calcNodePos() {
        calcNodePos(nodeLocation);
    }

    /**
     * Recalculate all node positions below this one (NOT including this one for the sake of root).
     */
    public void calcNodePosBelow() {
        for (Node current : children) {
            current.calcNodePos();
            current.calcNodePosBelow(); // Recurse down through the tree.
        }
    }

    /**
     * Resets the angle at which child nodes will fan out from this node in the visualization. This must be done
     * before the children are created. The angle is set to 3pi/2.
     */
    public void resetSweepAngle() {
        sweepAngle = (float)(3.*Math.PI/2.);
    }

    /**
     * Update which nodes are being drawn based on a distance threshold from other nodes. This is usually done
     * automatically, but sometimes it's nice to aggressively prune areas of the tree which aren't important any more.
     * @param threshold Distance squared threshold. Nodes closer to another drawn node than this threshold are not
     *                  drawn.
     */
    public void postPruneDrawingBelow(float threshold) {
        float xDiff;
        float yDiff;
        float zDiff;

        pointsToDraw.remove(this);
        notDrawnForSpeed = false;

        for (Node n : pointsToDraw) {
            xDiff = n.nodeLocation[0] - nodeLocation[0];
            yDiff = n.nodeLocation[1] - nodeLocation[1];
            zDiff = n.nodeLocation[2] - nodeLocation[2] + (Node.includeZOffsetInDrawFiltering ?
                    n.nodeLocationZOffset - nodeLocationZOffset : 0);

            // Actually distance squared to avoid sqrt.
            if ((xDiff * xDiff + yDiff * yDiff + zDiff * zDiff) < threshold) {
                notDrawnForSpeed = true;
                break;
            }
        }

        if (!notDrawnForSpeed)
            pointsToDraw.add(this);

        for (Node child : children) {
            child.postPruneDrawingBelow(threshold);
        }
    }

    /**
     * Draw the line connecting this node to its parent.
     */
    private void drawLine(GL2 gl) {
        if ((getTreeDepth() > 0) && displayLine && (parent != null)) { // No lines for root.
            if (overrideLineColor == null) {
                gl.glColor3fv(getColorFromTreeDepth(getTreeDepth(), lineBrightness).getColorComponents(null), 0);
            } else {
                // The most obtuse possible way to change just the brightness of the color. Ridiculous, but I can't
                // find anything else.
                float[] color = Color.RGBtoHSB(overrideLineColor.getRed(), overrideLineColor.getGreen(),
                        overrideLineColor.getBlue(), null);
                gl.glColor3fv(Color.getHSBColor(color[0], color[1], lineBrightness).getColorComponents(null), 0);
            }
            gl.glVertex3d(parent.nodeLocation[0], parent.nodeLocation[1], parent.nodeLocation[2] + nodeLocationZOffset);
            gl.glVertex3d(nodeLocation[0], nodeLocation[1], nodeLocation[2] + nodeLocationZOffset);
        }
    }

    /**
     * Draw the node point if enabled
     */
    private void drawPoint(GL2 gl) {
        if (displayPoint) {
            if (overrideNodeColor == null) {
                gl.glColor3fv(nodeColor.getColorComponents(null), 0);
            } else {
                gl.glColor3fv(overrideNodeColor.getColorComponents(null), 0);
            }
            gl.glVertex3d(nodeLocation[0], nodeLocation[1], nodeLocation[2] + nodeLocationZOffset);
        }
    }

    /**
     * Draw all lines in the subtree below this node.
     *
     * @param gl OpenGL drawing object.
     */
    public void drawLines_below(GL2 gl) {
        for (Node current : children) {
            if (!current.notDrawnForSpeed) current.drawLine(gl);
            assert current.getTreeDepth() > this.getTreeDepth(); // Make sure that the children are further down the
            // tree.
            current.drawLines_below(gl); // Recurse down through the tree.
        }
    }

    /**
     * Draw all nodes in the subtree below this node.
     *
     * @param gl OpenGL drawing object.
     */
    public void drawNodes_below(GL2 gl) {
        if (!notDrawnForSpeed) drawPoint(gl);
        for (Node child : children) {
            child.drawPoint(gl);
            child.drawNodes_below(gl); // Recurse down through the tree.
        }
    }

    /**
     * Turn off all display for this node onward.
     */
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
     */
    public void highlightSingleRunToThisNode() {
        Node rt = getRoot();
        rt.setLineBrightness_below(0.4f); // Fade the entire tree, then go and highlight the run we care about.

        Node currentNode = this;
        while (currentNode.getTreeDepth() > 0) {
            currentNode.setLineBrightness(lineBrightness_default);
            currentNode = currentNode.parent;
        }
    }

    /**
     * Fade a single line going from this node to its parent.
     */
    private void setLineBrightness(float brightness) {
        lineBrightness = brightness;
    }

    /**
     * Fade a certain part of the tree.
     */
    public void setLineBrightness_below(float brightness) {
        setLineBrightness(brightness);
        for (Node child : children) {
            child.setLineBrightness_below(brightness);
        }
    }

    /**
     * Reset line brightnesses to default.
     */
    public void resetLineBrightness_below() {
        setLineBrightness_below(lineBrightness_default);
    }

    /**
     * Color the node scaled by depth in the tree. Skip the brightness argument for default value.
     */
    private static Color getColorFromTreeDepth(int depth, float brightness) {
        float colorOffset = 0.35f;
        float scaledDepth = (float) depth / (float) maxDepthYet;
        float H = scaledDepth * 0.38f + colorOffset;
        float S = 0.8f;
        return Color.getHSBColor(H, S, brightness);
    }

    /**
     * Color the node scaled by depth in the tree. Totally for gradient pleasantness.
     */
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
     */
    public void setBranchColor(Color newColor) {
        overrideLineColor = newColor;
        for (Node child : children) {
            child.setBranchColor(newColor);
        }
    }

    /**
     * Set an override line color for this path (all ancestors).
     */
    public void setBackwardsBranchColor(Color newColor) {
        overrideLineColor = newColor;
        if (getTreeDepth() > 0) {
            parent.setBackwardsBranchColor(newColor);
        }
    }

    /**
     * Clear an overridden line color on this branch. Call from root to get all line colors back to default.
     */
    public void clearBranchColor() {
        overrideLineColor = null;
        for (Node child : children) {
            child.clearBranchColor();
        }
    }

    /**
     * Clear an overridden line color on this branch. Goes back towards root.
     */
    public void clearBackwardsBranchColor() {
        overrideLineColor = null;
        if (getTreeDepth() > 0) {
            parent.clearBackwardsBranchColor();
        }
    }

    /**
     * Clear node override colors from this node onward. Only clear the specified color. Call from root to clear all.
     */
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
     */
    private void clearBackwardsNodeOverrideColor(Color colorToClear) {
        if (overrideNodeColor == colorToClear) {
            overrideNodeColor = null;
            displayPoint = false;
        }
        if (getTreeDepth() > 0) {
            parent.clearBackwardsNodeOverrideColor(colorToClear);
        }
    }

    /**
     * Clear all node override colors from this node onward. Call from root to clear all.
     */
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
     */
    private void clearBackwardsNodeOverrideColor() {
        if (overrideNodeColor != null) {
            overrideNodeColor = null;
            displayPoint = false;
        }
        if (getTreeDepth() > 0) {
            parent.clearBackwardsNodeOverrideColor();
        }
    }

    /**
     * Give this branch a nodeLocationZOffset to make it stand out.
     */
    public void setBranchZOffset(float zOffset) {
        this.nodeLocationZOffset = zOffset;
        for (Node child : children) {
            child.setBranchZOffset(zOffset);
        }
    }

    /**
     * Give this branch a nodeLocationZOffset to make it stand out. Goes backwards towards root.
     */
    public void setBackwardsBranchZOffset(float zOffset) {
        this.nodeLocationZOffset = zOffset;

        if (treeDepth > 0) {
            parent.setBackwardsBranchZOffset(zOffset);
        }
    }

    /**
     * Clear z offsets in this branch. Works backwards towards root.
     */
    public void clearBackwardsBranchZOffset() {
        setBackwardsBranchZOffset(0f);
    }

    /**
     * Clear z offsets in this branch. Called from root, it resets all back to 0.
     */
    public void clearBranchZOffset() {
        setBranchZOffset(0f);
    }

    /**
     * How deep is this node down the tree? 0 is root.
     */
    public int getTreeDepth() {
        return treeDepth;
    }

    /**
     * Can pass a lambda to recurse down the tree. Will include the node called.
     * @param operation Lambda to run on all the nodes in the branch below and including the node called upon.
     */
    public void recurseOnTreeInclusive(Consumer<Node> operation) {
        operation.accept(this);
        for (Node child : children) {
            child.recurseOnTreeInclusive(operation);
        }
    }

    /**
     * Can pass a lambda to recurse down the tree. Will NOT include the node called.
     * @param operation Lambda to run on all the nodes in the branch below and excluding the node called upon.
     */
    public void recurseOnTreeExclusive(Consumer<Node> operation) {
        for (Node child : children) {
            operation.accept(child);
            child.recurseOnTreeExclusive(operation);
        }
    }
}
