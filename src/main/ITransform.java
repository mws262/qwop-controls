package main;

import java.util.List;

public interface ITransform {
	
	/** Update any necessary transform calculations. Not really used by all types of transforms. **/
	public void updateTransform(List<State> nodesToUpdateFrom);
	
	/** Transform one state into a potentially reduced one by means of an autoencoder, PCA, etc. **/
	public List<float[]> transform(List<State> originalStates);
	
	/** Reverse a transform, if possible. **/
	public List<float[]> untransform(List<State> transformedStates);
	
	/** Find out how much the state is reduced. How many numbers will be left after transformation? **/
	public int getOutputStateSize();
}
