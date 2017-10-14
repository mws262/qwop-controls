package main;
import java.util.ArrayList;


public class MAIN_test {

	private static long ticTime;
	private static long tocTime;
	
	private static final boolean useTreePhysics = false;
	
	public MAIN_test() {}

	public static void main(String[] args) {
		
		/* Load */
//		System.out.println("Reading saved games file."); tic();
		QWOP_fileIO<CondensedRunInfo> io = new QWOP_fileIO<CondensedRunInfo>();
//		ArrayList<CondensedRunInfo> runs1 = io.loadObjects("test");
//		//ArrayList<CondensedRunInfo> runs2 = io.loadObjects("test2");
//		toc();
//		
//		/* Convert from runs to tree with nodes */
//		System.out.println("Converting from games to a tree."); tic();
//		TrialNodeMinimal treeRoot = TrialNodeMinimal.makeNodesFromRunInfo(runs1, useTreePhysics);
//		//TrialNodeMinimal.makeNodesFromRunInfo(runs2, treeRoot);
//
//		toc();
		//System.out.println("Done. Imported " + runs1.size() + runs2.size() + " runs. Starting graphics and tree builder.");
		
		/* Add more choices for the tree to explore, and see if this changes anything. */
//		treeRoot.expandNodeChoices_allBelow(2);
//		treeRoot.checkFullyExplored_complete();
		
		
		TrialNodeMinimal treeRoot = new TrialNodeMinimal(useTreePhysics);
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
