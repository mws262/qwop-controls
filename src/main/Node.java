package main;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.MassData;
import org.jbox2d.collision.shapes.PolygonDef;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;

import com.jogamp.opengl.GL2;

/*
 * This version will hopefully get rid of many legacy features.
 * Also, this should handle adding actions which are not from a 
 * predetermined set.
 * 
 */
public class Node {

	/******** All node stats *********/
	private static int nodesCreated = 0;
	private static int nodesImported = 0;
	private static int gamesImported = 0;
	private static int gamesCreated = 0;

	/********* QWOP IN/OUT ************/
	/** Actual numeric control action **/
	public final Action action; //Actual delay used as control.

	/** What is the state after taking this node's action? **/
	public State state;

	/** Keypress action. **/
	//TODO

	/********* TREE CONNECTION INFO ************/

	/** Node which leads up to this node. **/
	public final Node parent; // Parentage may not be changed.

	/** Child nodes. Not fixed size any more. **/
	public CopyOnWriteArrayList<Node> children = new CopyOnWriteArrayList<Node>();

	/** Untried child actions. **/
	public ArrayList<Action> uncheckedActions = new ArrayList<Action>();

	/** Are there any untried things below this node? **/
	public boolean fullyExplored = false;

	/** Does this node represent a failed state? Stronger than fullyExplored. **/
	public boolean isTerminal = false;

	/** How deep is this node down the tree? 0 is root. **/
	public final int treeDepth;

	/** Maximum depth yet seen in this tree. **/
	public static int maxDepthYet = 1;

	/********* TREE VISUALIZATION ************/
	public Color overrideLineColor = null; // Only set when the color is overridden.
	public Color overrideNodeColor = null; // Only set when the color is overridden.
	
	public Color nodeColor = Color.GREEN;

	private static final float lineBrightness_default = 0.85f;
	private float lineBrightness = lineBrightness_default;

	public boolean displayPoint = false; // Round dot at this node. Is it on?
	public boolean displayLine = true; // Line from this node to parent. Is it on?

	/********* NODE PLACEMENT ************/

	/** Parameters for visualizing the tree **/
	public float[] nodeLocation = new float[3]; // Location that this node appears on the tree visualization
	public float nodeAngle = 0; // Keep track of the angle that the line previous node to this node makes.
	public float sweepAngle = 8f*(float)Math.PI;

	public float edgeLength = 1.f;

	/** If we want to bring out a certain part of the tree so it doesn't hide under other parts, give it a small z offset. **/
	private float zOffset = 0.f; 

	/********* TREE PHYSICS ************/

	/** Are the Box2D physics FOR THE TREE branches enabled? **/
	public static boolean useTreePhysics;

	/** Are the physics things fully initialized for this node. Prevents null pointers. **/
	private boolean arePhysicsInitialized = false;

	/** Are we in the middle of a physics step? **/
	public static volatile boolean stepping = false;

	/** Node mass and inertia for tree physics. **/
	float physMass = 5f;
	static final float physInertia = 1f;

	/** Tree physics timestep. **/
	static float physDt = 0.04f;
	static final int physIterations = 15;

	/** Physics world for the TREE not for the runner. **/
	static World treePhysWorld;

	/** Shape and body definition for the tree node physics. **/
	public PolygonDef physShape;
	public Body physBody;

	/** Each node is jointed to its parent. **/
	public RevoluteJoint jointToParent;

	/** Length and width of the node boxes in the physics (half-extent). **/
	static final float physSize = 0.1f/2f;
	private final static int TREE_GROUP = -1; // No collisions between branches.

	/** Motor at every joint tries to bring the branches together **/
	static final float anglePGain = 0.02f;
	static final float angleDGain = 0.8f;
	static final float angleSpeedCap = 1f;
	static final float maxTorque = 1000f;

	/** Damping general motion **/
	static final float linearDamping = 10f;
	static final float angularDamping = 10f;

	/** Repulser force gains **/
	static final float centralRepelGain = 50f;
	static final float nodeRepelGain = 10f;

	static Vec2 repulserPoint = new Vec2(0f,0f); // Point everything is pushed from.

	Vec2 localForce = new Vec2(0f,0f); // Latest force applied to this node.

	/*********** UTILITY **************/

	/** Are we bulk adding saved nodes? If so, some construction things are deferred. **/
	private static boolean currentlyAddingSavedNodes = false;

	/** Random number generator for new node selection **/
	private final static Random rand = new Random();

	/**********************************/
	/********** CONSTRUCTORS **********/
	/**********************************/

	/** 
	 * Make a new node which is NOT the root. 
	 **/
	public Node(Node parent, Action action) {
		this.parent = parent;
		treeDepth = parent.treeDepth + 1;
		this.action = action;

		// Error check for duplicate actions.
		for (Node parentChildren : parent.children){
			if (parentChildren.action == action){
				throw new RuntimeException("Tried to add a duplicate action node at depth " + treeDepth + ". Action was: " + action.toString() + ".");
			}	
		}
		parent.children.add(this);
		if (treeDepth > maxDepthYet){
			maxDepthYet = treeDepth;
		}

		edgeLength = 5.00f * (float)Math.pow(0.6947, 0.1903 * treeDepth ) + 1.5f;
		//(float)Math.pow(0.787, 0.495) + 1.0f; // Optimized exponential in Matlab 

		lineBrightness = parent.lineBrightness;
		zOffset = parent.zOffset;

		if (!currentlyAddingSavedNodes){
			calcNodePos();
			if (useTreePhysics){
				initTreePhys_single();
			}
		}
	}

	/**
	 * Make the root of a new tree.
	 **/
	public Node(boolean treePhysOn) {
		parent = null;
		action = null;
		treeDepth = 0; 
		nodeLocation[0] = 0f;
		nodeLocation[1] = 0f;
		nodeLocation[2] = 0f;

		nodeColor = Color.BLUE;
		displayPoint = true;

		// Initialize the tree physics world if this is enabled.
		useTreePhysics = treePhysOn;

		// Root node gets the QWOP initial condition. Yay!
		setState(FSM_Game.getInitialState());

		if (useTreePhysics){
			initTreePhys_single();
		}
	}

	/************************************/
	/******* ADDING TO THE TREE *********/
	/************************************/

	/** Sample random node. Either create or return an existing not fully explored child. **/
	public Node sampleNode(){

		Node sample;
		ArrayList<Node> unexploredChildren = new ArrayList<Node>();

		Iterator<Node> iter = children.iterator();
		while(iter.hasNext()){ // Check how many children are still valid choices to expand.
			Node child = iter.next();
			if (!child.fullyExplored) unexploredChildren.add(child);
		}

		if (fullyExplored) {
			nodeColor = Color.RED;
			throw new RuntimeException("Tried to sample from a node which thinks it's fully explored at depth " + treeDepth + ". It has " + unexploredChildren 
					+ " children it thinks are not fully explored and " 
					+ uncheckedActions.size() + " unchecked potential actions. Total children: " + children.size());
		}



		int selection = 0;
		try{
			selection = randInt(1,unexploredChildren.size() + uncheckedActions.size());
		}catch(IllegalArgumentException e){
			throw new RuntimeException("Invalid range when using RNG to sample a new node from this one at depth "
					+ treeDepth + ". Unexplored children: " + unexploredChildren + ", Unchecked potential actions: "
					+ uncheckedActions + " , Descendant count: " + countDescendants(),e); 
		}

		// Make a new node or pick a not fully explored child.
		if (selection > unexploredChildren.size()){
			sample = new Node(this,uncheckedActions.get(selection - unexploredChildren.size() - 1));
			nodesCreated++;
			uncheckedActions.remove(selection - unexploredChildren.size() - 1); // Don't allow this one to be picked again.
		}else{
			sample = unexploredChildren.get(selection - 1).sampleNode(); // Pick child and recurse this method.
		}

		//sample.fullyExplored = true;
		//sample.checkFullyExplored_lite();
		return sample;
	}

	/**
	 * Expand the sets of potential children to check.
	 * Will bilaterally add the number of specified options to the existing, tried ones.
	 * Will also fill any gaps, e.g. if we have nodes for 55,57,58, will make 56 a potential option.
	 */
//	public void expandNodeChoices(int doubleSidedExpansionNumber){
//
//		if (children.isEmpty()){
//			this.fullyExplored = true;
//			return; // Not well defined if there are no children to begin with. TODO fix this.
//		}
//
//		// Get all existing child actions.
//		int[] existingActions = new int[children.size()];
//		for (int i = 0; i < children.size(); i++){
//			existingActions[i] = children.get(i).action;
//		}
//		Arrays.sort(existingActions);
//
//		// Check for gaps and fill them in.
//		int count = 0;
//		for (int i = existingActions[0]; i <= existingActions[existingActions.length - 1]; i++){	
//			if (existingActions[count] == i){
//				count++;
//			}else{
//				uncheckedActions.add(i);
//			}
//		}
//
//		// Add new ones on either side of the existing set.
//		for (int i = 1; i <= doubleSidedExpansionNumber; i++){
//			uncheckedActions.add(existingActions[0] - i);
//			uncheckedActions.add(existingActions[existingActions.length - 1] + i);
//		}
//	}
//
//	/**
//	 * Add nodes around some center value.
//	 */
//	public void expandNodeChoices(int centerValue, int doubleSidedExpansionNumber){
//		for (int i = centerValue - doubleSidedExpansionNumber; i <= centerValue + doubleSidedExpansionNumber; i++){
//			if (!uncheckedActions.contains(i)){
//				uncheckedActions.add(i);
//			}
//		}
//	}
//
//	/** Expand all below this node. **/
//	public void expandNodeChoices_allBelow(int doubleSidedExpansionNumber){
//		expandNodeChoices(doubleSidedExpansionNumber);
//		fullyExplored = false;
//		for (Node child : children){
//			child.expandNodeChoices_allBelow(doubleSidedExpansionNumber);
//		}
//	}
//
//	/** Expand all below this node. **/
//	public void expandNodeChoices_range(int doubleSidedExpansionNumber, int firstLayer, int endLayer){
//		//TODO
//	}
//
//	/** Expand to include certain values. **/
//	public void expandNodeChoices_fullCycle(int[] nil1, int[] W_O, int[] nil2, int[] Q_P){
//		int[] newChoices = {};
//		switch(treeDepth % 4){ // Figure out which action in the cycle this is.
//		case 0:
//			newChoices = nil1;
//			break;
//		case 1:
//			newChoices = W_O;
//			break;	
//		case 2:
//			newChoices = nil2;
//			break;
//		case 3:
//			newChoices = Q_P;
//			break;
//		}
//		// Check whether the actions are already tested in the children, or included in the uncheckedActions list. If not, add.
//		for (int i = 0; i < newChoices.length; i++){
//			boolean isDuplicate = false;
//			for (Node child : children){
//				if (child.action == newChoices[i]){
//					isDuplicate = true;
//					break;
//				}
//			}
//			if (!isDuplicate && uncheckedActions.contains(newChoices[i])){
//				isDuplicate = true;
//			}
//			
//			if (!isDuplicate){
//				uncheckedActions.add(newChoices[i]);
//			}
//		}
//		
//		// Recurse.
//		for (Node child : children){
//			child.expandNodeChoices_fullCycle(nil1, W_O, nil2, Q_P);
//		}
//		
//	}
	
	/***********************************************/
	/******* GETTING CERTAIN SETS OF NODES *********/
	/***********************************************/

	/** Get a random child **/
	public Node getRandomChild(){
		return children.get(randInt(0,children.size()-1));
	}

	/** Add all the nodes below and including this one to a list. Does not include nodes whose state have not yet been assigned. **/
	public ArrayList<Node> getNodes_below(ArrayList<Node> nodeList){
		if (state != null){
			nodeList.add(this);
		}
		for (Node child : children){
			child.getNodes_below(nodeList);
		}	
		return nodeList;
	}


	/** Locate all the endpoints in the tree. Starts from the node it is called from. **/
	public void getLeaves(ArrayList<Node> leaves){
		for (Node child : children){
			if (child.children.isEmpty()){
				leaves.add(child);
			}else{
				child.getLeaves(leaves);
			}
		}	
	}

	/** Returns the tree root no matter which node in the tree this is called from. **/
	public Node getRoot(){
		Node currentNode = this;
		while (currentNode.treeDepth > 0){
			currentNode = currentNode.parent;
		}
		return currentNode;
	}

	/** Recount how many descendants this node has. **/
	public int countDescendants(){
		int count = 0;
		for (Node current : children){
			count++;
			count += current.countDescendants(); // Recurse down through the tree.
		}
		return count;
	}

	/** Check whether a node is an ancestor of this one. */
	public boolean isOtherNodeAncestor(Node otherNode){
		if (otherNode.treeDepth >= this.treeDepth){ // Don't need to check if this is as far down the tree.
			return false;
		}
		Node currNode = parent;

		while (currNode.treeDepth != otherNode.treeDepth){ // Find the node at the same depth as the one we're checking.
			currNode = currNode.parent;
		}

		if (currNode.equals(otherNode)){
			return true;
		}

		return false;
	}

	/** Change whether this node or any above it have become fully explored. 
	 * This is lite because it assumes all existing fullyExplored tags in its children are accurate. 
	 * Call from a leaf node that we just assigned to be fully explored.
	 **/
	public void checkFullyExplored_lite(){
		boolean flag = true;

		if (!uncheckedActions.isEmpty()){
			flag = false;
		}
		for (Node child : children){
			if (!child.fullyExplored){ // If any child is not fully explored, then this node isn't too.
				flag = false;
			}
		}
		fullyExplored = flag;

		if (treeDepth > 0){ // We already know this node is fully explored, check the parent.
			parent.checkFullyExplored_lite();
		}
	}

	/** Check from this node and below which nodes are fully explored. Good to call this after a 
	 * bunch of nodes have been loaded. 
	 **/
	public void checkFullyExplored_complete(){
		ArrayList<Node> leaves = new ArrayList<Node>();
		getLeaves(leaves);

		// Reset all existing exploration flags out there.
		for (Node leaf : leaves){
			Node currNode = leaf;
			while (currNode.treeDepth > 0){
				currNode.fullyExplored = false;
				currNode = currNode.parent;
			}
			currNode.fullyExplored = false;
		}

		for (Node leaf : leaves){
			leaf.checkFullyExplored_lite();
		}
	}

	/** Total number of nodes in this or any tree. **/
	public static int getTotalNodeCount(){
		return nodesImported + nodesCreated;
	}

	/** Total number of nodes imported from save file. **/
	public static int getImportedNodeCount(){
		return nodesImported;
	}

	/** Total number of nodes created this session. **/
	public static int getCreatedNodeCount(){
		return nodesCreated;
	}

	/** Total number of nodes created this session. **/
	public static int getImportedGameCount(){
		return gamesImported;
	}
	/** Total number of nodes created this session. **/
	public static int getCreatedGameCount(){
		return gamesCreated;
	}

	/** Mark this node as representing a terminal state. **/
	public void markTerminal(){
		isTerminal = true;
		gamesCreated++;
	}

	/***************************************************/
	/******* STATE & SEQUENCE SETTING/GETTING **********/
	/***************************************************/

	//	/** Capture the state from an active game **/
	//	@Deprecated
	//	public void captureState(QWOPInterface QWOPHandler){
	//		state = new CondensedStateInfo(QWOPHandler.game); // MATT add 8/22/17
	//	}

	/** Capture the state from an active game **/
	public void captureState(QWOPGame game){
		state = new State(game); // MATT add 8/22/17
	}

	/** Assign the state directly. Usually when loading nodes. **/
	public void setState(State newState){
		state = newState;
	}

	/** Get the sequence of actions up to, and including this node **/
	public Action[] getSequence(){
		Action[] sequence = new Action[treeDepth];
		if (treeDepth == 0) return sequence; // Empty array for root node.
		Node current = this;
		sequence[treeDepth-1] = current.action;
		for (int i = treeDepth - 2; i >= 0; i--){
			current = current.parent;
			sequence[i] = current.action;
		}
		return sequence;
	}

	/****************************************/
	/******* SAVE/LOAD AND UTILITY **********/
	/****************************************/

	/* Takes a list of runs and figures out the tree hierarchy without duplicate objects. Returns the ROOT of a tree. */
	public static Node makeNodesFromRunInfo(ArrayList<SaveableSingleGame> runs, boolean initializeTreePhysics){
		Node rootNode = new Node(initializeTreePhysics);
		return makeNodesFromRunInfo(runs, rootNode);
	}

	/* Takes a list of runs and figures out the tree hierarchy without duplicate objects. Adds to an existing given root. **/
	public static Node makeNodesFromRunInfo(ArrayList<SaveableSingleGame> runs, Node existingRootToAddTo){
		Node rootNode = existingRootToAddTo;
		currentlyAddingSavedNodes = true;
		for (SaveableSingleGame run : runs){ // Go through all runs, placing them in the tree.
			Node currentNode = rootNode;	

			for (int i = 0; i < run.actions.length; i++){ // Iterate through individual actions of this run, travelling down the tree in the process.

				boolean foundExistingMatch = false;
				for (Node child: currentNode.children){ // Look at each child of the currently investigated node.
					if (child.action == run.actions[i]){ // If there is already a node for the action we are trying to place, just use it.
						currentNode = child;
						foundExistingMatch = true;
						break; // We found a match, move on.
					}
				}
				// If this action is unique at this point in the tree, we need to add a new node there.
				if (!foundExistingMatch){
					Node newNode = new Node(currentNode, run.actions[i]);
					newNode.setState(run.states[i]);
					newNode.calcNodePos();
					currentNode = newNode;
					nodesImported++;
				}
			}
			gamesImported++;
		}
		rootNode.checkFullyExplored_complete(); // Handle marking the nodes which are fully explored.
		currentlyAddingSavedNodes = false;
		rootNode.calcNodePos_below();
		if (Node.useTreePhysics) rootNode.initTreePhys_below();
		return rootNode;
	}

	/** Generate a random integer between two values, inclusive. **/
	public static int randInt(int min, int max) {
		if (min > max) throw new IllegalArgumentException("Random int sampler should be given a minimum value which is less than or equal to the given max value.");
		int randomNum = rand.nextInt((max - min) + 1) + min;
		return randomNum;
	}

	/************************************************/
	/******* NODE POSITIONING INCL PHYSICS **********/
	/************************************************/

	/** Vanilla node positioning from all previous versions.
	 * Makes a rough best guess for position based on its parent.
	 * Should still be used for initial conditions before letting
	 * the physics kick in.
	 **/
	public void calcNodePos(float[] nodeLocationsToAssign){
		//Angle of this current node -- parent node's angle - half the total sweep + some increment so that all will span the required sweep.
		if(treeDepth == 0){ //If this is the root node, we shouldn't change stuff yet.
			if (children.size() > 1 ){ //Catch the div by 0
				nodeAngle = -sweepAngle/2.f + (float)(parent.children.indexOf(this)) * sweepAngle/(float)(children.size() - 1);
			}else{
				nodeAngle = (float)Math.PI/2f;
			}
		}else{
			if (parent.children.size() > 1){ //Catch the div by 0
				sweepAngle = parent.sweepAngle/(4);
				nodeAngle = parent.nodeAngle - sweepAngle/2.f + (float)(parent.children.indexOf(this)) * sweepAngle/(float)(parent.children.size() - 1);
			}else{
				sweepAngle = parent.sweepAngle; //Only reduce the sweep angle if the parent one had more than one child.
				nodeAngle = parent.nodeAngle;
			}
		}

		nodeLocationsToAssign[0] = (float) (parent.nodeLocation[0] + edgeLength*Math.cos(nodeAngle));
		nodeLocationsToAssign[1] = (float) (parent.nodeLocation[1] + edgeLength*Math.sin(nodeAngle));
		nodeLocationsToAssign[2] = 0f; // No out of plane stuff yet.
	}

	/** Same, but assumes that we're talking about this node's nodeLocation **/
	public void calcNodePos(){
		calcNodePos(nodeLocation);
	}

	/** Recalculate all node positions below this one (NOT including this one for the sake of root). **/
	public void calcNodePos_below(){		
		for (Node current : children){
			current.calcNodePos();	
			current.calcNodePos_below(); // Recurse down through the tree.
		}
	}

	/** Initialize all the physics stuff for this node. **/
	public void initTreePhys_single(){
		if (treeDepth == 0){ // Root creates the WORLD.
			treePhysWorld = new World(new AABB(new Vec2(-10, -10f), new Vec2(10f,10f)), new Vec2(0f, 0f), true);

			treePhysWorld.setWarmStarting(true);
			treePhysWorld.setPositionCorrection(true);
			treePhysWorld.setContinuousPhysics(true);
		}

		physShape = new PolygonDef();
		physShape.setAsBox(physSize, physSize); // Shape is just a small square centered at the node. No need to do rectangles.
		physShape.filter.groupIndex = TREE_GROUP; // Filter out all collisions.
		BodyDef physBodyDef = new BodyDef();

		// Add at existing node positions. 
		physBodyDef.position = new Vec2(nodeLocation[0],nodeLocation[1]);
		physBodyDef.angle = nodeAngle;

		if (treeDepth > 0){ // Setting the mass/inertia of root to zero makes it fixed.
			MassData physMassData = new MassData();
			physMassData.mass = physMass;
			physMassData.I = physInertia;
			physBodyDef.massData = physMassData;
		}
		physBody = null;
		while (physBody == null){ // World locks while stepping. "m_lock" isn't visible outside the package, so this is the hack.
			physBody = treePhysWorld.createBody(physBodyDef);
		}
		physBody.setLinearDamping(linearDamping);
		physBody.setAngularDamping(angularDamping);
		physBody.createShape(physShape);

		if (treeDepth > 0){
			RevoluteJointDef physJDef = new RevoluteJointDef(); 
			physJDef.initialize(physBody,parent.physBody, new Vec2(parent.nodeLocation[0],parent.nodeLocation[1])); //Body1, body2, anchor in world coords
			physJDef.enableLimit = false;
			physJDef.upperAngle = 0.5f;
			physJDef.lowerAngle = -0.5f;
			physJDef.collideConnected = false;
			physJDef.enableMotor = true;
			physJDef.maxMotorTorque = maxTorque;
			physJDef.motorSpeed = 0;

			jointToParent = (RevoluteJoint)treePhysWorld.createJoint(physJDef);
		}
		arePhysicsInitialized = true;
	}

	/** Initialize the tree physics below the called node. Useful when importing lots or doing in batches. **/
	public void initTreePhys_below(){
		for (Node child : children){
			child.initTreePhys_single();
			child.initTreePhys_below();
		}
	}

	/** Advance the tree physics, and update node positions. **/
	public void stepTreePhys(int timesteps){
		stepping = true;
		if (treeDepth != 0) throw new RuntimeException("Tree physics updates are only supported from root for now.");
		if (!useTreePhysics){
			stepping = false;
			return;
		}
		ArrayList<Node> nodeList = new ArrayList<Node>();
		getNodes_below(nodeList);

		for (int i = 0; i < timesteps; i++){
			// Calculate added forces before stepping the world.
			applyForce(nodeList);
			treePhysWorld.step(physDt, physIterations);
			updatePositionFromPhys();
		}
		stepping = false;
	}

	/** Apply all my made up forces to the nodes. **/
	public void applyForce(ArrayList<Node> nodeList){
		if (arePhysicsInitialized){
			for (Node thisNode : nodeList){
				if (thisNode.treeDepth == 0 || !thisNode.arePhysicsInitialized) continue; // Skip forces on root.
				localForce.setZero();

				// Repulser from the specified origin.
				Vec2 o_pt = thisNode.physBody.getPosition().sub(repulserPoint); // Repulser point to this node.
				//System.out.println(thisNode.physBody.getPosition().x + "," + thisNode.physBody.getPosition().y);
				float lengthSq = Math.max(o_pt.lengthSquared(), 0.1f); // Set minimum distance to prevent div0 errors
				Vec2 repulserForce = o_pt.mul(centralRepelGain * thisNode.physMass/(lengthSq)); // Only inverse distance law right now.
				localForce.addLocal(repulserForce);

				//				// Repulser from EVERYONE is way too slow. >1 second with big tree. We need AABB equivalent.
				//				for (TrialNodeMinimal otherNode : nodeList){
				//					if (!otherNode.equals(thisNode) && otherNode.arePhysicsInitialized){
				//						o_pt = thisNode.physBody.getPosition().sub(otherNode.physBody.getPosition()); // Repulser point to this node.
				//						lengthSq = Math.max(o_pt.lengthSquared(), 0.1f); // Set minimum distance to prevent div0 errors
				//
				//						// Ancestor nodes have a bigger repulsive effect than others.
				//						if (thisNode.isOtherNodeAncestor(otherNode)){
				//							repulserForce = o_pt.mul((ancestorMultiplier * nodeRepelGain * thisNode.physMass * otherNode.physMass)/(lengthSq*lengthSq)); // Only inverse distance law right now.
				//						}else{
				//							repulserForce = o_pt.mul((nodeRepelGain * thisNode.physMass * otherNode.physMass)/(lengthSq*lengthSq)); // Only inverse distance law right now.
				//						}
				//						localForce.addLocal(repulserForce);
				//					}
				//				}

				// Repulser force from cousins (parent's sibling's children)
				if (thisNode.treeDepth > 1){
					Node grandparent = thisNode.parent.parent;
					for (Node aunt : grandparent.children){ // Also happens to include parent
						for (Node cousin : aunt.children){ // Also happens to include siblings
							if (!cousin.equals(thisNode) && cousin.arePhysicsInitialized){
								localForce.addLocal(repulserForce(thisNode, cousin));
							}
						}
					}
				}else if (thisNode.treeDepth == 1){ // Forces on nodes one layer in. It's important to balance the repulsive forces or things blow up.
					for (Node sibling : thisNode.parent.children){
						if (!sibling.equals(thisNode) && sibling.arePhysicsInitialized){
							localForce.addLocal(repulserForce(thisNode,sibling));
						}
					}
				}

				// From grandchildren
				for (Node child : children){
					for (Node grandchild : child.children){
						if (grandchild.arePhysicsInitialized){
							localForce.addLocal(repulserForce(thisNode, grandchild));
						}
					}
				}

				// From grandparent
				if (treeDepth > 1){
					localForce.addLocal(repulserForce(thisNode, thisNode.parent.parent));
				}


				thisNode.physBody.applyForce(localForce, thisNode.physBody.getWorldCenter());

				// Pseudo-spring in the joint
				float motorSpeed = Math.min((float)thisNode.countDescendants() * anglePGain, angleSpeedCap) * (-thisNode.parent.physBody.getAngle() + thisNode.physBody.getAngle() - angleDGain * thisNode.jointToParent.getJointSpeed());
				thisNode.jointToParent.setMotorSpeed(motorSpeed);
			}
		}
	}

	/** Node repulser force rule so I don't have to keep ctrl-c this crap **/
	private static Vec2 repulserForce(Node thisNode, Node otherNode){
		if (!otherNode.equals(thisNode) && otherNode.arePhysicsInitialized){
			Vec2 o_pt = thisNode.physBody.getPosition().sub(otherNode.physBody.getPosition()); // Repulser point to this node.
			float lengthSq = Math.max(o_pt.lengthSquared(), 0.1f); // Set minimum distance to prevent div0 errors
			Vec2 repulserForce = o_pt.mul(nodeRepelGain * thisNode.physMass/(lengthSq));//(float)Math.sqrt((double)lengthSq))); // Only inverse distance law right now.
			return repulserForce;
		}else{
			return new Vec2(0,0);
		}
	}

	/** Propagate all physics world info back to the node drawing info. **/
	public void updatePositionFromPhys(){
		if (arePhysicsInitialized){
			Vec2 newPos = physBody.getPosition();
			nodeLocation[0] = newPos.x;
			nodeLocation[1] = newPos.y;
			nodeAngle = physBody.getAngle();
			//nodeAngle = jointToParent.getJointAngle(); // Relative angle.
			for (Node child : children){		
				child.updatePositionFromPhys();
			}
		}
	}

	/*******************************/
	/******* TREE DRAWING **********/
	/*******************************/

	/** Draw the line connecting this node to its parent. **/
	public void drawLine(GL2 gl){
		if (treeDepth > 0 && displayLine){ // No lines for root.
			if (overrideLineColor == null){
				gl.glColor3fv(getColorFromTreeDepth(treeDepth,lineBrightness).getColorComponents(null),0);
			}else{
				// The most obtuse possible way to change just the brightness of the color. Rediculous, but I can't find anything else.
				float[] color = Color.RGBtoHSB(overrideLineColor.getRed(),overrideLineColor.getGreen(),overrideLineColor.getBlue(),null);
				gl.glColor3fv(Color.getHSBColor(color[0], color[1], lineBrightness).getColorComponents(null),0);
			}
			gl.glVertex3d(parent.nodeLocation[0], parent.nodeLocation[1], parent.nodeLocation[2] + zOffset);
			gl.glVertex3d(nodeLocation[0], nodeLocation[1], nodeLocation[2] + zOffset);
		}
	}

	/** Draw the node point if enabled **/
	public void drawPoint(GL2 gl){
		if(displayPoint){
			if (overrideNodeColor == null){
				gl.glColor3fv(nodeColor.getColorComponents(null),0);
			}else{
				gl.glColor3fv(overrideNodeColor.getColorComponents(null),0);
			}
			gl.glVertex3d(nodeLocation[0], nodeLocation[1], nodeLocation[2] + zOffset);
		}
	}


	/** Draw all lines in the subtree below this node **/
	public void drawLines_below(GL2 gl){	
		Iterator<Node> iter = children.iterator();
		while (iter.hasNext()){
			Node current = iter.next();
			current.drawLine(gl);
			if (current.treeDepth <= this.treeDepth) throw new RuntimeException("Node hierarchy problem. Node with an equal or lesser depth is below another. At " + current.treeDepth + " and " + this.treeDepth + ".");
			current.drawLines_below(gl); // Recurse down through the tree.
		}
	}

	/** Draw all nodes in the subtree below this node. **/
	public void drawNodes_below(GL2 gl){
		drawPoint(gl);
		Iterator<Node> iter = children.iterator();
		while (iter.hasNext()){
			Node child = iter.next();
			child.drawPoint(gl);	
			child.drawNodes_below(gl); // Recurse down through the tree.
		}
	}

	/** Single out one run up to this node to highline the lines, while dimming the others. **/
	public void highlightSingleRunToThisNode(){
		Node rt = getRoot();
		rt.setLineBrightness_below(0.4f); // Fade the entire tree, then go and highlight the run we care about.

		Node currentNode = this;
		while (currentNode.treeDepth > 0){
			currentNode.setLineBrightness(0.85f);
			currentNode = currentNode.parent;
		}
	}

	/** Fade a single line going from this node to its parent. **/
	public void setLineBrightness(float brightness){
		lineBrightness = brightness;
	}

	/** Fade a certain part of the tree. **/
	public void setLineBrightness_below(float brightness){
		setLineBrightness(brightness);
		for (Node child : children){
			child.setLineBrightness_below(brightness);
		}
	}

	/** Reset line brightnesses to default. **/
	public void resetLineBrightness_below(){
		setLineBrightness_below(lineBrightness_default);
	}

	/** Color the node scaled by depth in the tree. Skip the brightness argument for default value. **/
	public static Color getColorFromTreeDepth(int depth, float brightness){
		float coloffset = 0.35f;
		float scaledDepth = (float)depth/(float)maxDepthYet;
		float H = scaledDepth* 0.38f+coloffset;
		float S = 0.8f;
		return Color.getHSBColor(H, S, brightness);
	}

	/** Color the node scaled by depth in the tree. Totally for gradient pleasantness. **/
	public static Color getColorFromTreeDepth(int depth){
		return getColorFromTreeDepth(depth, lineBrightness_default);
	}

	/** Set an override line color for this branch (all descendants). **/
	public void setBranchColor(Color newColor){
		overrideLineColor = newColor;
		for (Node child : children){
			child.setBranchColor(newColor);
		}
	}

	/** Clear an overriden line color on this branch. Call from root to get all line colors back to default. **/
	public void clearBranchColor(){
		overrideLineColor = null;
		for (Node child : children){
			child.clearBranchColor();
		}
	}

	/** Clear node override colors from this node onward. Only clear the specified color. Call from root to clear all. **/
	public void clearNodeOverrideColor(Color colorToClear){
		if (overrideNodeColor == colorToClear){
			overrideNodeColor = null;
			displayPoint = false;
		}
		for (Node child : children){
			child.clearNodeOverrideColor(colorToClear);
		}
	}
	
	/** Clear all node override colors from this node onward. Call from root to clear all. **/
	public void clearNodeOverrideColor(){
		if (overrideNodeColor != null){
			overrideNodeColor = null;
			displayPoint = false;
		}
		for (Node child : children){
			child.clearNodeOverrideColor();
		}
	}
	
	/** Give this branch a zOffset to make it stand out. **/
	public void setBranchZOffset(float zOffset){
		this.zOffset = zOffset;
		for (Node child : children){
			child.setBranchZOffset(zOffset);
		}
	}

	/** Give this branch a zOffset to make it stand out. Goes backwards towards root. **/
	public void setBackwardsBranchZOffset(float zOffset){
		this.zOffset = zOffset;
		Node currentNode = this;
		while (currentNode.treeDepth > 0){
			currentNode.zOffset = zOffset;
			currentNode = currentNode.parent;
		}
	}

	/** Clear z offsets in this branch. Works backwards towards root. **/
	public void clearBackwardsBranchZOffset(){
		setBackwardsBranchZOffset(0f);
	}

	/** Clear z offsets in this branch. Called from root, it resets all back to 0. **/
	public void clearBranchZOffset(){
		setBranchZOffset(0f);
	}
}
