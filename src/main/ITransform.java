package main;

import java.util.List;

public interface ITransform {
	
	/** Update any necessary transform calculations. Not really used by all types of transforms. **/
	public void updateTransform(List<State> nodesToUpdateFrom);
	
	/** Transform one state into a potentially reduced one by means of an autoencoder, PCA, etc. **/
	public List<float[]> transform(List<State> originalStates);
	
	/** Reverse a transform, if possible. **/
	public List<State> untransform(List<float[]> transformedStates);
	
	/** Compress and then decompress states to see what is lost in the process. **/
	public List<State> compressAndDecompress(List<State> originalStates);
	
	/** Find out how much the state is reduced. How many numbers will be left after transformation? **/
	public int getOutputStateSize();
}
