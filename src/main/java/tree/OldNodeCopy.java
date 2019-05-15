//package tree;
//
//import actions.Action;
//import actions.ActionList;
//import actions.ActionQueue;
//import actions.IActionGenerator;
//import com.jogamp.opengl.GL2;
//import data.SavableSingleGame;
//import game.GameUnified;
//import game.IGame;
//import game.State;
//
//import java.awt.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.concurrent.atomic.LongAdder;
//import java.util.function.Consumer;
//
///**
// * Representation of a node in a tree of actions for QWOP. Most of the tree management stuff is handled by recursing
// * through up and down the linked connections of nodes. All nodes past the root node contain a game state and the
// * single action since the previous node taken to get there. The root node is the same, except it has only a state
// * and not an action associated with it.
// * <p>
// * The convention "up the tree" is used to mean towards the root node, or towards lower tree depth. "Down the tree"
// * means towards the leaves, or to greater tree depth.
// *
// * @author matt
// */
//
//public class OldNodeCopy implements INode {
//
//    /**
//     * True if this node represents a failed state.
//     */
//    private final AtomicBoolean isFailed = new AtomicBoolean();
//
//    /**
//     * A "score" or value associated with this node's performance. The usage of this field is up to the sampler.
//     */
//    private float value = 0;
//
//    /**
//     * Some sampling methods want to track how many times this node has been visited during tree sampling.
//     */
//    public AtomicLong visitCount = new AtomicLong();
//
//    /**
//     * Total number of nodes created this execution.
//     */
//    private static final LongAdder nodesCreated = new LongAdder();
//
//    /**
//     * Total number of nodes imported from a save file.
//     */
//    private static final LongAdder nodesImported = new LongAdder();
//
//    /**
//     * Total number of games imported from a save file.
//     */
//    private static final LongAdder gamesImported = new LongAdder();
//
//    /**
//     * Maximum depth anywhere below this node in the tree. The tree root is 0.
//     */
//    public final AtomicInteger maxBranchDepth = new AtomicInteger();
//
//    /**
//     * Depth of this node in the tree. Root node is 0; its children are 1, etc.
//     */
//    private final int treeDepth;
//
//    /**
//     * Maximum depth yet seen anywhere in this tree.
//     */
//    private static int maxDepthYet = 1;
//
//
//    /**
//     * Tree node connection line color override. Will override the default gradient coloring with this color if not
//     * null. If null, the default depth-based coloring will be used. No lines drawn regardless if {@link OldNodeCopy#displayLine} is
//     * false.
//     */
//    public Color overrideLineColor = null;
//
//    /**
//     * Tree node dot color override. Will override the default node color if this field is not null and
//     * {@link OldNodeCopy#displayPoint} is true.
//     * is true.
//     */
//    public Color overrideNodeColor = null; // Only set when the color is overridden.
//
//    /**
//     * Default node point color. Will not be drawn if {@link OldNodeCopy#displayPoint} is not true. Can also be overridden
//     * by {@link OldNodeCopy#overrideNodeColor}.
//     */
//
//    public boolean nodeColorValue = false;
//    public static float maxVal = -Float.MAX_VALUE;
//    public static float minVal = Float.MAX_VALUE;
//
//    private static final float lineBrightness_default = 0.85f;
//    private float lineBrightness = lineBrightness_default;
//
//    public String nodeLabel = "";
//    public String nodeLabelAlt = "";
//
//
//    public static final Set<OldNodeCopy> pointsToDraw = ConcurrentHashMap.newKeySet(10000);
//
//    // Disable node position calculations (for when running headless)
//    private static final boolean calculateNodeVisPositions = true;
//
//    /**
//     * Parameters for visualizing the tree
//     */
//
//
//
//
//    /**
//     * Are we bulk adding saved nodes? If so, some construction things are deferred.
//     */
//    private static boolean currentlyAddingSavedNodes = false;
//
//    /**
//     * Make a new node which is NOT the root. Add this to the tree hierarchy.
//     */
//    public OldNodeCopy(OldNodeCopy parent, Action action) {
//        this(parent, action, true);
//    }
//
//    /**
//     * Make a new node which is NOT the root. connectNodeToTree is the default.
//     *
//     * @param parent            Parent node of this new node to be created.
//     * @param action            Action associated with the new node which takes the parent node's state to this node's state.
//     * @param connectNodeToTree Whether this node is connected to the tree, i.e. does the parent node know about this
//     *                          child.
//     * @throws IllegalArgumentException Trying to add a node with the same action as another in the parent.
//     */
//    public OldNodeCopy(OldNodeCopy parent, Action action, boolean connectNodeToTree) {
//        this.parent = parent;
//        treeDepth = parent.getTreeDepth() + 1;
//        this.action = action;
//
//        // Error check for duplicate actions.
//        if (connectNodeToTree) { // If we want to connect this node to the existing tree, a node with the same action
//            // must not already exist.
//            for (OldNodeCopy parentChildren : parent.children) {
//                if (parentChildren.action == action) {
//                    throw new IllegalArgumentException("Tried to add a duplicate action node at depth " + getTreeDepth() + ". Action " +
//                            "was: " + action.toString() + ".");
//                }
//            }
//        }
//        // Add some child actions to try if an action generator is assigned.
//        autoAddUncheckedActions();
//
//        edgeLength = 5.00f * (float) Math.pow(0.6947, 0.1903 * getTreeDepth()) + 1.5f;
//
//        lineBrightness = parent.lineBrightness;
//        nodeLocationZOffset = parent.nodeLocationZOffset;
//
//        // Should only add to parent's list after everything else has been done to prevent half-initialized nodes
//        // from being used by other workers.
//        if (connectNodeToTree) {
//            parent.children.add(this);
//            if (getTreeDepth() > maxDepthYet) {
//                maxDepthYet = getTreeDepth();
//            }
//
//            // Update max branch depth
//            maxBranchDepth.set(getTreeDepth());
//            OldNodeCopy currentNode = this;
//            while (currentNode.getTreeDepth() > 0 && currentNode.parent.maxBranchDepth.get() < currentNode.maxBranchDepth.get()) {
//                currentNode.parent.maxBranchDepth.set(currentNode.maxBranchDepth.get());
//                currentNode = currentNode.parent;
//            }
//        }
//
//        if (connectNodeToTree && !currentlyAddingSavedNodes && calculateNodeVisPositions) {
//            calcNodePos(); // Must be called after this node has been added to its parent's list!
//        }
//        nodesCreated.increment();
//    }
//
//    /**
//     * Make the root of a new tree.
//     **/
//    public OldNodeCopy() {
//        parent = null;
//        action = null;
//        treeDepth = 0;
//        nodeLocation[0] = 0f;
//        nodeLocation[1] = 0f;
//        nodeLocation[2] = 0f;
//
//        nodeColor = Color.BLUE;
//        displayPoint = true;
//
//        // Root node gets the QWOP initial condition.
//        setState(GameUnified.getInitialState());
//
//        // Add some child actions to try if an action generator is assigned.
//        autoAddUncheckedActions();
//
//        nodesCreated.increment();
//    }
//
//    /**
//     * Add a new child node from a given action. If the action is in uncheckedActions, remove it.
//     *
//     * @param childAction The action which defines a child node, i.e. it takes this node's state to the new child's
//     *                    state.
//     * @return A new child node defined by the provided action.
//     */
//    public synchronized OldNodeCopy addChild(Action childAction) {
//        if (uncheckedActions != null)
//            uncheckedActions.removeIf(action -> action.equals(childAction));
//        return new OldNodeCopy(this, childAction);
//    }
//
//    /**
//     * Get the node's value in a thread-safe way. The value or 'score' is totally at the discretion of the sampler.
//     * The node value is 0 until otherwise set.
//     *
//     * @return A score/cost/value associated with this node.
//     */
//    public synchronized float getValue() {
//        return value;
//    }
//
//    /**
//     * Set the node's value in a thread-safe way. The sampler must decide how to use this.
//     *
//     * @param val A new value for this node.
//     */
//    public synchronized void setValue(float val) {
//        value = val;
//        if (val > maxVal) {
//            maxVal = val;
//        }
//        if (val < minVal) {
//            minVal = val;
//        }
//        float avgVal = getValue()/visitCount.floatValue();
//        nodeLabel = String.format("%.2f", avgVal);
//    }
//
//    /**
//     * Add to the node's existing value in a thread-safe way. The sampler must decide how to use this.
//     *
//     * @param val A value to add to this node's existing value.
//     */
//    public synchronized void addToValue(float val) {
//        value += val;
//        if (val > maxVal) {
//            maxVal = val;
//        }
//        if (val < minVal) {
//            minVal = val;
//        }
//        float avgVal = getValue()/visitCount.floatValue();
//        nodeLabel = String.format("%.2f", avgVal);
//    }
//
//    /* ********************************************* */
//    /* ****** GETTING CERTAIN SETS OF NODES ******** */
//    /* ********************************************* */
//
//    /**
//     * Destroy a branch and try to free up its memory. Mark the trimmed branch as fully explored and propagate the
//     * status. This method can be useful when the sampler or user decides that one branch is bad and wants to keep it
//     * from being used later in sampling.
//     */
//    public void destroyNodesBelowAndCheckExplored() {
//        destroyNodesBelow();
//        propagateFullyExploredStatusLite();
//    }
//
//    /**
//     * Try to de-reference everything on this branch so garbage collection throws out all the state values and other
//     * info stored for this branch to keep memory in check.
//     */
//    public void destroyNodesBelow() {
//        OldNodeCopy[] childrenCopy = children.toArray(new OldNodeCopy[0]);
//        children.clear();
//
//        for (OldNodeCopy child : childrenCopy) {
//            pointsToDraw.remove(child);
//            child.state = null;
//            child.parent = null;
//            child.nodeLocation = null;
//            child.destroyNodesBelow();
//        }
//        children.clear();
//    }
//
//    /**
//     * Get the number of nodes in this or any tree.
//     *
//     * @return Total number of nodes, both imported and created.
//     */
//    public static long getTotalNodeCount() {
//        return nodesImported.longValue() + nodesCreated.longValue();
//    }
//
//    /**
//     * Get the number of nodes imported from save file.
//     *
//     * @return Number of nodes imported from a save file.
//     */
//    public static long getImportedNodeCount() {
//        return nodesImported.longValue();
//    }
//
//    /**
//     * Get the total number of nodes created this session.
//     *
//     * @return Number of nodes created during this execution.
//     */
//    public static long getCreatedNodeCount() {
//        return nodesCreated.longValue();
//    }
//
//    /**
//     * Get the total number of games (not nodes) imported from save file.
//     *
//     * @return Number of imported games.
//     */
//    public static long getImportedGameCount() {
//        return gamesImported.longValue();
//    }
//
//    /**
//     * Does this node represent a failed state?
//     *
//     * @return Whether this node represents a failed state (true/false).
//     */
//    public boolean isFailed() {
//        return isFailed.get();
//    }
//
//    /**
//     * Manually set that this node represents a failed state. Failure status will be automatically set when assigning
//     * this node a state and does not need to be manually done. Only useful if we want to assign failure status
//     * without assigning a state.
//     *
//     * @param failed Whether this node should represent a failed state.
//     */
//    public void setFailed(boolean failed) {
//        isFailed.set(failed);
//    }
//
//
//    /* Takes a list of runs and figures out the tree hierarchy without duplicate objects. Adds to an existing given
//    root.
//     * If trimActionAddingToDepth is >= than 0, then actions will be stripped from the imported nodes up to, and
//     * including the depth specified.
//     * Set to -1 or something if you don't want this.**/
//    public static synchronized OldNodeCopy makeNodesFromRunInfo(List<SavableSingleGame> runs, OldNodeCopy existingRootToAddTo,
//                                                                int trimActionAddingToDepth) {
//        OldNodeCopy rootNode = existingRootToAddTo;
//        currentlyAddingSavedNodes = true;
//        for (SavableSingleGame run : runs) { // Go through all runs, placing them in the tree.
//            OldNodeCopy currentNode = rootNode;
//
//            for (int i = 0; i < run.actions.length; i++) { // Iterate through individual actions of this run,
//                // travelling down the tree in the process.
//
//                boolean foundExistingMatch = false;
//                for (OldNodeCopy child : currentNode.children) { // Look at each child of the currently investigated node.
//                    if (child.action == run.actions[i]) { // If there is already a node for the action we are trying
//                        // to place, just use it.
//                        currentNode = child;
//                        foundExistingMatch = true;
//                        break; // We found a match, move on.
//                    }
//                }
//                // If this action is unique at this point in the tree, we need to add a new node there.
//                if (!foundExistingMatch) {
//                    OldNodeCopy newNode = new OldNodeCopy(currentNode, run.actions[i]);
//                    newNode.setState(run.states[i]);
//                    if (rootNode.uncheckedActions != null) newNode.calcNodePos();
//                    currentNode = newNode;
//                    nodesImported.increment();
//                }
//            }
//            gamesImported.increment();
//        }
//        //if (rootNode.uncheckedActions != null) {
//        rootNode.propagateFullyExplored_complete(); // Handle marking the nodes which are fully explored.
//        if (trimActionAddingToDepth >= 0) stripUncheckedActionsExceptOnLeaves(rootNode, trimActionAddingToDepth);
//        rootNode.calcNodePosBelow();
//        //}
//        currentlyAddingSavedNodes = false;
//        return rootNode;
//    }
//
//    /**
//     * Helper for node adding from file. Clears unchecked actions from non-leaf nodes.
//     * Only does it for things below minDepth. Forces new building to happen only at the boundaries of this.
//     */
//    public static void stripUncheckedActionsExceptOnLeaves(OldNodeCopy node, int minDepth) {
//        if (!node.children.isEmpty() && node.getTreeDepth() <= minDepth) {
//            node.uncheckedActions.clear();
//            for (OldNodeCopy child : node.children) {
//                stripUncheckedActionsExceptOnLeaves(child, minDepth);
//            }
//        }
//    }
//
//    /**
//     * Add nodes based on saved action sequences. Has to re-simulate each to get the states.
//     */
//    public static void makeNodesFromActionSequences(List<Action[]> actions, OldNodeCopy root, IGame game) {
//
//        ActionQueue actQueue = new ActionQueue();
//        //root.setState(GameThreadSafe.getInitialState());
//        root.visitCount.getAndIncrement();
//
//        for (Action[] acts : actions) {
//            game.makeNewWorld();
//            OldNodeCopy currNode = root;
//            actQueue.clearAll();
//
//            for (Action act : acts) {
//                act = act.getCopy();
//                act.reset();
//
//                // If there is already a node for this action, use it.
//                boolean foundExisting = false;
//                for (OldNodeCopy child : currNode.children) {
//                    if (child.action.equals(act)) {
//                        currNode = child;
//                        foundExisting = true;
//                        break;
//                    }
//                }
//
//                // Otherwise, make a new one.
//                if (!foundExisting) currNode = currNode.addChild(act);
//
//                // Simulate
//                actQueue.addAction(act);
//                while (!actQueue.isEmpty()) {
//                    boolean[] nextCommand = actQueue.pollCommand(); // Get and remove the next keypresses
//                    boolean Q = nextCommand[0];
//                    boolean W = nextCommand[1];
//                    boolean O = nextCommand[2];
//                    boolean P = nextCommand[3];
//                    game.step(Q, W, O, P);
//                }
//                if (!foundExisting){
//                    currNode.setState(game.getCurrentState());
//                    currNode.visitCount.getAndIncrement();
//                }
//            }
//        }
//    }
//
//    /**
//     * Vanilla node positioning from all previous versions.
//     * Makes a rough best guess for position based on its parent.
//     * Should still be used for initial conditions before letting
//     * the physics kick in.
//     */
//
//    /**
//     * Same, but assumes that we're talking about this node's nodeLocation
//     */
//    private void calcNodePos() {
//        calcNodePos(nodeLocation);
//    }
//
//    /**
//     * Recalculate all node positions below this one (NOT including this one for the sake of root).
//     */
//    public void calcNodePosBelow() {
//        for (OldNodeCopy current : children) {
//            current.calcNodePos();
//            current.calcNodePosBelow(); // Recurse down through the tree.
//        }
//    }
//
//
//
//    /**
//     * Update which nodes are being drawn based on a distance threshold from other nodes. This is usually done
//     * automatically, but sometimes it's nice to aggressively prune areas of the tree which aren't important any more.
//     * @param threshold Distance squared threshold. Nodes closer to another drawn node than this threshold are not
//     *                  drawn.
//     */
//    public void postPruneDrawingBelow(float threshold) {
//        float xDiff;
//        float yDiff;
//        float zDiff;
//
//        pointsToDraw.remove(this);
//        notDrawnForSpeed = false;
//
//        for (OldNodeCopy n : pointsToDraw) {
//            xDiff = n.nodeLocation[0] - nodeLocation[0];
//            yDiff = n.nodeLocation[1] - nodeLocation[1];
//            zDiff = n.nodeLocation[2] - nodeLocation[2] + (OldNodeCopy.includeZOffsetInDrawFiltering ?
//                    n.nodeLocationZOffset - nodeLocationZOffset : 0);
//
//            // Actually distance squared to avoid sqrt.
//            if ((xDiff * xDiff + yDiff * yDiff + zDiff * zDiff) < threshold) {
//                notDrawnForSpeed = true;
//                break;
//            }
//        }
//
//        if (!notDrawnForSpeed)
//            pointsToDraw.add(this);
//
//        for (OldNodeCopy child : children) {
//            child.postPruneDrawingBelow(threshold);
//        }
//    }
//
//
//    /**
//     * Draw all lines in the subtree below this node.
//     *
//     * @param gl OpenGL drawing object.
//     */
//    public void drawLines_below(GL2 gl) {
//        for (OldNodeCopy current : children) {
//            if (!current.notDrawnForSpeed) current.drawLine(gl);
//            assert current.getTreeDepth() > this.getTreeDepth(); // Make sure that the children are further down the
//            // tree.
//            current.drawLines_below(gl); // Recurse down through the tree.
//        }
//    }
//
//    /**
//     * Draw all nodes in the subtree below this node.
//     *
//     * @param gl OpenGL drawing object.
//     */
//    public void drawNodes_below(GL2 gl) {
//        if (!notDrawnForSpeed) drawPoint(gl);
//        for (OldNodeCopy child : children) {
//            //child.drawPoint(gl);
//            child.drawNodes_below(gl); // Recurse down through the tree.
//        }
//    }
//
//    /**
//     * Turn off all display for this node onward.
//     */
//    public void turnOffBranchDisplay() {
//        displayLine = false;
//        displayPoint = false;
//        notDrawnForSpeed = true;
//        pointsToDraw.remove(this);
//
//        for (OldNodeCopy child : children) {
//            child.turnOffBranchDisplay();
//        }
//    }
//
//    /**
//     * Single out one run up to this node to highlight the lines, while dimming the others.
//     */
//    public void highlightSingleRunToThisNode() {
//        OldNodeCopy rt = getRoot();
//        rt.setLineBrightness_below(0.4f); // Fade the entire tree, then go and highlight the run we care about.
//
//        OldNodeCopy currentNode = this;
//        while (currentNode.getTreeDepth() > 0) {
//            currentNode.setLineBrightness(lineBrightness_default);
//            currentNode = currentNode.parent;
//        }
//    }
//
//    /**
//     * Fade a single line going from this node to its parent.
//     */
//    private void setLineBrightness(float brightness) {
//        lineBrightness = brightness;
//    }
//
//    /**
//     * Fade a certain part of the tree.
//     */
//    public void setLineBrightness_below(float brightness) {
//        setLineBrightness(brightness);
//        for (OldNodeCopy child : children) {
//            child.setLineBrightness_below(brightness);
//        }
//    }
//
//    /**
//     * Reset line brightnesses to default.
//     */
//    public void resetLineBrightness_below() {
//        setLineBrightness_below(lineBrightness_default);
//    }
//
//
//    public void setChildrenColorFromRelativeValues() {
//        OldNodeCopy[] children = getChildren();
//        float minVal = Float.MAX_VALUE;
//        float maxVal = -Float.MAX_VALUE;
//        for (OldNodeCopy child : children) {
//            float avgVal = child.getValue()/child.visitCount.floatValue();
//            if (avgVal < minVal)
//                minVal = avgVal;
//            if (avgVal > maxVal)
//                maxVal = avgVal;
//        }
//        for (OldNodeCopy child : children) {
//            float avgVal = child.getValue()/child.visitCount.floatValue();
//            child.nodeColor = getColorFromScaledValue(avgVal - minVal, maxVal - minVal, 1f);
//        }
//    }
//
//    /**
//     * Set an override line color for this branch (all descendants).
//     */
//    public void setBranchColor(Color newColor) {
//        overrideLineColor = newColor;
//        for (OldNodeCopy child : children) {
//            child.setBranchColor(newColor);
//        }
//    }
//
//    /**
//     * Set an override line color for this path (all ancestors).
//     */
//    public void setBackwardsBranchColor(Color newColor) {
//        overrideLineColor = newColor;
//        if (getTreeDepth() > 0) {
//            parent.setBackwardsBranchColor(newColor);
//        }
//    }
//
//    /**
//     * Clear an overridden line color on this branch. Call from root to get all line colors back to default.
//     */
//    public void clearBranchColor() {
//        overrideLineColor = null;
//        for (OldNodeCopy child : children) {
//            child.clearBranchColor();
//        }
//    }
//
//    /**
//     * Clear an overridden line color on this branch. Goes back towards root.
//     */
//    public void clearBackwardsBranchColor() {
//        overrideLineColor = null;
//        if (getTreeDepth() > 0) {
//            parent.clearBackwardsBranchColor();
//        }
//    }
//
//    /**
//     * Clear node override colors from this node onward. Only clear the specified color. Call from root to clear all.
//     */
//    private void clearNodeOverrideColor(Color colorToClear) {
//        if (overrideNodeColor == colorToClear) {
//            overrideNodeColor = null;
//            displayPoint = false;
//        }
//        for (OldNodeCopy child : children) {
//            child.clearNodeOverrideColor(colorToClear);
//        }
//    }
//
//    /**
//     * Clear node override colors from this node backwards. Only clear the specified color. Goes towards root.
//     */
//    private void clearBackwardsNodeOverrideColor(Color colorToClear) {
//        if (overrideNodeColor == colorToClear) {
//            overrideNodeColor = null;
//            displayPoint = false;
//        }
//        if (getTreeDepth() > 0) {
//            parent.clearBackwardsNodeOverrideColor(colorToClear);
//        }
//    }
//
//    /**
//     * Clear all node override colors from this node onward. Call from root to clear all.
//     */
//    public void clearNodeOverrideColor() {
//        if (overrideNodeColor != null) {
//            overrideNodeColor = null;
//            displayPoint = false;
//        }
//        for (OldNodeCopy child : children) {
//            child.clearNodeOverrideColor();
//        }
//    }
//
//    /**
//     * Clear all node override colors from this node onward. Call from root to clear all.
//     */
//    private void clearBackwardsNodeOverrideColor() {
//        if (overrideNodeColor != null) {
//            overrideNodeColor = null;
//            displayPoint = false;
//        }
//        if (getTreeDepth() > 0) {
//            parent.clearBackwardsNodeOverrideColor();
//        }
//    }
//
//    /**
//     * Give this branch a nodeLocationZOffset to make it stand out.
//     */
//    public void setBranchZOffset(float zOffset) {
//        this.nodeLocationZOffset = zOffset;
//        for (OldNodeCopy child : children) {
//            child.setBranchZOffset(zOffset);
//        }
//    }
//
//    /**
//     * Give this branch a nodeLocationZOffset to make it stand out. Goes backwards towards root.
//     */
//    public void setBackwardsBranchZOffset(float zOffset) {
//        this.nodeLocationZOffset = zOffset;
//
//        if (treeDepth > 0) {
//            parent.setBackwardsBranchZOffset(zOffset);
//        }
//    }
//
//    /**
//     * Clear z offsets in this branch. Works backwards towards root.
//     */
//    public void clearBackwardsBranchZOffset() {
//        setBackwardsBranchZOffset(0f);
//    }
//
//    /**
//     * Clear z offsets in this branch. Called from root, it resets all back to 0.
//     */
//    public void clearBranchZOffset() {
//        setBranchZOffset(0f);
//    }
//
//}
