package main;

import java.util.List;

import game.State;

public interface ITransform {

    /**
     * Update any necessary transform calculations. Not really used by all types of transforms.
     **/
    void updateTransform(List<State> nodesToUpdateFrom);

    /**
     * Transform one state into a potentially reduced one by means of an autoencoder, PCA, etc.
     **/
    List<float[]> transform(List<State> originalStates);

    /**
     * Reverse a transform, if possible.
     **/
    List<State> untransform(List<float[]> transformedStates);

    /**
     * Compress and then decompress states to see what is lost in the process.
     **/
    List<State> compressAndDecompress(List<State> originalStates);

    /**
     * Find out how much the state is reduced. How many numbers will be left after transformation?
     **/
    int getOutputStateSize();

    /**
     * Get the name of this transform.
     **/
    String getName();
}
