package transformations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import game.State;
import main.ITransform;

public class Transform_Autoencoder implements ITransform{

	private Session sess;

	private final int outputDim;

	private float[][] flatSt = new float[1][72];

	public Transform_Autoencoder(String filename, int outputDim) {
		this.outputDim = outputDim;
		String modelDir = "./src/python/models";
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

	@Override
	public void updateTransform(List<State> nodesToUpdateFrom) {} // N/A

	@Override
	public List<float[]> transform(List<State> originalStates) {
		List<float[]> transformedStates = new ArrayList<float[]>();

		for (State st : originalStates) {
			flatSt[0] = st.flattenState();
			Tensor<Float> inputTensor = Tensor.create(flatSt, Float.class);
			Tensor<Float> result =
					sess.runner().feed("Squeeze:0", inputTensor)
					.fetch("decoder/decoder_input:0")
					.run().get(0).expect(Float.class);

			float[][] res = result.copyTo(new float[1][1]);
			transformedStates.add(res[0]);
		}
		return transformedStates;
	}

	@Override
	public List<State> untransform(List<float[]> transformedStates) {
		return null; // TODO figure out how to get the correct layers in/out for this.
	}


	@Override
	public List<State> compressAndDecompress(List<State> originalStates) {
		List<State> transformedStates = new ArrayList<State>();

		for (State st : originalStates) {
			flatSt[0] = st.flattenState();
			Tensor<Float> inputTensor = Tensor.create(flatSt, Float.class);
			Tensor<Float> result =
					sess.runner().feed("Squeeze:0", inputTensor)
					.fetch("transform_out/Add_1:0")
					.run().get(0).expect(Float.class);

			float[][] res = result.copyTo(new float[1][72]);
			transformedStates.add(new State(res[0]));
		}
		return transformedStates;
	}

	@Override
	public int getOutputStateSize() {
		return outputDim;
	}	

	@Override
	public String getName() {
		return "AutoEnc " + getOutputStateSize();
	}

	// Example usage:
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
}
