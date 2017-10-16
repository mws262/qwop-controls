package main;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MAIN_test {

	private static long ticTime;
	private static long tocTime;
	
	private static final boolean useTreePhysics = false;
	
	public MAIN_test() {}

	public static void main(String[] args) {
	
		
		/******* Decide the rules for picking child actions to test. ********/
		
		// Repeated sequence of key presses.
		boolean[][] keySequence = new boolean[][]{{false,false,false,false},
												  {false,true,true,false},
												  {false,false,false,false},
												  {true,false,false,true}};
												  
		// Selection of delays for each key press combo. 
		Integer[][] actionRepeats = new Integer[][] {{3,4,5,6,7,8,9,10},
													 {29,30,31,32,33,34,35,36,37,38,39,40,41,42},
												     {3,4,5,6,7,8,9,10},
												     {29,30,31,32,33,34,35,36,37,38,39,40,41,42}};
												     
		// Exceptions to the actionRepeats above. Mainly for the first few actions.   
		Map<Integer,Integer[]> actionExceptions = new HashMap<Integer,Integer[]>();
		//actionExceptions.put(key, value)
		
		IActionGenerator actionGenerator = new ActionGenerator_FixedSequence(keySequence, actionRepeats, actionExceptions);
		Node.potentialActionGenerator = actionGenerator;
		
		
		
		// TODO Temp removed the imported games.
//		/* Load */
//		System.out.println("Reading saved games file."); tic();
		SaveableFileIO<SaveableSingleGame> io = new SaveableFileIO<SaveableSingleGame>();
//		ArrayList<SaveableSingleGame> runs1 = io.loadObjects("test");
//		//ArrayList<CondensedRunInfo> runs2 = io.loadObjects("test2");
//		toc();
//		
//		/* Convert from runs to tree with nodes */
//		System.out.println("Converting from games to a tree."); tic();
//		Node treeRoot = Node.makeNodesFromRunInfo(runs1, useTreePhysics);
//		//TrialNodeMinimal.makeNodesFromRunInfo(runs2, treeRoot);
//
//		toc();
//		//System.out.println("Done. Imported " + runs1.size() + runs2.size() + " runs. Starting graphics and tree builder.");
//		
//		/* Add more choices for the tree to explore, and see if this changes anything. */
//		treeRoot.expandNodeChoices_allBelow(2);
//		treeRoot.checkFullyExplored_complete();
		
		
		Node treeRoot = new Node(useTreePhysics);
		/* Start tree processes */
		FSM_UI ui = new FSM_UI();
		FSM_Tree tree = new FSM_Tree();
		FSM_Game game = new FSM_Game();
		
		/* Manage the tree, UI, and game. Start some threads. */
		Negotiator negotiator = new Negotiator(tree,ui,game,io,"test3",false);
		
		tree.setNegotiator(negotiator);
		ui.setNegotiator(negotiator);
		game.setNegotiator(negotiator);
		negotiator.addTreeRoot(treeRoot);
		
		Thread treeThread = new Thread(tree);
		Thread uiThread = new Thread(ui);
		Thread gameThread = new Thread(game); 
		//uiThread.setPriority(Thread.MAX_PRIORITY);
		
		gameThread.start();
		uiThread.start();
		treeThread.start();
		
		System.out.println("All initialized.");
	}
	
	/** Matlab tic and toc functionality. **/
	public static void tic(){
		ticTime = System.currentTimeMillis();
	}
	public static long toc(){
		tocTime = System.currentTimeMillis();
		long difference = tocTime - ticTime;
		if (difference < 1000){
			System.out.println(difference + " ms elapsed.");
		}else{
			System.out.println(Math.floor(difference/10.)/100. + " s elapsed.");
		}
		return difference;
	}
}
