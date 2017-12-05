package data;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.tensorflow.Tensor;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Output;
import org.tensorflow.DataType;


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
		
		for (int i = 0; i < input.length; i++) {
			input[0][i] = (float)i;
		}
		Tensor<Float> inputTensor = Tensor.create(input, Float.class);
		
		float[] result = executeGraph(graphDef, inputTensor);
		
		for (int i = 0; i < result.length; i++) {
			System.out.println(result[i]);
		}

	}

	private static float[] executeGraph(byte[] graphDef, Tensor<Float> input) {
		Tensor<Float> dropVal = Tensor.create(new float[] {1}, Float.class);	
		try (Graph g = new Graph()) {
			g.importGraphDef(graphDef);
			try (Session s = new Session(g); 
					Tensor<Float> result =
							s.runner().feed("input/x-input:0", input).feed("dropout/Placeholder:0", dropVal).fetch("layer5/activation:0").run().get(0).expect(Float.class)) {
				final long[] rshape = result.shape();
				if (result.numDimensions() != 2 || rshape[0] != 1) {
					throw new RuntimeException(
							String.format(
									"Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
									Arrays.toString(rshape)));
				}
				int nlabels = (int) rshape[1];
				return result.copyTo(new float[1][nlabels])[0];
			}
		}
	}
}