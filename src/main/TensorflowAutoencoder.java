package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

public class TensorflowAutoencoder {

	public Session sess;
	
	public String encoderName;
	
	public TensorflowAutoencoder(String filename, String encoderName) {
		String modelDir = "./src/python/models";
		this.encoderName = encoderName;
		byte[] graphDef = null;
		try {
			graphDef = Files.readAllBytes(Paths.get(modelDir, filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Graph g = new Graph();
		g.importGraphDef(graphDef);
		sess = new Session(g); 
	}
	
	public float[] getPrediction(State state) {
		
		
		Tensor<Float> inputTensor = Tensor.create(flattenState(state), Float.class);
		Tensor<Float> result =
				sess.runner().feed("Squeeze:0", inputTensor)
				.fetch("transform_out/Add_1:0")
				.run().get(0).expect(Float.class);
		
		float[][] res = result.copyTo(new float[1][72]);
		return res[0];
	}
	
	public float[] getEncoding(State state) {
		Tensor<Float> inputTensor = Tensor.create(flattenState(state), Float.class);
		Tensor<Float> result =
				sess.runner().feed("Squeeze:0", inputTensor)
				.fetch("decoder/decoder_input:0")
				.run().get(0).expect(Float.class);
		
		float[][] res = result.copyTo(new float[1][1]);
		return res[0];
		
	}
	
	
//	public static void main(String[] args) {
//		TensorflowAutoencoder enc = new TensorflowAutoencoder();
//		
//		float[][] dummy = new float[1][72];
//		for (int i = 0; i < dummy[0].length; i++) {
//			dummy[0][i] = 0.1f;
//		}
//		
//		Tensor<Float> inputTensor = Tensor.create(dummy, Float.class);
//		long init = System.currentTimeMillis();
//		Tensor<Float> result =
//				enc.sess.runner().feed("Squeeze:0", inputTensor)
//				.fetch("transform_out/Add_1:0")
//				.run().get(0).expect(Float.class);
//		
//
//		float[][] res = result.copyTo(new float[1][72]);
//		
//		for (int i = 0; i < res[0].length; i++) {
//			System.out.println(res[0][i]);
//		}
//	}	
	
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
		
		// head
		flatState[0][6] = state.head.x - bodyX;
		flatState[0][7] = state.head.y;
		flatState[0][8] = state.head.th;
		flatState[0][9] = state.head.dx;
		flatState[0][10] = state.head.dy;
		flatState[0][11] = state.head.dth;	
		
		// rthigh
		flatState[0][12] = state.rthigh.x - bodyX;
		flatState[0][13] = state.rthigh.y;
		flatState[0][14] = state.rthigh.th;
		flatState[0][15] = state.rthigh.dx;
		flatState[0][16] = state.rthigh.dy;
		flatState[0][17] = state.rthigh.dth;
		
		// lthigh
		flatState[0][18] = state.lthigh.x - bodyX;
		flatState[0][19] = state.lthigh.y;
		flatState[0][20] = state.lthigh.th;
		flatState[0][21] = state.lthigh.dx;
		flatState[0][22] = state.lthigh.dy;
		flatState[0][23] = state.lthigh.dth;
		
		// rcalf
		flatState[0][24] = state.rcalf.x - bodyX;
		flatState[0][25] = state.rcalf.y;
		flatState[0][26] = state.rcalf.th;
		flatState[0][27] = state.rcalf.dx;
		flatState[0][28] = state.rcalf.dy;
		flatState[0][29] = state.rcalf.dth;
		
		// lcalf
		flatState[0][30] = state.lcalf.x - bodyX;
		flatState[0][31] = state.lcalf.y;
		flatState[0][32] = state.lcalf.th;
		flatState[0][33] = state.lcalf.dx;
		flatState[0][34] = state.lcalf.dy;
		flatState[0][35] = state.lcalf.dth;	
		
		// rfoot
		flatState[0][36] = state.rfoot.x - bodyX;
		flatState[0][37] = state.rfoot.y;
		flatState[0][38] = state.rfoot.th;
		flatState[0][39] = state.rfoot.dx;
		flatState[0][40] = state.rfoot.dy;
		flatState[0][41] = state.rfoot.dth;
		
		// lfoot
		flatState[0][42] = state.lfoot.x - bodyX;
		flatState[0][43] = state.lfoot.y;
		flatState[0][44] = state.lfoot.th;
		flatState[0][45] = state.lfoot.dx;
		flatState[0][46] = state.lfoot.dy;
		flatState[0][47] = state.lfoot.dth;	
		
		// ruarm
		flatState[0][48] = state.ruarm.x - bodyX;
		flatState[0][49] = state.ruarm.y;
		flatState[0][50] = state.ruarm.th;
		flatState[0][51] = state.ruarm.dx;
		flatState[0][52] = state.ruarm.dy;
		flatState[0][53] = state.ruarm.dth;
		
		// luarm
		flatState[0][54] = state.luarm.x - bodyX;
		flatState[0][55] = state.luarm.y;
		flatState[0][56] = state.luarm.th;
		flatState[0][57] = state.luarm.dx;
		flatState[0][58] = state.luarm.dy;
		flatState[0][59] = state.luarm.dth;
		// rlarm
		flatState[0][60] = state.rlarm.x - bodyX;
		flatState[0][61] = state.rlarm.y;
		flatState[0][62] = state.rlarm.th;
		flatState[0][63] = state.rlarm.dx;
		flatState[0][64] = state.rlarm.dy;
		flatState[0][65] = state.rlarm.dth;	
	
		// llarm
		flatState[0][66] = state.llarm.x - bodyX;
		flatState[0][67] = state.llarm.y;
		flatState[0][68] = state.llarm.th;
		flatState[0][69] = state.llarm.dx;
		flatState[0][70] = state.llarm.dy;
		flatState[0][71] = state.llarm.dth;	
		
		return flatState;
	
}
}
