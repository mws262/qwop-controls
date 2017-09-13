import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import javax.media.opengl.GL2;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.MassData;
import org.jbox2d.collision.shapes.PolygonDef;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.RevoluteJoint;
import org.jbox2d.dynamics.joints.RevoluteJointDef;

/*
 * This version will hopefully get rid of many legacy features.
 * Also, this should handle adding actions which are not from a 
 * predetermined set.
 * 
 */
public class TrialNodeMinimal {


	/********* QWOP IN/OUT ************/
	/** Actual numeric control action **/
	public final int controlAction; //Actual delay used as control.

	/** What is the state after taking this node's action? **/
	public CondensedStateInfo state;

	/** Keypress action. **/
	//TODO

	/********* TREE CONNECTION INFO ************/

	/** Node which leads up to this node. **/
	public final TrialNodeMinimal parent; // Parentage may not be changed.

	/** Child nodes. Not fixed size any more. **/
	public ArrayList<TrialNodeMinimal> children = new ArrayList<TrialNodeMinimal>();

	/** Untried child actions. **/
	public ArrayList<Integer> uncheckedActions = new ArrayList<Integer>();

	/** Are there any untried things below this node? **/
	public boolean fullyExplored = false;

	/** How deep is this node down the tree? 0 is root. **/
	public final int treeDepth;

	/** Maximum depth yet seen in this tree. **/
	public static int maxDepthYet = 1;

	/********* TREE VISUALIZATION ************/
	public Color lineColor = Color.BLUE; 
	public Color nodeColor = Color.GREEN;

	public boolean displayPoint = false; // Round dot at this node. Is it on?
	public boolean displayLine = true; // Line from this node to parent. Is it on?

	/********* NODE PLACEMENT ************/

	/** Parameters for visualizing the tree **/
	public float[] nodeLocation = new float[3]; // Location that this node appears on the tree visualization
	public float nodeAngle = 0; // Keep track of the angle that the line previous node to this node makes.
	public float sweepAngle = 2f*(float)Math.PI;

	public float edgeLength = 1.f;

	/********* TREE PHYSICS ************/

	/** Are the Box2D physics FOR THE TREE branches enabled? **/
	public static boolean useTreePhysics;

	/** Are the physics things fully initialized for this node. Prevents null pointers. **/
	private boolean arePhysicsInitialized = false;

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
	static final float anglePGain = 0.05f;
	static final float angleDGain = 0.1f;
	static final float angleSpeedCap = 4f;
	static final float maxTorque = 1000f;

	/** Damping general motion **/
	static final float linearDamping = 20f;
	static final float angularDamping = 20f;

	/** Repulser force gains **/
	static final float centralRepelGain = 500f;
	static final float nodeRepelGain = 2f;
	static final float ancestorMultiplier = 5f; // Ancestors of this node push harder

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
	public TrialNodeMinimal(TrialNodeMinimal parent, int controlAction) {
		this.parent = parent;
		treeDepth = parent.treeDepth + 1;
		this.controlAction = controlAction;

		// Error check for duplicate actions.
		for (TrialNodeMinimal parentChildren : parent.children){
			if (parentChildren.controlAction == controlAction){
				throw new RuntimeException("Tried to add a duplicate action node at depth " + treeDepth + ". Action was: " + controlAction + ".");
			}	
		}
		parent.children.add(this);
		if (treeDepth > maxDepthYet){
			maxDepthYet = treeDepth;
		}

		edgeLength = 4.00f * (float)Math.pow(0.6947, 0.1903 * treeDepth ) + 1.0f;
		//(float)Math.pow(0.787, 0.495) + 1.0f; // Optimized exponential in Matlab 

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
	public TrialNodeMinimal(boolean treePhysOn) {
		parent = null;
		controlAction = Integer.MIN_VALUE;
		treeDepth = 0; 
		nodeLocation[0] = 0f;
		nodeLocation[1] = 0f;
		nodeLocation[2] = 0f;

		// Initialize the tree physics world if this is enabled.
		useTreePhysics = treePhysOn;

		if (useTreePhysics){
			treePhysWorld = new World(new AABB(new Vec2(-10, -10f), new Vec2(10f,10f)), new Vec2(0f, 0f), true);

			treePhysWorld.setWarmStarting(true);
			treePhysWorld.setPositionCorrection(true);
			treePhysWorld.setContinuousPhysics(true);

			initTreePhys_single();
		}
	}

	/************************************/
	/******* ADDING TO THE TREE *********/
	/************************************/

	/** Sample random node. Either create or return an existing not fully explored child. **/
	public TrialNodeMinimal sampleNode(){

		TrialNodeMinimal sample;
		ArrayList<TrialNodeMinimal> unexploredChildren = new ArrayList<TrialNodeMinimal>();

		Iterator<TrialNodeMinimal> iter = children.iterator();
		while(iter.hasNext()){ // Check how many children are still valid choices to expand.
			TrialNodeMinimal child = iter.next();
			if (!child.fullyExplored) unexploredChildren.add(child);
		}

		System.out.println("Unexplored: " + unexploredChildren.size() + ", Unchecked: " + uncheckedActions + ", Node depth: " + treeDepth + ", Total children: " + children.size());

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
		System.out.println("Selection is: " + selection);

		// Make a new node or pick a not fully explored child.
		if (selection > unexploredChildren.size()){
			sample = new TrialNodeMinimal(this,uncheckedActions.get(selection - unexploredChildren.size() - 1));
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
	public void expandNodeChoices(int doubleSidedExpansionNumber){

		if (children.isEmpty()){
			this.fullyExplored = true;
			return; // Not well defined if there are no children to begin with. TODO fix this.
		}

		// Get all existing child actions.
		int[] existingActions = new int[children.size()];
		for (int i = 0; i < children.size(); i++){
			existingActions[i] = children.get(i).controlAction;
		}
		Arrays.sort(existingActions);

		// Check for gaps and fill them in.
		int count = 0;
		for (int i = existingActions[0]; i <= existingActions[existingActions.length - 1]; i++){	
			if (existingActions[count] == i){
				count++;
			}else{
				uncheckedActions.add(i);
			}
		}

		// Add new ones on either side of the existing set.
		for (int i = 1; i <= doubleSidedExpansionNumber; i++){
			uncheckedActions.add(existingActions[0] - i);
			uncheckedActions.add(existingActions[existingActions.length - 1] + i);
		}
	}

	/**
	 * Add nodes around some center value.
	 */
	public void expandNodeChoices(int centerValue, int doubleSidedExpansionNumber){
		for (int i = centerValue - doubleSidedExpansionNumber; i <= centerValue + doubleSidedExpansionNumber; i++){
			if (!uncheckedActions.contains(i)){
				uncheckedActions.add(i);
			}
		}
	}

	/** Expand all below this node. **/
	public void expandNodeChoices_allBelow(int doubleSidedExpansionNumber){
		expandNodeChoices(doubleSidedExpansionNumber);
		fullyExplored = false;
		for (TrialNodeMinimal child : children){
			child.expandNodeChoices_allBelow(doubleSidedExpansionNumber);
		}
	}

	/** Expand all below this node. **/
	public void expandNodeChoices_range(int doubleSidedExpansionNumber, int firstLayer, int endLayer){
		//TODO
	}

	/***********************************************/
	/******* GETTING CERTAIN SETS OF NODES *********/
	/***********************************************/

	/** Get a random child **/
	public TrialNodeMinimal getRandomChild(){
		return children.get(randInt(0,children.size()-1));
	}

	/** Add all the nodes below this one to a list. **/
	public ArrayList<TrialNodeMinimal> getNodes_below(ArrayList<TrialNodeMinimal> nodeList){

		nodeList.add(this);
		for (TrialNodeMinimal child : children){
			child.getNodes_below(nodeList);
		}	
		return nodeList;
	}

	/** Locate all the endpoints in the tree. Starts from the node it is called from. **/
	public void getLeaves(ArrayList<TrialNodeMinimal> leaves){
		for (TrialNodeMinimal child : children){
			if (child.children.isEmpty()){
				leaves.add(child);
			}else{
				child.getLeaves(leaves);
			}
		}	
	}

	/** Recount how many descendants this node has. **/
	public int countDescendants(){
		int count = 0;
		for (TrialNodeMinimal current : children){
			count++;
			count += current.countDescendants(); // Recurse down through the tree.
		}
		return count;
	}

	/** Check whether a node is an ancestor of this one. */
	public boolean isOtherNodeAncestor(TrialNodeMinimal otherNode){
		if (otherNode.treeDepth >= this.treeDepth){ // Don't need to check if this is as far down the tree.
			return false;
		}
		TrialNodeMinimal currNode = parent;

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
		for (TrialNodeMinimal child : children){
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
		ArrayList<TrialNodeMinimal> leaves = new ArrayList<TrialNodeMinimal>();
		getLeaves(leaves);
		System.out.println("Number of leaves in this tree: " + leaves.size());

		// Reset all existing exploration flags out there.
		for (TrialNodeMinimal leaf : leaves){
			TrialNodeMinimal currNode = leaf;
			while (currNode.treeDepth > 0){
				currNode.fullyExplored = false;
				currNode = currNode.parent;
			}
			currNode.fullyExplored = false;
		}

		for (TrialNodeMinimal leaf : leaves){
			leaf.checkFullyExplored_lite();
		}
	}

	/***************************************************/
	/******* STATE & SEQUENCE SETTING/GETTING **********/
	/***************************************************/

	/** Capture the state from an active game **/
	@Deprecated
	public void captureState(QWOPInterface QWOPHandler){
		state = new CondensedStateInfo(QWOPHandler.game); // MATT add 8/22/17
	}

	/** Capture the state from an active game **/
	public void captureState(QWOPGame game){
		state = new CondensedStateInfo(game); // MATT add 8/22/17
	}

	/** Assign the state directly. Usually when loading nodes. **/
	public void setState(CondensedStateInfo newState){
		state = newState;
	}

	/** Get the sequence of actions up to, and including this node **/
	public int[] getSequence(){
		int[] sequence = new int[treeDepth];
		TrialNodeMinimal current = this;
		sequence[treeDepth-1] = current.controlAction;
		for (int i = treeDepth - 2; i >= 0; i--){
			current = current.parent;
			sequence[i] = current.controlAction;
		}
		return sequence;
	}

	/****************************************/
	/******* SAVE/LOAD AND UTILITY **********/
	/****************************************/

	/* Takes a list of runs and figures out the tree hierarchy without duplicate objects. Returns the ROOT of a tree. */
	public static TrialNodeMinimal makeNodesFromRunInfo(ArrayList<CondensedRunInfo> runs, boolean initializeTreePhysics){
		TrialNodeMinimal rootNode = new TrialNodeMinimal(initializeTreePhysics);
		currentlyAddingSavedNodes = true;
		for (CondensedRunInfo run : runs){ // Go through all runs, placing them in the tree.
			TrialNodeMinimal currentNode = rootNode;	

			for (int i = 0; i < run.actions.length; i++){ // Iterate through individual actions of this run, travelling down the tree in the process.

				boolean foundExistingMatch = false;
				for (TrialNodeMinimal child: currentNode.children){ // Look at each child of the currently investigated node.
					if (child.controlAction == run.actions[i]){ // If there is already a node for the action we are trying to place, just use it.
						currentNode = child;
						foundExistingMatch = true;
						break; // We found a match, move on.
					}
				}
				// If this action is unique at this point in the tree, we need to add a new node there.
				if (!foundExistingMatch){
					TrialNodeMinimal newNode = new TrialNodeMinimal(currentNode, run.actions[i]);
					newNode.setState(run.states[i]);
					newNode.calcNodePos();
					currentNode = newNode;
				}
			}
		}
		rootNode.checkFullyExplored_complete(); // Handle marking the nodes which are fully explored.
		currentlyAddingSavedNodes = false;
		rootNode.calcNodePos_below();
		if (initializeTreePhysics) rootNode.initTreePhys_below();
		return rootNode;
	}

	/** Generate a random integer between two values, inclusive. **/
	public static int randInt(int min, int max) {
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
	public void calcNodePos(){
		//Angle of this current node -- parent node's angle - half the total sweep + some increment so that all will span the required sweep.
		if(treeDepth == 0){ //If this is the root node, we shouldn't change stuff yet.
			if (children.size() > 1 ){ //Catch the div by 0
				nodeAngle = -sweepAngle/2.f + (float)(parent.children.indexOf(this)) * sweepAngle/(float)(children.size() - 1);
			}else{
				nodeAngle = (float)Math.PI/2f;
			}
		}else{
			if (parent.children.size() > 1){ //Catch the div by 0
				sweepAngle = parent.sweepAngle/(2);
				nodeAngle = parent.nodeAngle - sweepAngle/2.f + (float)(parent.children.indexOf(this)) * sweepAngle/(float)(parent.children.size() - 1);
			}else{
				sweepAngle = parent.sweepAngle; //Only reduce the sweep angle if the parent one had more than one child.
				nodeAngle = parent.nodeAngle;
			}
		}

		nodeLocation[0] = (float) (parent.nodeLocation[0] + edgeLength*Math.cos(nodeAngle));
		nodeLocation[1] = (float) (parent.nodeLocation[1] + edgeLength*Math.sin(nodeAngle));
		nodeLocation[2] = 0f; // No out of plane stuff yet.
	}

	/** Recalculate all node positions below this one (NOT including this one for the sake of root). **/
	public void calcNodePos_below(){		
		for (TrialNodeMinimal current : children){
			current.calcNodePos();	
			current.calcNodePos_below(); // Recurse down through the tree.
		}
	}

	/** Initialize all the physics stuff for this node. **/
	public void initTreePhys_single(){

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
		for (TrialNodeMinimal child : children){
			child.initTreePhys_single();
			child.initTreePhys_below();
		}
	}

	/** Advance the tree physics, and update node positions. **/
	public void stepTreePhys(int timesteps){
		if (treeDepth != 0) throw new RuntimeException("Tree physics updates are only supported from root for now.");

		ArrayList<TrialNodeMinimal> nodeList = new ArrayList<TrialNodeMinimal>();
		getNodes_below(nodeList);

		for (int i = 0; i < timesteps; i++){
			// Calculate added forces before stepping the world.
			applyForce(nodeList);
			treePhysWorld.step(physDt, physIterations);
			updatePositionFromPhys();
		}
	}

	public void applyForce(ArrayList<TrialNodeMinimal> nodeList){
		if (arePhysicsInitialized){
			for (TrialNodeMinimal thisNode : nodeList){
				if (thisNode.treeDepth == 0 || !thisNode.arePhysicsInitialized) continue; // Skip forces on root.
				localForce.setZero();

				// Repulser from the specified origin.
				Vec2 o_pt = thisNode.physBody.getPosition().sub(repulserPoint); // Repulser point to this node.
				//System.out.println(thisNode.physBody.getPosition().x + "," + thisNode.physBody.getPosition().y);
				float lengthSq = Math.max(o_pt.lengthSquared(), 0.1f); // Set minimum distance to prevent div0 errors
				Vec2 repulserForce = o_pt.mul(centralRepelGain * thisNode.physMass/(lengthSq)); // Only inverse distance law right now.
				localForce.addLocal(repulserForce);

				// Repulser from EVERYONE
				for (TrialNodeMinimal otherNode : nodeList){
					if (!otherNode.equals(thisNode) && otherNode.arePhysicsInitialized){
						o_pt = thisNode.physBody.getPosition().sub(otherNode.physBody.getPosition()); // Repulser point to this node.
						lengthSq = Math.max(o_pt.lengthSquared(), 0.1f); // Set minimum distance to prevent div0 errors

						// Ancestor nodes have a bigger repulsive effect than others.
						if (thisNode.isOtherNodeAncestor(otherNode)){
							repulserForce = o_pt.mul((ancestorMultiplier * nodeRepelGain * thisNode.physMass * otherNode.physMass)/(lengthSq*lengthSq)); // Only inverse distance law right now.
						}else{
							repulserForce = o_pt.mul((nodeRepelGain * thisNode.physMass * otherNode.physMass)/(lengthSq*lengthSq)); // Only inverse distance law right now.
						}

						localForce.addLocal(repulserForce);
					}
				}
				thisNode.physBody.applyForce(localForce, thisNode.physBody.getWorldCenter());

				// Pseudo-spring in the joint
				float motorSpeed = Math.min((float)thisNode.countDescendants() * anglePGain, angleSpeedCap) * (-thisNode.parent.physBody.getAngle() + thisNode.physBody.getAngle() - angleDGain * thisNode.jointToParent.getJointSpeed());
				thisNode.jointToParent.setMotorSpeed(motorSpeed);
			}
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
			for (TrialNodeMinimal child : children){		
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
			gl.glColor3fv(getColorFromTreeDepth(treeDepth).getColorComponents(null),0);
			gl.glVertex3d(parent.nodeLocation[0], parent.nodeLocation[1], parent.nodeLocation[2]);
			gl.glVertex3d(nodeLocation[0], nodeLocation[1], nodeLocation[2]);
		}
	}

	/** Draw the node point if enabled **/
	public void drawPoint(GL2 gl){
		if(fullyExplored){//if (displayPoint){
			gl.glColor3fv(nodeColor.getColorComponents(null),0);
			gl.glVertex3d(nodeLocation[0], nodeLocation[1], nodeLocation[2]);
		}
	}

	/** Draw all lines in the subtree below this node **/
	public void drawLines_below(GL2 gl){	
		Iterator<TrialNodeMinimal> iter = children.iterator();
		while (iter.hasNext()){
			TrialNodeMinimal current = iter.next();
			current.drawLine(gl);
			if (current.treeDepth <= this.treeDepth) throw new RuntimeException("Node hierarchy problem. Node with an equal or lesser depth is below another. At " + current.treeDepth + " and " + this.treeDepth + ".");
			current.drawLines_below(gl); // Recurse down through the tree.
		}
	}

	/** Draw all nodes in the subtree below this node. **/
	public void drawNodes_below(GL2 gl){
		drawPoint(gl);
		Iterator<TrialNodeMinimal> iter = children.iterator();
		while (iter.hasNext()){
			TrialNodeMinimal child = iter.next();
			child.drawPoint(gl);	
			child.drawNodes_below(gl); // Recurse down through the tree.
		}
	}

	/** Color the node scaled by depth in the tree. Totally for gradient pleasantness. **/
	public static Color getColorFromTreeDepth(int depth)
	{
		float coloffset = 0.35f;
		float scaledDepth = (float)depth/(float)maxDepthYet;
		float H = scaledDepth* 0.38f+coloffset;
		float S = 0.8f;
		float B = 0.85f;

		return Color.getHSBColor(H, S, B);
	}
}
