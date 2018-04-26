package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import game.State;

/**
 * Basic utilities and loaders for getting tensorflow models
 * in here and working.
 * 
 * @author matt
 *
 */
public abstract class TensorflowLoader {

	/** Current tflow session. **/
	private Session session;
	
	/** Computational graph loaded from tflow saves. **/
	private Graph graph;
	
	private List<Float> predictionList = new ArrayList<Float>();

	public TensorflowLoader(String pbFile, String directory) {
		loadGraph(pbFile, directory);
	}
	
	/** Load the computational graph from a .pb file and also make a new session. **/
	private void loadGraph(String pbFile, String directory) {
		byte[] graphDef = null;
		try {
			graphDef = Files.readAllBytes(Paths.get(directory, pbFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		graph = new Graph();
		graph.importGraphDef(graphDef);
		session = new Session(graph); 
	}
	
	/** Simple prediction from the model where we give a state and expect a list of floats out.
	 *  Only applies when a single output is required from the graph.
	 * @param state
	 * @param inputName
	 * @param outputName
	 * @return
	 */
	public List<Float> sisoFloatPrediction(State state, String inputName, String outputName) {
		Tensor<Float> inputTensor = Tensor.create(flattenState(state), Float.class);
		Tensor<Float> result =
				session.runner().feed(inputName + ":0", inputTensor)
				.fetch(outputName + ":0")
				.run().get(0).expect(Float.class);
		long[] outputShape = result.shape();

		float[] res = result.copyTo(new float[(int) outputShape[0]][(int) outputShape[1]])[0];
		
		predictionList.clear();
		for (float f : res) {
			predictionList.add(f);
		}
		return predictionList;	
	}
	
	/** Print out all operations in the graph for identifying which ones we want. **/
	private void printOperations() {
		Iterator<Operation> iter = graph.operations();
		while (iter.hasNext()) {
			System.out.println(iter.next().name());
		}
	}
	
	/** Get the current tflow session. **/
	public Session getSession() {
		return session;
	}
	
	/** Get the tflow computationa graph (ie model). **/
	public Graph getGraph() {
		return graph;
	}
	
	/** Make a state object into an array the way TFlow likes it. **/
	public static float[][] flattenState(State state) {
		float[][] flatState = new float[1][72];
		float bodyX = state.body.x;
		
		// Body
		flatState[0][0] = 0;
		flatState[0][1] = state.body.y;
		flatState[0][2] = state.body.th;
		flatState[0][3] = state.body.dx;
		flatState[0][4] = state.body.dy;
		flatState[0][5] = state.body.dth;
		
		// rthigh
		flatState[0][6] = state.rthigh.x - bodyX;
		flatState[0][7] = state.rthigh.y;
		flatState[0][8] = state.rthigh.th;
		flatState[0][9] = state.rthigh.dx;
		flatState[0][10] = state.rthigh.dy;
		flatState[0][11] = state.rthigh.dth;
		
		// rcalf
		flatState[0][12] = state.rcalf.x - bodyX;
		flatState[0][13] = state.rcalf.y;
		flatState[0][14] = state.rcalf.th;
		flatState[0][15] = state.rcalf.dx;
		flatState[0][16] = state.rcalf.dy;
		flatState[0][17] = state.rcalf.dth;
		
		// rfoot
		flatState[0][18] = state.rfoot.x - bodyX;
		flatState[0][19] = state.rfoot.y;
		flatState[0][20] = state.rfoot.th;
		flatState[0][21] = state.rfoot.dx;
		flatState[0][22] = state.rfoot.dy;
		flatState[0][23] = state.rfoot.dth;
		
		// lthigh
		flatState[0][24] = state.lthigh.x - bodyX;
		flatState[0][25] = state.lthigh.y;
		flatState[0][26] = state.lthigh.th;
		flatState[0][27] = state.lthigh.dx;
		flatState[0][28] = state.lthigh.dy;
		flatState[0][29] = state.lthigh.dth;
		
		// lcalf
		flatState[0][30] = state.lcalf.x - bodyX;
		flatState[0][31] = state.lcalf.y;
		flatState[0][32] = state.lcalf.th;
		flatState[0][33] = state.lcalf.dx;
		flatState[0][34] = state.lcalf.dy;
		flatState[0][35] = state.lcalf.dth;	
		
		// lfoot
		flatState[0][36] = state.lfoot.x - bodyX;
		flatState[0][37] = state.lfoot.y;
		flatState[0][38] = state.lfoot.th;
		flatState[0][39] = state.lfoot.dx;
		flatState[0][40] = state.lfoot.dy;
		flatState[0][41] = state.lfoot.dth;	
		
		// ruarm
		flatState[0][42] = state.ruarm.x - bodyX;
		flatState[0][43] = state.ruarm.y;
		flatState[0][44] = state.ruarm.th;
		flatState[0][45] = state.ruarm.dx;
		flatState[0][46] = state.ruarm.dy;
		flatState[0][47] = state.ruarm.dth;
		
		// rlarm
		flatState[0][48] = state.rlarm.x - bodyX;
		flatState[0][49] = state.rlarm.y;
		flatState[0][50] = state.rlarm.th;
		flatState[0][51] = state.rlarm.dx;
		flatState[0][52] = state.rlarm.dy;
		flatState[0][53] = state.rlarm.dth;	
		
		// luarm
		flatState[0][54] = state.luarm.x - bodyX;
		flatState[0][55] = state.luarm.y;
		flatState[0][56] = state.luarm.th;
		flatState[0][57] = state.luarm.dx;
		flatState[0][58] = state.luarm.dy;
		flatState[0][59] = state.luarm.dth;
		
		// llarm
		flatState[0][60] = state.llarm.x - bodyX;
		flatState[0][61] = state.llarm.y;
		flatState[0][62] = state.llarm.th;
		flatState[0][63] = state.llarm.dx;
		flatState[0][64] = state.llarm.dy;
		flatState[0][65] = state.llarm.dth;	
		
		// head
		flatState[0][66] = state.head.x - bodyX;
		flatState[0][67] = state.head.y;
		flatState[0][68] = state.head.th;
		flatState[0][69] = state.head.dx;
		flatState[0][70] = state.head.dy;
		flatState[0][71] = state.head.dth;	
		
		return flatState;
	}
}
