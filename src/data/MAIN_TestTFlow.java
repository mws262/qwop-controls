package data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.tensorflow.Tensor;

import org.tensorflow.Graph;
import org.tensorflow.Session;


public class MAIN_TestTFlow {


	public MAIN_TestTFlow() {

	}

	public static void main(String[] args) {

		String modelDir = "./src/python/tmp";

		byte[] graphDef = null;
		try {
			graphDef = Files.readAllBytes(Paths.get(modelDir, "frozen_model.pb"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		float[][] input = new float[1][72];
		
		for (int i = 0; i < input[0].length; i++) {
			input[0][i] = i;
		}
		Tensor<Float> inputTensor = Tensor.create(input, Float.class);
		
		float[] result = executeGraph(graphDef, inputTensor);
		
		for (int i = 0; i < result.length; i++) {
			System.out.println(result[i]);
		}
	}

	private static float[] executeGraph(byte[] graphDef, Tensor<Float> input) {
		Tensor<Float> dropVal = Tensor.create(new float[] {1.f}, Float.class);	
		try (Graph g = new Graph()) {
			g.importGraphDef(graphDef);
			//Utility.tic();
			try (Session s = new Session(g); 
					Tensor<Float> result =
							s.runner().feed("input/x-input:0", input).feed("dropout/Placeholder:0", dropVal).fetch("layer5/activation:0").run().get(0).expect(Float.class)) {
				//Utility.toc();
				return result.copyTo(new float[1][1])[0];
			}
		}
	}
}